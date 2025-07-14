package com.example.myapplication4.domain.usecase

import com.example.myapplication4.data.repository.LoginRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val repository: LoginRepository
) {
    fun execute(username: String, password: String): Boolean {
        return repository.validateUser(username, password)
    }
}