package com.example.myapplication4.data.repository

import com.example.myapplication4.data.model.Admin // Changed from User to Admin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import java.time.LocalDateTime // Import LocalDateTime

@Singleton
class UserProfileRepositoryImpl @Inject constructor() : UserProfileRepository {

    private var _currentUserProfile = MutableStateFlow(
        Admin( // Changed from User to Admin
            id = 1,
            name = "Fionadhilla Gustriani", // Changed from fullName to name
            password = "hashed_password_example", // Added for Admin
            email = "fiona.dhilla@gmail.com",
            role = "admin", // Added for Admin
            createdAt = LocalDateTime.now() // Added for Admin
        )
    )

    override fun getUserProfile(): Flow<Admin> = _currentUserProfile.asStateFlow() // Changed from User to Admin

    override suspend fun updateUserProfile(profile: Admin): Boolean { // Changed from User to Admin
        delay(500)
        _currentUserProfile.value = profile
        return true
    }
}