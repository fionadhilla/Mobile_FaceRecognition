package com.example.myapplication4.data.repository

interface LoginRepository {
    suspend fun loginUser(email: String, password: String): Result<String>
}