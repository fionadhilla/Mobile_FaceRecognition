package com.example.myapplication4.ui.camera

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication4.face.MediaPipeFaceDetector
import com.google.mediapipe.tasks.vision.facedetector.FaceDetectorResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay

import com.example.myapplication4.domain.utils.ImageCropper
import java.io.File
import java.io.FileOutputStream
import android.net.Uri
import android.graphics.Matrix
import javax.inject.Inject

import com.example.myapplication4.face.FaceEmbedder
import com.example.myapplication4.domain.usecase.VerifyFaceUseCase
import com.example.myapplication4.data.model.FaceVerificationResult

@HiltViewModel
class CameraViewModel @Inject constructor(
    application: Application,
    private val faceEmbedder: FaceEmbedder,
    private val verifyFaceUseCase: VerifyFaceUseCase
) : AndroidViewModel(application) {

    private val _lensFacing = MutableStateFlow(CameraSelector.LENS_FACING_FRONT)
    val lensFacing: StateFlow<Int> = _lensFacing

    private val _detectionResult = MutableStateFlow<FaceDetectorResult?>(null)
    val detectionResult: StateFlow<FaceDetectorResult?> = _detectionResult

    private val _isFaceDetected = MutableStateFlow(false)
    val isFaceDetected: StateFlow<Boolean> = _isFaceDetected

    private val _croppedFaceImageUri = MutableStateFlow<Uri?>(null)
    val croppedFaceImageUri: StateFlow<Uri?> = _croppedFaceImageUri

    private val _verificationResult = MutableStateFlow<FaceVerificationResult?>(null) // State untuk hasil verifikasi
    val verificationResult: StateFlow<FaceVerificationResult?> = _verificationResult

    private var faceDetector: MediaPipeFaceDetector? = null

    private var lastProcessedBitmap: Bitmap? = null
    private var lastBitmapRotationDegrees: Int = 0
    private val cropExpansionFactor = 0.9f

    init {
        initFaceDetector()
    }

    private fun initFaceDetector() {
        faceDetector = MediaPipeFaceDetector(
            context = getApplication(),
            onResult = { result ->
                _detectionResult.value = result
                handleDetection(result)
            },
            onError = { error ->
                Log.e("CameraViewModel", "Face detection error: ${error.message}")
            }
        )
    }

    private fun handleDetection(result: FaceDetectorResult) {
        val detected = result.detections().isNotEmpty()

        if (detected && !_isFaceDetected.value) {
            _isFaceDetected.value = true
            viewModelScope.launch {
                delay(1500)
                if (_isFaceDetected.value) {
                    verifyDetectedFace()
                }
                //_isFaceDetected.value = false
            }
        } else if (!detected && _isFaceDetected.value) {
            _isFaceDetected.value = false
            _verificationResult.value = null
        }
    }

    fun switchCamera() {
        _lensFacing.value = if (_lensFacing.value == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
    }

    fun processFrame(bitmap: Bitmap, rotationDegrees: Int) {
        lastProcessedBitmap?.recycle()
        lastProcessedBitmap = bitmap.config?.let { bitmap.copy(it, true) }
        lastBitmapRotationDegrees = rotationDegrees
        faceDetector?.detect(bitmap)
    }

    fun verifyDetectedFace() {
        viewModelScope.launch(Dispatchers.Default) {
            val currentBitmap = lastProcessedBitmap
            val currentDetectionResult = _detectionResult.value
            val currentRotationDegrees = lastBitmapRotationDegrees
            val isFrontCamera = (_lensFacing.value == CameraSelector.LENS_FACING_FRONT)

            if (currentBitmap != null && currentDetectionResult != null && currentDetectionResult.detections().isNotEmpty()) {
                val firstFaceBox = currentDetectionResult.detections().first().boundingBox()
                try {
                    val expandedFaceBox = ImageCropper.expandBoundingBox(
                        boundingBox = firstFaceBox,
                        imageWidth = currentBitmap.width,
                        imageHeight = currentBitmap.height,
                        expansionFactor = cropExpansionFactor
                    )

                    var croppedBitmap = ImageCropper.cropBitmap(currentBitmap, expandedFaceBox)

                    if (currentRotationDegrees != 0) {
                        val matrix = Matrix()
                        matrix.postRotate(currentRotationDegrees.toFloat())
                        val rotatedBitmap = Bitmap.createBitmap(
                            croppedBitmap,
                            0,
                            0,
                            croppedBitmap.width,
                            croppedBitmap.height,
                            matrix,
                            true
                        )
                        croppedBitmap.recycle()
                        croppedBitmap = rotatedBitmap
                    }

                    if (isFrontCamera) {
                        val matrixFlip = Matrix()
                        matrixFlip.postScale(-1f, 1f, croppedBitmap.width / 2f, croppedBitmap.height / 2f)
                        val flippedBitmap = Bitmap.createBitmap(
                            croppedBitmap,
                            0,
                            0,
                            croppedBitmap.width,
                            croppedBitmap.height,
                            matrixFlip,
                            true
                        )
                        croppedBitmap.recycle()
                        croppedBitmap = flippedBitmap
                    }

                    val embeddings = faceEmbedder.getEmbeddings(croppedBitmap)
                    croppedBitmap.recycle()

                    if (embeddings != null) {
                        val result = verifyFaceUseCase(embeddings)
                        _verificationResult.value = result
                        Log.d("CameraViewModel", "Verification result: ${result.isMatch}, Matched User: ${result.matchedUser?.name}, Distance: ${result.distance}")
                    } else {
                        Log.e("CameraViewModel", "Gagal mendapatkan embeddings untuk verifikasi.")
                        _verificationResult.value = FaceVerificationResult(isMatch = false, matchedUser = null, distance = -1.0f)
                    }

                } catch (e: Exception) {
                    Log.e("CameraViewModel", "Gagal memotong atau memverifikasi wajah: ${e.message}", e)
                    _verificationResult.value = FaceVerificationResult(isMatch = false, matchedUser = null, distance = -1.0f)
                }
            } else {
                Log.d("CameraViewModel", "Tidak ada bitmap atau hasil deteksi wajah yang tersedia untuk verifikasi.")
                _verificationResult.value = FaceVerificationResult(isMatch = false, matchedUser = null, distance = -1.0f)
            }
        }
    }


    fun cropDetectedFace() {
        viewModelScope.launch(Dispatchers.Default) {
            val currentBitmap = lastProcessedBitmap
            val currentDetectionResult = _detectionResult.value
            val currentRotationDegrees = lastBitmapRotationDegrees
            val isFrontCamera = (_lensFacing.value == CameraSelector.LENS_FACING_FRONT)

            if (currentBitmap != null && currentDetectionResult != null && currentDetectionResult.detections().isNotEmpty()) {
                val firstFaceBox = currentDetectionResult.detections().first().boundingBox()
                try {
                    val expandedFaceBox = ImageCropper.expandBoundingBox(
                        boundingBox = firstFaceBox,
                        imageWidth = currentBitmap.width,
                        imageHeight = currentBitmap.height,
                        expansionFactor = cropExpansionFactor
                    )

                    var croppedBitmap = ImageCropper.cropBitmap(currentBitmap, expandedFaceBox)

                    if (currentRotationDegrees != 0) {
                        val matrix = Matrix()
                        matrix.postRotate(currentRotationDegrees.toFloat())
                        val rotatedBitmap = Bitmap.createBitmap(
                            croppedBitmap,
                            0,
                            0,
                            croppedBitmap.width,
                            croppedBitmap.height,
                            matrix,
                            true
                        )
                        croppedBitmap.recycle()
                        croppedBitmap = rotatedBitmap
                    }

                    if (isFrontCamera) {
                        val matrixFlip = Matrix()
                        matrixFlip.postScale(-1f, 1f, croppedBitmap.width / 2f, croppedBitmap.height / 2f)
                        val flippedBitmap = Bitmap.createBitmap(
                            croppedBitmap,
                            0,
                            0,
                            croppedBitmap.width,
                            croppedBitmap.height,
                            matrixFlip,
                            true
                        )
                        croppedBitmap.recycle()
                        croppedBitmap = flippedBitmap
                    }


                    val outputDir = getApplication<Application>().cacheDir
                    val outputFile = File(outputDir, "cropped_face_${System.currentTimeMillis()}.jpg")
                    FileOutputStream(outputFile).use { out ->
                        croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                    val uri = Uri.fromFile(outputFile)
                    _croppedFaceImageUri.value = uri
                    croppedBitmap.recycle()

                } catch (e: Exception) {
                    Log.e("CameraViewModel", "Gagal menyimpan atau memotong wajah: ${e.message}", e)
                    _croppedFaceImageUri.value = null
                }
            } else {
                Log.d("CameraViewModel", "Tidak ada bitmap atau hasil deteksi wajah yang tersedia untuk di-crop. isFaceDetected was ${isFaceDetected.value}")
                _croppedFaceImageUri.value = null
            }
        }
    }

    fun clearCroppedFaceData() {
        _croppedFaceImageUri.value?.let { uri ->
            val file = File(uri.path!!)
            if (file.exists()) {
                file.delete()
                Log.d("CameraViewModel", "Temporary cropped face file deleted: $uri")
            }
        }
        _croppedFaceImageUri.value = null
    }

    override fun onCleared() {
        super.onCleared()
        faceDetector?.close()
        lastProcessedBitmap?.recycle()
        lastProcessedBitmap = null
        clearCroppedFaceData()
    }
}