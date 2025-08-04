package com.example.myapplication4.face

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.os.SystemClock
import android.util.Log
import com.example.myapplication4.domain.utils.BoundingBox
import com.example.myapplication4.domain.utils.applyNMS
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YoloV8PeopleDetector @Inject constructor(
    private val context: Context
) {

    private var interpreter: Interpreter? = null
    private val inputSize = 640
    private val outputSize = 84
    private val numBoxes = 8400
    private val personClassId = 0
    private val scoreThreshold = 0.5f // Ambang batas untuk confidence

    init {
        try {
            val modelFile = FileUtil.loadMappedFile(context, "yolov8s_float32.tflite")
            val options = Interpreter.Options()
            options.setNumThreads(4)
            interpreter = Interpreter(modelFile, options)
            Log.d("YoloV8Detector", "Model TFLite YoloV8 berhasil dimuat.")
        } catch (e: Exception) {
            Log.e("YoloV8Detector", "Gagal memuat model TFLite: ${e.message}")
            interpreter = null
        }
    }

    fun analyzeFrame(bitmap: Bitmap): List<BoundingBox> {
        if (interpreter == null) {
            return emptyList()
        }

        val tensorImage = preprocessImage(bitmap)
        // Output model YOLOv8 TFLite memiliki shape [1, 84, 8400]
        val outputBuffer = Array(1) { Array(outputSize) { FloatArray(numBoxes) } }

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
    ): List<BoundingBox> {
        val rawBoxes = mutableListOf<BoundingBox>()
        val output = outputBuffer[0]

        val xScale = originalWidth.toFloat() / inputSize.toFloat()
        val yScale = originalHeight.toFloat() / inputSize.toFloat()

        for (i in 0 until numBoxes) {
            // Ambil koordinat bounding box
            val x_center = output[0][i]
            val y_center = output[1][i]
            val width = output[2][i]
            val height = output[3][i]

            // Dapatkan skor tertinggi dan ID kelas
            var maxConfidence = 0f
            var maxClassId = -1
            for (j in 4 until outputSize) { // Iterasi dari index 4 hingga 83
                val confidence = output[j][i]
                if (confidence > maxConfidence) {
                    maxConfidence = confidence
                    maxClassId = j - 4
                }
            }

            // Hanya proses jika confidence di atas threshold dan kelasnya adalah 'person'
            if (maxConfidence > scoreThreshold && maxClassId == personClassId) {
                // Konversi koordinat dari center ke (left, top, right, bottom)
                val left = (x_center - width / 2f) * xScale
                val top = (y_center - height / 2f) * yScale
                val right = (x_center + width / 2f) * xScale
                val bottom = (y_center + height / 2f) * yScale

                rawBoxes.add(
                    BoundingBox(
                        rect = RectF(left, top, right, bottom),
                        confidence = maxConfidence,
                        classId = maxClassId
                    )
                )
            }
        }
        // Terapkan NMS untuk menghilangkan duplikat
        return applyNMS(rawBoxes, iouThreshold = 0.45f)
    }
}