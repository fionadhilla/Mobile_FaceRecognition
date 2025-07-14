package com.example.myapplication4.ui.notifikasi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication4.data.model.AttendanceLog
import com.example.myapplication4.domain.usecase.GetHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getHistoryUseCase: GetHistoryUseCase
) : ViewModel() {

    private val _historyItems = MutableStateFlow<List<AttendanceLog>>(emptyList())
    val historyItems: StateFlow<List<AttendanceLog>> = _historyItems

    init {
        viewModelScope.launch {
            getHistoryUseCase().collect {
                _historyItems.value = it
            }
        }
    }
}