package com.example.myapplication4.face

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facedetector.FaceDetector
import com.google.mediapipe.tasks.vision.facedetector.FaceDetectorResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AddFaceDetector @Inject constructor(context: Context) {
    private val faceDetector: FaceDetector

    init {
        try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath("blaze_face_short_range.tflite")
                .build()

            val options = FaceDetector.FaceDetectorOptions.builder()
                .setBaseOptions(baseOptions)
                .setMinDetectionConfidence(0.4f)
                .setRunningMode(RunningMode.IMAGE)
                .build()
            faceDetector = FaceDetector.createFromOptions(context, options)
        } catch (e: Exception) {
            Log.e("AddFaceDetector", "Error initializing AddFaceDetector: ${e.message}")
            throw RuntimeException("Failed to initialize AddFaceDetector", e)
        }
    }

    fun detect(bitmap: Bitmap): FaceDetectorResult? {
        try {
            val mpImage = BitmapImageBuilder(bitmap).build()
            return faceDetector.detect(mpImage)
        } catch (e: Exception) {
            Log.e("AddFaceDetector", "Detection failed: ${e.message}")
            return null
        }
    }

    fun close() {
        faceDetector.close()
    }
}