package com.example.myapplication4.ui.components

import android.graphics.RectF
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.myapplication4.ui.camera.CameraPreviewTransformer

@Composable
fun FaceDetectionOverlay(
    modifier: Modifier = Modifier,
    detectedFaces: List<RectF>, // Menggunakan parameter yang sesuai dengan ViewModel
    imageWidth: Int,
    imageHeight: Int,
    isFrontCamera: Boolean
) {
    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        detectedFaces.forEachIndexed { index, box ->
            val mappedBox = CameraPreviewTransformer.mapBoundingBoxToView(
                boundingBox = box,
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                viewWidth = canvasWidth,
                viewHeight = canvasHeight,
                isFrontCamera = isFrontCamera,
                expansionFactor = 0.7f,
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

            Log.d("FaceDetectionOverlay", "Face #$index mapped to $mappedBox (orig=${box})")
        }

    }
}
