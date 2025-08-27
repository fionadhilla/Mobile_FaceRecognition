package com.example.myapplication4.ui.components

import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.myapplication4.ml.DetectionResult
import com.example.myapplication4.domain.utils.flipHorizontal

@Composable
fun CrowdDetectionOverlay(
    modifier: Modifier = Modifier,
    detectionResult: DetectionResult?,
    imageWidth: Int,
    imageHeight: Int,
    isFrontCamera: Boolean
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val scaleX = size.width / imageWidth.toFloat()
        val scaleY = size.height / imageHeight.toFloat()

        detectionResult?.boundingBoxes?.forEach { boundingBox ->
            var adjustedBox = boundingBox
            if (isFrontCamera) {
                adjustedBox = boundingBox.flipHorizontal(imageWidth.toFloat())
            }

            val left = adjustedBox.left * scaleX
            val top = adjustedBox.top * scaleY
            val right = adjustedBox.right * scaleX
            val bottom = adjustedBox.bottom * scaleY

            drawRect(
                color = Color.Red,
                topLeft = Offset(left, top),
                size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
                style = Stroke(width = 4f)
            )
        }
    }
}