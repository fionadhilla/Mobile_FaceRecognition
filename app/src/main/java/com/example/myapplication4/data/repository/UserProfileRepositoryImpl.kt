package com.example.myapplication4.data.repository

import com.example.myapplication4.data.model.User
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserProfileRepositoryImpl @Inject constructor() : UserProfileRepository {

    private var _currentUserProfile = MutableStateFlow(
        User(
            fullName = "Fionadhilla Gustriani",
            email = "fiona.dhilla@gmail.com",
            phoneNumber = "081234567890"
        )
    )

    override fun getUserProfile(): Flow<User> = _currentUserProfile.asStateFlow()

    override suspend fun updateUserProfile(profile: User): Boolean {
        delay(500)
        _currentUserProfile.value = profile
        return true
    }
}