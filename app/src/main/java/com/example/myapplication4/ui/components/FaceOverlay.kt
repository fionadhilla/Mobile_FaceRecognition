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
    imageHeight: Int
) {
    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        val scaleX = canvasWidth / imageWidth
        val scaleY = canvasHeight / imageHeight

        detectionResult.detections().forEach { detection ->
            val box = detection.boundingBox()

            val left = box.left * scaleX
            val top = box.top * scaleY
            val width = box.width() * scaleX
            val height = box.height() * scaleY

            drawRect(
                color = Color.Blue,
                topLeft = Offset(left, top),
                size = Size(width, height),
                style = Stroke(width = 4f)
            )
        }
    }
}