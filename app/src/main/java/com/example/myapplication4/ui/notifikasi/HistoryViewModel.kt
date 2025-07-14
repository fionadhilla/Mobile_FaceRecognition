package com.example.myapplication4.ui.notifikasi

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class HistoryItem(
    val name: String,
    val description: String
)

class HistoryViewModel : ViewModel() {
    private val _historyItems = MutableStateFlow(
        listOf(
            HistoryItem("Nama Placeholder", "Hadir pada 04/07/2025 jam 18.00"),
            HistoryItem("Nama Placeholder", "Hadir pada 04/07/2025 jam 18.00"),
            HistoryItem("Nama Placeholder", "Hadir pada 04/07/2025 jam 18.00"),
            HistoryItem("Nama Placeholder", "Hadir pada 04/07/2025 jam 18.00"),
            HistoryItem("Nama Placeholder", "Hadir pada 04/07/2025 jam 18.00")
        )
    )
    val historyItems: StateFlow<List<HistoryItem>> = _historyItems
}