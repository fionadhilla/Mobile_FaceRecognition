package com.example.myapplication4.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import android.graphics.RectF // Import Android's RectF
import android.util.Log
import com.example.myapplication4.ui.camera.CameraPreviewTransformer

@Composable
fun FaceOverlay(
    modifier: Modifier = Modifier,
    detectedFaces: List<RectF>, // Change type to List<RectF>
    imageWidth: Int,
    imageHeight: Int,
    isFrontCamera: Boolean
) {
    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        detectedFaces.forEachIndexed { index, box ->
            // CameraPreviewTransformer.mapBoundingBoxToView might need adjustment if it was strictly for MediaPipe's box format.
            // Assuming it can handle android.graphics.RectF directly or can be adapted.
            // If the RectF is already scaled to original image dimensions, you just need to map from image to view coordinates.
            val mappedBox = CameraPreviewTransformer.mapBoundingBoxToView(
                boundingBox = box,
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                viewWidth = canvasWidth,
                viewHeight = canvasHeight,
                isFrontCamera = isFrontCamera,
                expansionFactor = 0.7f, // Keep or adjust as needed
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