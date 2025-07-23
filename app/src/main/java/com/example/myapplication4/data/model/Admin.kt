package com.example.myapplication4.data.model

import java.time.LocalDateTime

data class Admin(
    val id: String,
    val name: String,
    val email: String,
    val role: String, // 'admin', 'superadmin'
)