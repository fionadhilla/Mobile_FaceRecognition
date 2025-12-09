package com.example.myapplication4.data.model

import android.graphics.RectF

data class ThreatDetectionResult(
    val boundingBox: RectF,
    val label: String,
    val score: Float
)
