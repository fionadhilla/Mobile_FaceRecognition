package com.example.myapplication4.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF

data class DetectionResult(
    val boundingBoxes: List<RectF>,
    val labels: List<String>,
    val scores: List<Float>
)
