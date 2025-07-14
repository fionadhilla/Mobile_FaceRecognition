package com.example.myapplication4.data.repository

import com.example.myapplication4.data.model.AttendanceLog
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    fun getHistory(): Flow<List<AttendanceLog>>
}