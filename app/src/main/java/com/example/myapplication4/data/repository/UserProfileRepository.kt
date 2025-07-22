package com.example.myapplication4.data.repository

import com.example.myapplication4.data.model.Admin
import kotlinx.coroutines.flow.Flow

interface UserProfileRepository {
    fun getUserProfile(): Flow<Admin>
    suspend fun updateUserProfile(profile: Admin): Boolean
}