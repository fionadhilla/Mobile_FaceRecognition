package com.example.myapplication4.ui.vehicle_detection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication4.domain.utils.MediaPipeUtils.toBitmapWithoutConverter
import com.example.myapplication4.ml.DetectionResult
import com.example.myapplication4.ml.VehicleDetectionProcessor
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class VehicleDetectionViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val vehicleProcessor = VehicleDetectionProcessor(context)

    private val _lensFacing = MutableStateFlow(CameraSelector.LENS_FACING_BACK)
    val lensFacing: StateFlow<Int> = _lensFacing.asStateFlow()

    private val _detectionResult = MutableStateFlow<DetectionResult?>(null)
    val detectionResult: StateFlow<DetectionResult?> = _detectionResult.asStateFlow()

    private val _imageDimensions = MutableStateFlow(Size(0, 0))
    val imageDimensions = _imageDimensions.asStateFlow()

    private val _isModelLoaded = MutableStateFlow(false)
    val isModelLoaded = _isModelLoaded.asStateFlow()

    init {
        initializeModel()
        _isModelLoaded.value = true
    }

    fun initializeModel() {
        vehicleProcessor.initialize(context)
    }


    @androidx.annotation.OptIn(ExperimentalGetImage::class)
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
                        vehicleProcessor.process(rotatedBitmap, rotationDegrees)
                    }
                    _detectionResult.value = result

                    rotatedBitmap.recycle()
                    originalBitmap.recycle()
                } else {
                    Log.e("VehicleDetectionViewModel", "Bitmap conversion failed.")
                }
            } catch (e: Exception) {
                Log.e("VehicleDetectionViewModel", "Error processing image: ${e.message}")
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
        vehicleProcessor.close()
    }
}