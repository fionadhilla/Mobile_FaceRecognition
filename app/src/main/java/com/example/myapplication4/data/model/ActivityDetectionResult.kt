package com.example.myapplication4.data.model

import android.graphics.RectF

data class ActivityDetectionResult(
    val boundingBox: RectF,
    val label: String,
    val score: Float
)
