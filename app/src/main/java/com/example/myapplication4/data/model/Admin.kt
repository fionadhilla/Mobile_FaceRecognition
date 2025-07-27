package com.example.myapplication4.data.model

data class Admin(
    val id: String,
    val name: String,
    val email: String,
    val role: String = "admin, superadmin"
)