package com.example.Mobile_FaceRecognition.presentation.viewmodel

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.Mobile_FaceRecognition.DependencyInjector.AppModule
import com.example.Mobile_FaceRecognition.data.repository.FaceRecognitionEvent
import com.example.Mobile_FaceRecognition.data.repository.FaceRecognitionRepositoryImpl
import com.example.Mobile_FaceRecognition.domain.model.FaceRecognition
import com.example.Mobile_FaceRecognition.domain.usecase.RecognizeFaceUseCase
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import com.example.Mobile_FaceRecognition.domain.usecase.RegisterFaceUseCase
import android.hardware.camera2.CameraCharacteristics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class MainViewModel(
    private val recognizeFaceUseCase: RecognizeFaceUseCase,
    private val registerFaceUseCase: RegisterFaceUseCase,
    private var faceDetector: FaceDetector,
    private val faceRecognitionRepository: FaceRecognitionRepositoryImpl,
    private val CROP_SIZE: Int
) : ViewModel() {

    private val _mappedRecognitions = MutableLiveData<List<FaceRecognition>>(emptyList())
    val mappedRecognitions: LiveData<List<FaceRecognition>> = _mappedRecognitions

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    private var recognizedLabel: String? = null
    private var recognizedDistance: Double = 0.0
    private var lastRecognitionTime: Long = 0
    private var clearJob: Job? = null
    var isRegisteringFace: Boolean = false

    init {
        viewModelScope.launch {
            faceRecognitionRepository.recognitionResult.collect { event ->
                when (event) {
                    is FaceRecognitionEvent.RecognitionSuccess -> {
                        val match = event.match
                        val name = event.name
                        val distance = event.distance
                        recognizedLabel = name
                        recognizedDistance = distance
                        lastRecognitionTime = System.currentTimeMillis()
                        val message = if (match) {
                            "Server recognized: $recognizedLabel (Distance: ${String.format("%.2f", recognizedDistance)})"
                        } else {
                            "Face not recognized by server."
                        }
                        _uiEvent.emit(UiEvent.ShowToast(message))
                    }
                    is FaceRecognitionEvent.Message -> {
                        _uiEvent.emit(UiEvent.ShowToast(event.message))
                    }
                    is FaceRecognitionEvent.Error -> {
                        _uiEvent.emit(UiEvent.ShowToast(event.errorMessage))
                    }
                }
            }
        }
    }

    fun processImageForDetectionAndRecognition(
        croppedBitmap: Bitmap,
        previewWidth: Int,
        previewHeight: Int,
        cropToFrameTransform: Matrix,
        isFrontCamera: Boolean
    ) {
        viewModelScope.launch {
            val image = InputImage.fromBitmap(croppedBitmap, 0)

            faceDetector.process(image)
                .addOnSuccessListener { faces ->
                    viewModelScope.launch(Dispatchers.Default) {
                        if (faces.isEmpty()) {
                            Log.d("DBG", "Faces=0, schedule clear recognitions")
                            clearJob?.cancel()
                            clearJob = launch {
                                delay(500)
                                if (_mappedRecognitions.value?.isEmpty() != false) {
                                    _mappedRecognitions.postValue(emptyList())
                                    Log.d("DBG", "Clear recognitions executed after delay")
                                }
                            }
                            return@launch
                        }

                        clearJob?.cancel()

                        val newRecognitions = mutableListOf<FaceRecognition>()
                        for (face in faces) {
                            val bounds = face.boundingBox
                            val croppedFace = cropFaceBitmap(croppedBitmap, bounds) ?: continue

                            val result = recognizeFaceUseCase(croppedFace, isRegisteringFace) ?: continue

                            if (isRegisteringFace) {
                                _uiEvent.emit(UiEvent.ShowRegistrationDialog(croppedFace, result))
                                isRegisteringFace = false
                                continue          // skip tracker saat registrasi
                            }

                            // hitung label & confidence
                            var title = "Unknown"
                            var confidence = 0f
                            if (result.distance != null && result.distance < 0.75f) {
                                title = result.title
                                confidence = result.distance
                            }

                            // mirror untuk kamera depan
                            val location = RectF(bounds)
                            if (isFrontCamera) {
                                val tmp = location.left
                                location.left  = CROP_SIZE - location.right
                                location.right = CROP_SIZE - tmp
                            }

                            cropToFrameTransform.mapRect(location)

                            newRecognitions.add(
                                FaceRecognition(
                                    id        = face.trackingId?.toString() ?: "N/A",
                                    title     = title,
                                    distance  = confidence,
                                    location  = location,
                                    embedding = result.embedding,
                                    crop      = result.crop
                                )
                            )
                        }

                        // 3. kirim ke LiveData (bisa kosong jika tak ada wajah valid)
                        _mappedRecognitions.postValue(newRecognitions)
                        Log.d("DBG", "Faces=${faces.size}, recognitions=${newRecognitions.size}")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("MainViewModel", "Face detection failed: ${e.message}")
                    viewModelScope.launch {
                        _uiEvent.emit(UiEvent.ShowToast("Face detection failed: ${e.message}"))
                    }
                }

        }
    }

    fun registerFace(name: String, embedding: FloatArray) {
        viewModelScope.launch {
            val success = registerFaceUseCase(name, embedding)
            if (success) {
                // Local registration in TFLite model, if still needed after server registration
                // tfliteSource.register(name, FaceRecognition(title = name, embedding = embedding))
                _uiEvent.emit(UiEvent.ShowToast("Face Registered Successfully"))
            } else {
                _uiEvent.emit(UiEvent.ShowToast("Face Registration Failed"))
            }
        }
    }

    fun updateDetector(newDetector: FaceDetector) {
        faceDetector.close()
        faceDetector = newDetector
    }

    private fun cropFaceBitmap(input: Bitmap, bounds: Rect): Bitmap? {
        // Ensure bounds are within bitmap dimensions
        val safeBounds = Rect(bounds)
        safeBounds.left = safeBounds.left.coerceAtLeast(0)
        safeBounds.top = safeBounds.top.coerceAtLeast(0)
        safeBounds.right = safeBounds.right.coerceAtMost(input.width)
        safeBounds.bottom = safeBounds.bottom.coerceAtMost(input.height)

        if (safeBounds.width() <= 0 || safeBounds.height() <= 0) {
            Log.e("MainViewModel", "Invalid crop bounds: $safeBounds")
            return null
        }

        return try {
            Bitmap.createBitmap(
                input,
                safeBounds.left,
                safeBounds.top,
                safeBounds.width(),
                safeBounds.height()
            )
        } catch (e: IllegalArgumentException) {
            Log.e("MainViewModel", "Error creating cropped bitmap: ${e.message}")
            null
        }
    }


    override fun onCleared() {
        super.onCleared()
        faceDetector.close()
        AppModule.clearFaceDetector()
        faceRecognitionRepository.close()
    }
}

sealed class UiEvent {
    object Idle : UiEvent()
    data class ShowToast(val message: String) : UiEvent()
    data class ShowRegistrationDialog(val croppedFace: Bitmap, val recognition: FaceRecognition) : UiEvent()
}