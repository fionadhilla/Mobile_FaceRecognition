package com.example.myapplication4.domain.usecase

import com.example.myapplication4.data.model.User
import com.example.myapplication4.data.repository.UserProfileRepository
import javax.inject.Inject

class UpdateUserProfileUseCase @Inject constructor(
    private val repository: UserProfileRepository
) {
    suspend operator fun invoke(profile: User): Boolean {
        return repository.updateUserProfile(profile)
    }
}