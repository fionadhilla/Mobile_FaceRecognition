package com.example.myapplication4.domain.utils

import android.graphics.*
import androidx.camera.core.ImageProxy
import com.google.mediapipe.tasks.vision.facedetector.FaceDetectorResult
import java.io.ByteArrayOutputStream

object MediaPipeUtils {
    fun drawBoundingBoxes(bitmap: Bitmap, result: FaceDetectorResult): Bitmap {
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        val paint = Paint().apply {
            color = Color.BLUE
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }

        val expansionFactor = 0.15f

        for (detection in result.detections()) {
            val box = detection.boundingBox()

            val expandedWidth = box.width() * expansionFactor
            val expandedHeight = box.height() * expansionFactor

            val left = box.left - expandedWidth / 2
            val top = box.top - expandedHeight / 2
            val width = box.width() + expandedWidth
            val height = box.height() + expandedHeight

            canvas.drawRect(left, top, left + width, top + height, paint)
        }

        return mutableBitmap
    }

    fun ImageProxy.toBitmap(): Bitmap {
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    fun Bitmap.cropBitmap(rect: RectF): Bitmap {
        val cropX = rect.left.toInt().coerceIn(0, this.width -1)
        val cropY = rect.top.toInt().coerceIn(0, this.height - 1)
        val cropWidth = rect.width().toInt().coerceIn(0, this.width - cropX)
        val cropHeight = rect.height().toInt().coerceIn(0, this.height - cropY)

        if (cropWidth <= 0 || cropHeight <= 0) {
            return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.TRANSPARENT) }
        }
        return Bitmap.createBitmap(this, cropX, cropY, cropWidth, cropHeight)
    }

    fun Bitmap.resizeBitmap(newWidth: Int, newHeight: Int): Bitmap {
        if (this.width == newWidth && this.height == newHeight) {
            return this
        }
        return Bitmap.createScaledBitmap(this, newWidth, newHeight, true)
    }
}