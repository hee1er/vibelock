package com.vibelock.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class WsMessage {
    abstract val type: String
}

// ── Outgoing ──────────────────────────────────────────────────────────────────

@Serializable
@SerialName("join_queue")
data class JoinQueueMsg(
    override val type: String = "join_queue",
    val userId: String,
    val displayName: String,
    val isPremium: Boolean = false,
) : WsMessage()

@Serializable
@SerialName("draw_start")
data class DrawStartMsg(
    override val type: String = "draw_start",
    val x: Float,
    val y: Float,
    val color: String,
    val strokeWidth: Float,
) : WsMessage()

@Serializable
@SerialName("draw_move")
data class DrawMoveMsg(
    override val type: String = "draw_move",
    val x: Float,
    val y: Float,
) : WsMessage()

@Serializable
@SerialName("draw_end")
data class DrawEndMsg(override val type: String = "draw_end") : WsMessage()

@Serializable
@SerialName("clear")
data class ClearMsg(override val type: String = "clear") : WsMessage()

@Serializable
@SerialName("skip")
data class SkipMsg(override val type: String = "skip") : WsMessage()

@Serializable
@SerialName("guess")
data class GuessMsg(
    override val type: String = "guess",
    val word: String,
) : WsMessage()

@Serializable
@SerialName("heartbeat")
data class HeartbeatMsg(override val type: String = "heartbeat") : WsMessage()

// ── Incoming ──────────────────────────────────────────────────────────────────

@Serializable
data class ServerMessage(
    val type: String,
    val roomId: String? = null,
    val role: String? = null,       // "drawer" | "guesser"
    val word: String? = null,
    val x: Float? = null,
    val y: Float? = null,
    val color: String? = null,
    val strokeWidth: Float? = null,
    val partnerName: String? = null,
    val queueSize: Int? = null,
    val correct: Boolean? = null,
)

// ── Local draw model ──────────────────────────────────────────────────────────

data class DrawPath(
    val points: MutableList<DrawPoint> = mutableListOf(),
    val color: Long = 0xFFFFFFFF,
    val strokeWidth: Float = 8f,
    val isLocal: Boolean = true,
    val brushType: Any? = null, // BrushType — Any to avoid circular import
)

data class DrawPoint(val x: Float, val y: Float)
