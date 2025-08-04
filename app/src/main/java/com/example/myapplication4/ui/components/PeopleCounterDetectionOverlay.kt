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

@Composable
fun DetectionOverlay(
    boundingBoxes: List<BoundingBox>
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        boundingBoxes.forEach { box ->
            drawRect(
                color = Color.Green,
                topLeft = Offset(x = box.rect.left, y = box.rect.top),
                size = Size(width = box.rect.width(), height = box.rect.height()),
                style = Stroke(width = 4.dp.toPx())
            )
        }
    }
}
