package com.example.myapplication4.ui.attendace_confirmation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication4.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AttendanceStatus {
    CHECK_IN,
    CHECK_OUT
}

class AttendanceConfirmationViewModel : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user

    private val _timestamp = MutableStateFlow("")
    val timestamp: StateFlow<String> = _timestamp

    private val _attendanceStatus = MutableStateFlow(AttendanceStatus.CHECK_IN)
    val attendanceStatus: StateFlow<AttendanceStatus> = _attendanceStatus

    fun setAttendanceData(user: User) {
        viewModelScope.launch {
            _user.value = user
            _timestamp.value = SimpleDateFormat("hh:mm a, MMMM d, yyyy", Locale.getDefault()).format(Date())
        }
    }

    fun setAttendanceStatus(status: AttendanceStatus) {
        _attendanceStatus.value = status
    }
}