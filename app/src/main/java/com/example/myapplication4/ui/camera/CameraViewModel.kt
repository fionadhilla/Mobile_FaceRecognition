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
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val _lensFacing = MutableStateFlow(CameraSelector.LENS_FACING_FRONT)
    val lensFacing: StateFlow<Int> = _lensFacing

    private val _detectionResult = MutableStateFlow<FaceDetectorResult?>(null)
    val detectionResult: StateFlow<FaceDetectorResult?> = _detectionResult

    private val _isFaceDetected = MutableStateFlow(false)
    val isFaceDetected: StateFlow<Boolean> = _isFaceDetected

    private var faceDetector: MediaPipeFaceDetector? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                faceDetector = MediaPipeFaceDetector(getApplication())
            } catch (e: Exception) {
                Log.e("CameraViewModel", "Failed to initialize FaceDetector: ${e.message}")
            }
        }
    }

    fun switchCamera() {
        _lensFacing.value =
            if (_lensFacing.value == CameraSelector.LENS_FACING_BACK) {
                CameraSelector.LENS_FACING_FRONT
            } else {
                CameraSelector.LENS_FACING_BACK
            }
    }

    fun processFrame(bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.Default) {
            if (faceDetector == null) {
                Log.e("CameraViewModel", "FaceDetector not initialized.")
                return@launch
            }
            try {
                val result = faceDetector?.detect(bitmap)
                _detectionResult.value = result

                val detected = result?.detections()?.isNotEmpty() == true
                if (detected && !_isFaceDetected.value) {
                    _isFaceDetected.value = true
                    viewModelScope.launch {
                        delay(1500)
                        _isFaceDetected.value = false
                    }
                } else if (!detected && _isFaceDetected.value) {
                    _isFaceDetected.value = false
                }


            } catch (e: Exception) {
                Log.e("CameraViewModel", "Error processing frame: ${e.message}")
                _detectionResult.value = null
                _isFaceDetected.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        faceDetector?.close()
    }
}