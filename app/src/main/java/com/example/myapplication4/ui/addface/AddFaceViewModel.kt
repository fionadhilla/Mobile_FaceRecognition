package com.example.myapplication4.ui.addface

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication4.domain.usecase.RegisterUserWithFaceUseCase
import com.example.myapplication4.face.FaceEmbedder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageProxy
import androidx.compose.runtime.mutableStateOf
import com.example.myapplication4.data.api.ApiResult
import com.example.myapplication4.data.model.User
import com.example.myapplication4.face.AddFaceDetector
import com.example.myapplication4.domain.utils.MediaPipeUtils.toBitmap
import com.example.myapplication4.domain.utils.MediaPipeUtils.cropBitmap
import com.example.myapplication4.domain.utils.MediaPipeUtils.resizeBitmap
import com.google.mediapipe.tasks.vision.facedetector.FaceDetectorResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers

@HiltViewModel
class AddFaceViewModel @Inject constructor(
    private val registerUserWithFaceUseCase: RegisterUserWithFaceUseCase,
    private val faceEmbedder: FaceEmbedder,
    private val faceDetector: AddFaceDetector
) : ViewModel() {

    val name = mutableStateOf("")
    val email = mutableStateOf("")
    val phone = mutableStateOf("")

    private val _lensFacing = MutableStateFlow(CameraSelector.LENS_FACING_FRONT)
    val lensFacing: StateFlow<Int> = _lensFacing

    private val _recordingState = MutableStateFlow(RecordingState.IDLE)
    val recordingState: StateFlow<RecordingState> = _recordingState

    private val _recordingProgress = MutableStateFlow(0f)
    val recordingProgress: StateFlow<Float> = _recordingProgress

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private val capturedFrames = mutableListOf<Bitmap>()
    private var recordingJob: Job? = null
    private var lastFrameCaptureTime: Long = 0L

    companion object {
        private const val RECORD_DURATION_MILLIS = 5000L // 5 detik
        private const val FRAME_CAPTURE_INTERVAL_MILLIS = 500L // Ambil frame tiap 500ms
        private const val TARGET_FACE_SIZE = 160 // Ukuran target untuk FaceNet
        private const val FACE_EXPANSION_FACTOR = 0.15f // Ekspansi bounding box saat crop
    }

    fun startRecording(userName: String, userEmail: String, userPhone: String) {
        if (_recordingState.value != RecordingState.IDLE) return

        name.value = userName
        email.value = userEmail
        phone.value = userPhone

        _recordingState.value = RecordingState.RECORDING
        _recordingProgress.value = 0f
        capturedFrames.clear()
        _message.value = "Merekam wajah, mohon tetap di tengah..."
        lastFrameCaptureTime = System.currentTimeMillis()

        recordingJob = viewModelScope.launch(Dispatchers.Default) {
            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < RECORD_DURATION_MILLIS) {
                _recordingProgress.value = ((System.currentTimeMillis() - startTime).toFloat() / RECORD_DURATION_MILLIS)
                delay(100) // Update progress secara berkala
            }
            _recordingState.value = RecordingState.PROCESSING
            _message.value = "Memproses frame dan membuat embedding..."
            processBufferedFrames()
        }
    }

    fun processFrameForRecording(imageProxy: ImageProxy) {
        if (_recordingState.value != RecordingState.RECORDING) {
            imageProxy.close()
            return
        }

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastFrameCaptureTime >= FRAME_CAPTURE_INTERVAL_MILLIS) {
            lastFrameCaptureTime = currentTime
            viewModelScope.launch(Dispatchers.Default) {
                try {
                    val bitmap = imageProxy.toBitmap()
                    capturedFrames.add(bitmap)
                    Log.d("AddFaceViewModel", "Frame captured. Total: ${capturedFrames.size}")
                } catch (e: Exception) {
                    Log.e("AddFaceViewModel", "Error capturing frame: ${e.message}", e)
                } finally {
                    imageProxy.close()
                }
            }
        } else {
            imageProxy.close()
        }
    }

    private suspend fun processBufferedFrames() {
        if (capturedFrames.isEmpty()) {
            _message.value = "Tidak ada frame yang diambil. Coba lagi."
            _recordingState.value = RecordingState.IDLE
            return
        }

        val allEmbeddings = mutableListOf<FloatArray>()
        var facesDetected = 0

        withContext(Dispatchers.Default) {
            for ((index, bitmap) in capturedFrames.withIndex()) {
                _message.value = "Memproses frame ${index + 1}/${capturedFrames.size}..."
                val detectionResult: FaceDetectorResult? = faceDetector.detect(bitmap)

                detectionResult?.detections()?.firstOrNull()?.let { detection ->
                    facesDetected++
                    val box = detection.boundingBox()

                    val expandedWidth = box.width() * FACE_EXPANSION_FACTOR
                    val expandedHeight = box.height() * FACE_EXPANSION_FACTOR

                    val left = (box.left - expandedWidth / 2).roundToInt()
                    val top = (box.top - expandedHeight / 2).roundToInt()
                    val width = (box.width() + expandedWidth).roundToInt()
                    val height = (box.height() + expandedHeight).roundToInt()

                    val cropLeft = left.coerceIn(0, bitmap.width - 1)
                    val cropTop = top.coerceIn(0, bitmap.height - 1)
                    val cropRight = (left + width).coerceIn(0, bitmap.width)
                    val cropBottom = (top + height).coerceIn(0, bitmap.height)

                    val actualCropWidth = cropRight - cropLeft
                    val actualCropHeight = cropBottom - cropTop

                    if (actualCropWidth > 0 && actualCropHeight > 0) {
                        val croppedFace = bitmap.cropBitmap(
                            RectF(
                                cropLeft.toFloat(), cropTop.toFloat(),
                                cropRight.toFloat(), cropBottom.toFloat()
                            )
                        )
                        val resizedFace = croppedFace.resizeBitmap(TARGET_FACE_SIZE, TARGET_FACE_SIZE)

                        val embedding = faceEmbedder.getEmbeddings(resizedFace)
                        if (embedding != null) {
                            allEmbeddings.add(embedding)
                        }
                        croppedFace.recycle() // Recycle cropped bitmap
                        resizedFace.recycle() // Recycle resized bitmap
                    }
                }
                bitmap.recycle() // Buang bitmap setelah diproses
            }
            capturedFrames.clear() // Bersihkan buffer
        }

        if (allEmbeddings.isNotEmpty()) {
            val averagedEmbedding = averageEmbeddings(allEmbeddings)
            val user = User(
                name = name.value,
                email = email.value,
                phone = phone.value,
                embeddings = averagedEmbedding
            )

            when (val result = registerUserWithFaceUseCase(user.name, user.email, user.phone, user.embeddings)) {
                is ApiResult.Success -> {
                    if (result.data) {
                        _message.value = "Wajah berhasil disimpan!"
                        _recordingState.value = RecordingState.DONE
                    } else {
                        _message.value = "Gagal menyimpan data wajah di server."
                        _recordingState.value = RecordingState.IDLE
                    }
                }
                is ApiResult.Error -> {
                    _message.value = "Error: ${result.message}"
                    Log.e("AddFace", "Error saving face data: ${result.message}", result.exception)
                    _recordingState.value = RecordingState.IDLE
                }
                ApiResult.Loading -> { } // Should not happen here
            }
        } else {
            _message.value = "Tidak ada wajah yang terdeteksi di frame yang diambil. Terdeteksi ${facesDetected} wajah. Coba lagi."
            _recordingState.value = RecordingState.IDLE
        }
    }

    private fun averageEmbeddings(embeddings: List<FloatArray>): FloatArray {
        if (embeddings.isEmpty()) return floatArrayOf()
        val embeddingSize = embeddings[0].size
        val sumEmbedding = FloatArray(embeddingSize) { 0f }

        for (embedding in embeddings) {
            for (i in 0 until embeddingSize) {
                sumEmbedding[i] += embedding[i]
            }
        }

        return FloatArray(embeddingSize) { i -> sumEmbedding[i] / embeddings.size }
    }

    fun resetState() {
        recordingJob?.cancel()
        capturedFrames.forEach { it.recycle() }
        capturedFrames.clear()
        _recordingState.value = RecordingState.IDLE
        _recordingProgress.value = 0f
        _message.value = null
        name.value = ""
        email.value = ""
        phone.value = ""
    }

    override fun onCleared() {
        super.onCleared()
        recordingJob?.cancel()
        capturedFrames.forEach { it.recycle() }
        capturedFrames.clear()
        faceDetector.close()
        faceEmbedder.close()
    }
}

enum class RecordingState {
    IDLE, RECORDING, PROCESSING, DONE
}