package com.example.myapplication4.domain.usecase

import com.example.myapplication4.data.model.UserProfile
import com.example.myapplication4.data.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetUserProfileUseCase @Inject constructor(
    private val repository: UserProfileRepository
) {
    operator fun invoke(): Flow<UserProfile> {
        return repository.getUserProfile()
    }
}