package com.example.myapplication4.ui.camera

import android.app.Application
import android.graphics.Bitmap
import androidx.camera.core.CameraSelector
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication4.face.MediaPipeFaceDetector
import com.google.mediapipe.tasks.vision.facedetector.FaceDetectorResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class CameraViewModel(application: Application) : AndroidViewModel(application) {

    // Status Face Detection
    private val _isFaceDetected = MutableStateFlow(false)
    val isFaceDetected: StateFlow<Boolean> = _isFaceDetected

    // Untuk switch kamera
    private val _lensFacing = MutableStateFlow(CameraSelector.LENS_FACING_FRONT)
    val lensFacing: StateFlow<Int> = _lensFacing

    // Untuk deteksi wajah
    private val faceDetector = MediaPipeFaceDetector(application.applicationContext)
    private val _detectionResult = MutableStateFlow<FaceDetectorResult?>(null)
    val detectionResult: StateFlow<FaceDetectorResult?> = _detectionResult

    fun switchCamera() {
        viewModelScope.launch {
            val newLens = if (_lensFacing.value == CameraSelector.LENS_FACING_BACK)
                CameraSelector.LENS_FACING_FRONT
            else
                CameraSelector.LENS_FACING_BACK
            _lensFacing.emit(newLens)
        }
    }

    fun processFrame(bitmap: Bitmap) {
        viewModelScope.launch {
            val result = faceDetector.detect(bitmap)
            _detectionResult.value = result

            // Periksa apakah ada wajah
            val detected = result?.detections()?.isNotEmpty() == true
            _isFaceDetected.value = detected

            // Reset status deteksi setelah 1.5 detik jika tidak ada wajah baru
            if (detected) {
                delay(1500)
                _isFaceDetected.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        faceDetector.close()
    }
}