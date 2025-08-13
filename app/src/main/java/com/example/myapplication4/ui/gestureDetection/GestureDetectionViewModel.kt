package com.example.myapplication4.ui.gestureDetection

import android.graphics.Bitmap
import android.util.Log
import android.util.Size
import androidx.annotation.OptIn
import androidx.camera.core.ImageProxy
import androidx.camera.core.ExperimentalGetImage
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication4.modelLoad.GestureDetector
import com.example.myapplication4.domain.utils.MediaPipeUtils.toBitmapWithoutConverter
import com.example.myapplication4.data.model.ActivityDetectionResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class GestureDetectionViewModel @Inject constructor(
    private val gestureDetector: GestureDetector
) : ViewModel() {

    private val _detectedGestures = MutableStateFlow<List<ActivityDetectionResult>>(emptyList())
    val detectedGestures = _detectedGestures.asStateFlow()

    private val _imageDimensions = MutableStateFlow(Size(0, 0))
    val imageDimensions = _imageDimensions.asStateFlow()

    private val _lensFacing = MutableStateFlow(androidx.camera.core.CameraSelector.LENS_FACING_BACK)
    val lensFacing = _lensFacing.asStateFlow()

    private val _isModelLoaded = MutableStateFlow(false)
    val isModelLoaded = _isModelLoaded.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            gestureDetector.loadModel()
            _isModelLoaded.value = true
        }
    }

    @OptIn(ExperimentalGetImage::class)
    fun processImageProxy(imageProxy: ImageProxy) {
        viewModelScope.launch {
            try {
                if (!isModelLoaded.value) {
                    imageProxy.close()
                    return@launch
                }

                _imageDimensions.value = Size(imageProxy.width, imageProxy.height)

                val bitmap = withContext(Dispatchers.Default) {
                    imageProxy.toBitmapWithoutConverter()
                }

                if (bitmap != null) {
                    val detections = withContext(Dispatchers.Default) {
                        gestureDetector.detect(bitmap)
                    }
                    _detectedGestures.value = detections
                    detections.forEach { result ->
                        Log.d("GestureViewModel", "Detected: ${result.label} with score ${result.score}")
                    }
                } else {
                    Log.e("GestureDetectionViewModel", "Bitmap conversion failed.")
                }
            } catch (e: Exception) {
                Log.e("GestureDetectionViewModel", "Error processing image: ${e.message}")
            } finally {
                imageProxy.close()
            }
        }
    }

    fun switchCamera() {
        _lensFacing.value = if (_lensFacing.value == androidx.camera.core.CameraSelector.LENS_FACING_FRONT) {
            androidx.camera.core.CameraSelector.LENS_FACING_BACK
        } else {
            androidx.camera.core.CameraSelector.LENS_FACING_FRONT
        }
    }

    override fun onCleared() {
        super.onCleared()
        gestureDetector.close()
    }
}