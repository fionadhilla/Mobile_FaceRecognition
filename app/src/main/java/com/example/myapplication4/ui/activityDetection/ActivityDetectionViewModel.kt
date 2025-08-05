package com.example.myapplication4.ui.activityDetection

import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication4.data.model.ActivityDetectionResult
import com.example.myapplication4.modelLoad.YoloV6ActivityDetector
import com.example.myapplication4.domain.utils.MediaPipeUtils.toBitmapWithoutConverter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import javax.inject.Inject

@HiltViewModel
class ActivityDetectionViewModel @Inject constructor(
    private val yoloV6ActivityDetector: YoloV6ActivityDetector
) : ViewModel() {

    private val _detectionResults = MutableStateFlow<List<ActivityDetectionResult>>(emptyList())
    val detectionResults: StateFlow<List<ActivityDetectionResult>> = _detectionResults

    private val _isModelReady = MutableStateFlow(false)
    val isModelReady: StateFlow<Boolean> = _isModelReady

    private val _lensFacing = MutableStateFlow(CameraSelector.LENS_FACING_BACK)
    val lensFacing: StateFlow<Int> = _lensFacing

    private val cameraExecutor = Executors.newSingleThreadExecutor()

    fun loadModel() {
        viewModelScope.launch(Dispatchers.IO) {
            yoloV6ActivityDetector.loadModel()
            _isModelReady.value = true
        }
    }

    @OptIn(ExperimentalGetImage::class)
    fun startCameraAnalysis(imageAnalysis: ImageAnalysis) {
        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
            viewModelScope.launch(Dispatchers.Default) {
                if (_isModelReady.value) {
                    try {
                        val bitmap = imageProxy.toBitmapWithoutConverter()
                        bitmap?.let {
                            val results = yoloV6ActivityDetector.runInference(it)
                            _detectionResults.value = results
                        }
                    } catch (e: Exception) {
                        Log.e("ActivityDetectionViewModel", "Error processing frame", e)
                    } finally {
                        imageProxy.close()
                    }
                } else {
                    imageProxy.close()
                }
            }
        }
    }

    fun switchCamera() {
        _lensFacing.value = when (_lensFacing.value) {
            CameraSelector.LENS_FACING_FRONT -> CameraSelector.LENS_FACING_BACK
            else -> CameraSelector.LENS_FACING_FRONT
        }
    }

    override fun onCleared() {
        super.onCleared()
        cameraExecutor.shutdown()
    }
}