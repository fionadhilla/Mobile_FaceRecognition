package com.example.myapplication4.domain.utils

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import android.util.Log
import kotlin.math.roundToInt

object ImageCropper {
    fun cropBitmap(originalBitmap: Bitmap, boundingBox: RectF): Bitmap {
        val left = boundingBox.left.roundToInt().coerceAtLeast(0)
        val top = boundingBox.top.roundToInt().coerceAtLeast(0)
        val right = boundingBox.right.roundToInt().coerceAtMost(originalBitmap.width)
        val bottom = boundingBox.bottom.roundToInt().coerceAtMost(originalBitmap.height)

        val width = right - left
        val height = bottom - top

        if (width <= 0 || height <= 0) {
            Log.e("ImageCropper", "Ukuran crop tidak valid: width=$width, height=$height. Original Dims: ${originalBitmap.width}x${originalBitmap.height}, Bounding Box: $boundingBox")
            throw IllegalArgumentException("Invalid crop size: width=$width, height=$height")
        }

        return try {
            Bitmap.createBitmap(originalBitmap, left, top, width, height)
        } catch (e: Exception) {
            Log.e("ImageCropper", "Gagal membuat bitmap hasil crop: ${e.message}", e)
            throw e
        }
    }

    fun expandBoundingBox(
        boundingBox: RectF,
        imageWidth: Int,
        imageHeight: Int,
        expansionFactor: Float
    ): RectF {
        val width = boundingBox.width()
        val height = boundingBox.height()

        val expandX = width * (expansionFactor - 1f) / 2f
        val expandY = height * (expansionFactor - 1f) / 2f

        val left = (boundingBox.left - expandX).coerceAtLeast(0f)
        val top = (boundingBox.top - expandY).coerceAtLeast(0f)
        val right = (boundingBox.right + expandX).coerceAtMost(imageWidth.toFloat())
        val bottom = (boundingBox.bottom + expandY).coerceAtMost(imageHeight.toFloat())

        return RectF(left, top, right, bottom)
    }
}