package com.example.Mobile_FaceRecognition.data.remote

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class FaceRecognitionWebSocketService(private val url: String, private val listener: WebSocketEventListener) {

    private var client: OkHttpClient? = null
    private var webSocket: WebSocket? = null

    interface WebSocketEventListener {
        fun onConnected()
        fun onRecognitionResult(match: Boolean, name: String, distance: Double)
        fun onMessageReceived(message: String)
        fun onError(message: String)
    }

    fun initWebSocket() {
        client = OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build()

        val request = Request.Builder().url(url).build()
        webSocket = client?.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                super.onOpen(webSocket, response)
                Log.d("WebSocketService", "Connected to WebSocket server!")
                (listener as? WebSocketEventListener)?.onConnected()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                super.onMessage(webSocket, text)
                Log.d("WebSocketService", "Receiving: $text")
                try {
                    val jsonResponse = JSONObject(text)
                    val status = jsonResponse.optString("status")
                    val type = jsonResponse.optString("type")

                    if ("recognize_face" == type) {
                        val match = jsonResponse.getBoolean("match")
                        val name = jsonResponse.getString("name")
                        val distance = jsonResponse.getDouble("distance")
                        (listener as? WebSocketEventListener)?.onRecognitionResult(match, name, distance)
                    } else if ("success" == status && "recognition_result" == type) {
                        val user = jsonResponse.optString("user")
                        val confidence = jsonResponse.optDouble("confidence")
                        Log.d("FaceRecognition", "Server Recognition: $user (Confidence: ${String.format("%.2f", confidence)})")
                        (listener as? WebSocketEventListener)?.onMessageReceived("Server recognized: $user")
                    } else if ("success" == status && jsonResponse.has("message")) {
                        val message = jsonResponse.optString("message")
                        (listener as? WebSocketEventListener)?.onMessageReceived(message)
                    }

                } catch (e: JSONException) {
                    Log.e("WebSocketService", "Error parsing JSON message: ${e.message}")
                    (listener as? WebSocketEventListener)?.onError("Error parsing message: ${e.message}")
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                super.onMessage(webSocket, bytes)
                Log.d("WebSocketService", "Receiving bytes: ${bytes.hex()}")
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                super.onClosing(webSocket, code, reason)
                Log.d("WebSocketService", "Closing: $code / $reason")
                (listener as? WebSocketEventListener)?.onError("Disconnected from server.")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                super.onFailure(webSocket, t, response)
                Log.e("WebSocketService", "Error: ${t.message}", t)
                (listener as? WebSocketEventListener)?.onError("WebSocket Error: ${t.message}")
            }
        })
    }

    fun sendMessage(message: String) {
        if (webSocket != null) {
            webSocket?.send(message)
            Log.d("WebSocketService", "Sending: $message")
        } else {
            Log.e("WebSocketService", "WebSocket not initialized.")
            (listener as? WebSocketEventListener)?.onError("WebSocket not connected. Please restart app.")
        }
    }

    fun closeWebSocket() {
        webSocket?.close(1000, "App closing")
        client?.dispatcher?.executorService?.shutdown()
    }
}