package com.example.myapplication4.data.model

import java.time.LocalDateTime

data class AttendanceLog(
    val logId: Int,
    val userId: Int,
    val deviceId: Int?,
    val checkType: String,
    val confidence: Float,
    val timestamp: LocalDateTime
)
