package com.example.myapplication4.domain.usecase

import com.example.myapplication4.data.model.Admin
import com.example.myapplication4.data.repository.UserProfileRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class UpdateUserProfileUseCase @Inject constructor(
    private val repository: UserProfileRepository
) {
    suspend operator fun invoke(profile: Admin ): Boolean {
        val adminId = profile.id
        val username = profile.name
        val email = profile.email

        return repository.updateProfile(adminId, username, email).first().getOrThrow()
    }
}