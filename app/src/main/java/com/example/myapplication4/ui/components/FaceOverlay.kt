package com.example.myapplication4.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.google.mediapipe.tasks.vision.facedetector.FaceDetectorResult
import android.util.Log
import com.example.myapplication4.ui.camera.CameraPreviewTransformer

@Composable
fun FaceOverlay(
    modifier: Modifier = Modifier,
    detectionResult: FaceDetectorResult,
    imageWidth: Int,
    imageHeight: Int,
    isFrontCamera: Boolean
) {
    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        detectionResult.detections().forEachIndexed { index, detection ->
            val box = detection.boundingBox()

            val mappedBox = CameraPreviewTransformer.mapBoundingBoxToView(
                boundingBox = box,
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                viewWidth = canvasWidth,
                viewHeight = canvasHeight,
                isFrontCamera = isFrontCamera,
                expansionFactor = 0.7f
            )

            drawRect(
                color = Color.Blue,
                topLeft = Offset(mappedBox.left.toFloat(), mappedBox.top.toFloat()),
                size = Size(
                    width = (mappedBox.right - mappedBox.left).toFloat(),
                    height = (mappedBox.bottom - mappedBox.top).toFloat()
                ),
                style = Stroke(width = 6f)
            )

            Log.d("FaceOverlay", "Face #$index mapped to $mappedBox (orig=${box})")
        }

    }
}