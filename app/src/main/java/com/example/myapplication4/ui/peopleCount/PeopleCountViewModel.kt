package com.example.myapplication4.ui.peopleCount

import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import com.example.myapplication4.domain.utils.MediaPipeUtils.toBitmapWithoutConverter
import kotlinx.coroutines.Dispatchers
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication4.domain.utils.BoundingBox
import com.example.myapplication4.modelLoad.YoloV8PeopleDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import javax.inject.Inject

@HiltViewModel
class PeopleCountViewModel @Inject constructor(
    private val peopleDetector: YoloV8PeopleDetector
) : ViewModel() {

    private val _detectedPeople = MutableStateFlow<List<BoundingBox>>(emptyList())
    val detectedPeople = _detectedPeople.asStateFlow()

    private val _peopleCount = MutableStateFlow(0)
    val peopleCount = _peopleCount.asStateFlow()

    private val _lensFacing = MutableStateFlow(CameraSelector.LENS_FACING_FRONT)
    val lensFacing: StateFlow<Int> = _lensFacing

    private val cameraExecutor = Executors.newSingleThreadExecutor()

    @OptIn(ExperimentalGetImage::class)
    fun startCameraAnalysis(imageAnalysis: ImageAnalysis) {
        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
            viewModelScope.launch {
                try {
                    val bitmap = withContext(Dispatchers.Default) {
                        imageProxy.toBitmapWithoutConverter()
                    }
                    if (bitmap != null) {
                        val results = withContext(Dispatchers.Default) {
                            peopleDetector.analyzeFrame(bitmap)
                        }
                        _detectedPeople.value = results
                        _peopleCount.value = results.size
                    }
                } catch (e: Exception) {
                    Log.e("PeopleCountViewModel", "Error processing image: ${e.message}")
                } finally {
                    imageProxy.close()
                }
            }
        }
    }

    fun switchCamera() {
        _lensFacing.value = if (_lensFacing.value == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
    }

    override fun onCleared() {
        super.onCleared()
        cameraExecutor.shutdown()
    }
}
