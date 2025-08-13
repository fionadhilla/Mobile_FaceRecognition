package com.example.myapplication4.ui.vehicle_detection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.YuvImage
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication4.domain.utils.ImageCropper
import com.example.myapplication4.ml.DetectionResult
import com.example.myapplication4.ml.VehicleDetectionProcessor
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import javax.inject.Inject

@HiltViewModel
class VehicleDetectionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageCropper: ImageCropper // Jika ini tidak digunakan, Anda bisa menghapusnya
) : ViewModel() {

    private val vehicleProcessor = VehicleDetectionProcessor(context)

    private val _lensFacing = MutableStateFlow(CameraSelector.LENS_FACING_BACK)
    val lensFacing: StateFlow<Int> = _lensFacing.asStateFlow()

    private val _detectionResult = MutableStateFlow<DetectionResult?>(null)
    val detectionResult: StateFlow<DetectionResult?> = _detectionResult.asStateFlow()

    init {
        initializeModel()
    }

    fun initializeModel() {
        vehicleProcessor.initialize(context)
    }

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    fun processFrame(imageProxy: ImageProxy) {
        viewModelScope.launch(Dispatchers.Default) {
            val bitmap = toBitmapWithoutConverter(imageProxy)
            if (bitmap != null) {
                // Rotasi bitmap jika diperlukan, sama seperti di FaceDetectionViewModel
                val rotatedBitmap = rotateBitmap(bitmap, imageProxy.imageInfo.rotationDegrees)
                val result = vehicleProcessor.process(rotatedBitmap, imageProxy.imageInfo.rotationDegrees)
                _detectionResult.value = result

                rotatedBitmap.recycle()
                bitmap.recycle() // Recycle original bitmap as well
            }
            imageProxy.close()
        }
    }

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    private fun toBitmapWithoutConverter(imageProxy: ImageProxy): Bitmap? {
        val yBuffer = imageProxy.planes[0].buffer
        val uBuffer = imageProxy.planes[1].buffer
        val vBuffer = imageProxy.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(
            nv21,
            ImageFormat.NV21,
            imageProxy.width,
            imageProxy.height,
            null
        )
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(
            android.graphics.Rect(0, 0, yuvImage.width, yuvImage.height),
            100,
            out
        )
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
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
