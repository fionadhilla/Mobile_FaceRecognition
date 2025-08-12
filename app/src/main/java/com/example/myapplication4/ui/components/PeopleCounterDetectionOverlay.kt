package com.example.myapplication4.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.myapplication4.domain.utils.BoundingBox
import android.util.Log

@Composable
fun PeopleCounterDetectionOverlay(
    boundingBoxes: List<BoundingBox>
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        boundingBoxes.forEach { box ->
            val rect = box.rect

            val scaledLeft = rect.left * canvasWidth
            val scaledTop = rect.top * canvasHeight
            val scaledWidth = rect.width() * canvasWidth
            val scaledHeight = rect.height() * canvasHeight

            Log.d(
                "DetectionOverlay",
                "Drawing box (scaled): left=$scaledLeft, top=$scaledTop, " +
                        "width=$scaledWidth, height=$scaledHeight"
            )

            drawRect(
                color = Color.Green,
                topLeft = Offset(x = scaledLeft, y = scaledTop),
                size = Size(width = scaledWidth, height = scaledHeight),
                style = Stroke(width = 4.dp.toPx())
            )
        }
    }
}