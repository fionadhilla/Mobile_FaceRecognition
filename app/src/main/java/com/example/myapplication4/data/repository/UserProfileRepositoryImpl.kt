package com.example.myapplication4.data.repository

import android.util.Log
import com.example.myapplication4.data.api.WebSocketClient
import com.example.myapplication4.data.model.Admin
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class UserProfileRepositoryImpl @Inject constructor(
    private val webSocketAuthService: WebSocketClient
) : UserProfileRepository {

    override suspend fun getProfile(adminId: String): Flow<Result<Admin>> = callbackFlow {
        Log.d("UserProfileRepository", "Requesting profile for adminId: $adminId")
        webSocketAuthService.onProfileReceived = { admin, errorMessage ->
            if (admin != null) {
                trySend(Result.success(admin))
                Log.d("UserProfileRepository", "Profile received: $admin")
            } else {
                trySend(Result.failure(Exception(errorMessage ?: "Unknown error fetching profile")))
                Log.e("UserProfileRepository", "Failed to receive profile: $errorMessage")
            }
        }
        webSocketAuthService.requestProfile(adminId)
        awaitClose {
            webSocketAuthService.onProfileReceived = null
        }
    }

    override suspend fun updateProfile(adminId: String, name: String, email: String): Flow<Result<Boolean>> = callbackFlow {
        Log.d("UserProfileRepository", "Updating profile for adminId: $adminId")
        webSocketAuthService.onProfileUpdateResult = { success, message ->
            if (success) {
                trySend(Result.success(true))
                Log.d("UserProfileRepository", "Profile update successful: $message")
            } else {
                trySend(Result.failure(Exception(message ?: "Unknown error updating profile")))
                Log.e("UserProfileRepository", "Profile update failed: $message")
            }
        }
        webSocketAuthService.updateProfile(adminId, name, email)
        awaitClose {
            // Optional: Clean up listeners or resources if needed when the flow is closed
            webSocketAuthService.onProfileUpdateResult = null
        }
    }
}