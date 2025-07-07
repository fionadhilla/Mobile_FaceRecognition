package com.example.Mobile_FaceRecognition.domain.usecase

import com.example.Mobile_FaceRecognition.data.repository.FaceRecognitionRepository

class RegisterFaceUseCase(private val repository: FaceRecognitionRepository) {
    suspend operator fun invoke(name: String, embedding: FloatArray): Boolean {
        return repository.registerFace(name, embedding)
    }
}