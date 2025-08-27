package com.example.myapplication4.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.ops.ResizeOp
import com.example.myapplication4.domain.utils.BoundingBox
import com.example.myapplication4.domain.utils.applyNMS
import java.io.IOException

class CrowdDetectionProcessor(private val context: Context) : ModelProcessor<DetectionResult> {

    private var interpreter: Interpreter? = null
    private val modelFileName = "yolov8s_float32_crowd.tflite"
    private val inputSize = 640
    private val scoreThreshold = 0.5f // Sesuaikan ambang batas kepercayaan
    private val labels = listOf("crowd")

    // Variabel untuk ukuran output
    private val outputTensorSize = 84
    private val numBoxes = 8400

    override fun initialize(context: Context) {
        try {
            val modelBuffer = FileUtil.loadMappedFile(context, modelFileName)
            val options = Interpreter.Options()
            options.setNumThreads(4)
            interpreter = Interpreter(modelBuffer, options)
            Log.d("CrowdProcessor", "Model TFLite $modelFileName berhasil dimuat.")
        } catch (e: IOException) {
            Log.e("CrowdProcessor", "Gagal memuat model TFLite", e)
        }
    }

    override fun process(bitmap: Bitmap, rotationDegrees: Int): DetectionResult? {
        if (interpreter == null) {
            Log.e("CrowdProcessor", "Interpreter belum diinisialisasi")
            return null
        }

        // Pra-pemrosesan gambar menggunakan ImageProcessor
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(inputSize, inputSize, ResizeOp.ResizeMethod.BILINEAR))
            .add(NormalizeOp(0.0f, 255.0f))
            .build()

        val tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(bitmap)
        val processedImage = imageProcessor.process(tensorImage)

        // Output model: [1, 84, 8400]
        val outputBuffer = Array(1) { Array(outputTensorSize) { FloatArray(numBoxes) } }
        val inputs = arrayOf(processedImage.buffer)
        interpreter?.runForMultipleInputsOutputs(inputs, mapOf(0 to outputBuffer))

        return postprocessYoloOutput(outputBuffer, bitmap.width, bitmap.height)
    }

    private fun postprocessYoloOutput(
        outputBuffer: Array<Array<FloatArray>>,
        originalWidth: Int,
        originalHeight: Int
    ): DetectionResult {
        val boxes = mutableListOf<BoundingBox>()
        val output = outputBuffer[0]
        val xScale = originalWidth.toFloat() / inputSize.toFloat()
        val yScale = originalHeight.toFloat() / inputSize.toFloat()

        for (i in 0 until numBoxes) {
            val x_center = output[0][i]
            val y_center = output[1][i]
            val width = output[2][i]
            val height = output[3][i]

            val score = output[4][i]

            if (score > scoreThreshold) {
                val left = (x_center - width / 2f) * xScale
                val top = (y_center - height / 2f) * yScale
                val right = (x_center + width / 2f) * xScale
                val bottom = (y_center + height / 2f) * yScale

                boxes.add(
                    BoundingBox(
                        rect = RectF(left, top, right, bottom),
                        confidence = score,
                        classId = 0
                    )
                )
            }
        }

        val nmsBoxes = applyNMS(boxes, iouThreshold = 0.45f)

        val resultRects = nmsBoxes.map { it.rect }
        val resultLabels = nmsBoxes.map { labels.getOrElse(it.classId) { "Unknown" } }
        val resultScores = nmsBoxes.map { it.confidence }

        return DetectionResult(resultRects, resultLabels, resultScores)
    }

    override fun close() {
        interpreter?.close()
        interpreter = null
        Log.d("CrowdProcessor", "Interpreter ditutup")
    }
}