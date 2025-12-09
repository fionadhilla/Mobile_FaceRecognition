package com.example.myapplication4.ui.login

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LoginStateViewModel : ViewModel() {

    private val _currentAdminId = MutableStateFlow<String?>(null)
    val currentAdminId: StateFlow<String?> = _currentAdminId.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    fun login() {
        _isLoggedIn.value = true
    }

    fun setLoggedInAdmin(adminId: String) {
        _currentAdminId.value = adminId
        _isLoggedIn.value = true
    }

    fun logout() {
        _currentAdminId.value = null
        _isLoggedIn.value = false
    }
}