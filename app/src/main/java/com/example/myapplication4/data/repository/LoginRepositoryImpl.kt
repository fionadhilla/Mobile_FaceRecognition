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
    private val webSocketAuthService: WebSocketAuthService
) : LoginRepository {
    private val TAG = "LoginRepositoryImpl"


    private val WEBSOCKET_URL = "wss://echo.websocket.org"

    override suspend fun loginUser(email: String, password: String): Result<String> {
        Log.d(TAG, "Attempting to login user: $email")
        return suspendCancellableCoroutine { continuation ->
            if (webSocketAuthService.webSocket == null) {
                webSocketAuthService.connect(WEBSOCKET_URL)
            }

            // Atur listener untuk hasil autentikasi
            webSocketAuthService.onTokenReceived = { token ->
                if (continuation.isActive) {
                    Log.d(TAG, "Login successful, token received.")
                    continuation.resume(Result.success(token))
                    webSocketAuthService.onTokenReceived = null
                    webSocketAuthService.onAuthResult = null
                }
            }

            webSocketAuthService.onAuthResult = { success, message ->
                if (continuation.isActive && !success) {
                    Log.d(TAG, "Login failed: $message")
                    continuation.resume(Result.failure(RuntimeException(message ?: "Unknown login error")))
                    webSocketAuthService.onAuthResult = null
                    webSocketAuthService.onTokenReceived = null
                }
            }

            val loginMessage = JSONObject().apply {
                put("type", "LOGIN_REQUEST")
                put("email", email)
                put("password", password)
            }.toString()

            val sent = webSocketAuthService.sendMessage(loginMessage)
            if (!sent) {
                val errorMsg = "Failed to send login request via WebSocket. Connection might not be open."
                Log.e(TAG, errorMsg)
                continuation.resume(Result.failure(RuntimeException(errorMsg)))
                webSocketAuthService.onAuthResult = null
                webSocketAuthService.onTokenReceived = null
            }

            continuation.invokeOnCancellation {
                Log.w(TAG, "Login coroutine cancelled.")
                webSocketAuthService.onAuthResult = null
                webSocketAuthService.onTokenReceived = null
            }
        }
    }
}