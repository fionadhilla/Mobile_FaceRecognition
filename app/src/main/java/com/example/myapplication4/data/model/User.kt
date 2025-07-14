package com.example.myapplication4.data.model

import java.time.LocalDateTime

data class User (
    val userId: Int,
    val name: String,
    val email: String,
    val phone: String,
    val embeddings: List<Float>,
    val role: String = "User",
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)


