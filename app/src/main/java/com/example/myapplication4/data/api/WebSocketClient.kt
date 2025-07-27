package com.example.myapplication4.data.api

import android.util.Log
import com.example.myapplication4.data.model.Admin // Import your Admin data class
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
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebSocketClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {
    private val TAG = "AppWebSocketService"
    var webSocket: WebSocket? = null
    private var currentUrl: String? = null

    // Callbacks for authentication
    var onAuthResult: ((Boolean, String?) -> Unit)? = null
    var onTokenReceived: ((String) -> Unit)? = null

    // Callbacks for profile data
    var onProfileReceived: ((Admin?, String?) -> Unit)? = null // Admin object or error message
    var onProfileUpdateResult: ((Boolean, String?) -> Unit)? = null // Success/failure, message

    // Channel for general incoming messages (non-specific responses)
    private val _incomingMessages = Channel<String>(Channel.UNLIMITED)
    val incomingMessages: Flow<String> = _incomingMessages.receiveAsFlow()

    private val _isConnected = MutableStateFlow<Boolean>(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    // Variable to store the last sent login message for echo check
    private var lastSentLoginRequest: String? = null

    init {
        val defaultClient = okHttpClient.newBuilder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build()
    }

    fun connect(wsUrl: String) {
        if (webSocket != null && _isConnected.value) {
            Log.d(TAG, "WebSocket is already connected.")
            return
        }
        currentUrl = wsUrl
        val request = Request.Builder().url(wsUrl).build()
        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket Connected: ${response.message}")
                _isConnected.value = true
                _incomingMessages.trySend("Connected")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "Receiving: $text")
                _incomingMessages.trySend(text) // Send to general message flow

                try {
                    val jsonResponse = JSONObject(text)
                    when (jsonResponse.optString("type")) {
                        "login" -> {
                            val success = jsonResponse.optBoolean("success")
                            val message = jsonResponse.optString("message")
                            val token = jsonResponse.optString("token")
                            if (success && token.isNotEmpty()) {
                                onAuthResult?.invoke(true, null)
                                onTokenReceived?.invoke(token)
                            } else {
                                onAuthResult?.invoke(false, message)
                            }
                        }
                        "profile_data" -> {
                            val success = jsonResponse.optBoolean("success")
                            if (success) {
                                val data = jsonResponse.optJSONObject("data")
                                if (data != null) {
                                    val admin = Admin(
                                        id = data.optString("id"),
                                        name = data.optString("username"),
                                        email = data.optString("email"),
                                        role = data.optString("role")
                                    )
                                    onProfileReceived?.invoke(admin, null)
                                } else {
                                    onProfileReceived?.invoke(null, "Profile data is null.")
                                }
                            } else {
                                onProfileReceived?.invoke(null, jsonResponse.optString("message"))
                            }
                        }
                        "profile_update" -> {
                            val success = jsonResponse.optBoolean("success")
                            val message = jsonResponse.optString("message")
                            onProfileUpdateResult?.invoke(success, message)
                        }
                        // "recognize_face" and "insert_face" responses are handled by collectUntilResponse
                        else -> {
                            Log.w(TAG, "Unknown or unhandled message type received: ${jsonResponse.optString("type")}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing WebSocket message as JSON: ${e.message}", e)
                    // If parsing fails for a login response, we might want to inform
                    if (lastSentLoginRequest != null) { // If the last message was a login request
                        onAuthResult?.invoke(false, "Invalid or unexpected server response format.")
                    }
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                Log.d(TAG, "Receiving bytes: ${bytes.hex()}")
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "Closing: $code / $reason")
                _incomingMessages.trySend("Closing: $reason")
                _isConnected.value = false
                this@WebSocketClient.webSocket?.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket Closed: $code / $reason")
                _incomingMessages.trySend("Closed: $reason")
                _isConnected.value = false
                this@WebSocketClient.webSocket = null
                lastSentLoginRequest = null
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket Failed: ${t.message}", t)
                _incomingMessages.trySend("Error: ${t.message}")
                _isConnected.value = false
                onAuthResult?.invoke(false, t.message ?: "Network error or connection failed")
                this@WebSocketClient.webSocket = null
                lastSentLoginRequest = null
                currentUrl?.let { connect(it) }
            }
        })
    }

    suspend fun send(message: String): Boolean {
        if (webSocket == null || !(_isConnected.value)) {
            Log.d(TAG, "WebSocket not connected or not open. Attempting to reconnect...")
            currentUrl?.let { url ->
                connect(url)
                val connected = withTimeoutOrNull(5000L) {
                    _isConnected.first { it }
                }
                if (connected == null || !connected) {
                    Log.e(TAG, "Failed to reconnect WebSocket within timeout.")
                    return false
                }
            } ?: run {
                Log.e(TAG, "Cannot reconnect, no URL available.")
                return false
            }
        }

        try {
            val jsonMessage = JSONObject(message)
            if (jsonMessage.optString("type") == "LOGIN_REQUEST") {
                lastSentLoginRequest = message
                Log.d(TAG, "Stored lastSentLoginRequest for echo check.")
            } else {
                lastSentLoginRequest = null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Message is not a valid JSON or not a login request. Not storing for echo check.")
            lastSentLoginRequest = null
        }

        val sent = webSocket?.send(message) ?: false
        if (sent) {
            Log.d(TAG, "Sending: $message")
        } else {
            Log.e(TAG, "Failed to send message: $message. WebSocket might not be open.")
        }
        return sent
    }

    fun requestProfile(adminId: String) {
        val message = JSONObject().apply {
            put("type", "GET_PROFILE_REQUEST")
            put("adminId", adminId)
        }.toString()
        sendMessageFireAndForget(message)
    }

    fun updateProfile(adminId: String, username: String, email: String) {
        val message = JSONObject().apply {
            put("type", "UPDATE_PROFILE_REQUEST")
            put("adminId", adminId)
            put("username", username)
            put("email", email)
        }.toString()
        sendMessageFireAndForget(message)
    }

    // Helper function for sending messages that don't need immediate suspend feedback
    private fun sendMessageFireAndForget(message: String): Boolean {
        val sent = webSocket?.send(message) ?: false
        if (sent) {
            Log.d(TAG, "Sending (fire and forget): $message")
        } else {
            Log.e(TAG, "Failed to send (fire and forget) message: $message. WebSocket might not be open.")
        }
        return sent
    }

    suspend fun sendVerificationRequest(embeddings: FloatArray): ApiResult<FaceVerificationResult> {
        val message = mapOf(
            "type" to "recognize_face",
            "embeddings" to embeddings.map { it.toDouble() }
        )
        val jsonMessage = gson.toJson(message)
        Log.d(TAG, "Sending verification request: $jsonMessage")
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
        Log.d(TAG, "Sending insert face request: $jsonMessage")
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

    fun disconnect() {
        webSocket?.close(1000, "Disconnected by client")
        webSocket = null
        currentUrl = null
        _isConnected.value = false
        _incomingMessages.close() // Close the channel when disconnecting
        lastSentLoginRequest = null
    }
}

//sealed class ApiResult<out T> {
//    data class Success<out T>(val data: T) : ApiResult<T>()
//    data class Error(val exception: Throwable, val message: String? = null) : ApiResult<Nothing>()
//}