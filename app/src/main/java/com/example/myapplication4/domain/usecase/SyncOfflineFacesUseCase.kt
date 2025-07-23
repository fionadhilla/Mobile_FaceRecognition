package com.example.myapplication4.domain.usecase

import android.util.Log
import com.example.myapplication4.data.api.ApiResult
import com.example.myapplication4.data.api.WebSocketClient
import com.example.myapplication4.data.model.PendingSyncData
import com.example.myapplication4.data.model.User
import com.example.myapplication4.data.repository.FaceRepository
import com.example.myapplication4.domain.utils.NetworkUtils
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class SyncOfflineFacesUseCase @Inject constructor(
    private val faceRepository: FaceRepository,
    private val webSocketClient: WebSocketClient,
    private val networkUtils: NetworkUtils
) {
    suspend operator fun invoke(): Boolean {
        if (!networkUtils.isOnline()) {
            Log.d("SyncOfflineFacesUseCase", "Device is offline, skipping sync.")
            return false
        }

        Log.d("SyncOfflineFacesUseCase", "Waiting for WebSocket client to be connected...")
        val isWebSocketConnected = webSocketClient.isConnected.first { it }
        if (!isWebSocketConnected) {
            Log.e("SyncOfflineFacesUseCase", "WebSocket client is not connected, cannot sync online.")
            return false
        }


        val pendingSyncs = faceRepository.getPendingSyncs().first()
        if (pendingSyncs.isEmpty()) {
            Log.d("SyncOfflineFacesUseCase", "No pending sync operations.")
            return true
        }

        var allSyncedSuccessfully = true

        for (syncItem in pendingSyncs) {
            val user = faceRepository.getLocalUsers().first().find { it.localId == syncItem.userLocalId }

            if (user == null) {
                faceRepository.deleteLocalUser(syncItem.userLocalId)
                faceRepository.markUserForSync(syncItem.userLocalId, "DELETE")
                continue
            }

            when (syncItem.action) {
                "ADD" -> {
                    Log.d("SyncOfflineFacesUseCase", "Attempting to sync ADD for user ${user.name}")
                    val result = webSocketClient.sendInsertFaceRequest(user)
                    if (result is ApiResult.Success && result.data) {
                        Log.d("SyncOfflineFacesUseCase", "Successfully synced ADD for user ${user.name}")
                        faceRepository.deleteLocalUser(syncItem.userLocalId)
                        faceRepository.clearPendingSyncs()
                    } else if (result is ApiResult.Error) {
                        Log.e("SyncOfflineFacesUseCase", "Failed to sync ADD for user ${user.name}: ${result.message}")
                        allSyncedSuccessfully = false
                    }
                }
                "DELETE" -> {
                    Log.d("SyncOfflineFacesUseCase", "Attempting to sync DELETE for user ${user.name}")
                    faceRepository.clearPendingSyncs()
                }
            }
        }
        return allSyncedSuccessfully
    }
}