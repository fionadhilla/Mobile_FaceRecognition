package com.example.myapplication4.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.google.mediapipe.tasks.vision.facedetector.FaceDetectorResult

@Composable
fun FaceOverlay(
    modifier: Modifier = Modifier,
    detectionResult: FaceDetectorResult,
    imageWidth: Int,
    imageHeight: Int,
    isFrontCamera: Boolean
) {
    Canvas(modifier = modifier) {
        if (size.width == 0f || size.height == 0f) return@Canvas
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Penting! Gunakan image dimensions yang sesuai orientasi canvas (portrait)
        val scaleX = canvasWidth / imageWidth.toFloat()
        val scaleY = canvasHeight / imageHeight.toFloat()
        val scale = maxOf(scaleX, scaleY)

        val offsetX = (canvasWidth - imageWidth * scale) / 2f - 5f
        val offsetY = (canvasHeight - imageHeight * scale) / 2f

        detectionResult.detections().forEach { detection ->
            val box = detection.boundingBox()

            var left = box.left * scale + offsetX
            var top = box.top * scale + offsetY
            var right = box.right * scale + offsetX
            var bottom = box.bottom * scale + offsetY

            if (isFrontCamera) {
                val flippedLeft = canvasWidth - right
                val flippedRight = canvasWidth - left
                left = flippedLeft
                right = flippedRight
            }

            drawRect(
                color = Color.Blue,
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top),
                style = Stroke(width = 6f)
            )
        }
    }
}