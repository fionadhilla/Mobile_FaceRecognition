// In app/src/main/java/com/example/myapplication4/ui/facedetection/FaceDetectionCameraViewModel.kt
package com.example.myapplication4.ui.facedetection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import android.util.Log
import androidx.camera.core.CameraSelector
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

    fun processFrame(bitmap: Bitmap, rotationDegrees: Int) {
        viewModelScope.launch(Dispatchers.Default) {
            if (interpreter == null) {
                Log.e("FaceDetViewModel", "Interpreter is not initialized.")
                return@launch
            }

            val rotatedBitmap = rotateBitmap(bitmap, rotationDegrees)
            val resizedBitmap = Bitmap.createScaledBitmap(rotatedBitmap, inputImageWidth, inputImageHeight, true)

            val inputBuffer = ByteBuffer.allocateDirect(1 * inputImageWidth * inputImageHeight * 3 * 4) // Float32
            inputBuffer.order(ByteOrder.nativeOrder())
            inputBuffer.rewind()

            for (y in 0 until inputImageHeight) {
                for (x in 0 until inputImageWidth) {
                    val pixel = resizedBitmap.getPixel(x, y)
                    // Normalize pixel values to [0, 1] if your model expects it, or [-1, 1]
                    // YOLO models typically expect normalized values (0-1 or -1 to 1).
                    // This example uses 0-1, adjust if your model's training was different.
                    inputBuffer.putFloat((android.graphics.Color.red(pixel) / 255.0f))
                    inputBuffer.putFloat((android.graphics.Color.green(pixel) / 255.0f))
                    inputBuffer.putFloat((android.graphics.Color.blue(pixel) / 255.0f))
                }
            }

            // Output tensor for YOLOv12s. Adjust the size based on your model's exact output shape.
            // A common output shape for YOLO is [1, num_boxes, num_elements_per_box]
            // For YOLOv12, it might be [1, 25200, 6] where 6 is [x, y, w, h, confidence, class_id]
            val outputBuffer = ByteBuffer.allocateDirect(1 * outputTensorSize * outputElementsPerDetection * 4) // Float32
            outputBuffer.order(ByteOrder.nativeOrder())
            outputBuffer.rewind()

            val outputs = mutableMapOf<Int, Any>()
            outputs[0] = outputBuffer // Output at index 0

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

    private fun postProcessOutput(
        rawOutput: java.nio.FloatBuffer,
        originalImageWidth: Int,
        originalImageHeight: Int
    ): List<RectF> {
        val detections = mutableListOf<RectF>()
        // Assuming rawOutput contains detections in the format [x_center, y_center, width, height, confidence, class_id]
        // You'll need to confirm the exact output format of your YOLOv12s model.
        // The `outputTensorSize` and `outputElementsPerDetection` should match your model.

        // Loop through each detection.
        // For YOLOv12s, the number of potential detections might be large (e.g., 25200).
        for (i in 0 until outputTensorSize) {
            val offset = i * outputElementsPerDetection
            if (rawOutput.capacity() < offset + outputElementsPerDetection) {
                // Ensure we don't read out of bounds
                Log.e("FaceDetViewModel", "Output buffer too small for index $i. Capacity: ${rawOutput.capacity()}, Required: ${offset + outputElementsPerDetection}")
                break
            }

            val x_center = rawOutput.get(offset)
            val y_center = rawOutput.get(offset + 1)
            val width = rawOutput.get(offset + 2)
            val height = rawOutput.get(offset + 3)
            val confidence = rawOutput.get(offset + 4) // Confidence score
            val class_id = rawOutput.get(offset + 5).toInt() // Class ID (0 for face, if trained that way)

            // Define a confidence threshold to filter out weak detections
            val CONFIDENCE_THRESHOLD = 0.4f // Adjust as needed

            if (confidence > CONFIDENCE_THRESHOLD && class_id == 0) { // Assuming class_id 0 is for "face"
                // Convert normalized coordinates (0-1) to actual pixel coordinates relative to the 640x640 input image
                val x = x_center * inputImageWidth
                val y = y_center * inputImageHeight
                val w = width * inputImageWidth
                val h = height * inputImageHeight

                // Calculate bounding box corners
                val left = (x - w / 2).coerceIn(0f, inputImageWidth.toFloat())
                val top = (y - h / 2).coerceIn(0f, inputImageHeight.toFloat())
                val right = (x + w / 2).coerceIn(0f, inputImageWidth.toFloat())
                val bottom = (y + h / 2).coerceIn(0f, inputImageHeight.toFloat())

                // Scale bounding boxes from 640x640 input resolution to original camera frame resolution
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