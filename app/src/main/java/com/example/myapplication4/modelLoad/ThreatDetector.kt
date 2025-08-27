package com.example.myapplication4.modelLoad


import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import com.example.myapplication4.data.model.ThreatDetectionResult
import com.example.myapplication4.domain.utils.BoundingBox
import com.example.myapplication4.domain.utils.applyNMS
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

@Singleton
class ThreatDetector @Inject constructor(
    private val context: Context
) {

    private var interpreter: Interpreter? = null
    private val inputSize = 640
    private val numBoxes = 8400
    private val outputTensorSize = 6
    private val scoreThreshold = 0.5f

    private val labels = listOf("threat")

    fun loadModel() {
        try {
            val modelFile = FileUtil.loadMappedFile(context, "ThreatDetectionBest_Float32.tflite")
            val options = Interpreter.Options()
            val gpuDelegate = GpuDelegate()
            options.addDelegate(gpuDelegate)
            interpreter = Interpreter(modelFile, options)
            Log.d("ThreatDetector", "Model TFLite YoloV5 berhasil dimuat.")
        } catch (e: Exception) {
            Log.e("ThreatDetector", "Gagal memuat model TFLite: ${e.message}")
            interpreter = null
        }
    }

    fun detect(bitmap: Bitmap): List<ThreatDetectionResult> {
        if (interpreter == null) {
            Log.e("ThreatDetector", "Interpreter belum diinisialisasi.")
            return emptyList()
        }
        val tensorImage = preprocessImage(bitmap)
        val outputBuffer = Array(1) { Array(outputTensorSize) { FloatArray(numBoxes) } }
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
    ): List<ThreatDetectionResult> {
        val boxes = mutableListOf<BoundingBox>()
        val output = outputBuffer[0]
        val xScale = originalWidth.toFloat() / inputSize.toFloat()
        val yScale = originalHeight.toFloat() / inputSize.toFloat()

        for (i in 0 until numBoxes) {
            val x_center = output[0][i]
            val y_center = output[1][i]
            val width = output[2][i]
            val height = output[3][i]
            val confidence = output[4][i]
            val classProb = output[5][i]

            val score = confidence * classProb

            if (score > scoreThreshold) {
                val left = (x_center - width / 2f) * xScale
                val top = (y_center - height / 2f) * yScale
                val right = (x_center + width / 2f) * xScale
                val bottom = (y_center + height / 2f) * yScale

                boxes.add(
                    BoundingBox(
                        rect = RectF(left, top, right, bottom),
                        confidence = score,
                        classId = 0 // Karena hanya ada 1 kelas
                    )
                )
            }
        }

        val nmsBoxes = applyNMS(boxes, iouThreshold = 0.45f)
        return nmsBoxes.map { nmsBox ->
            ThreatDetectionResult(
                boundingBox = nmsBox.rect,
                label = labels[nmsBox.classId],
                score = nmsBox.confidence
            )
        }
    }

    fun close() {
        interpreter?.close()
    }
}