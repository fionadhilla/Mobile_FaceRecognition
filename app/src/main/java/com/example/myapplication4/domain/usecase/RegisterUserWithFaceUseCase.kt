package com.example.myapplication4.domain.usecase

import com.example.myapplication4.data.model.User
import com.example.myapplication4.data.repository.UserRepository
import javax.inject.Inject

class RegisterUserWithFaceUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(name: String, email: String, embeddings: ByteArray): Boolean {
        val user = User(name = name, email = email, embeddings = embeddings)
        return userRepository.saveUserWithFace(user)
    }
}