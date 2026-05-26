package com.vibelock.ui.screen

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibelock.data.model.PREMIUM_PLANS
import com.vibelock.data.model.PremiumPlan
import com.vibelock.ui.theme.*
import com.vibelock.ui.viewmodel.StoreViewModel

private val PREMIUM_FEATURES = listOf(
    "⚡" to "무제한 스킵 (광고 없이)",
    "🎨" to "모든 특수 브러시 포함",
    "🖼️" to "모든 락스크린 프레임 포함",
    "⭐" to "K-POP 스티커 팩 포함",
    "🚀" to "우선 매칭 (대기 시간 단축)",
    "📴" to "배너 광고 제거",
    "🎆" to "정답 폭죽/불꽃 효과 모두 포함",
)

@Composable
fun PremiumScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StoreViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedPlan by remember { mutableStateOf(PREMIUM_PLANS[1]) } // 연간 기본 선택

    // 토스트
    uiState.toast?.let { msg ->
        LaunchedEffect(msg) {
            kotlinx.coroutines.delay(2000)
            viewModel.dismissToast()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(VibeSurface)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 120.dp),
        ) {
            // 헤더
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF1A0030), VibeSurface)
                        )
                    )
                    .padding(top = 56.dp, bottom = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.align(Alignment.Start).padding(start = 8.dp),
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "닫기", tint = Color.White.copy(alpha = 0.6f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("⭐", fontSize = 52.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "VibeLock Premium",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                    )
                    Text(
                        text = "더 자유롭게, 더 창의적으로",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }

            // 혜택 목록
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "프리미엄 혜택",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                PREMIUM_FEATURES.forEach { (emoji, text) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(VibeCard)
                            .padding(14.dp),
                    ) {
                        Text(emoji, fontSize = 22.sp)
                        Text(text, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // 플랜 선택
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "플랜 선택",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                PREMIUM_PLANS.forEach { plan ->
                    PlanCard(
                        plan = plan,
                        isSelected = selectedPlan.skuId == plan.skuId,
                        onSelect = { selectedPlan = plan },
                    )
                }
            }
        }

        // 하단 구독 버튼 (고정)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, VibeSurface, VibeSurface))
                )
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .navigationBarsPadding(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.subscribePremium(context as Activity, selectedPlan.skuId) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp),
                    enabled = !uiState.isLoadingPurchase,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(listOf(VibePurple, VibePink)),
                                RoundedCornerShape(16.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (uiState.isLoadingPurchase) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                        } else {
                            Text(
                                text = "⭐ ${selectedPlan.price}${selectedPlan.period} 시작하기",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }
                    }
                }
                Text(
                    text = "언제든지 취소 가능 · 자동 갱신 · 구글 플레이 결제",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.3f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // 토스트
        uiState.toast?.let { msg ->
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF2A2A30),
            ) {
                Text(
                    msg, style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun PlanCard(plan: PremiumPlan, isSelected: Boolean, onSelect: () -> Unit) {
    val borderColor by animateColorAsState(
        if (isSelected) VibePurple else VibeBorder, label = "border"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .background(if (isSelected) VibePurple.copy(alpha = 0.1f) else VibeCard)
            .clickable(onClick = onSelect)
            .padding(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RadioButton(
                    selected = isSelected,
                    onClick = onSelect,
                    colors = RadioButtonDefaults.colors(selectedColor = VibePurple),
                )
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(plan.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
                        if (plan.isRecommended) {
                            Surface(shape = RoundedCornerShape(6.dp), color = VibePurple) {
                                Text(
                                    "추천", style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold, color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }
                    Text(plan.perMonth, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.5f))
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(plan.price, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                Text(plan.period, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.4f))
                if (plan.savings.isNotEmpty()) {
                    Text(plan.savings, style = MaterialTheme.typography.labelSmall, color = VibeGreen, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
