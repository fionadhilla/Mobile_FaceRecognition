package com.example.myapplication4.domain.usecase

import com.example.myapplication4.data.model.Admin
import com.example.myapplication4.data.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetUserProfileUseCase @Inject constructor(
    private val repository: UserProfileRepository
) {
    operator suspend fun invoke(adminId: String): Flow<Admin> {
        return repository.getProfile(adminId).map { result ->
            result.getOrThrow()
        }
    }
}