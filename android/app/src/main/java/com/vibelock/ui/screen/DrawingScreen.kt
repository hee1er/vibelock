package com.vibelock.ui.screen

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibelock.data.model.DrawPath
import com.vibelock.data.model.GuessResult
import com.vibelock.data.model.MatchState
import com.vibelock.data.model.PlayerRole
import com.vibelock.ui.component.ColorPicker
import com.vibelock.ui.component.DrawingCanvas
import com.vibelock.ui.component.SkipLimitDialog
import com.vibelock.ui.theme.*
import com.vibelock.ui.viewmodel.BrushType

@Composable
fun DrawingScreen(
    matchState: MatchState.Matched,
    localPaths: List<DrawPath>,
    remotePaths: List<DrawPath>,
    currentLocalPath: DrawPath?,
    currentRemotePath: DrawPath?,
    selectedColor: Long,
    strokeWidth: Float,
    selectedBrush: BrushType,
    guessInput: String,
    guessResult: GuessResult?,
    showSkipConfirm: Boolean,
    skipLimitReached: Boolean,
    isPremium: Boolean,
    unlockedItems: Set<String>,
    toast: String?,
    onDrawStart: (Float, Float) -> Unit,
    onDrawMove: (Float, Float) -> Unit,
    onDrawEnd: () -> Unit,
    onClear: () -> Unit,
    onColorSelected: (Long) -> Unit,
    onStrokeSelected: (Float) -> Unit,
    onBrushSelected: (BrushType) -> Unit,
    onGuessInput: (String) -> Unit,
    onGuessSubmit: () -> Unit,
    onSkipRequest: (Activity) -> Unit,
    onSkipConfirm: () -> Unit,
    onSkipDismiss: () -> Unit,
    onGuessResultDismiss: () -> Unit,
    onSkipLimitWatchAd: (Activity) -> Unit,
    onSkipLimitGoPremium: () -> Unit,
    onSkipLimitDismiss: () -> Unit,
    onDismissToast: () -> Unit,
    onGoStore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val isDrawer = matchState.role == PlayerRole.DRAWER || matchState.role == PlayerRole.FREE
    val isGuesser = matchState.role == PlayerRole.GUESSER

    // 토스트 자동 숨김
    toast?.let {
        LaunchedEffect(it) {
            kotlinx.coroutines.delay(2500)
            onDismissToast()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(VibeSurface)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 상단 바
            DrawingTopBar(
                partnerName = matchState.partnerName,
                role = matchState.role,
                word = matchState.word,
                isPremium = isPremium,
                onSkip = { onSkipRequest(context as Activity) },
                onGoStore = onGoStore,
            )

            // 분할 캔버스
            Column(modifier = Modifier.weight(1f)) {
                // 상대 화면
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    DrawingCanvas(
                        localPaths = if (!isDrawer) localPaths else emptyList(),
                        remotePaths = remotePaths,
                        currentLocalPath = if (!isDrawer) currentLocalPath else null,
                        currentRemotePath = currentRemotePath,
                        isDrawingEnabled = !isDrawer,
                        onDrawStart = onDrawStart, onDrawMove = onDrawMove, onDrawEnd = onDrawEnd,
                        modifier = Modifier.fillMaxSize(),
                        backgroundColor = Color(0xFF0E0E12),
                    )
                    CanvasLabel(Modifier.align(Alignment.TopStart), if (isDrawer) "상대방 화면" else "내 그림")
                }
                Divider(color = VibeBorder)
                // 내 캔버스
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    DrawingCanvas(
                        localPaths = if (isDrawer) localPaths else remotePaths,
                        remotePaths = if (isDrawer) remotePaths else emptyList(),
                        currentLocalPath = if (isDrawer) currentLocalPath else null,
                        currentRemotePath = if (isDrawer) currentRemotePath else null,
                        isDrawingEnabled = isDrawer,
                        onDrawStart = onDrawStart, onDrawMove = onDrawMove, onDrawEnd = onDrawEnd,
                        modifier = Modifier.fillMaxSize(),
                    )
                    CanvasLabel(Modifier.align(Alignment.TopStart), if (isDrawer) "내 그림 (터치하여 그리기)" else "상대방 그림")
                }
            }

            // 하단 도구 패널
            Surface(color = VibeCard, tonalElevation = 8.dp) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(12.dp).navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (isDrawer) {
                        // 브러시 선택
                        BrushSelector(
                            selectedBrush = selectedBrush,
                            isPremium = isPremium,
                            unlockedItems = unlockedItems,
                            onSelect = onBrushSelected,
                            onGoStore = onGoStore,
                        )
                        // 색상 + 굵기
                        ColorPicker(
                            selectedColor = selectedColor,
                            strokeWidth = strokeWidth,
                            onColorSelected = onColorSelected,
                            onStrokeSelected = onStrokeSelected,
                        )
                        OutlinedButton(
                            onClick = onClear,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(alpha = 0.5f)),
                        ) {
                            Icon(Icons.Default.Clear, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("캔버스 지우기")
                        }
                    }

                    if (isGuesser) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = guessInput,
                                onValueChange = onGuessInput,
                                placeholder = { Text("정답을 입력하세요...", color = Color.White.copy(alpha = 0.3f)) },
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = VibePurple,
                                    unfocusedBorderColor = VibeBorder,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(onSend = { onGuessSubmit(); focusManager.clearFocus() }),
                                shape = RoundedCornerShape(12.dp),
                            )
                            Button(
                                onClick = { onGuessSubmit(); focusManager.clearFocus() },
                                colors = ButtonDefaults.buttonColors(containerColor = VibePurple),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(56.dp),
                            ) {
                                Icon(Icons.Default.Send, null)
                            }
                        }
                    }
                }
            }
        }

        // 스킵 확인 다이얼로그
        if (showSkipConfirm) {
            AlertDialog(
                onDismissRequest = onSkipDismiss,
                title = { Text("다음 상대 찾기") },
                text = { Text("현재 연결을 끊고 새 상대를 찾을까요?") },
                confirmButton = {
                    TextButton(onClick = { onSkipRequest(context as Activity) }, colors = ButtonDefaults.textButtonColors(contentColor = VibePurple)) {
                        Text("찾기", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = { TextButton(onClick = onSkipDismiss) { Text("취소") } },
                containerColor = VibeCard,
            )
        }

        // 스킵 제한 다이얼로그
        if (skipLimitReached) {
            SkipLimitDialog(
                onWatchAd = { onSkipLimitWatchAd(it) },
                onGoPremium = onSkipLimitGoPremium,
                onDismiss = onSkipLimitDismiss,
            )
        }

        // 정답 결과 오버레이
        AnimatedVisibility(
            visible = guessResult != null,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier.align(Alignment.Center),
        ) {
            guessResult?.let { result ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (result.correct) Color(0xFF1E3A2F) else Color(0xFF3A1E1E),
                    modifier = Modifier.padding(32.dp),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(if (result.correct) "🎉 정답!" else "❌ 틀렸어요", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(result.word, style = MaterialTheme.typography.titleLarge, color = if (result.correct) VibeGreen else Color.White.copy(alpha = 0.7f))
                        TextButton(onClick = onGuessResultDismiss) { Text("계속", color = Color.White.copy(alpha = 0.6f)) }
                    }
                }
            }
        }

        // 토스트
        toast?.let { msg ->
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF2A2A30),
            ) {
                Text(msg, style = MaterialTheme.typography.bodyMedium, color = Color.White, modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
            }
        }
    }
}

@Composable
private fun BrushSelector(
    selectedBrush: BrushType,
    isPremium: Boolean,
    unlockedItems: Set<String>,
    onSelect: (BrushType) -> Unit,
    onGoStore: () -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(BrushType.entries) { brush ->
            val isUnlocked = brush.itemId == null || isPremium || unlockedItems.contains(brush.itemId)
            val isSelected = selectedBrush == brush
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        when {
                            isSelected -> VibePurple.copy(alpha = 0.3f)
                            !isUnlocked -> VibeCard
                            else -> VibeCard
                        }
                    )
                    .clickable { if (isUnlocked) onSelect(brush) else onGoStore() }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(brush.emoji, fontSize = 18.sp)
                    Text(
                        brush.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            isSelected -> VibePurple
                            !isUnlocked -> Color.White.copy(alpha = 0.3f)
                            else -> Color.White.copy(alpha = 0.6f)
                        },
                    )
                    if (!isUnlocked) {
                        Text("🔒", fontSize = 9.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun CanvasLabel(modifier: Modifier, text: String) {
    Surface(
        modifier = modifier.padding(8.dp),
        shape = RoundedCornerShape(8.dp),
        color = Color.Black.copy(alpha = 0.6f),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun DrawingTopBar(
    partnerName: String,
    role: PlayerRole,
    word: String?,
    isPremium: Boolean,
    onSkip: () -> Unit,
    onGoStore: () -> Unit,
) {
    Surface(color = VibeCard, tonalElevation = 4.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text("🌍 $partnerName 와 연결됨", style = MaterialTheme.typography.labelLarge, color = Color.White)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = when (role) {
                            PlayerRole.DRAWER -> VibePurple.copy(alpha = 0.2f)
                            PlayerRole.GUESSER -> VibePink.copy(alpha = 0.2f)
                            PlayerRole.FREE -> VibeBlue.copy(alpha = 0.2f)
                        },
                    ) {
                        Text(
                            text = when (role) {
                                PlayerRole.DRAWER -> "🎨 그리기"
                                PlayerRole.GUESSER -> "🤔 맞추기"
                                PlayerRole.FREE -> "✏️ 자유"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                    word?.let {
                        Text("\"$it\"", style = MaterialTheme.typography.labelMedium, color = VibePurple, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onGoStore) {
                    Text("🛍️", fontSize = 20.sp)
                }
                IconButton(
                    onClick = onSkip,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = if (isPremium) VibePurple else Color.White.copy(alpha = 0.6f)
                    ),
                ) {
                    Icon(Icons.Default.SkipNext, contentDescription = "다음 상대")
                }
            }
        }
    }
}
