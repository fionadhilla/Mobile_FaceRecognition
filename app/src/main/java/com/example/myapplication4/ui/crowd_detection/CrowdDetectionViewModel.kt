package com.example.myapplication4.ui.crowd_detection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import android.util.Size
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication4.domain.utils.MediaPipeUtils.toBitmapWithoutConverter
import com.example.myapplication4.ml.CrowdDetectionProcessor
import com.example.myapplication4.ml.DetectionResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class CrowdDetectionViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val crowdProcessor = CrowdDetectionProcessor(context)

    private val _detectionResult = MutableStateFlow<DetectionResult?>(null)
    val detectionResult = _detectionResult.asStateFlow()

    private val _imageDimensions = MutableStateFlow(Size(0, 0))
    val imageDimensions = _imageDimensions.asStateFlow()

    private val _lensFacing = MutableStateFlow(CameraSelector.LENS_FACING_BACK)
    val lensFacing = _lensFacing.asStateFlow()

    private val _isModelLoaded = MutableStateFlow(false)
    val isModelLoaded = _isModelLoaded.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            crowdProcessor.initialize(context)
            _isModelLoaded.value = true
        }
    }

    @OptIn(ExperimentalGetImage::class)
    fun processFrame(imageProxy: ImageProxy) {
        viewModelScope.launch {
            try {
                if (!isModelLoaded.value) {
                    imageProxy.close()
                    return@launch
                }

                _imageDimensions.value = Size(imageProxy.width, imageProxy.height)

                val originalBitmap = withContext(Dispatchers.Default) {
                    imageProxy.toBitmapWithoutConverter()
                }

                if (originalBitmap != null) {
                    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                    val rotatedBitmap = withContext(Dispatchers.Default) {
                        rotateBitmap(originalBitmap, rotationDegrees)
                    }

                    val result = withContext(Dispatchers.Default) {
                        crowdProcessor.process(rotatedBitmap, rotationDegrees)
                    }
                    _detectionResult.value = result

                    rotatedBitmap.recycle()
                    originalBitmap.recycle()
                } else {
                    Log.e("CrowdDetectionViewModel", "Bitmap conversion failed.")
                }
            } catch (e: Exception) {
                Log.e("CrowdDetectionViewModel", "Error processing image: ${e.message}")
            } finally {
                imageProxy.close()
            }
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(rotationDegrees.toFloat())
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
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
        crowdProcessor.close()
    }
}