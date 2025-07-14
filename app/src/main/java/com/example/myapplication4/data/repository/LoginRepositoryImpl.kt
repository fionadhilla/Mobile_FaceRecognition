package com.example.myapplication4.data.repository

import com.example.myapplication4.data.model.Admin
import javax.inject.Inject

class LoginRepositoryImpl @Inject constructor() : LoginRepository {
    private val dummyUsers = listOf(
        Admin("admin", "admin123"),
        Admin("user", "user123")
    )

    override fun validateUser(username: String, password: String): Boolean {
        return dummyUsers.any { it.username == username && it.password == password }
    }
}