package com.vibelock.ui.component

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.vibelock.data.local.UserDataStore.Companion.FREE_DAILY_SKIPS
import com.vibelock.ui.theme.*

@Composable
fun SkipLimitDialog(
    onWatchAd: (Activity) -> Unit,
    onGoPremium: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = VibeCard,
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("⏭️", fontSize = 48.sp)

                Text(
                    text = "오늘 스킵 횟수를\n모두 사용했어요",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )

                Text(
                    text = "무료 플랜은 하루 ${FREE_DAILY_SKIPS}회 스킵 가능해요.\n내일 자정에 다시 채워져요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                )

                Divider(color = VibeBorder)

                // 광고 보고 계속
                Button(
                    onClick = { onWatchAd(context as Activity) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2000)),
                    border = ButtonDefaults.outlinedButtonBorder,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("🎬", fontSize = 18.sp)
                        Column {
                            Text("광고 보고 스킵하기", fontWeight = FontWeight.SemiBold, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                            Text("30초 광고 시청 + 🪙 30코인 보너스", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFFD60A))
                        }
                    }
                }

                // 프리미엄 업그레이드
                Button(
                    onClick = onGoPremium,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .androidx.compose.ui.draw.clip(RoundedCornerShape(14.dp))
                            .androidx.compose.foundation.background(
                                Brush.horizontalGradient(listOf(VibePurple, VibePink))
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("⭐", fontSize = 18.sp)
                            Text("프리미엄으로 무제한 스킵", fontWeight = FontWeight.Bold, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.3f))) {
                    Text("닫기")
                }
            }
        }
    }
}
