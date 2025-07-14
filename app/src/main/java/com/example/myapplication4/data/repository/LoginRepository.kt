package com.example.myapplication4.data.repository

interface LoginRepository {
    fun validateUser(username: String, password: String): Boolean
}