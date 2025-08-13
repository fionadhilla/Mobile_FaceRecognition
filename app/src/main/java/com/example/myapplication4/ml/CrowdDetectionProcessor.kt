package com.example.myapplication4.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.IOException

// Menggunakan interface ModelProcessor yang sudah ada.
class CrowdDetectionProcessor(private val context: Context) : ModelProcessor<DetectionResult> {
    private var interpreter: Interpreter? = null
    private val modelFileName = "yolov8s_float32_crowd.tflite"
    private val inputSize = 640
    private val labels = listOf("crowd")

    private val outputBoxes = arrayOf(Array(25200) { FloatArray(4) })
    private val outputClasses = arrayOf(FloatArray(25200))
    private val outputScores = arrayOf(FloatArray(25200))
    private val outputNumDetections = FloatArray(1)

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

        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, false)
        val tensorImage = TensorImage(DataType.FLOAT32).apply {
            load(resizedBitmap)
        }

        val outputs = mapOf(
            0 to outputBoxes,
            1 to outputClasses,
            2 to outputScores,
            3 to outputNumDetections
        )
        interpreter?.runForMultipleInputsOutputs(arrayOf(tensorImage.buffer), outputs)

        val results = mutableListOf<RectF>()
        val resultLabels = mutableListOf<String>()
        val resultScores = mutableListOf<Float>()

        val numDetections = outputNumDetections[0].toInt()
        for (i in 0 until numDetections) {
            val score = outputScores[0][i]
            if (score > 0.5) {
                val box = outputBoxes[0][i]
                val ymin = box[0] * bitmap.height
                val xmin = box[1] * bitmap.width
                val ymax = box[2] * bitmap.height
                val xmax = box[3] * bitmap.width

                results.add(RectF(xmin, ymin, xmax, ymax))
                resultLabels.add(labels.getOrElse(outputClasses[0][i].toInt()) { "Unknown" })
                resultScores.add(score)
            }
        }
        return DetectionResult(results, resultLabels, resultScores)
    }

    override fun close() {
        interpreter?.close()
        interpreter = null
        Log.d("CrowdProcessor", "Interpreter ditutup")
    }
}