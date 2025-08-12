package com.example.myapplication4.modelLoad

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmotionDetector @Inject constructor(
    private val context: Context
) {

    private var interpreter: Interpreter? = null
    private val inputSize = 224
    private val outputSize = 7

    // Label emosi sesuai urutan output model
    private val emotionLabels = listOf(
        "Anger", "Disgust", "Fear", "Happy", "Neutral", "Sad", "Surprise"
    )

    fun loadModel() {
        try {
            val modelFile = FileUtil.loadMappedFile(context, "EfficientNetB0_float32.tflite")
            val options = Interpreter.Options()
            val gpuDelegate = GpuDelegate()
            options.addDelegate(gpuDelegate)
            interpreter = Interpreter(modelFile, options)
            Log.d("EmotionDetector", "Model TFLite emosi berhasil dimuat.")
        } catch (e: Exception) {
            Log.e("EmotionDetector", "Gagal memuat model TFLite: ${e.message}")
            interpreter = null
        }
    }

    fun detect(bitmap: Bitmap): String {
        if (interpreter == null) {
            Log.e("EmotionDetector", "Interpreter belum diinisialisasi.")
            return "Model belum dimuat"
        }

        val tensorImage = preprocessImage(bitmap)
        val outputBuffer = Array(1) { FloatArray(outputSize) }
        interpreter?.run(tensorImage.buffer, outputBuffer)
        return postprocessResults(outputBuffer)
    }

    private fun preprocessImage(bitmap: Bitmap): TensorImage {
        val tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(bitmap)
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(inputSize, inputSize, ResizeOp.ResizeMethod.BILINEAR))
            .add(NormalizeOp(0.0f, 255.0f))
            .build()
        return imageProcessor.process(tensorImage)
    }

    private fun postprocessResults(outputBuffer: Array<FloatArray>): String {
        val emotionScores = outputBuffer[0]
        var maxScore = -1.0f
        var maxIndex = -1

        emotionScores.forEachIndexed { index, score ->
            if (score > maxScore) {
                maxScore = score
                maxIndex = index
            }
        }

        return if (maxIndex != -1) {
            emotionLabels[maxIndex]
        } else {
            "Tidak Terdeteksi"
        }
    }

    fun close() {
        interpreter?.close()
    }
}