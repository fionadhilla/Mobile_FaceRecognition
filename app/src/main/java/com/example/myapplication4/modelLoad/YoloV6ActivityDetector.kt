package com.example.myapplication4.modelLoad

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import com.example.myapplication4.data.model.ActivityDetectionResult
import com.example.myapplication4.domain.utils.BoundingBox
import com.example.myapplication4.domain.utils.applyNMS
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YoloV6ActivityDetector @Inject constructor(
    private val context: Context
) {

    private var interpreter: Interpreter? = null
    private val inputSize = 320
    private val numBoxes = 2100
    private val numClasses = 6
    private val scoreThreshold = 0.5f

    private val labels = listOf(
        "stand", "walk", "interact", "run", "sit", "steal"
    )

    fun loadModel() {
        try {
            val modelFile = FileUtil.loadMappedFile(context, "Activity_Detection_yolov12_VER6_float32.tflite")
            val options = Interpreter.Options()
            val gpuDelegate = GpuDelegate()
            options.addDelegate(gpuDelegate)
            interpreter = Interpreter(modelFile, options)
            Log.d("YoloV6ActivityDetector", "Model TFLite YoloV6 berhasil dimuat.")
        } catch (e: Exception) {
            Log.e("YoloV6ActivityDetector", "Gagal memuat model TFLite: ${e.message}")
            interpreter = null
        }
    }

    fun runInference(bitmap: Bitmap): List<ActivityDetectionResult> {
        if (interpreter == null) {
            Log.e("YoloV6ActivityDetector", "Interpreter is not initialized.")
            return emptyList()
        }
        val tensorImage = preprocessImage(bitmap)
        val outputBuffer = Array(1) { Array(4 + numClasses) { FloatArray(numBoxes) } }
        interpreter?.run(tensorImage.buffer, outputBuffer)
        return postprocessResults(outputBuffer, bitmap.width, bitmap.height)
    }

    private fun preprocessImage(bitmap: Bitmap): TensorImage {
        val tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(bitmap)
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(inputSize, inputSize, ResizeOp.ResizeMethod.BILINEAR))
            .build()
        return imageProcessor.process(tensorImage)
    }

    private fun postprocessResults(
        outputBuffer: Array<Array<FloatArray>>,
        originalWidth: Int,
        originalHeight: Int
    ): List<ActivityDetectionResult> {
        val boxes = mutableListOf<BoundingBox>()
        val output = outputBuffer[0]
        val xScale = originalWidth.toFloat() / inputSize.toFloat()
        val yScale = originalHeight.toFloat() / inputSize.toFloat()
        val numDetections = output[0].size

        for (i in 0 until numDetections) {
            var maxConfidence = 0f
            var maxClassId = -1

            for (j in 0 until numClasses) {
                val confidence = output[4 + j][i]
                if (confidence > maxConfidence) {
                    maxConfidence = confidence
                    maxClassId = j
                }
            }

            if (maxConfidence > scoreThreshold) {
                val x_center = output[0][i]
                val y_center = output[1][i]
                val width = output[2][i]
                val height = output[3][i]

                val left = (x_center - width / 2f) * xScale
                val top = (y_center - height / 2f) * yScale
                val right = (x_center + width / 2f) * xScale
                val bottom = (y_center + height / 2f) * yScale

                boxes.add(
                    BoundingBox(
                        rect = RectF(left, top, right, bottom),
                        confidence = maxConfidence,
                        classId = maxClassId
                    )
                )
            }
        }

        val nmsBoxes = applyNMS(boxes, iouThreshold = 0.45f)
        return nmsBoxes.map { nmsBox ->
            ActivityDetectionResult(
                boundingBox = nmsBox.rect,
                label = labels[nmsBox.classId],
                score = nmsBox.confidence
            )
        }
    }
}