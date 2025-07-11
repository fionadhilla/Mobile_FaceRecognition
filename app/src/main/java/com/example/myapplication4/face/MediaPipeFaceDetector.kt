package com.example.myapplication4.face

import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facedetector.FaceDetector
import com.google.mediapipe.tasks.vision.facedetector.FaceDetectorResult

class MediaPipeFaceDetector(context: Context) {
    private val faceDetector: FaceDetector =
        FaceDetector.createFromFile(context, "blaze_face_short_range.tflite")

    fun detect(bitmap: Bitmap): FaceDetectorResult? {
        val mpImage = BitmapImageBuilder(bitmap).build()
        return faceDetector.detect(mpImage)
    }

    fun close() {
        faceDetector.close()
    }
}