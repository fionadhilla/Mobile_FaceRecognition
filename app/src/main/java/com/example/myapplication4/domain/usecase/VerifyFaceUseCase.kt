package com.example.myapplication4.domain.usecase

import com.example.myapplication4.data.model.FaceVerificationResult
import javax.inject.Inject
import com.example.myapplication4.data.api.ApiResult
import com.example.myapplication4.data.repository.FaceRepository

class VerifyFaceUseCase @Inject constructor(
    private val faceRepository: FaceRepository
) {
    suspend operator fun invoke(newFaceEmbeddings: FloatArray): ApiResult<FaceVerificationResult> {
        return faceRepository.verifyFace(newFaceEmbeddings)
    }
}