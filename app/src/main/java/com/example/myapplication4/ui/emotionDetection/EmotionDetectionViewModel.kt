package com.example.myapplication4.ui.emotionDetection

import android.graphics.Bitmap
import android.util.Log
import android.util.Size
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication4.modelLoad.EmotionDetector
import com.example.myapplication4.domain.utils.MediaPipeUtils.toBitmapWithoutConverter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class EmotionDetectionViewModel @Inject constructor(
    private val emotionDetector: EmotionDetector
) : ViewModel() {

    private val _detectedEmotion = MutableStateFlow("Loading...")
    val detectedEmotion = _detectedEmotion.asStateFlow()

    private val _lensFacing = MutableStateFlow(CameraSelector.LENS_FACING_FRONT)
    val lensFacing = _lensFacing.asStateFlow()

    private val _isModelLoaded = MutableStateFlow(false)
    val isModelLoaded = _isModelLoaded.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            emotionDetector.loadModel()
            _isModelLoaded.value = true
            _detectedEmotion.value = "Model Loaded"
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

                val bitmap = withContext(Dispatchers.Default) {
                    imageProxy.toBitmapWithoutConverter()
                }

                if (bitmap != null) {
                    val emotion = withContext(Dispatchers.Default) {
                        emotionDetector.detect(bitmap)
                    }
                    _detectedEmotion.value = emotion
                    Log.d("EmotionViewModel", "Detected: $emotion")
                } else {
                    Log.e("EmotionViewModel", "Bitmap conversion failed.")
                }
            } catch (e: Exception) {
                Log.e("EmotionViewModel", "Error processing image: ${e.message}")
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
        emotionDetector.close()
    }
}