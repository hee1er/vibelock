package com.vibelock.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibelock.data.model.DrawPath
import com.vibelock.data.model.MatchState
import com.vibelock.data.model.PlayerRole
import com.vibelock.ui.component.ColorPicker
import com.vibelock.ui.component.DrawingCanvas
import com.vibelock.ui.theme.*

@Composable
fun DrawingScreen(
    matchState: MatchState.Matched,
    localPaths: List<DrawPath>,
    remotePaths: List<DrawPath>,
    currentLocalPath: DrawPath?,
    currentRemotePath: DrawPath?,
    selectedColor: Long,
    strokeWidth: Float,
    guessInput: String,
    guessResult: com.vibelock.data.model.GuessResult?,
    showSkipConfirm: Boolean,
    onDrawStart: (Float, Float) -> Unit,
    onDrawMove: (Float, Float) -> Unit,
    onDrawEnd: () -> Unit,
    onClear: () -> Unit,
    onColorSelected: (Long) -> Unit,
    onStrokeSelected: (Float) -> Unit,
    onGuessInput: (String) -> Unit,
    onGuessSubmit: () -> Unit,
    onSkipRequest: () -> Unit,
    onSkipConfirm: () -> Unit,
    onSkipDismiss: () -> Unit,
    onGuessResultDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val isDrawer = matchState.role == PlayerRole.DRAWER || matchState.role == PlayerRole.FREE
    val isGuesser = matchState.role == PlayerRole.GUESSER

    Box(modifier = modifier.fillMaxSize().background(VibeSurface)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            TopBar(
                partnerName = matchState.partnerName,
                role = matchState.role,
                word = matchState.word,
                onSkip = onSkipRequest,
            )

            // Split canvas: top = partner drawing, bottom = your drawing
            Column(modifier = Modifier.weight(1f)) {
                // Partner's canvas (read-only view)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    DrawingCanvas(
                        localPaths = if (!isDrawer) localPaths else emptyList(),
                        remotePaths = remotePaths,
                        currentLocalPath = if (!isDrawer) currentLocalPath else null,
                        currentRemotePath = currentRemotePath,
                        isDrawingEnabled = !isDrawer,
                        onDrawStart = onDrawStart,
                        onDrawMove = onDrawMove,
                        onDrawEnd = onDrawEnd,
                        modifier = Modifier.fillMaxSize(),
                        backgroundColor = Color(0xFF0E0E12),
                    )
                    // Label
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.6f),
                    ) {
                        Text(
                            text = if (isDrawer) "상대방 화면" else "내 그림",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }

                Divider(color = VibeBorder, thickness = 1.dp)

                // Your canvas (drawable)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    DrawingCanvas(
                        localPaths = if (isDrawer) localPaths else remotePaths,
                        remotePaths = if (isDrawer) remotePaths else emptyList(),
                        currentLocalPath = if (isDrawer) currentLocalPath else null,
                        currentRemotePath = if (isDrawer) currentRemotePath else null,
                        isDrawingEnabled = isDrawer,
                        onDrawStart = onDrawStart,
                        onDrawMove = onDrawMove,
                        onDrawEnd = onDrawEnd,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.6f),
                    ) {
                        Text(
                            text = if (isDrawer) "내 그림 (터치하여 그리기)" else "상대방 그림",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }

            // Bottom controls
            Surface(
                color = VibeCard,
                tonalElevation = 8.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (isDrawer) {
                        // Drawing tools
                        ColorPicker(
                            selectedColor = selectedColor,
                            strokeWidth = strokeWidth,
                            onColorSelected = onColorSelected,
                            onStrokeSelected = onStrokeSelected,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = onClear,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(alpha = 0.6f)),
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("지우기")
                            }
                        }
                    }

                    if (isGuesser) {
                        // Guess input
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
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
                                keyboardActions = KeyboardActions(onSend = {
                                    onGuessSubmit()
                                    focusManager.clearFocus()
                                }),
                                shape = RoundedCornerShape(12.dp),
                            )
                            Button(
                                onClick = {
                                    onGuessSubmit()
                                    focusManager.clearFocus()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = VibePurple),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(56.dp),
                            ) {
                                Icon(Icons.Default.Send, contentDescription = "제출")
                            }
                        }
                    }
                }
            }
        }

        // Skip confirm dialog
        if (showSkipConfirm) {
            AlertDialog(
                onDismissRequest = onSkipDismiss,
                title = { Text("다음 상대 찾기") },
                text = { Text("현재 연결을 끊고 새 상대를 찾을까요?") },
                confirmButton = {
                    TextButton(onClick = onSkipConfirm, colors = ButtonDefaults.textButtonColors(contentColor = VibePurple)) {
                        Text("찾기", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = onSkipDismiss) { Text("취소") }
                },
                containerColor = VibeCard,
            )
        }

        // Guess result overlay
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
                        Text(
                            text = if (result.correct) "🎉 정답!" else "❌ 틀렸어요",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        Text(
                            text = result.word,
                            style = MaterialTheme.typography.titleLarge,
                            color = if (result.correct) VibeGreen else Color.White.copy(alpha = 0.7f),
                        )
                        TextButton(onClick = onGuessResultDismiss) {
                            Text("계속", color = Color.White.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    partnerName: String,
    role: PlayerRole,
    word: String?,
    onSkip: () -> Unit,
) {
    Surface(
        color = VibeCard,
        tonalElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = "🌍 $partnerName 와 연결됨",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
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
                        Text(
                            text = "\"$it\"",
                            style = MaterialTheme.typography.labelMedium,
                            color = VibePurple,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            IconButton(
                onClick = onSkip,
                colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White.copy(alpha = 0.6f)),
            ) {
                Icon(Icons.Default.SkipNext, contentDescription = "다음 상대")
            }
        }
    }
}
