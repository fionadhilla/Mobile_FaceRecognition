package com.example.myapplication4.data.repository

import com.example.myapplication4.data.model.Admin
import kotlinx.coroutines.flow.Flow

interface UserProfileRepository {
    suspend fun getProfile(adminId: String): Flow<Result<Admin>>
    suspend fun updateProfile(adminId: String, name: String, email: String): Flow<Result<Boolean>>
}