package com.example.myapplication4.data.repository

import com.example.myapplication4.data.model.AttendanceLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.LocalDateTime

class HistoryRepositoryImpl : HistoryRepository {
    override fun getHistory(): Flow<List<AttendanceLog>> = flow {
        emit(
            listOf(
                AttendanceLog(
                    logId = 1,
                    userId = 1001,
                    deviceId = 501,
                    checkType = "IN",
                    confidence = 0.97f,
                    timestamp = LocalDateTime.of(2025, 7, 14, 8, 0)
                ),
                AttendanceLog(
                    logId = 2,
                    userId = 1001,
                    deviceId = 501,
                    checkType = "OUT",
                    confidence = 0.93f,
                    timestamp = LocalDateTime.of(2025, 7, 14, 17, 0)
                ),
                AttendanceLog(
                    logId = 3,
                    userId = 1001,
                    deviceId = 501,
                    checkType = "IN",
                    confidence = 0.95f,
                    timestamp = LocalDateTime.of(2025, 7, 13, 7, 58)
                ),
                AttendanceLog(
                    logId = 4,
                    userId = 1001,
                    deviceId = 501,
                    checkType = "IN",
                    confidence = 0.97f,
                    timestamp = LocalDateTime.of(2025, 7, 14, 8, 0)
                ),
                AttendanceLog(
                    logId = 5,
                    userId = 1001,
                    deviceId = 501,
                    checkType = "IN",
                    confidence = 0.97f,
                    timestamp = LocalDateTime.of(2025, 7, 14, 8, 0)
                ),
                AttendanceLog(
                    logId = 6,
                    userId = 1001,
                    deviceId = 501,
                    checkType = "IN",
                    confidence = 0.97f,
                    timestamp = LocalDateTime.of(2025, 7, 14, 8, 0)
                ),
                AttendanceLog(
                    logId = 7,
                    userId = 1001,
                    deviceId = 501,
                    checkType = "IN",
                    confidence = 0.97f,
                    timestamp = LocalDateTime.of(2025, 7, 14, 8, 0)
                ),
                AttendanceLog(
                    logId = 8,
                    userId = 1001,
                    deviceId = 501,
                    checkType = "IN",
                    confidence = 0.97f,
                    timestamp = LocalDateTime.of(2025, 7, 14, 8, 0)
                ),
                AttendanceLog(
                    logId = 9,
                    userId = 1001,
                    deviceId = 501,
                    checkType = "IN",
                    confidence = 0.97f,
                    timestamp = LocalDateTime.of(2025, 7, 14, 8, 0)
                ),
                AttendanceLog(
                    logId = 10,
                    userId = 1001,
                    deviceId = 501,
                    checkType = "IN",
                    confidence = 0.97f,
                    timestamp = LocalDateTime.of(2025, 7, 14, 8, 0)
                ),
                AttendanceLog(
                    logId = 11,
                    userId = 1001,
                    deviceId = 501,
                    checkType = "IN",
                    confidence = 0.97f,
                    timestamp = LocalDateTime.of(2025, 7, 14, 8, 0)
                ),
                AttendanceLog(
                    logId = 12,
                    userId = 1001,
                    deviceId = 501,
                    checkType = "IN",
                    confidence = 0.97f,
                    timestamp = LocalDateTime.of(2025, 7, 14, 8, 0)
                ),
                AttendanceLog(
                    logId = 13,
                    userId = 1001,
                    deviceId = 501,
                    checkType = "IN",
                    confidence = 0.97f,
                    timestamp = LocalDateTime.of(2025, 7, 14, 8, 0)
                ),
                AttendanceLog(
                    logId = 14,
                    userId = 1001,
                    deviceId = 501,
                    checkType = "IN",
                    confidence = 0.97f,
                    timestamp = LocalDateTime.of(2025, 7, 14, 8, 0)
                ),
                AttendanceLog(
                    logId = 15,
                    userId = 1001,
                    deviceId = 501,
                    checkType = "IN",
                    confidence = 0.97f,
                    timestamp = LocalDateTime.of(2025, 7, 14, 8, 0)
                ),
            )
        )
    }
}