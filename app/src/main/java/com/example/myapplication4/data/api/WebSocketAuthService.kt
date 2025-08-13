package com.example.myapplication4.data.api

import android.util.Log
import com.example.myapplication4.data.model.Admin // Import your Admin data class
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
class WebSocketAuthService @Inject constructor() {

    private val TAG = "WebSocketAuthService"
    var webSocket: WebSocket? = null
    private val client: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    var onAuthResult: ((Boolean, String?) -> Unit)? = null
    var onTokenReceived: ((String) -> Unit)? = null
    // New callbacks for profile data
    var onProfileReceived: ((Admin?, String?) -> Unit)? = null // Admin object or error message
    var onProfileUpdateResult: ((Boolean, String?) -> Unit)? = null // Success/failure, message

    // Variabel untuk menyimpan pesan login terakhir yang dikirim
    private var lastSentLoginRequest: String? = null

    fun connect(wsUrl: String) {
        if (webSocket != null && webSocket?.send("") == true) { // Cek apakah koneksi masih aktif
            Log.d(TAG, "WebSocket is already connected.")
            return
        }
        // Pastikan webSocket diset null jika tidak aktif untuk memungkinkan koneksi baru
        webSocket = null

        val request = Request.Builder().url(wsUrl).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket Connected: ${response.message}")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "Receiving: $text")
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
                        "profile_data" -> { // New: Handle profile data response
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
                        "profile_update" -> { // New: Handle profile update response
                            val success = jsonResponse.optBoolean("success")
                            val message = jsonResponse.optString("message")
                            onProfileUpdateResult?.invoke(success, message)
                        }
                        else -> {
                            Log.w(TAG, "Unknown message type received: ${jsonResponse.optString("type")}")
                        }
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing WebSocket message as JSON: ${e.message}", e)
                    onAuthResult?.invoke(false, "Invalid or unexpected server response format.")
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                Log.d(TAG, "Receiving bytes: ${bytes.hex()}")
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "Closing: $code / $reason")
                webSocket.close(1000, null)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket Failed: ${t.message}", t)
                onAuthResult?.invoke(false, t.message ?: "Network error or connection failed")
                this@WebSocketAuthService.webSocket = null
                lastSentLoginRequest = null
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket Closed: $code / $reason")
                this@WebSocketAuthService.webSocket = null
                lastSentLoginRequest = null
            }
        })
    }

    fun sendMessage(message: String): Boolean {
        // Simpan pesan terakhir yang dikirim jika itu adalah LOGIN_REQUEST
        try {
            val jsonMessage = JSONObject(message)
            if (jsonMessage.optString("type") == "LOGIN_REQUEST") {
                lastSentLoginRequest = message
                Log.d(TAG, "Stored lastSentLoginRequest for echo check.")
            } else {
                lastSentLoginRequest = null // Hapus jika bukan request login
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
        sendMessage(message)
    }

    fun updateProfile(adminId: String, username: String, email: String) {
        val message = JSONObject().apply {
            put("type", "UPDATE_PROFILE_REQUEST")
            put("adminId", adminId)
            put("username", username)
            put("email", email)
            // Add other fields if you want to update them (e.g., password, but handle securely)
        }.toString()
        sendMessage(message)
    }

    fun disconnect() {
        webSocket?.close(1000, "User logout")
        webSocket = null
        lastSentLoginRequest = null
    }
}