package com.example.myapplication4.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF

// Data class untuk menampung hasil deteksi dari model kustom
data class DetectionResult(
    val boundingBoxes: List<RectF>,
    val labels: List<String>,
    val scores: List<Float>
)
