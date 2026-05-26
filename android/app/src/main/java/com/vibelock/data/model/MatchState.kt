package com.vibelock.data.model

sealed class MatchState {
    data object Idle : MatchState()
    data class Searching(val queueSize: Int = 1) : MatchState()
    data class Matched(
        val roomId: String,
        val partnerName: String,
        val role: PlayerRole,
        val word: String?,
    ) : MatchState()
    data object Disconnected : MatchState()
}

enum class PlayerRole { DRAWER, GUESSER, FREE }

data class GuessResult(
    val word: String,
    val correct: Boolean,
)
