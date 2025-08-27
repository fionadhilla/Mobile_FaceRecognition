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
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.example.myapplication4.ml.DetectionResult // Tambahkan import ini
import com.example.myapplication4.ui.camera.CameraPreviewTransformer
import com.google.mediapipe.tasks.vision.core.RunningMode // Tambahkan import ini

@Composable
fun VehicleDetectionOverlay(
    modifier: Modifier = Modifier,
    detectionResult: DetectionResult?,
    imageWidth: Int,
    imageHeight: Int,
    isFrontCamera: Boolean
) {
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Pastikan ada hasil deteksi sebelum menggambar
        if (detectionResult == null || detectionResult.boundingBoxes.isEmpty()) {
            return@Canvas
        }

        detectionResult.boundingBoxes.forEachIndexed { index, box ->
            // Gunakan CameraPreviewTransformer untuk memetakan kotak ke koordinat tampilan
            val mappedBox = CameraPreviewTransformer.mapBoundingBoxToView(
                boundingBox = box,
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                viewWidth = canvasWidth,
                viewHeight = canvasHeight,
                isFrontCamera = isFrontCamera,
                expansionFactor = 0.7f,
            )

            // Menggambar kotak deteksi dengan warna hijau untuk kendaraan
            drawRect(
                color = Color.Green,
                topLeft = Offset(mappedBox.left.toFloat(), mappedBox.top.toFloat()),
                size = Size(
                    width = (mappedBox.right - mappedBox.left).toFloat(),
                    height = (mappedBox.bottom - mappedBox.top).toFloat()
                ),
                style = Stroke(width = 6f)
            )

            // Dapatkan label dan skor dari DetectionResult
            val label = detectionResult.labels.getOrElse(index) { "Unknown" }
            val score = detectionResult.scores.getOrElse(index) { 0.0f }
            val displayText = "$label: ${"%.2f".format(score)}"

            // Menggambar teks label dan skor
            drawText(
                textMeasurer = textMeasurer,
                text = displayText,
                topLeft = Offset(mappedBox.left.toFloat(), mappedBox.top.toFloat() - 30f),
                style = TextStyle(
                    color = Color.White,
                    fontSize = 12.sp,
                    background = Color.Black.copy(alpha = 0.5f)
                )
            )

            Log.d("VehicleOverlay", "Box #$index for '$label' mapped to $mappedBox (orig=${box})")
        }
    }
}