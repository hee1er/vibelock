package com.vibelock.ui.viewmodel

import android.app.Activity
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibelock.ads.AdManager
import com.vibelock.data.local.UserDataStore
import com.vibelock.data.local.UserState
import com.vibelock.data.model.*
import com.vibelock.data.repository.MatchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
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
    val selectedBrush: BrushType = BrushType.NORMAL,
    val guessInput: String = "",
    val guessResult: GuessResult? = null,
    val showSkipConfirm: Boolean = false,
    val userState: UserState? = null,
    val skipLimitReached: Boolean = false,  // 무료 스킵 5회 초과
    val toast: String? = null,
)

// 브러시 종류
enum class BrushType(val label: String, val emoji: String, val itemId: String?) {
    NORMAL("기본", "✏️", null),
    WATERCOLOR("수채화", "🎨", "brush_watercolor"),
    GLOW("형광", "✨", "brush_glow"),
    CHALK("분필", "🖊️", "brush_chalk"),
    PIXEL("픽셀", "👾", "brush_pixel"),
    RAINBOW("무지개", "🌈", "brush_rainbow"),
}

@HiltViewModel
class DrawingViewModel @Inject constructor(
    private val repo: MatchRepository,
    private val userDataStore: UserDataStore,
    private val adManager: AdManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DrawingUiState())
    val uiState: StateFlow<DrawingUiState> = _uiState.asStateFlow()

    // 스킵 3회마다 광고 표시 (무료 유저)
    private var skipsSinceLastAd = 0

    init {
        repo.connect()
        observeSocket()
        observeMatchState()
        observeUserState()
    }

    private fun observeUserState() = viewModelScope.launch {
        userDataStore.userState.collect { user ->
            _uiState.update { it.copy(userState = user) }
        }
    }

    private fun observeMatchState() = viewModelScope.launch {
        repo.matchState.collect { state ->
            _uiState.update { it.copy(matchState = state) }
            if (state is MatchState.Searching) clearCanvases()
            if (state is MatchState.Matched) {
                viewModelScope.launch { userDataStore.recordMatch() }
            }
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
                        it.copy(remotePaths = it.remotePaths + path, currentRemotePath = null)
                    }
                }
                "partner_clear" -> {
                    _uiState.update { it.copy(remotePaths = emptyList(), currentRemotePath = null) }
                }
                "correct_guess" -> {
                    _uiState.update { it.copy(guessResult = GuessResult(msg.word ?: "", true)) }
                }
                "wrong_guess" -> {
                    _uiState.update { it.copy(guessResult = GuessResult(msg.word ?: "", false)) }
                }
            }
        }
    }

    fun findMatch() {
        val name = _uiState.value.userState?.displayName ?: "Anonymous"
        val isPremium = _uiState.value.userState?.isPremium ?: false
        repo.joinQueue(UUID.randomUUID().toString(), name, isPremium)
    }

    // ── 그림 이벤트 ───────────────────────────────────────────────

    fun onDrawStart(x: Float, y: Float) {
        val colorHex = colorLongToHex(_uiState.value.selectedColor)
        val sw = effectiveStrokeWidth()
        val path = DrawPath(
            points = mutableListOf(DrawPoint(x, y)),
            color = _uiState.value.selectedColor,
            strokeWidth = sw,
            brushType = _uiState.value.selectedBrush,
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
        if (_uiState.value.matchState is MatchState.Matched) repo.sendDrawMove(x, y)
    }

    fun onDrawEnd() {
        val path = _uiState.value.currentLocalPath ?: return
        _uiState.update {
            it.copy(localPaths = it.localPaths + path, currentLocalPath = null)
        }
        if (_uiState.value.matchState is MatchState.Matched) repo.sendDrawEnd()
    }

    fun clearCanvas() {
        _uiState.update { it.copy(localPaths = emptyList(), currentLocalPath = null) }
        if (_uiState.value.matchState is MatchState.Matched) repo.sendClear()
    }

    // ── 브러시 선택 (프리미엄/아이템 체크) ───────────────────────

    fun selectBrush(brush: BrushType): Boolean {
        val user = _uiState.value.userState
        if (brush.itemId != null) {
            val isUnlocked = user?.unlockedItemIds?.contains(brush.itemId) ?: false
            val isPremium = user?.isPremium ?: false
            if (!isUnlocked && !isPremium) {
                showToast("🔒 ${brush.label} 브러시를 먼저 구매하세요!")
                return false
            }
        }
        _uiState.update { it.copy(selectedBrush = brush) }
        return true
    }

    fun selectColor(color: Long) = _uiState.update { it.copy(selectedColor = color) }
    fun selectStroke(width: Float) = _uiState.update { it.copy(strokeWidth = width) }

    // ── 스킵 (광고 + 횟수 제한) ───────────────────────────────────

    fun requestSkip(activity: Activity) {
        val user = _uiState.value.userState ?: return
        val isPremium = user.isPremium

        // 프리미엄은 제한 없이 스킵
        if (isPremium) {
            doSkip()
            return
        }

        // 무료: 하루 5회 제한
        if (user.skipsToday >= UserDataStore.FREE_DAILY_SKIPS) {
            _uiState.update { it.copy(skipLimitReached = true) }
            return
        }

        // 3번 스킵할 때마다 전면 광고 표시
        skipsSinceLastAd++
        if (skipsSinceLastAd >= 3 && adManager.interstitialReady.value) {
            skipsSinceLastAd = 0
            adManager.showInterstitial(activity) { doSkip() }
        } else {
            doSkip()
        }
    }

    private fun doSkip() {
        viewModelScope.launch { userDataStore.recordSkip() }
        repo.sendSkip()
        _uiState.update { it.copy(showSkipConfirm = false, skipLimitReached = false) }
    }

    fun dismissSkipLimit() = _uiState.update { it.copy(skipLimitReached = false) }

    // ── 추가 스킵: 광고 보고 스킵 횟수 복구 ──────────────────────

    fun watchAdForSkip(activity: Activity) {
        adManager.showRewarded(
            activity = activity,
            onRewarded = { _ ->
                viewModelScope.launch {
                    userDataStore.addCoins(UserDataStore.COINS_PER_REWARDED_AD)
                }
                _uiState.update { it.copy(skipLimitReached = false) }
                doSkip()
            },
            onDismissed = {},
        )
    }

    // ── 정답 맞추기 ───────────────────────────────────────────────

    fun onGuessInput(text: String) = _uiState.update { it.copy(guessInput = text) }

    fun submitGuess() {
        val word = _uiState.value.guessInput.trim()
        if (word.isEmpty()) return
        repo.sendGuess(word)
        _uiState.update { it.copy(guessInput = "") }
    }

    fun dismissGuessResult() = _uiState.update { it.copy(guessResult = null) }
    fun requestSkipConfirm() = _uiState.update { it.copy(showSkipConfirm = true) }
    fun dismissSkipConfirm() = _uiState.update { it.copy(showSkipConfirm = false) }
    fun dismissToast() = _uiState.update { it.copy(toast = null) }

    fun resetToIdle() {
        repo.reset()
        clearCanvases()
    }

    private fun clearCanvases() = _uiState.update {
        it.copy(
            localPaths = emptyList(), remotePaths = emptyList(),
            currentLocalPath = null, currentRemotePath = null,
            guessResult = null,
        )
    }

    private fun effectiveStrokeWidth(): Float {
        return when (_uiState.value.selectedBrush) {
            BrushType.WATERCOLOR -> _uiState.value.strokeWidth * 2.5f
            BrushType.PIXEL -> (_uiState.value.strokeWidth).coerceIn(6f, 12f)
            else -> _uiState.value.strokeWidth
        }
    }

    private fun parseColor(hex: String): Long {
        return try {
            android.graphics.Color.parseColor(hex).toLong() and 0xFFFFFFFFL or 0xFF000000L
        } catch (e: Exception) { 0xFFFFFFFF }
    }

    private fun colorLongToHex(color: Long): String {
        val c = Color(color)
        return "#%02X%02X%02X".format(
            (c.red * 255).toInt(), (c.green * 255).toInt(), (c.blue * 255).toInt()
        )
    }

    private fun showToast(msg: String) = _uiState.update { it.copy(toast = msg) }
}
