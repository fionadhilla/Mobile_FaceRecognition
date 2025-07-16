package com.example.myapplication4.face

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facedetector.FaceDetector
import com.google.mediapipe.tasks.vision.facedetector.FaceDetectorResult
import android.os.SystemClock

class MediaPipeFaceDetector(
    context: Context,
    private val onResult: (FaceDetectorResult) -> Unit,
    private val onError: (Exception) -> Unit
) {

    private val faceDetector: FaceDetector

    init {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("blaze_face_short_range.tflite")
            .build()

        val options = FaceDetector.FaceDetectorOptions.builder()
            .setBaseOptions(baseOptions)
            .setMinDetectionConfidence(0.5f)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setResultListener { result: FaceDetectorResult, _ ->
                onResult(result)
            }
            .setErrorListener { exception ->
                onError(exception)
            }
            .build()
        faceDetector = FaceDetector.createFromOptions(context, options)

    }

    fun detect(bitmap: Bitmap) {
        try {
            val mpImage = BitmapImageBuilder(bitmap).build()
            faceDetector.detectAsync(mpImage, SystemClock.uptimeMillis())
        } catch (e: Exception) {
            Log.e("MediaPipeFaceDetector", "Detection failed: ${e.message}")
            onError(e)
        }
    }

    fun close() {
        faceDetector.close()
    }
}