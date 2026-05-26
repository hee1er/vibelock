package com.vibelock.ui.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibelock.data.local.UserState
import com.vibelock.ui.theme.*

@Composable
fun HomeScreen(
    userState: UserState?,
    onFindMatch: () -> Unit,
    onGoStore: () -> Unit,
    onGoPremium: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1600, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "scale",
    )

    Box(modifier = modifier.fillMaxSize().background(VibeSurface), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier.size(400.dp).background(
                Brush.radialGradient(listOf(VibePurple.copy(alpha = 0.10f), Color.Transparent))
            )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            // 상단 상태바 (코인 + 프리미엄 뱃지)
            if (userState != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 코인 잔액
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF2A2000),
                        modifier = Modifier.clickable(onClick = onGoStore),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Text("🪙", fontSize = 14.sp)
                            Text(
                                userState.coinBalance.toString(),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFD60A),
                            )
                            Text("+", style = MaterialTheme.typography.labelMedium, color = Color(0xFFFFD60A).copy(alpha = 0.5f))
                        }
                    }

                    // 통계 요약
                    Text(
                        "매칭 ${userState.totalMatches}회",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.3f),
                    )

                    // 프리미엄 배지
                    if (userState.isPremium) {
                        Surface(shape = RoundedCornerShape(20.dp), color = VibePurple.copy(alpha = 0.2f)) {
                            Text(
                                "⭐ PRO",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = VibePurple,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = VibeCard,
                            modifier = Modifier.clickable(onClick = onGoPremium),
                        ) {
                            Text(
                                "⭐ PRO 업그레이드",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            )
                        }
                    }
                }
            }

            // 메인 로고
            Box(
                modifier = Modifier
                    .scale(scale)
                    .size(100.dp)
                    .background(Brush.radialGradient(listOf(VibePurple, VibePink)), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("🔒", fontSize = 44.sp)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "VibeLock",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                )
                Text(
                    text = "지구 반대편 누군가와\n그림으로 소통하세요",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    lineHeight = 26.sp,
                )
            }

            // 스킵 제한 안내 (무료 유저)
            if (userState != null && !userState.isPremium) {
                val remaining = (5 - userState.skipsToday).coerceAtLeast(0)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (remaining > 0) VibeCard else Color(0xFF3A1A1A),
                ) {
                    Text(
                        text = if (remaining > 0) "오늘 스킵 가능 횟수: ${remaining}회 남음"
                               else "⚠️ 오늘 스킵 횟수를 모두 사용했어요",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (remaining > 0) Color.White.copy(alpha = 0.4f) else Color(0xFFFF453A),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }

            // 매칭 시작 버튼
            Button(
                onClick = onFindMatch,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.horizontalGradient(listOf(VibePurple, VibePink)), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("매칭 시작하기 →", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            // 하단 버튼 줄
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // 스토어
                OutlinedButton(
                    onClick = onGoStore,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(alpha = 0.7f)),
                ) {
                    Text("🛍️ 스토어")
                }
                // 프리미엄 (무료 유저만 표시)
                if (userState?.isPremium == false) {
                    Button(
                        onClick = onGoPremium,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VibePurple.copy(alpha = 0.15f)),
                    ) {
                        Text("⭐ 프리미엄", color = VibePurple, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Text(
                text = "락스크린에서도 바로 대화 가능",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.25f),
            )
        }
    }
}
