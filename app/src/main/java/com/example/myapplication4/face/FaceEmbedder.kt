package com.example.myapplication4.face

import android.graphics.Bitmap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FaceEmbedder @Inject constructor(
    private val faceNetModel: FaceNetModel
) {
    fun getEmbeddings(faceBitmap: Bitmap): ByteArray? {
        return faceNetModel.getFaceEmbedding(faceBitmap)
    }
}