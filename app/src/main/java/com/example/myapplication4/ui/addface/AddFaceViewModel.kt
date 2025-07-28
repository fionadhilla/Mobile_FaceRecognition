// In app/src/main/java/com/example/myapplication4/ui/addface/AddFaceViewModel.kt
package com.example.myapplication4.ui.addface

import android.app.Application
import android.graphics.Bitmap
import android.graphics.RectF
import androidx.lifecycle.AndroidViewModel
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
import com.example.myapplication4.domain.utils.MediaPipeUtils.resizeBitmap
// import com.google.mediapipe.tasks.vision.facedetector.FaceDetectorResult // Ini tidak lagi diperlukan secara langsung untuk liveDetectionResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import android.graphics.Matrix
import com.example.myapplication4.domain.utils.ImageCropper
import com.example.myapplication4.face.MediaPipeFaceDetector


@HiltViewModel
class AddFaceViewModel @Inject constructor(
    application: Application,
    private val registerUserWithFaceUseCase: RegisterUserWithFaceUseCase,
    private val faceEmbedder: FaceEmbedder,
    private val addFaceDetector: AddFaceDetector
) : AndroidViewModel(application) {

    val name = mutableStateOf("")
    val email = mutableStateOf("")
    val phone = mutableStateOf("")

    private val _lensFacing = MutableStateFlow(CameraSelector.LENS_FACING_FRONT)
    val lensFacing: StateFlow<Int> = _lensFacing

    private val _isFaceDetected = MutableStateFlow(false)
    val isFaceDetected: StateFlow<Boolean> = _isFaceDetected

    private val _recordingState = MutableStateFlow(RecordingState.IDLE)
    val recordingState: StateFlow<RecordingState> = _recordingState

    private val _recordingProgress = MutableStateFlow(0f)
    val recordingProgress: StateFlow<Float> = _recordingProgress

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    // UBAH TIPE INI menjadi List<RectF>
    private val _liveDetectionResult = MutableStateFlow<List<RectF>>(emptyList())
    val liveDetectionResult: StateFlow<List<RectF>> = _liveDetectionResult // Untuk FaceOverlay

    private val capturedFrames = mutableListOf<Bitmap>()
    private var recordingJob: Job? = null
    private var lastFrameCaptureTime: Long = 0L

    private val _countdown = MutableStateFlow(5)
    val countdown: StateFlow<Int> = _countdown

    private var isFrontCamera: Boolean = true

    private var liveFaceDetector: MediaPipeFaceDetector? = null

    companion object {
        private const val RECORD_DURATION_MILLIS = 5000L
        private const val FRAME_CAPTURE_INTERVAL_MILLIS = 500L
        private const val TARGET_FACE_SIZE = 160
        private const val FACE_EXPANSION_FACTOR = 0.9f
        private const val TAG = "AddFaceViewModel"
    }

    init {
        liveFaceDetector = MediaPipeFaceDetector(
            context = getApplication(),
            onResult = { result ->
                // KONVERSI FaceDetectorResult ke List<RectF> di sini
                val detectedBoxes = result.detections().map { it.boundingBox() }
                _isFaceDetected.value = detectedBoxes.isNotEmpty()
                _liveDetectionResult.value = detectedBoxes // Update liveDetectionResult dengan List<RectF>
                Log.d(TAG, "Live face detected: ${_isFaceDetected.value}")
            },
            onError = { error ->
                Log.e(TAG, "Live Face Detector error: ${error.message}")
                _isFaceDetected.value = false
                _liveDetectionResult.value = emptyList() // Set ke emptyList()
            }
        )

        viewModelScope.launch {
            _lensFacing.collect { lens ->
                isFrontCamera = (lens == CameraSelector.LENS_FACING_FRONT)
                Log.d(TAG, "Lens facing updated: ${if (isFrontCamera) "FRONT" else "BACK"}")
            }
        }
    }

    fun startRecording(userName: String, userEmail: String, userPhone: String) {
        if (_recordingState.value != RecordingState.IDLE) {
            Log.w(TAG, "Already recording or processing. Ignoring startRecording call.")
            return
        }

        name.value = userName
        email.value = userEmail
        phone.value = userPhone

        _recordingState.value = RecordingState.RECORDING
        _recordingProgress.value = 0f
        _countdown.value = (RECORD_DURATION_MILLIS / 1000).toInt()
        capturedFrames.clear()
        _message.value = "Merekam wajah, mohon tetap di tengah dan sedikit gerakkan kepala secara perlahan!"
        lastFrameCaptureTime = System.currentTimeMillis()
        Log.d(TAG, "Recording started for ${RECORD_DURATION_MILLIS / 1000} seconds.")


        recordingJob = viewModelScope.launch(Dispatchers.Default) {
            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < RECORD_DURATION_MILLIS) {
                _recordingProgress.value = ((System.currentTimeMillis() - startTime).toFloat() / RECORD_DURATION_MILLIS)
                delay(100)
            }
            Log.d(TAG, "Recording finished. Captured ${capturedFrames.size} frames.")
            _recordingState.value = RecordingState.PROCESSING
            _message.value = "Memproses frame dan membuat embedding..."
            processBufferedFrames()
        }

        viewModelScope.launch {
            val totalSeconds = (RECORD_DURATION_MILLIS / 1000).toInt()
            for (second in totalSeconds downTo 0) {
                _countdown.value = second
                delay(1000L)
            }
        }
    }

    // Fungsi handleDetection yang tidak terpakai telah dihapus dari sini.

    fun processLiveFrame(bitmap: Bitmap, rotationDegrees: Int) {
        var processedBitmap = bitmap
        if (rotationDegrees != 0) {
            val matrix = Matrix()
            matrix.postRotate(rotationDegrees.toFloat())
            val rotatedBitmap = Bitmap.createBitmap(
                processedBitmap,
                0,
                0,
                processedBitmap.width,
                processedBitmap.height,
                matrix,
                true
            )
            processedBitmap.recycle()
            processedBitmap = rotatedBitmap
        }

        if (isFrontCamera) {
            val matrixFlip = Matrix()
            matrixFlip.postScale(-1f, 1f, processedBitmap.width / 2f, processedBitmap.height / 2f)
            val flippedBitmap = Bitmap.createBitmap(
                processedBitmap,
                0,
                0,
                processedBitmap.width,
                processedBitmap.height,
                matrixFlip,
                true
            )
            processedBitmap.recycle()
            processedBitmap = flippedBitmap
        }
        liveFaceDetector?.detect(processedBitmap)
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
                var bitmap = imageProxy.toBitmap()
                val rotationDegrees = imageProxy.imageInfo.rotationDegrees

                try {
                    if (rotationDegrees != 0) {
                        val matrix = Matrix()
                        matrix.postRotate(rotationDegrees.toFloat())
                        val rotatedBitmap = Bitmap.createBitmap(
                            bitmap,
                            0,
                            0,
                            bitmap.width,
                            bitmap.height,
                            matrix,
                            true
                        )
                        bitmap.recycle()
                        bitmap = rotatedBitmap
                        Log.d(TAG, "Bitmap rotated by $rotationDegrees degrees.")
                    }

                    if (isFrontCamera) {
                        val matrixFlip = Matrix()
                        matrixFlip.postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
                        val flippedBitmap = Bitmap.createBitmap(
                            bitmap,
                            0,
                            0,
                            bitmap.width,
                            bitmap.height,
                            matrixFlip,
                            true
                        )
                        bitmap.recycle()
                        bitmap = flippedBitmap
                        Log.d(TAG, "Bitmap horizontally flipped for front camera.")
                    }

                    capturedFrames.add(bitmap)
                    Log.d(TAG, "Frame captured and added to buffer. Buffer size: ${capturedFrames.size}. Bitmap Dims: ${bitmap.width}x${bitmap.height} (After orientation correction)")
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing or capturing frame to buffer: ${e.message}", e)
                    bitmap?.recycle()
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
            Log.w(TAG, "No frames captured for processing.")
            return
        }

        val allEmbeddings = mutableListOf<FloatArray>()
        var facesDetectedCount = 0
        var processedFramesForEmbeddingCount = 0

        withContext(Dispatchers.Default) {
            val totalFrames = capturedFrames.size
            for ((index, bitmap) in capturedFrames.withIndex()) {
                _message.value = "Memproses frame ${index + 1}/${totalFrames}..."
                Log.d(TAG, "Processing frame ${index + 1}/${totalFrames}. Original Bitmap Dims: ${bitmap.width}x${bitmap.height}")

                val detectionResult: com.google.mediapipe.tasks.vision.facedetector.FaceDetectorResult? = try {
                    addFaceDetector.detect(bitmap)
                } catch (e: Exception) {
                    Log.e(TAG, "Error during face detection for frame ${index + 1}: ${e.message}", e)
                    null
                }

                if (detectionResult == null || detectionResult.detections().isEmpty()) {
                    Log.w(TAG, "No detection result or no faces detected in frame ${index + 1}.")
                    bitmap.recycle()
                    continue
                }

                detectionResult.detections().firstOrNull()?.let { detection ->
                    facesDetectedCount++
                    Log.d(TAG, "Face detected in frame ${index + 1}.")
                    val originalFaceBox = detection.boundingBox()
                    Log.d(TAG, "Original Bounding Box for frame ${index + 1}: ${originalFaceBox.left}, ${originalFaceBox.top}, ${originalFaceBox.right}, ${originalFaceBox.bottom}")

                    val expandedFaceBox = ImageCropper.expandBoundingBox(
                        boundingBox = originalFaceBox,
                        imageWidth = bitmap.width,
                        imageHeight = bitmap.height,
                        expansionFactor = FACE_EXPANSION_FACTOR
                    )
                    Log.d(TAG, "Expanded Crop Box for frame ${index + 1}: left=${expandedFaceBox.left}, top=${expandedFaceBox.top}, right=${expandedFaceBox.right}, bottom=${expandedFaceBox.bottom}")

                    val croppedFace = try {
                        ImageCropper.cropBitmap(bitmap, expandedFaceBox)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error cropping bitmap for frame ${index + 1}: ${e.message}", e)
                        null
                    } finally {
                        bitmap.recycle()
                    }

                    if (croppedFace == null || croppedFace.isRecycled) {
                        Log.e(TAG, "Cropped bitmap is null or recycled for frame ${index + 1}.")
                        croppedFace?.recycle()
                        return@let
                    }
                    Log.d(TAG, "Cropped Bitmap Dims for frame ${index + 1}: ${croppedFace.width}x${croppedFace.height}")

                    val resizedFace = try {
                        croppedFace.resizeBitmap(TARGET_FACE_SIZE, TARGET_FACE_SIZE)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error resizing bitmap for frame ${index + 1}: ${e.message}", e)
                        null
                    } finally {
                        croppedFace.recycle()
                    }

                    if (resizedFace == null || resizedFace.isRecycled) {
                        Log.e(TAG, "Resized bitmap is null or recycled for frame ${index + 1}.")
                        resizedFace?.recycle()
                        return@let
                    }
                    Log.d(TAG, "Resized Bitmap Dims for frame ${index + 1}: ${resizedFace.width}x${resizedFace.height}")

                    val embedding = try {
                        faceEmbedder.getEmbeddings(resizedFace)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error getting embeddings for frame ${index + 1}: ${e.message}", e)
                        null
                    } finally {
                        resizedFace.recycle()
                    }

                    if (embedding != null) {
                        allEmbeddings.add(embedding)
                        Log.d(TAG, "Embedding successfully generated for frame ${index + 1}. Embedding size: ${embedding.size}")
                    } else {
                        Log.w(TAG, "Embedding is null for frame ${index + 1}.")
                    }
                    processedFramesForEmbeddingCount++
                }
            }
            capturedFrames.clear()
            Log.d(TAG, "Finished processing all frames. Total faces detected: $facesDetectedCount. Total frames processed for embedding: $processedFramesForEmbeddingCount.")
        }

        if (allEmbeddings.isNotEmpty()) {
            val averagedEmbedding = averageEmbeddings(allEmbeddings)
            Log.d(TAG, "Averaged embedding calculated. Size: ${averagedEmbedding.size}")
            val user = User(
                name = name.value,
                email = email.value,
                phone = phone.value,
                embeddings = averagedEmbedding
            )
            Log.d(TAG, "Attempting to register user with averaged embedding.")

            when (val result = registerUserWithFaceUseCase(user.name, user.email, user.phone, user.embeddings)) {
                is ApiResult.Success -> {
                    if (result.data) {
                        _message.value = "Wajah berhasil disimpan!"
                        _recordingState.value = RecordingState.DONE
                        Log.i(TAG, "User registration successful.")
                    } else {
                        _message.value = "Gagal menyimpan data wajah di server."
                        _recordingState.value = RecordingState.IDLE
                        Log.e(TAG, "User registration failed on server side (returned false).")
                    }
                }
                is ApiResult.Error -> {
                    _message.value = "Error: ${result.message}"
                    Log.e(TAG, "Error during user registration API call: ${result.message}", result.exception)
                    _recordingState.value = RecordingState.IDLE
                }
                ApiResult.Loading -> { }
            }
        } else {
            _message.value = "Tidak ada wajah yang terdeteksi di frame yang diambil. Terdeteksi ${facesDetectedCount} wajah. Coba lagi."
            _recordingState.value = RecordingState.IDLE
            Log.w(TAG, "No valid embeddings generated from captured frames.")
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
        _countdown.value = 5
        Log.d(TAG, "ViewModel state reset.")
    }

    override fun onCleared() {
        super.onCleared()
        recordingJob?.cancel()
        capturedFrames.forEach { it.recycle() }
        capturedFrames.clear()
        addFaceDetector.close()
        liveFaceDetector?.close()
        faceEmbedder.close()
        Log.d(TAG, "ViewModel cleared. Resources closed.")
    }
}

enum class RecordingState {
    IDLE, RECORDING, PROCESSING, DONE
}