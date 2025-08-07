package com.example.myapplication4.ui.facedetection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import androidx.camera.core.ExperimentalGetImage
import android.graphics.ImageFormat
import android.graphics.YuvImage
import java.io.ByteArrayOutputStream
import android.graphics.BitmapFactory

@HiltViewModel
class FaceDetectionCameraViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private var interpreter: Interpreter? = null
    private val inputImageWidth = 640 // YOLOv12s input width
    private val inputImageHeight = 640 // YOLOv12s input height
    private val outputTensorSize = 25200 // Number of detections, based on typical YOLO output for 640x640
    private val outputElementsPerDetection = 6 // [x, y, w, h, confidence, class_id]

    private val _lensFacing = MutableStateFlow(CameraSelector.LENS_FACING_BACK)
    val lensFacing: StateFlow<Int> = _lensFacing

    private val _detectedFaces = MutableStateFlow<List<RectF>>(emptyList())
    val detectedFaces: StateFlow<List<RectF>> = _detectedFaces

    private val _isFaceDetected = MutableStateFlow(false)
    val isFaceDetected: StateFlow<Boolean> = _isFaceDetected

    init {
        loadModel()
    }

    private fun loadModelFile(context: Context, modelFileName: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelFileName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val modelBuffer = inputStream.channel.map(
            FileChannel.MapMode.READ_ONLY,
            fileDescriptor.startOffset,
            fileDescriptor.declaredLength
        )
        fileDescriptor.close()
        return modelBuffer
    }

    private fun loadModel() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val modelFile = loadModelFile(context, "Face_Detection_Yolov12_float32.tflite")
                interpreter = Interpreter(modelFile)
                Log.d("FaceDetViewModel", "YOLOv12 Face Detection model loaded successfully.")
            } catch (e: Exception) {
                Log.e("FaceDetViewModel", "Failed to load YOLOv12 model: ${e.message}", e)
            }
        }
    }

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    fun processFrame(imageProxy: ImageProxy) {
        viewModelScope.launch(Dispatchers.Default) {
            if (interpreter == null) {
                Log.e("FaceDetViewModel", "Interpreter is not initialized.")
                imageProxy.close()
                return@launch
            }

            // Gunakan fungsi toBitmapWithoutConverter
            val bitmap = imageProxy.toBitmapWithoutConverter()
            if (bitmap == null) {
                Log.e("FaceDetViewModel", "Failed to convert ImageProxy to Bitmap.")
                imageProxy.close()
                return@launch
            }

            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            imageProxy.close() // Penting: tutup ImageProxy setelah selesai

            val rotatedBitmap = rotateBitmap(bitmap, rotationDegrees)
            val resizedBitmap = Bitmap.createScaledBitmap(rotatedBitmap, inputImageWidth, inputImageHeight, true)

            val inputBuffer = ByteBuffer.allocateDirect(1 * inputImageWidth * inputImageHeight * 3 * 4) // Float32
            inputBuffer.order(ByteOrder.nativeOrder())
            inputBuffer.rewind()

            for (y in 0 until inputImageHeight) {
                for (x in 0 until inputImageWidth) {
                    val pixel = resizedBitmap.getPixel(x, y)
                    inputBuffer.putFloat((android.graphics.Color.red(pixel) / 255.0f))
                    inputBuffer.putFloat((android.graphics.Color.green(pixel) / 255.0f))
                    inputBuffer.putFloat((android.graphics.Color.blue(pixel) / 255.0f))
                }
            }

            val outputBuffer = ByteBuffer.allocateDirect(1 * outputTensorSize * outputElementsPerDetection * 4) // Float32
            outputBuffer.order(ByteOrder.nativeOrder())
            outputBuffer.rewind()

            val outputs = mutableMapOf<Int, Any>()
            outputs[0] = outputBuffer

            try {
                interpreter?.runForMultipleInputsOutputs(arrayOf(inputBuffer), outputs)
                val rawOutput = outputBuffer.asFloatBuffer()

                val detectedBoundingBoxes = postProcessOutput(rawOutput, rotatedBitmap.width, rotatedBitmap.height)
                _detectedFaces.value = detectedBoundingBoxes
                _isFaceDetected.value = detectedBoundingBoxes.isNotEmpty()
            } catch (e: Exception) {
                Log.e("FaceDetViewModel", "Error running interpreter: ${e.message}", e)
                _detectedFaces.value = emptyList()
                _isFaceDetected.value = false
            } finally {
                resizedBitmap.recycle()
                rotatedBitmap.recycle()
            }
        }
    }

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    private fun ImageProxy.toBitmapWithoutConverter(): Bitmap? {
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer

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
            this.width,
            this.height,
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

    private fun postProcessOutput(
        rawOutput: java.nio.FloatBuffer,
        originalImageWidth: Int,
        originalImageHeight: Int
    ): List<RectF> {
        val detections = mutableListOf<RectF>()
        for (i in 0 until outputTensorSize) {
            val offset = i * outputElementsPerDetection
            if (rawOutput.capacity() < offset + outputElementsPerDetection) {
                Log.e("FaceDetViewModel", "Output buffer too small for index $i.")
                break
            }

            val x_center = rawOutput.get(offset)
            val y_center = rawOutput.get(offset + 1)
            val width = rawOutput.get(offset + 2)
            val height = rawOutput.get(offset + 3)
            val confidence = rawOutput.get(offset + 4)
            val class_id = rawOutput.get(offset + 5).toInt()

            val CONFIDENCE_THRESHOLD = 0.4f
            if (confidence > CONFIDENCE_THRESHOLD && class_id == 0) {
                val x = x_center * inputImageWidth
                val y = y_center * inputImageHeight
                val w = width * inputImageWidth
                val h = height * inputImageHeight

                val left = (x - w / 2).coerceIn(0f, inputImageWidth.toFloat())
                val top = (y - h / 2).coerceIn(0f, inputImageHeight.toFloat())
                val right = (x + w / 2).coerceIn(0f, inputImageWidth.toFloat())
                val bottom = (y + h / 2).coerceIn(0f, inputImageHeight.toFloat())

                val scaleX = originalImageWidth.toFloat() / inputImageWidth.toFloat()
                val scaleY = originalImageHeight.toFloat() / inputImageHeight.toFloat()

                val scaledLeft = left * scaleX
                val scaledTop = top * scaleY
                val scaledRight = right * scaleX
                val scaledBottom = bottom * scaleY

                detections.add(RectF(scaledLeft, scaledTop, scaledRight, scaledBottom))
            }
        }
        return detections
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
        interpreter?.close()
        interpreter = null
    }
}
