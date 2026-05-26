package com.vibelock.data.remote

import android.util.Log
import com.vibelock.BuildConfig
import com.vibelock.data.model.ServerMessage
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SocketClient"
private const val RECONNECT_DELAY_MS = 3000L
private const val MAX_RECONNECT_ATTEMPTS = 10

@Singleton
class VibeLockSocketClient @Inject constructor(
    private val httpClient: HttpClient,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sendChannel = Channel<String>(capacity = Channel.UNLIMITED)

    private val _messages = MutableSharedFlow<ServerMessage>(extraBufferCapacity = 64)
    val messages: SharedFlow<ServerMessage> = _messages

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private var connected = false
    private var reconnectAttempts = 0

    fun connect() {
        if (connected) return
        scope.launch { connectWithRetry() }
    }

    private suspend fun connectWithRetry() {
        while (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
            try {
                reconnectAttempts = 0
                connected = true
                httpClient.webSocket(BuildConfig.WS_BASE_URL) {
                    Log.d(TAG, "WebSocket connected")
                    // Drain outgoing channel
                    val sendJob = launch {
                        for (msg in sendChannel) {
                            send(msg)
                        }
                    }
                    // Receive incoming
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            try {
                                val msg = json.decodeFromString<ServerMessage>(frame.readText())
                                _messages.tryEmit(msg)
                            } catch (e: Exception) {
                                Log.w(TAG, "Parse error: ${e.message}")
                            }
                        }
                    }
                    sendJob.cancel()
                }
            } catch (e: Exception) {
                Log.w(TAG, "WS error (attempt $reconnectAttempts): ${e.message}")
            } finally {
                connected = false
                reconnectAttempts++
                if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                    delay(RECONNECT_DELAY_MS * reconnectAttempts.coerceAtMost(4))
                }
            }
        }
    }

    fun send(payload: Any) {
        val text = when (payload) {
            is String -> payload
            else -> json.encodeToString(payload as Map<String, *>)
        }
        sendChannel.trySend(text)
    }

    fun sendRaw(json: String) {
        sendChannel.trySend(json)
    }

    fun disconnect() {
        scope.cancel()
    }
}
