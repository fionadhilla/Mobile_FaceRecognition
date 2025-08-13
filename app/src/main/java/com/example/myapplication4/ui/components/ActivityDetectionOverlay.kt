package com.example.myapplication4.ui.components

import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication4.data.model.ActivityDetectionResult

@Composable
fun ActivityDetectionOverlay(
    results: List<ActivityDetectionResult>
) {
    val density = LocalDensity.current
    Canvas(modifier = Modifier.fillMaxSize()) {
        results.forEach { result ->
            val box = result.boundingBox
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Sesuaikan koordinat box dari model ke layar
            val mappedRect = RectF(
                box.left * canvasWidth,
                box.top * canvasHeight,
                box.right * canvasWidth,
                box.bottom * canvasHeight
            )

            drawContext.canvas.nativeCanvas.drawRect(
                mappedRect.left, mappedRect.top, mappedRect.right, mappedRect.bottom,
                Paint().apply {
                    color = android.graphics.Color.RED
                    style = Paint.Style.STROKE
                    strokeWidth = with(density) { 2.dp.toPx() }
                }
            )

            val textPaint = Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = with(density) { 16.sp.toPx() }
            }
            val textBackgroundPaint = Paint().apply {
                color = android.graphics.Color.RED
                style = Paint.Style.FILL
            }

            // Gambar latar belakang teks
            val textWidth = textPaint.measureText(result.label)
            val textHeight = textPaint.textSize
            drawContext.canvas.nativeCanvas.drawRect(
                mappedRect.left,
                mappedRect.top - textHeight,
                mappedRect.left + textWidth,
                mappedRect.top,
                textBackgroundPaint
            )

            // Gambar teks label
            drawContext.canvas.nativeCanvas.drawText(
                result.label,
                mappedRect.left, mappedRect.top - 5,
                textPaint
            )
        }
    }
}