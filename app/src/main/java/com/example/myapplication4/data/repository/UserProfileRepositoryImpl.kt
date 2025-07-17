package com.example.myapplication4.data.repository

import com.example.myapplication4.data.model.UserProfile
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserProfileRepositoryImpl @Inject constructor() : UserProfileRepository {
    private var currentUserProfile = UserProfile(
        name = "Fionadhilla Gustriani",
        email = "fiona.dhilla@gmail.com",
        phoneNumber = "081234567890"
    )

    override fun getUserProfile(): Flow<UserProfile> = flow { // Ubah return type
        emit(currentUserProfile)
    }

    override suspend fun updateUserProfile(profile: UserProfile): Boolean { // Ubah parameter type
        delay(500)
        currentUserProfile = profile
        return true
    }
}