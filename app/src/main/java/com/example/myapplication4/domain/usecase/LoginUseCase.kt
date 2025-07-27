package com.example.myapplication4.domain.usecase

import com.example.myapplication4.data.repository.LoginRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val repository: LoginRepository
) {
    suspend fun execute(email: String, password: String): Result<String> {
        return repository.loginUser(email, password)
    }
}