package com.example.myapplication4.domain.usecase

import com.example.myapplication4.data.model.User
import javax.inject.Inject
import com.example.myapplication4.data.api.ApiResult
import com.example.myapplication4.data.repository.FaceRepository

class RegisterUserWithFaceUseCase @Inject constructor(
    private val faceRepository: FaceRepository
) {
    suspend operator fun invoke(name: String, email: String, phone: String, embeddings: FloatArray): ApiResult<Boolean> {
        val user = User(name = name, email = email, phone = phone, embeddings = embeddings)
        return faceRepository.registerUserWithFace(user)
    }
}