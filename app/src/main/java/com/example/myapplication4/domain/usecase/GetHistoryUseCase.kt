package com.example.myapplication4.domain.usecase

import com.example.myapplication4.data.repository.HistoryRepository
import com.example.myapplication4.data.model.AttendanceLog
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetHistoryUseCase @Inject constructor(
    private val repository: HistoryRepository
) {
    operator fun invoke(): Flow<List<AttendanceLog>> = repository.getHistory()
}