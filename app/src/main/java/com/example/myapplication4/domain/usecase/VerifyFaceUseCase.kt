package com.example.myapplication4.domain.usecase

import com.example.myapplication4.data.model.User
import com.example.myapplication4.data.model.FaceVerificationResult
import com.example.myapplication4.data.repository.UserRepository
import com.example.myapplication4.face.FaceUtils
import com.example.myapplication4.face.FaceUtils.RECOGNITION_THRESHOLD
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class VerifyFaceUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(newFaceEmbeddings: ByteArray): FaceVerificationResult {
        val storedUsers = userRepository.getAllUsers().first()

        if (storedUsers.isEmpty()) {
            return FaceVerificationResult(isMatch = false, matchedUser = null, distance = -1.0f)
        }

        val newEmbeddingsFloat = FaceUtils.byteArrayToFloatArray(newFaceEmbeddings)

        var closestMatch: User? = null
        var minDistance = Float.MAX_VALUE

        for (user in storedUsers) {
            val storedEmbeddingsFloat = FaceUtils.byteArrayToFloatArray(user.embeddings)
            val distance = FaceUtils.calculateEuclideanDistance(newEmbeddingsFloat, storedEmbeddingsFloat)

            if (distance < minDistance) {
                minDistance = distance
                closestMatch = user
            }
        }
        val isMatch = closestMatch != null && minDistance < RECOGNITION_THRESHOLD

        return FaceVerificationResult(isMatch, closestMatch, minDistance)
    }
}