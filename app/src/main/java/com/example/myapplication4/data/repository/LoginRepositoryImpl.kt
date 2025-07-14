package com.example.myapplication4.data.repository

import com.example.myapplication4.data.model.Admin
import javax.inject.Inject

class LoginRepositoryImpl @Inject constructor() : LoginRepository {
    private val dummyUsers = listOf(
        Admin(
            adminID = 1,
            username = "admin1",
            password = "admin123",
            email = "admin1@example.com"
        ),
        Admin(
            adminID = 2,
            username = "admin2",
            password = "sayaadmin123",
            email = "admin2@example.com"
        ),

    )

    override fun validateUser(username: String, password: String): Boolean {
        return dummyUsers.any { it.username == username && it.password == password }
    }
}