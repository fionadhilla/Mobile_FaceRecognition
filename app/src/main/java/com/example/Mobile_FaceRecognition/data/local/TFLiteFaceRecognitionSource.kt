package com.example.Mobile_FaceRecognition.data.local

import android.content.res.AssetFileDescriptor
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import android.util.Pair
import androidx.core.util.component1
import androidx.core.util.component2
import com.example.Mobile_FaceRecognition.domain.model.FaceRecognition
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.sqrt

class TFLiteFaceRecognitionSource(
    private val interpreter: Interpreter,
    private val inputSize: Int,
    private val isModelQuantized: Boolean
) {

    private val intValues = IntArray(inputSize * inputSize)
    private val imgData: ByteBuffer
    private var lastEmbedding: FloatArray? = null
    private val registeredFaces = HashMap<String, FaceRecognition>()

    private var isClosed = false

    init {
        val bytesPerChannel = if (isModelQuantized) 1 else 4
        imgData = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * bytesPerChannel)
        imgData.order(ByteOrder.nativeOrder())
    }

    companion object {
        private const val OUTPUT_SIZE = 512
        private const val IMAGE_MEAN = 128.0f
        private const val IMAGE_STD = 128.0f

        @Throws(IOException::class)
        fun create(
            assetManager: AssetManager,
            modelFilename: String,
            inputSize: Int,
            isQuantized: Boolean
        ): TFLiteFaceRecognitionSource {
            val buffer = loadModelFile(assetManager, modelFilename)
            val interpreter = Interpreter(buffer)
            return TFLiteFaceRecognitionSource(interpreter, inputSize, isQuantized)
        }


        private fun loadModelFile(assets: AssetManager, filename: String): MappedByteBuffer {
            val fd: AssetFileDescriptor = assets.openFd(filename)
            FileInputStream(fd.fileDescriptor).channel.use { channel ->
                return channel.map(
                    FileChannel.MapMode.READ_ONLY,
                    fd.startOffset,
                    fd.declaredLength
                )
            }
        }
    }

    fun register(name: String, rec: FaceRecognition) {
        rec.embedding?.let { registeredFaces[name] = rec.copy(embedding = it.clone()) }
    }

    @Synchronized
    fun recognizeImage(bitmap: Bitmap, getExtra: Boolean): FaceRecognition {
        check(!isClosed) { "Interpreter has been closed." }

        // Resize jika diperlukan
        val scaled = if (bitmap.width != inputSize || bitmap.height != inputSize)
            Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, false)
        else bitmap

        // Konversi bitmap -> input tensor
        scaled.getPixels(intValues, 0, scaled.width, 0, 0, scaled.width, scaled.height)
        imgData.rewind()
        for (i in 0 until inputSize) {
            for (j in 0 until inputSize) {
                val p = intValues[i * inputSize + j]
                if (isModelQuantized) {
                    imgData.put(((p shr 16) and 0xFF).toByte())
                    imgData.put(((p shr 8) and 0xFF).toByte())
                    imgData.put((p and 0xFF).toByte())
                } else {
                    imgData.putFloat(((p shr 16 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                    imgData.putFloat(((p shr 8 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                    imgData.putFloat(((p and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                }
            }
        }

        // Jalankan model
        val output = Array(1) { FloatArray(OUTPUT_SIZE) }
        interpreter.run(imgData, output)
        val embedding = output[0]
        lastEmbedding = embedding.clone()

        // Bandingkan dengan wajah yang terdaftar
        var bestName = "?"
        var bestDist = Float.MAX_VALUE
        if (registeredFaces.isNotEmpty()) {
            val (name, dist) = findNearest(embedding) ?: Pair("?", Float.MAX_VALUE)
            bestName = name
            bestDist = dist
        }

        val res = FaceRecognition("0", bestName, bestDist, RectF(), embedding.clone(), scaled)
        if (getExtra) res.embedding = embedding.clone()
        return res
    }

    private fun findNearest(emb: FloatArray): Pair<String, Float>? {
        var best: Pair<String, Float>? = null
        for ((name, known) in registeredFaces) {
            val knownEmb = known.embedding ?: continue
            var dist = 0f
            for (i in emb.indices) {
                val diff = emb[i] - knownEmb[i]
                dist += diff * diff
            }
            dist = sqrt(dist)
            if (best == null || dist < best.second) best = Pair(name, dist)
        }
        return best
    }

    fun close() {
        if (!isClosed) {
            interpreter.close()
            isClosed = true
        }
    }
}
