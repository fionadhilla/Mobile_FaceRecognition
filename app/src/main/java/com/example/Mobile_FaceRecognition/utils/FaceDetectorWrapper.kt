package com.example.Mobile_FaceRecognition.presentation.utils
import android.content.Context
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions

class FaceDetectorWrapper(private val context: Context) {

    private var detector: FaceDetector? = null

    fun init() {
        detector?.close()

        val opts = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .enableTracking()
            .build()

        detector = FaceDetection.getClient(opts)
    }

    /**
     *  Proses 1 frame.
     *  @param image
     *  @param onSuccess
     *  @param onError
     */
    fun process(
        image: InputImage,
        onSuccess: (List<Face>) -> Unit,
        onError: (Exception) -> Unit = { e -> Log.e(TAG, "FaceDetector error", e) }
    ) {
        detector?.process(image)
            ?.addOnSuccessListener(onSuccess)
            ?.addOnFailureListener(onError)
    }

    fun raw(): FaceDetector = detector
        ?: throw IllegalStateException("Detector not initialized")

    fun close() {
        detector?.close()
        detector = null
    }

    fun recreate(): FaceDetector {
        val old = detector
        init()
        return old!!
    }

    companion object { private const val TAG = "FaceDetectorWrapper" }
}