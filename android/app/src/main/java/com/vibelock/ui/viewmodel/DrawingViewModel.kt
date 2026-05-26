package com.vibelock.ui.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibelock.data.model.*
import com.vibelock.data.repository.MatchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class DrawingUiState(
    val matchState: MatchState = MatchState.Idle,
    val localPaths: List<DrawPath> = emptyList(),
    val remotePaths: List<DrawPath> = emptyList(),
    val currentLocalPath: DrawPath? = null,
    val currentRemotePath: DrawPath? = null,
    val selectedColor: Long = 0xFFFFFFFF,
    val strokeWidth: Float = 8f,
    val guessInput: String = "",
    val guessResult: GuessResult? = null,
    val showSkipConfirm: Boolean = false,
)

@HiltViewModel
class DrawingViewModel @Inject constructor(
    private val repo: MatchRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DrawingUiState())
    val uiState: StateFlow<DrawingUiState> = _uiState.asStateFlow()

    private val userId = UUID.randomUUID().toString()

    init {
        repo.connect()
        observeSocket()
        observeMatchState()
    }

    private fun observeMatchState() = viewModelScope.launch {
        repo.matchState.collect { state ->
            _uiState.update { it.copy(matchState = state) }
            if (state is MatchState.Searching) clearCanvases()
        }
    }

    private fun observeSocket() = viewModelScope.launch {
        repo.incomingDrawEvents.collect { msg ->
            repo.handleServerMessage(msg)
            when (msg.type) {
                "partner_draw_start" -> {
                    val path = DrawPath(
                        color = parseColor(msg.color ?: "#FFFFFF"),
                        strokeWidth = msg.strokeWidth ?: 8f,
                        isLocal = false,
                    )
                    msg.x?.let { x -> msg.y?.let { y -> path.points.add(DrawPoint(x, y)) } }
                    _uiState.update { it.copy(currentRemotePath = path) }
                }
                "partner_draw_move" -> {
                    val path = _uiState.value.currentRemotePath ?: return@collect
                    msg.x?.let { x -> msg.y?.let { y -> path.points.add(DrawPoint(x, y)) } }
                    _uiState.update { it.copy(currentRemotePath = path.copy(points = path.points.toMutableList())) }
                }
                "partner_draw_end" -> {
                    val path = _uiState.value.currentRemotePath ?: return@collect
                    _uiState.update {
                        it.copy(
                            remotePaths = it.remotePaths + path,
                            currentRemotePath = null,
                        )
                    }
                }
                "partner_clear" -> {
                    _uiState.update { it.copy(remotePaths = emptyList(), currentRemotePath = null) }
                }
                "correct_guess" -> {
                    _uiState.update {
                        it.copy(guessResult = GuessResult(msg.word ?: "", true))
                    }
                }
                "wrong_guess" -> {
                    _uiState.update {
                        it.copy(guessResult = GuessResult(msg.word ?: "", false))
                    }
                }
            }
        }
    }

    fun findMatch(displayName: String = "Anonymous") {
        repo.joinQueue(userId, displayName)
    }

    fun onDrawStart(x: Float, y: Float) {
        val colorHex = colorLongToHex(_uiState.value.selectedColor)
        val sw = _uiState.value.strokeWidth
        val path = DrawPath(
            points = mutableListOf(DrawPoint(x, y)),
            color = _uiState.value.selectedColor,
            strokeWidth = sw,
        )
        _uiState.update { it.copy(currentLocalPath = path) }
        if (_uiState.value.matchState is MatchState.Matched) {
            repo.sendDrawStart(x, y, colorHex, sw)
        }
    }

    fun onDrawMove(x: Float, y: Float) {
        val path = _uiState.value.currentLocalPath ?: return
        path.points.add(DrawPoint(x, y))
        _uiState.update { it.copy(currentLocalPath = path.copy(points = path.points.toMutableList())) }
        if (_uiState.value.matchState is MatchState.Matched) {
            repo.sendDrawMove(x, y)
        }
    }

    fun onDrawEnd() {
        val path = _uiState.value.currentLocalPath ?: return
        _uiState.update {
            it.copy(
                localPaths = it.localPaths + path,
                currentLocalPath = null,
            )
        }
        if (_uiState.value.matchState is MatchState.Matched) {
            repo.sendDrawEnd()
        }
    }

    fun clearCanvas() {
        _uiState.update { it.copy(localPaths = emptyList(), currentLocalPath = null) }
        if (_uiState.value.matchState is MatchState.Matched) {
            repo.sendClear()
        }
    }

    fun selectColor(color: Long) = _uiState.update { it.copy(selectedColor = color) }
    fun selectStroke(width: Float) = _uiState.update { it.copy(strokeWidth = width) }

    fun onGuessInput(text: String) = _uiState.update { it.copy(guessInput = text) }

    fun submitGuess() {
        val word = _uiState.value.guessInput.trim()
        if (word.isEmpty()) return
        repo.sendGuess(word)
        _uiState.update { it.copy(guessInput = "") }
    }

    fun skip() {
        repo.sendSkip()
        _uiState.update { it.copy(showSkipConfirm = false) }
    }

    fun requestSkipConfirm() = _uiState.update { it.copy(showSkipConfirm = true) }
    fun dismissSkipConfirm() = _uiState.update { it.copy(showSkipConfirm = false) }

    fun dismissGuessResult() = _uiState.update { it.copy(guessResult = null) }

    private fun clearCanvases() = _uiState.update {
        it.copy(
            localPaths = emptyList(),
            remotePaths = emptyList(),
            currentLocalPath = null,
            currentRemotePath = null,
            guessResult = null,
        )
    }

    private fun parseColor(hex: String): Long {
        return try {
            android.graphics.Color.parseColor(hex).toLong() and 0xFFFFFFFFL or 0xFF000000L
        } catch (e: Exception) {
            0xFFFFFFFF
        }
    }

    private fun colorLongToHex(color: Long): String {
        val c = Color(color)
        return "#%02X%02X%02X".format(
            (c.red * 255).toInt(),
            (c.green * 255).toInt(),
            (c.blue * 255).toInt(),
        )
    }
}
