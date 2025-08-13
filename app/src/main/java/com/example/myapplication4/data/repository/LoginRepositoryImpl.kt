package com.example.myapplication4.data.repository

import android.util.Log
import com.example.myapplication4.data.api.WebSocketAuthService
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class LoginRepositoryImpl @Inject constructor(
    private val webSocketClient: WebSocketClient
) : LoginRepository {
    private val TAG = "LoginRepositoryImpl"


    private val WEBSOCKET_URL = "ws://192.168.1.6:3000"

    override suspend fun loginUser(email: String, password: String): Result<String> {
        Log.d(TAG, "Attempting to login user: $email")

        webSocketClient.connect(WEBSOCKET_URL)

        val loginMessage = JSONObject().apply {
            put("type", "LOGIN_REQUEST")
            put("email", email)
            put("password", password)
        }.toString()

        val sent = webSocketClient.send(loginMessage)

        if (!sent) {
            val errorMsg =
                "Failed to send login request via WebSocket. Connection might not be open or re-connection failed."
            Log.e(TAG, errorMsg)
            return Result.failure(RuntimeException(errorMsg))
        }

        return try {
            val responseText = webSocketClient.incomingMessages.first { message ->
                try {
                    val jsonResponse = JSONObject(message)
                    jsonResponse.optString("type") == "login"
                } catch (e: Exception) {
                    Log.w(TAG, "Invalid JSON response received: $message")
                    false
                }
            }
            
            val jsonResponse = JSONObject(responseText)
            val success = jsonResponse.optBoolean("success")
            val message = jsonResponse.optString("message")
            val token = jsonResponse.optString("token")

            if (success && token.isNotEmpty()) {
                Log.d(TAG, "Login successful, token received.")
                Result.success(token)
            } else {
                Log.d(TAG, "Login failed: $message")
                Result.failure(RuntimeException(message.ifEmpty { "Login failed: Unknown error" }))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing login response: ${e.message}")
            Result.failure(RuntimeException("Error processing login response: ${e.message}"))
        }
    }
}

