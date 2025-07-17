package com.example.myapplication4.face
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.google.mediapipe.tasks.vision.facedetector.FaceDetectorResult
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

object FaceUtils {
    fun drawBoundingBoxes(bitmap: Bitmap, result: FaceDetectorResult): Bitmap {
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        val paint = Paint().apply {
            color = Color.GREEN
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }

        for (detection in result.detections()) {
            val rect = detection.boundingBox()
            canvas.drawRect(rect, paint)
        }

        return mutableBitmap
    }

    fun byteArrayToFloatArray(byteArray: ByteArray): FloatArray {
        val buffer = ByteBuffer.wrap(byteArray).order(ByteOrder.nativeOrder())
        val floatArray = FloatArray(byteArray.size / 4)
        buffer.asFloatBuffer().get(floatArray)
        return floatArray
    }

    fun calculateEuclideanDistance(embeddings1: FloatArray, embeddings2: FloatArray): Float {
        if (embeddings1.size != embeddings2.size) {
            throw IllegalArgumentException("Embeddings arrays must have the same size.")
        }

        var sumOfSquares = 0.0f
        for (i in embeddings1.indices) {
            val diff = embeddings1[i] - embeddings2[i]
            sumOfSquares += diff * diff
        }
        return sqrt(sumOfSquares)
    }

    const val RECOGNITION_THRESHOLD = 0.9f
}