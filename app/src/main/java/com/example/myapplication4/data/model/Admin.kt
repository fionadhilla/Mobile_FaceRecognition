package com.example.myapplication4.data.model

data class Admin(
    val adminID: Int,
    val username: String,
    val password: String,
    val email: String,
    val role: String = "admin"
)