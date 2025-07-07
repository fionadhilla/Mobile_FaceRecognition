package com.example.Mobile_FaceRecognition.data.repository

import android.graphics.Bitmap
import com.example.Mobile_FaceRecognition.domain.model.FaceRecognition

interface FaceRecognitionRepository {
    suspend fun recognizeFace(bitmap: Bitmap, isRegistering: Boolean): FaceRecognition?
    suspend fun registerFace(name: String, embedding: FloatArray): Boolean
}