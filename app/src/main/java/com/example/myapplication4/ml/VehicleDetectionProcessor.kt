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


// Antarmuka generik untuk semua prosesor model
interface ModelProcessor<T> {
    fun initialize(context: Context)
    fun process(bitmap: Bitmap, rotationDegrees: Int): DetectionResult?
    fun close()
}

// Implementasi untuk deteksi kendaraan
class VehicleDetectionProcessor(private val context: Context) : ModelProcessor<Any?> {
    private var interpreter: Interpreter? = null
    private val modelFileName = "vehicle_detection.tflite" // Ganti dengan nama file TFLite Anda
    private val inputSize = 300 // Ukuran input yang diharapkan oleh model, sesuaikan
    private val outputBoxes = arrayOf(Array(10) { FloatArray(4) })
    private val outputClasses = arrayOf(FloatArray(10))
    private val outputScores = arrayOf(FloatArray(10))
    private val outputNumDetections = FloatArray(1)
    private val labels = listOf("vehicle") // Sesuaikan dengan label kelas dari model Anda

    override fun initialize(context: Context) {
        try {
            val modelBuffer = FileUtil.loadMappedFile(context, modelFileName)
            val options = Interpreter.Options()
            options.setNumThreads(4) // Atur jumlah thread untuk performa
            interpreter = Interpreter(modelBuffer, options)
            Log.d("VehicleProcessor", "Model TFLite $modelFileName berhasil dimuat.")
        } catch (e: IOException) {
            Log.e("VehicleProcessor", "Gagal memuat model TFLite", e)
        }
    }

    override fun process(bitmap: Bitmap, rotationDegrees: Int): DetectionResult? {
        if (interpreter == null) {
            Log.e("VehicleProcessor", "Interpreter belum diinisialisasi")
            return null
        }

        // Pra-pemrosesan (Preprocessing) gambar
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, false)
        val tensorImage = TensorImage(DataType.UINT8).apply {
            load(resizedBitmap)
        }

        // Menjalankan inferensi
        val inputs = arrayOf(tensorImage.buffer)
        val outputs = mapOf(
            0 to outputBoxes,
            1 to outputClasses,
            2 to outputScores,
            3 to outputNumDetections
        )
        interpreter?.runForMultipleInputsOutputs(inputs, outputs)

        // Pasca-pemrosesan (Postprocessing) hasil
        val results = mutableListOf<RectF>()
        val resultLabels = mutableListOf<String>()
        val resultScores = mutableListOf<Float>()

        val numDetections = outputNumDetections[0].toInt()
        for (i in 0 until numDetections) {
            val score = outputScores[0][i]
            if (score > 0.5) { // Thresholding: hanya deteksi dengan skor di atas 50%
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
        Log.d("VehicleProcessor", "Interpreter ditutup")
    }
}
