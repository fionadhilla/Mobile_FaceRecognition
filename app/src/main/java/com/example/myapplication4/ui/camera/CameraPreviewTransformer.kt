package com.example.myapplication4.ui.camera

import android.graphics.Rect
import android.graphics.RectF
import android.util.Log
import kotlin.math.roundToInt

object CameraPreviewTransformer {

    fun mapBoundingBoxToView(
        boundingBox: RectF,
        imageWidth: Int,
        imageHeight: Int,
        viewWidth: Float,
        viewHeight: Float,
        isFrontCamera: Boolean,
        expansionFactor: Float = 0.0f
    ): Rect {
        val imageAspectRatio = imageWidth.toFloat() / imageHeight.toFloat()
        val viewAspectRatio = viewWidth / viewHeight

        val scale: Float
        val offsetX: Float
        val offsetY: Float

        if (imageAspectRatio > viewAspectRatio) {
            scale = viewWidth / imageWidth.toFloat()
            offsetX = 0f
            offsetY = (viewHeight - imageHeight * scale) / 2f
        } else {
            scale = viewHeight / imageHeight.toFloat()
            offsetX = (viewWidth - imageWidth * scale) / 2f
            offsetY = 0f
        }

        var originalMappedLeft = boundingBox.left * scale + offsetX
        var originalMappedTop = boundingBox.top * scale + offsetY
        var originalMappedRight = boundingBox.right * scale + offsetX
        var originalMappedBottom = boundingBox.bottom * scale + offsetY

        val mappedWidth = originalMappedRight - originalMappedLeft
        val mappedHeight = originalMappedBottom - originalMappedTop

        val expandX = mappedWidth * expansionFactor / 2f
        val expandY = mappedHeight * expansionFactor / 2f

        var mappedLeft = originalMappedLeft - expandX
        var mappedTop = originalMappedTop - expandY
        var mappedRight = originalMappedRight + expandX
        var mappedBottom = originalMappedBottom + expandY

        if (isFrontCamera) {
            val tempLeft = mappedLeft
            mappedLeft = viewWidth - mappedRight
            mappedRight = viewWidth - tempLeft
        }

        mappedLeft = mappedLeft.coerceAtLeast(0f)
        mappedTop = mappedTop.coerceAtLeast(0f)
        mappedRight = mappedRight.coerceAtMost(viewWidth)
        mappedBottom = mappedBottom.coerceAtMost(viewHeight)

        Log.d("CameraPreviewTransformer", "Orig Box: $boundingBox, Image Dims: ${imageWidth}x${imageHeight}")
        Log.d("CameraPreviewTransformer", "View Dims: ${viewWidth}x${viewHeight}, Scale: $scale, OffsetX: $offsetX, OffsetY: $offsetY")
        Log.d("CameraPreviewTransformer", "Mapped Box (original): Rect(${originalMappedLeft.roundToInt()}, ${originalMappedTop.roundToInt()}, ${originalMappedRight.roundToInt()}, ${originalMappedBottom.roundToInt()})")
        Log.d("CameraPreviewTransformer", "Mapped Box (expanded): Rect(${mappedLeft.roundToInt()}, ${mappedTop.roundToInt()}, ${mappedRight.roundToInt()}, ${mappedBottom.roundToInt()})")


        return Rect(
            mappedLeft.roundToInt(),
            mappedTop.roundToInt(),
            mappedRight.roundToInt(),
            mappedBottom.roundToInt()
        )
    }
}