// MediaPipeUtils.kt

package com.example.myapplication4.domain.utils

import android.graphics.*
import androidx.camera.core.ImageProxy
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import java.io.ByteArrayOutputStream

object MediaPipeUtils {

    @ExperimentalGetImage
    fun ImageProxy.toBitmapWithoutConverter(): Bitmap? {
        val image = this.image ?: run {
            Log.e("MediaPipeUtils", "ImageProxy.image is null in toBitmap().")
            this.close()
            return null
        }

        val planes = image.planes
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

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
        val out = ByteArrayOutputStream()
        val rect = Rect(0, 0, this.width, this.height)

        yuvImage.compressToJpeg(rect, 100, out)

        val imageBytes = out.toByteArray()
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

        image.close() // Penting: Tutup image dari ImageProxy

        Log.d("MediaPipeUtils", "toBitmap(): ImageProxy Dims: ${this.width}x${this.height}, Bitmap Dims: ${bitmap?.width}x${bitmap?.height}")
        if (bitmap == null) {
            Log.e("MediaPipeUtils", "toBitmap(): Failed to decode byte array into Bitmap.")
        }
        return bitmap
    }

    @ExperimentalGetImage
    fun ImageProxy.toBitmap(yuvConverter: YuvToRgbConverter): Bitmap? {
        val image = this.image ?: run {
            Log.e("MediaPipeUtils", "ImageProxy.image is null for toBitmap.")
            this.close()
            return null
        }

        val outputBitmap = Bitmap.createBitmap(this.width, this.height, Bitmap.Config.ARGB_8888)

        try {
            yuvConverter.yuvToRgb(image, outputBitmap)
            Log.d("MediaPipeUtils", "toBitmap (with converter): ImageProxy Dims: ${this.width}x${this.height}, Bitmap Dims: ${outputBitmap.width}x${outputBitmap.height}")
            return outputBitmap
        } catch (e: Exception) {
            Log.e("MediaPipeUtils", "Error converting YUV to RGB using converter: ${e.message}", e)
            return null
        } finally {
            image.close() // Penting: Tutup image dari ImageProxy
        }
    }

    fun Bitmap.cropBitmap(rect: RectF): Bitmap {
        val cropX = rect.left.toInt().coerceIn(0, this.width -1)
        val cropY = rect.top.toInt().coerceIn(0, this.height - 1)
        val cropWidth = rect.width().toInt().coerceIn(0, this.width - cropX)
        val cropHeight = rect.height().toInt().coerceIn(0, this.height - cropY)

        if (cropWidth <= 0 || cropHeight <= 0) {
            Log.e("MediaPipeUtils", "Crop size invalid: width=$cropWidth, height=$cropHeight")
            return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.TRANSPARENT) }
        }
        return Bitmap.createBitmap(this, cropX, cropY, cropWidth, cropHeight)
    }

    fun Bitmap.resizeBitmap(newWidth: Int, newHeight: Int): Bitmap {
        if (this.isRecycled) {
            throw IllegalStateException("Cannot resize a recycled bitmap.")
        }

        if (this.width == newWidth && this.height == newHeight) {
            return this
        }

        return try {
            Bitmap.createScaledBitmap(this, newWidth, newHeight, true)
        } catch (e: Exception) {
            Log.e("BitmapResize", "Error resizing bitmap: ${e.message}", e)
            throw e
        }
    }
}