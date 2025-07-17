package com.example.myapplication4.data.repository

import com.example.myapplication4.data.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface UserProfileRepository {
    fun getUserProfile(): Flow<UserProfile>
    suspend fun updateUserProfile(profile: UserProfile): Boolean
}