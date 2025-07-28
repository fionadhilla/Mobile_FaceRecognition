package com.example.myapplication4.data.model

data class FaceVerificationResult(
    val isMatch: Boolean,
    val matchedUser: User?,
    val distance: Float = -1.0f
)
