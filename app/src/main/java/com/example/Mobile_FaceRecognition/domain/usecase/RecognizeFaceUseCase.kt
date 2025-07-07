package com.example.Mobile_FaceRecognition.domain.usecase

import android.graphics.Bitmap
import com.example.Mobile_FaceRecognition.data.repository.FaceRecognitionRepository
import com.example.Mobile_FaceRecognition.domain.model.FaceRecognition

class RecognizeFaceUseCase(private val repository: FaceRecognitionRepository) {
    suspend operator fun invoke(bitmap: Bitmap, isRegistering: Boolean): FaceRecognition? {
        return repository.recognizeFace(bitmap, isRegistering)
    }
}