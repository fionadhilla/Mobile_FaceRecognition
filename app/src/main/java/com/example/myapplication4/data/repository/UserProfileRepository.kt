package com.example.myapplication4.data.repository

import com.example.myapplication4.data.model.User
import kotlinx.coroutines.flow.Flow

interface UserProfileRepository {
    fun getUserProfile(): Flow<User>
    suspend fun updateUserProfile(profile: User): Boolean
}