package com.example.myapplication4.face
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.google.mediapipe.tasks.vision.facedetector.FaceDetectorResult

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
}