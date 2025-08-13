package com.example.myapplication4.ui.components

import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import com.example.myapplication4.data.model.ActivityDetectionResult

@Composable
fun GestureDetectionOverlay(
    detectedGestures: List<ActivityDetectionResult>,
    imageWidth: Int,
    imageHeight: Int
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        if (detectedGestures.isEmpty()) {
            return@Canvas
        }

        val scaleX = size.width / imageWidth.toFloat()
        val scaleY = size.height / imageHeight.toFloat()

        detectedGestures.forEach { detection ->
            val boundingBox = detection.boundingBox
            val label = detection.label
            val score = detection.score

            val paint = Paint().apply {
                color = Color.GREEN
                style = Paint.Style.STROKE
                strokeWidth = 5f
            }
            val textPaint = Paint().apply {
                color = Color.WHITE
                textSize = 50f
                style = Paint.Style.FILL
            }
            val bgPaint = Paint().apply {
                color = Color.GREEN
                style = Paint.Style.FILL
            }

            val scaledRect = RectF(
                boundingBox.left * scaleX,
                boundingBox.top * scaleY,
                boundingBox.right * scaleX,
                boundingBox.bottom * scaleY
            )

            drawContext.canvas.nativeCanvas.drawRect(scaledRect, paint)

            drawContext.canvas.nativeCanvas.drawRect(
                scaledRect.left,
                scaledRect.top - 60f,
                scaledRect.left + textPaint.measureText("$label (${"%.2f".format(score)})") + 20f,
                scaledRect.top,
                bgPaint
            )

            drawContext.canvas.nativeCanvas.drawText(
                "$label (${"%.2f".format(score)})",
                scaledRect.left + 10f,
                scaledRect.top - 10f,
                textPaint
            )
        }
    }
}
