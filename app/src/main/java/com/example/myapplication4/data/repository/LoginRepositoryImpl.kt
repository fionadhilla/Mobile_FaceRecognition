package com.example.myapplication4.data.repository

import com.example.myapplication4.data.model.User
import javax.inject.Inject

class LoginRepositoryImpl @Inject constructor() : LoginRepository {
    private val dummyUsers = listOf(
        User("admin", "admin123"),
        User("user", "user123")
    )

    override fun validateUser(username: String, password: String): Boolean {
        return dummyUsers.any { it.username == username && it.password == password }
    }
}