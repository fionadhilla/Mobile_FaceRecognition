package com.example.myapplication4.face

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import android.util.Log

class FaceNetModel @Inject constructor(context: Context) {
    private var interpreter: Interpreter? = null
    private val inputImageWidth = 160
    private val inputImageHeight = 160
    private val outputVectorSize = 512

    init {
        try {
            val model = loadModelFile(context, "facenet.tflite")
            interpreter = Interpreter(model)
            Log.d("FaceNetModel", "FaceNet model loaded successfully.")
        } catch (e: Exception) {
            Log.e("FaceNetModel", "Failed to load FaceNet model: ${e.message}", e)
        }
    }

    private fun loadModelFile(context: Context, modelFileName: String): ByteBuffer {
        val fileDescriptor = context.assets.openFd(modelFileName)
        val inputStream = java.io.FileInputStream(fileDescriptor.fileDescriptor)
        val modelBuffer = inputStream.channel.map(
            java.nio.channels.FileChannel.MapMode.READ_ONLY,
            fileDescriptor.startOffset,
            fileDescriptor.declaredLength
        )
        fileDescriptor.close()
        return modelBuffer
    }

    fun getFaceEmbedding(faceBitmap: Bitmap): ByteArray? {
        if (interpreter == null) {
            Log.e("FaceNetModel", "Interpreter is not initialized.")
            return null
        }

        val scaledBitmap = Bitmap.createScaledBitmap(faceBitmap, inputImageWidth, inputImageHeight, true)

        val inputBuffer = ByteBuffer.allocateDirect(1 * inputImageWidth * inputImageHeight * 3 * 4) // Float32
        inputBuffer.order(ByteOrder.nativeOrder())
        inputBuffer.rewind()

        for (y in 0 until inputImageHeight) {
            for (x in 0 until inputImageWidth) {
                val pixel = scaledBitmap.getPixel(x, y)
                // Normalisasi piksel ke [0, 1] dan kemudian ke [-1, 1] jika diperlukan oleh model
                inputBuffer.putFloat(((android.graphics.Color.red(pixel) / 255.0f) * 2.0f - 1.0f))
                inputBuffer.putFloat(((android.graphics.Color.green(pixel) / 255.0f) * 2.0f - 1.0f))
                inputBuffer.putFloat(((android.graphics.Color.blue(pixel) / 255.0f) * 2.0f - 1.0f))
            }
        }

        val outputBuffer = Array(1) { FloatArray(outputVectorSize) }

        try {
            interpreter?.run(inputBuffer, outputBuffer)
            val embeddingsFloatArray = outputBuffer[0]
            val embeddingsByteArray = ByteArray(embeddingsFloatArray.size * 4) // Float to ByteArray
            val byteBuffer = ByteBuffer.wrap(embeddingsByteArray).order(ByteOrder.nativeOrder())
            for (value in embeddingsFloatArray) {
                byteBuffer.putFloat(value)
            }
            Log.d("FaceNetModel", "Face embedding generated: ${embeddingsFloatArray.size} dimensions.")
            return embeddingsByteArray
        } catch (e: Exception) {
            Log.e("FaceNetModel", "Error running interpreter for embedding: ${e.message}", e)
            return null
        }
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}