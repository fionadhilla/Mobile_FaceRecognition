package com.example.myapplication4.data.api

import android.util.Log
import com.example.myapplication4.data.model.FaceVerificationResult
import com.example.myapplication4.data.model.User
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebSocketClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {
    private var webSocket: WebSocket? = null
    private var currentUrl: String? = null
    private val _incomingMessages = Channel<String>(Channel.UNLIMITED)
    val incomingMessages: Flow<String> = _incomingMessages.receiveAsFlow()

    private val _isConnected = MutableStateFlow<Boolean>(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    fun connect(url: String) {
        currentUrl = url
        val request = Request.Builder().url(url).build()
        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WebSocketClient", "Connected to WebSocket: ${response.message}")
                _incomingMessages.trySend("Connected")
                _isConnected.value = true
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("WebSocketClient", "Received text message: $text")
                _incomingMessages.trySend(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                Log.d("WebSocketClient", "Received byte message: ${bytes.hex()}")

            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WebSocketClient", "Closing WebSocket: $code / $reason")
                _incomingMessages.trySend("Closing: $reason")
                _isConnected.value = false
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WebSocketClient", "WebSocket Closed: $code / $reason")
                _incomingMessages.trySend("Closed: $reason")
                _isConnected.value = false
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WebSocketClient", "WebSocket Failure: ${t.message}", t)
                _incomingMessages.trySend("Error: ${t.message}")
                _isConnected.value = false
                currentUrl?.let { connect(it) }
            }
        })
    }

    fun disconnect() {
        webSocket?.close(1000, "Disconnected by client")
        webSocket = null
        currentUrl = null
        _incomingMessages.close()
        _isConnected.value = false
    }

    suspend fun send(message: String): Boolean {
        if (webSocket == null || !(_isConnected.value)) {
            Log.d("WebSocketClient", "WebSocket not connected or not open. Attempting to reconnect...")
            currentUrl?.let { url ->
                connect(url)
                val connected = withTimeoutOrNull(5000L) {
                    _isConnected.first { it }
                }
                if (connected == null || !connected) {
                    Log.e("WebSocketClient", "Failed to reconnect WebSocket within timeout.")
                    return false
                }
            } ?: run {
                Log.e("WebSocketClient", "Cannot reconnect, no URL available.")
                return false
            }
        }
        return webSocket?.send(message) ?: false
    }

    suspend fun sendVerificationRequest(embeddings: FloatArray): ApiResult<FaceVerificationResult> {
        val message = mapOf(
            "type" to "recognize_face",
            "embeddings" to embeddings.map { it.toDouble() }
        )
        val jsonMessage = gson.toJson(message)
        Log.d("WebSocketClient", "Sending verification request: $jsonMessage")
        if (send(jsonMessage)) {
            val responseText = incomingMessages.collectUntilResponse("recognize_face")
            return try {
                val responseMap = gson.fromJson(responseText, Map::class.java)
                val isMatch = responseMap["match"] as? Boolean ?: false
                val name = responseMap["name"] as? String
                val distance = (responseMap["distance"] as? Double)?.toFloat() ?: -1.0f
                val matchedUser = if (isMatch && name != null) User(name = name, email = "", phone = "", embeddings = floatArrayOf()) else null
                ApiResult.Success(FaceVerificationResult(isMatch, matchedUser, distance))
            } catch (e: JsonSyntaxException) {
                ApiResult.Error(e, "Invalid JSON response for face verification.")
            } catch (e: Exception) {
                ApiResult.Error(e, "Error processing face verification response.")
            }
        } else {
            return ApiResult.Error(Exception("Failed to send verification request via WebSocket."))
        }
    }

    suspend fun sendInsertFaceRequest(user: User): ApiResult<Boolean> {
        val message = mapOf(
            "type" to "insert_face",
            "name" to user.name,
            "email" to user.email,
            "phone" to user.phone,
            "embeddings" to user.embeddings.map { it.toDouble() }
        )
        val jsonMessage = gson.toJson(message)
        Log.d("WebSocketClient", "Sending insert face request: $jsonMessage")
        if (send(jsonMessage)) {
            val responseText = incomingMessages.collectUntilResponse("insert_face")
            return try {
                val responseMap = gson.fromJson(responseText, Map::class.java)
                val success = responseMap["success"] as? Boolean ?: false
                val errorMessage = responseMap["message"] as? String
                if (success) {
                    ApiResult.Success(true)
                } else {
                    ApiResult.Error(Exception(errorMessage ?: "Failed to insert face."), errorMessage)
                }
            } catch (e: JsonSyntaxException) {
                ApiResult.Error(e, "Invalid JSON response for face insertion.")
            } catch (e: Exception) {
                ApiResult.Error(e, "Error processing face insertion response.")
            }
        } else {
            return ApiResult.Error(Exception("Failed to send insert face request via WebSocket."))
        }
    }

    private suspend fun Flow<String>.collectUntilResponse(responseType: String): String {
        return this.first { message ->
            try {
                val jsonMap = gson.fromJson(message, Map::class.java)
                jsonMap["type"] == responseType
            } catch (e: JsonSyntaxException) {
                false
            }
        }
    }
}