package com.vibelock.data.repository

import com.vibelock.data.model.*
import com.vibelock.data.remote.VibeLockSocketClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MatchRepository @Inject constructor(
    private val socket: VibeLockSocketClient,
) {
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    private val _matchState = MutableStateFlow<MatchState>(MatchState.Idle)
    val matchState: StateFlow<MatchState> = _matchState.asStateFlow()

    val incomingDrawEvents: Flow<ServerMessage> = socket.messages.map { it }

    fun connect() = socket.connect()

    fun joinQueue(userId: String, displayName: String, isPremium: Boolean = false) {
        _matchState.value = MatchState.Searching()
        socket.sendRaw(json.encodeToString(JoinQueueMsg(userId = userId, displayName = displayName, isPremium = isPremium)))
    }

    fun sendDrawStart(x: Float, y: Float, color: String, strokeWidth: Float) {
        socket.sendRaw(json.encodeToString(DrawStartMsg(x = x, y = y, color = color, strokeWidth = strokeWidth)))
    }

    fun sendDrawMove(x: Float, y: Float) {
        socket.sendRaw(json.encodeToString(DrawMoveMsg(x = x, y = y)))
    }

    fun sendDrawEnd() {
        socket.sendRaw(json.encodeToString(DrawEndMsg()))
    }

    fun sendClear() {
        socket.sendRaw(json.encodeToString(ClearMsg()))
    }

    fun sendSkip() {
        _matchState.value = MatchState.Searching()
        socket.sendRaw(json.encodeToString(SkipMsg()))
    }

    fun sendGuess(word: String) {
        socket.sendRaw(json.encodeToString(GuessMsg(word = word)))
    }

    fun handleServerMessage(msg: ServerMessage) {
        when (msg.type) {
            "queue_update" -> {
                if (_matchState.value is MatchState.Searching) {
                    _matchState.value = MatchState.Searching(msg.queueSize ?: 1)
                }
            }
            "matched" -> {
                val role = when (msg.role) {
                    "drawer" -> PlayerRole.DRAWER
                    "guesser" -> PlayerRole.GUESSER
                    else -> PlayerRole.FREE
                }
                _matchState.value = MatchState.Matched(
                    roomId = msg.roomId ?: "",
                    partnerName = msg.partnerName ?: "Someone",
                    role = role,
                    word = msg.word,
                )
            }
            "partner_disconnected", "partner_skipped" -> {
                _matchState.value = MatchState.Searching()
            }
            "error" -> {
                _matchState.value = MatchState.Disconnected
            }
        }
    }

    fun reset() {
        _matchState.value = MatchState.Idle
    }
}
