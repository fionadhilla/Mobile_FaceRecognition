package com.example.myapplication4.data.repository

import com.example.myapplication4.data.api.ApiResult
import com.example.myapplication4.data.api.WebSocketClient
import com.example.myapplication4.data.database.dao.PendingSyncDao
import com.example.myapplication4.data.database.dao.UserDao
import com.example.myapplication4.data.model.FaceVerificationResult
import com.example.myapplication4.data.model.PendingSyncData
import com.example.myapplication4.data.model.User
import com.example.myapplication4.domain.utils.NetworkUtils
import com.example.myapplication4.face.FaceUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

interface FaceRepository {
    suspend fun registerUserWithFace(user: User): ApiResult<Boolean>
    suspend fun verifyFace(embeddings: FloatArray): ApiResult<FaceVerificationResult>
    fun getLocalUsers(): Flow<List<User>>
    suspend fun markUserForSync(localId: Int, action: String)
    suspend fun clearPendingSyncs()
    fun getPendingSyncs(): Flow<List<PendingSyncData>>
    suspend fun deleteLocalUser(localId: Int)
}

@Singleton
class FaceRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val pendingSyncDao: PendingSyncDao,
    private val webSocketClient: WebSocketClient,
    private val networkUtils: NetworkUtils
) : FaceRepository {

    override suspend fun registerUserWithFace(user: User): ApiResult<Boolean> {
        return if (networkUtils.isOnline()) {
            val result = webSocketClient.sendInsertFaceRequest(user)
            if (result is ApiResult.Success && result.data) {
                // If successful from backend, save to local DB and don't mark for pending sync
                userDao.insertUser(user)
                ApiResult.Success(true)
            } else {
                // If online but failed, save to local DB and mark for pending sync
                val localId = userDao.insertUser(user).toInt()
                pendingSyncDao.insertPendingSync(PendingSyncData(userLocalId = localId, action = "ADD"))
                ApiResult.Error(Exception("Failed to register face online. Added to pending sync."), "Online registration failed.")
            }
        } else {
            val localId = userDao.insertUser(user).toInt()
            pendingSyncDao.insertPendingSync(PendingSyncData(userLocalId = localId, action = "ADD"))
            ApiResult.Success(true) // Consider this a success for offline operation
        }
    }

    override suspend fun verifyFace(embeddings: FloatArray): ApiResult<FaceVerificationResult> {
        return if (networkUtils.isOnline()) {
            webSocketClient.sendVerificationRequest(embeddings)
        } else {
            // Fallback to local verification if offline
            val storedUsers = userDao.getAllUsers().first()
            if (storedUsers.isEmpty()) {
                return ApiResult.Success(FaceVerificationResult(isMatch = false, matchedUser = null, distance = -1.0f))
            }

            var closestMatch: User? = null
            var minDistance = Float.MAX_VALUE

            for (user in storedUsers) {
                val storedEmbeddingsFloat = user.embeddings
                val distance = FaceUtils.calculateEuclideanDistance(embeddings, storedEmbeddingsFloat)

                if (distance < minDistance) {
                    minDistance = distance
                    closestMatch = user
                }
            }
            val isMatch = closestMatch != null && minDistance < FaceUtils.RECOGNITION_THRESHOLD
            ApiResult.Success(FaceVerificationResult(isMatch, closestMatch, minDistance))
        }
    }

    override fun getLocalUsers(): Flow<List<User>> {
        return userDao.getAllUsers()
    }

    override suspend fun markUserForSync(localId: Int, action: String) {
        pendingSyncDao.insertPendingSync(PendingSyncData(userLocalId = localId, action = action))
    }

    override suspend fun clearPendingSyncs() {
        pendingSyncDao.clearAllPendingSyncs()
    }

    override fun getPendingSyncs(): Flow<List<PendingSyncData>> {
        return pendingSyncDao.getAllPendingSyncs()
    }

    override suspend fun deleteLocalUser(localId: Int) {
        userDao.deleteUserByLocalId(localId)
    }
}