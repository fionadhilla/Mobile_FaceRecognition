package com.example.Mobile_FaceRecognition.DependencyInjector

import android.annotation.SuppressLint
import android.content.Context
import com.example.Mobile_FaceRecognition.data.local.TFLiteFaceRecognitionSource
import com.example.Mobile_FaceRecognition.data.remote.FaceRecognitionWebSocketService
import com.example.Mobile_FaceRecognition.data.repository.FaceRecognitionRepositoryImpl
import com.example.Mobile_FaceRecognition.domain.usecase.RecognizeFaceUseCase
import com.example.Mobile_FaceRecognition.domain.usecase.RegisterFaceUseCase
import com.example.Mobile_FaceRecognition.presentation.utils.FaceDetectorWrapper
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions

object AppModule {
    private const val WEBSOCKET_URL = "ws://10.60.225.12:3000"
    private const val TF_OD_API_INPUT_SIZE = 160
    private const val TFLITE_MODEL_FILENAME = "facenet.tflite"

    private var tfliteSourceInstance: TFLiteFaceRecognitionSource? = null
    private var faceDetectorInstance: FaceDetector? = null
    @SuppressLint("StaticFieldLeak")
    private var faceDetectorWrapper: FaceDetectorWrapper? = null

    fun provideFaceRecognitionWebSocketService(listener: FaceRecognitionWebSocketService.WebSocketEventListener): FaceRecognitionWebSocketService {
        return FaceRecognitionWebSocketService(WEBSOCKET_URL, listener)
    }

    fun provideTFLiteFaceRecognitionSource(context: Context): TFLiteFaceRecognitionSource {
        if (tfliteSourceInstance == null) {
            tfliteSourceInstance = try {
                TFLiteFaceRecognitionSource.create(
                    context.assets,
                    TFLITE_MODEL_FILENAME,
                    TF_OD_API_INPUT_SIZE,
                    false
                )
            } catch (e: Exception) {
                throw RuntimeException("Failed to initialize TFLiteFaceRecognitionSource", e)
            }
        }
        return tfliteSourceInstance!!
    }

    fun provideFaceRecognitionRepository(
        tfliteSource: TFLiteFaceRecognitionSource,
        webSocketService: FaceRecognitionWebSocketService
    ): FaceRecognitionRepositoryImpl {
        return FaceRecognitionRepositoryImpl(tfliteSource, webSocketService)
    }

    fun provideRecognizeFaceUseCase(repository: FaceRecognitionRepositoryImpl): RecognizeFaceUseCase {
        return RecognizeFaceUseCase(repository)
    }

    fun provideRegisterFaceUseCase(repository: FaceRecognitionRepositoryImpl): RegisterFaceUseCase {
        return RegisterFaceUseCase(repository)
    }

    fun provideFaceDetector(context: Context): FaceDetectorWrapper {
        if (faceDetectorWrapper == null) {
            faceDetectorWrapper = FaceDetectorWrapper(context).apply { init() }
        }
        return faceDetectorWrapper!!
    }

    fun clearFaceDetector() {
        faceDetectorInstance = null
    }
}