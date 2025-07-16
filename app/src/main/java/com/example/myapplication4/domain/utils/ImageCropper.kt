package com.example.myapplication4.domain.utils

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import android.util.Log
import kotlin.math.roundToInt

object ImageCropper {
    fun cropBitmap(originalBitmap: Bitmap, boundingBox: RectF): Bitmap {
        val left = boundingBox.left.coerceAtLeast(0f).roundToInt()
        val top = boundingBox.top.coerceAtLeast(0f).roundToInt()
        val right = boundingBox.right.coerceAtMost(originalBitmap.width.toFloat()).roundToInt()
        val bottom = boundingBox.bottom.coerceAtMost(originalBitmap.height.toFloat()).roundToInt()

        val width = right - left
        val height = bottom - top

        if (width <= 0 || height <= 0) {
            Log.e("ImageCropper", "Ukuran crop tidak valid: width=$width, height=$height")
            return originalBitmap
        }

        try {
            return Bitmap.createBitmap(originalBitmap, left, top, width, height)
        } catch (e: Exception) {
            Log.e("ImageCropper", "Gagal membuat bitmap yang di-crop: ${e.message}", e)
            throw e
        }
    }

    fun expandBoundingBox(
        boundingBox: RectF,
        imageWidth: Int,
        imageHeight: Int,
        expansionFactor: Float
    ): RectF {
        val mappedWidth = boundingBox.width()
        val mappedHeight = boundingBox.height()

        val expandX = mappedWidth * expansionFactor / 2f
        val expandY = mappedHeight * expansionFactor / 2f

        val expandedLeft = (boundingBox.left - expandX).coerceAtLeast(0f)
        val expandedTop = (boundingBox.top - expandY).coerceAtLeast(0f)
        val expandedRight = (boundingBox.right + expandX).coerceAtMost(imageWidth.toFloat())
        val expandedBottom = (boundingBox.bottom + expandY).coerceAtMost(imageHeight.toFloat())

        return RectF(expandedLeft, expandedTop, expandedRight, expandedBottom)
    }
}