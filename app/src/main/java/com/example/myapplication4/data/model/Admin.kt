package com.example.myapplication4.data.model

import java.time.LocalDateTime

data class Admin(
    val id: Int,
    val username: String,
    val password: String,
    val email: String,
    val role: String, // 'admin', 'superadmin'
    val createdAt: LocalDateTime
)