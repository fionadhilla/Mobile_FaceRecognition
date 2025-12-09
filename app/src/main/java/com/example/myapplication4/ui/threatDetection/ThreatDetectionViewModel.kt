package com.example.myapplication4.ui.threatDetection

import android.util.Log
import android.util.Size
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication4.data.model.ThreatDetectionResult
import com.example.myapplication4.domain.utils.MediaPipeUtils.toBitmapWithoutConverter
import com.example.myapplication4.modelLoad.ThreatDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ThreatDetectionViewModel @Inject constructor(
    private val threatDetector: ThreatDetector
) : ViewModel() {

    private val _detectedThreats = MutableStateFlow<List<ThreatDetectionResult>>(emptyList())
    val detectedThreats = _detectedThreats.asStateFlow()

    private val _imageDimensions = MutableStateFlow(Size(0, 0))
    val imageDimensions = _imageDimensions.asStateFlow()

    private val _lensFacing = MutableStateFlow(CameraSelector.LENS_FACING_BACK)
    val lensFacing = _lensFacing.asStateFlow()

    private val _isModelLoaded = MutableStateFlow(false)
    val isModelLoaded = _isModelLoaded.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            threatDetector.loadModel()
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
                        threatDetector.detect(bitmap)
                    }
                    _detectedThreats.value = detections
                    detections.forEach { result ->
                        Log.d("ThreatDetectionViewModel", "Detected: ${result.label} with score ${result.score}")
                    }
                } else {
                    Log.e("ThreatDetectionViewModel", "Bitmap conversion failed.")
                }
            } catch (e: Exception) {
                Log.e("ThreatDetectionViewModel", "Error processing image: ${e.message}")
            } finally {
                imageProxy.close()
            }
        }
    }

    fun switchCamera() {
        _lensFacing.value = if (_lensFacing.value == CameraSelector.LENS_FACING_FRONT) {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }
    }

    override fun onCleared() {
        super.onCleared()
        threatDetector.close()
    }
}