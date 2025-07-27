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

    private val _countdown = MutableStateFlow(5)
    val countdown: StateFlow<Int> = _countdown

    companion object {
        private const val RECORD_DURATION_MILLIS = 5000L
        private const val FRAME_CAPTURE_INTERVAL_MILLIS = 500L
        private const val TARGET_FACE_SIZE = 160
        private const val FACE_EXPANSION_FACTOR = 0.15f
        private const val TAG = "AddFaceViewModel"
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
        _message.value = "Merekam wajah, mohon tetap di tengah..."
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
                    Log.d(TAG, "Frame captured and added to buffer. Buffer size: ${capturedFrames.size}. Bitmap Dims: ${bitmap.width}x${bitmap.height}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error capturing frame to buffer: ${e.message}", e)
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
        var facesDetected = 0
        var processedCount = 0

        withContext(Dispatchers.Default) {
            val totalFrames = capturedFrames.size
            for ((index, bitmap) in capturedFrames.withIndex()) {
                _message.value = "Memproses frame ${index + 1}/${totalFrames}..."
                Log.d(TAG, "Processing frame ${index + 1}/${totalFrames}. Original Bitmap Dims: ${bitmap.width}x${bitmap.height}")

                val detectionResult: FaceDetectorResult? = try {
                    faceDetector.detect(bitmap)
                } catch (e: Exception) {
                    Log.e(TAG, "Error during face detection for frame ${index + 1}: ${e.message}", e)
                    null
                }

                if (detectionResult == null) {
                    Log.w(TAG, "No detection result for frame ${index + 1}.")
                }

                detectionResult?.detections()?.firstOrNull()?.let { detection ->
                    facesDetected++
                    Log.d(TAG, "Face detected in frame ${index + 1}.")
                    val box = detection.boundingBox()
                    Log.d(TAG, "Original Bounding Box for frame ${index + 1}: ${box.left}, ${box.top}, ${box.right}, ${box.bottom}")


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

                    Log.d(TAG, "Expanded/Coerced Crop Box for frame ${index + 1}: left=$cropLeft, top=$cropTop, width=$actualCropWidth, height=$actualCropHeight")

                    if (actualCropWidth > 0 && actualCropHeight > 0) {
                        val croppedFace = try {
                            bitmap.cropBitmap(RectF(cropLeft.toFloat(), cropTop.toFloat(), cropRight.toFloat(), cropBottom.toFloat()))
                        } catch (e: Exception) {
                            Log.e(TAG, "Error cropping bitmap for frame ${index + 1}: ${e.message}", e)
                            null
                        }

                        if (croppedFace == null || croppedFace.isRecycled) {
                            Log.e(TAG, "Cropped bitmap is null or recycled for frame ${index + 1}.")
                            croppedFace?.recycle()
                            return@let // Skip this frame
                        }
                        Log.d(TAG, "Cropped Bitmap Dims for frame ${index + 1}: ${croppedFace.width}x${croppedFace.height}")

                        val resizedFace = try {
                            croppedFace.resizeBitmap(TARGET_FACE_SIZE, TARGET_FACE_SIZE)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error resizing bitmap for frame ${index + 1}: ${e.message}", e)
                            null
                        } finally {
                            croppedFace.recycle() // Recycle cropped bitmap immediately
                        }

                        if (resizedFace == null || resizedFace.isRecycled) {
                            Log.e(TAG, "Resized bitmap is null or recycled for frame ${index + 1}.")
                            resizedFace?.recycle()
                            return@let // Skip this frame
                        }
                        Log.d(TAG, "Resized Bitmap Dims for frame ${index + 1}: ${resizedFace.width}x${resizedFace.height}")

                        // HAPUS: Normalisasi manual tidak digunakan lagi sesuai instruksi Tuan
                        // val normalizedFace = try {
                        //     resizedFace.normalizeBitmap()
                        // } catch (e: Exception) {
                        //     Log.e(TAG, "Error normalizing bitmap for frame ${index + 1}: ${e.message}", e)
                        //     null
                        // } finally {
                        //     resizedFace.recycle()
                        // }
                        //
                        // if (normalizedFace == null || normalizedFace.isRecycled) {
                        //     Log.e(TAG, "Normalized bitmap is null or recycled for frame ${index + 1}.")
                        //     normalizedFace?.recycle()
                        //     return@let
                        // }
                        // Log.d(TAG, "Normalized Bitmap Dims for frame ${index + 1}: ${normalizedFace.width}x${normalizedFace.height}")

                        val embedding = try {
                            // Menggunakan resizedFace langsung karena normalizeBitmap() dihapus
                            faceEmbedder.getEmbeddings(resizedFace)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error getting embeddings for frame ${index + 1}: ${e.message}", e)
                            null
                        } finally {
                            // Recycle resizedFace setelah digunakan untuk embedding
                            resizedFace.recycle()
                        }


                        if (embedding != null) {
                            allEmbeddings.add(embedding)
                            Log.d(TAG, "Embedding successfully generated for frame ${index + 1}. Embedding size: ${embedding.size}")
                        } else {
                            Log.w(TAG, "Embedding is null for frame ${index + 1}.")
                        }
                        processedCount++
                    } else {
                        Log.w(TAG, "Actual cropped width or height is 0 or less for frame ${index + 1}. Skipping frame.")
                    }
                }
                bitmap.recycle() // Recycle original captured bitmap after all processing
            }
            capturedFrames.clear() // Bersihkan buffer
            Log.d(TAG, "Finished processing all frames. Total faces detected: $facesDetected. Total frames processed for embedding: $processedCount.")
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
            _message.value = "Tidak ada wajah yang terdeteksi di frame yang diambil. Terdeteksi ${facesDetected} wajah. Coba lagi."
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
        faceDetector.close()
        faceEmbedder.close()
        Log.d(TAG, "ViewModel cleared. Resources closed.")
    }
}

enum class RecordingState {
    IDLE, RECORDING, PROCESSING, DONE
}