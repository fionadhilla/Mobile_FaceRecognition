package com.example.Mobile_FaceRecognition.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.example.Mobile_FaceRecognition.data.local.TFLiteFaceRecognitionSource
import com.example.Mobile_FaceRecognition.data.remote.FaceRecognitionWebSocketService
import com.example.Mobile_FaceRecognition.domain.model.FaceRecognition
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import okhttp3.WebSocketListener
import okhttp3.WebSocket
import okhttp3.Response
import okio.ByteString

class FaceRecognitionRepositoryImpl(
    private val tfliteSource: TFLiteFaceRecognitionSource,
    private val webSocketService: FaceRecognitionWebSocketService
) : FaceRecognitionRepository, FaceRecognitionWebSocketService.WebSocketEventListener {

    private val _recognitionResult = MutableSharedFlow<FaceRecognitionEvent>()
    val recognitionResult: SharedFlow<FaceRecognitionEvent> = _recognitionResult

    init {
        webSocketService.initWebSocket()
    }

    override suspend fun recognizeFace(bitmap: Bitmap, isRegistering: Boolean): FaceRecognition? {
        val result = tfliteSource.recognizeImage(bitmap, isRegistering)
        if (result.embedding != null && !isRegistering) {
            try {
                val jsonEmbedding = JSONArray()
                for (val_ in result.embedding!!) {
                    jsonEmbedding.put(val_)
                }

                val jsonRequest = JSONObject().apply {
                    put("type", "recognize_face")
                    put("embedding", jsonEmbedding)
                }
                webSocketService.sendMessage(jsonRequest.toString())
            } catch (e: JSONException) {
                Log.e("FaceRecognitionRepository", "Error creating recognition request JSON: ${e.message}")
            }
        }
        return result
    }

    override suspend fun registerFace(name: String, embedding: FloatArray): Boolean {
        return try {
            val jsonEmbedding = JSONArray()
            for (val_ in embedding) {
                jsonEmbedding.put(val_)
            }

            val jsonRequest = JSONObject().apply {
                put("type", "insert_face")
                put("name", name)
                put("embedding", jsonEmbedding)
            }
            webSocketService.sendMessage(jsonRequest.toString())
            true
        } catch (e: JSONException) {
            Log.e("FaceRecognitionRepository", "Error creating registration request JSON: ${e.message}")
            false
        }
    }

    //region WebSocketEventListener implementation
    override fun onConnected() {
        // Handle connected event if needed, e.g., update UI state
    }

    override fun onRecognitionResult(match: Boolean, name: String, distance: Double) {
        // Emit event to ViewModel
        _recognitionResult.tryEmit(FaceRecognitionEvent.RecognitionSuccess(match, name, distance))
    }

    override fun onMessageReceived(message: String) {
        _recognitionResult.tryEmit(FaceRecognitionEvent.Message(message))
    }

    override fun onError(message: String) {
        _recognitionResult.tryEmit(FaceRecognitionEvent.Error(message))
    }
    //endregion

    fun close() {
        webSocketService.closeWebSocket()
    }
}

sealed class FaceRecognitionEvent {
    data class RecognitionSuccess(val match: Boolean, val name: String, val distance: Double) : FaceRecognitionEvent()
    data class Message(val message: String) : FaceRecognitionEvent()
    data class Error(val errorMessage: String) : FaceRecognitionEvent()
}