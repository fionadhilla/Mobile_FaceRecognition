package com.example.myapplication4.ui.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication4.domain.usecase.LoginUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val loginStateViewModel: LoginStateViewModel
) : ViewModel() {
    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password

    private val _isLoggingIn = MutableStateFlow(false)
    val isLoggingIn: StateFlow<Boolean> = _isLoggingIn

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError

    private val _jwtToken = MutableStateFlow<String?>(null) // Untuk menyimpan JWT
    val jwtToken: StateFlow<String?> = _jwtToken

    fun onEmailChange(newEmail: String) {
        _email.value = newEmail
        _loginError.value = null
    }

    fun onPasswordChange(newPassword: String) {
        _password.value = newPassword
        _loginError.value = null
    }

    fun login(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoggingIn.value = true
            _loginError.value = null
            _jwtToken.value = null

            Log.d("LoginViewModel", "Attempting login for email: ${_email.value}")

            val result = loginUseCase.execute(_email.value, _password.value)

            result.onSuccess { token ->
                _jwtToken.value = token
                loginStateViewModel.login()
                onSuccess()
                Log.d("LoginViewModel", "Login successful. JWT Token: $token")
            }.onFailure { exception ->
                _loginError.value = exception.message ?: "Login failed"
                Log.e("LoginViewModel", "Login failed: ${_loginError.value}")
            }
            _isLoggingIn.value = false
        }
    }
}