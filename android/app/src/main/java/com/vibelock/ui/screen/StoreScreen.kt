package com.vibelock.ui.screen

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.vibelock.data.model.*
import com.vibelock.ui.theme.*
import com.vibelock.ui.viewmodel.StoreViewModel

@Composable
fun StoreScreen(
    onBack: () -> Unit,
    onGoPremium: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StoreViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val rewardedReady by viewModel.rewardedAdReady.collectAsStateWithLifecycle()

    var selectedCategory by remember { mutableStateOf<ItemCategory?>(null) }
    val filteredItems = if (selectedCategory == null) viewModel.items
                        else viewModel.items.filter { it.category == selectedCategory }

    // 토스트 자동 숨김
    uiState.toast?.let { msg ->
        LaunchedEffect(msg) {
            kotlinx.coroutines.delay(2500)
            viewModel.dismissToast()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(VibeSurface)) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // 상단 바
            item {
                TopBar(
                    coins = uiState.userState?.coinBalance ?: 0,
                    isPremium = uiState.userState?.isPremium ?: false,
                    onBack = onBack,
                    onGoPremium = onGoPremium,
                )
            }

            // 코인 충전 배너
            item {
                CoinBanner(
                    rewardedAdReady = rewardedReady,
                    onWatchAd = { viewModel.watchAdForCoins(context as Activity) },
                    onBuyCoinPack = { skuId -> viewModel.buyCoinPack(context as Activity, skuId) },
                )
            }

            // 카테고리 필터
            item {
                CategoryFilter(
                    selected = selectedCategory,
                    onSelect = { selectedCategory = if (selectedCategory == it) null else it },
                )
            }

            // 아이템 그리드
            items(filteredItems.chunked(2)) { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    row.forEach { item ->
                        ItemCard(
                            item = item,
                            isOwned = uiState.userState?.unlockedItemIds?.contains(item.id) ?: false,
                            isPremiumUser = uiState.userState?.isPremium ?: false,
                            userCoins = uiState.userState?.coinBalance ?: 0,
                            onBuy = { viewModel.buyItemWithCoins(item) },
                            onGoPremium = onGoPremium,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }

            item { Spacer(Modifier.height(32.dp).navigationBarsPadding()) }
        }

        // 토스트
        uiState.toast?.let { msg ->
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
                    .navigationBarsPadding(),
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
private fun TopBar(coins: Int, isPremium: Boolean, onBack: () -> Unit, onGoPremium: () -> Unit) {
    Surface(color = VibeCard, tonalElevation = 4.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "뒤로", tint = Color.White)
            }
            Text("스토어", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                // 코인 잔액
                Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFF2A2A10)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text("🪙", fontSize = 14.sp)
                        Text(
                            coins.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD60A),
                        )
                    }
                }
                if (!isPremium) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = VibePurple.copy(alpha = 0.2f),
                        modifier = Modifier.clickable(onClick = onGoPremium),
                    ) {
                        Text(
                            "⭐ 프리미엄",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = VibePurple,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                } else {
                    Text("⭐ PRO", style = MaterialTheme.typography.labelMedium, color = VibePurple, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CoinBanner(
    rewardedAdReady: Boolean,
    onWatchAd: () -> Unit,
    onBuyCoinPack: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // 광고 시청 코인 버튼
        if (rewardedAdReady) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF1A1A00), Color(0xFF2A2000))))
                    .border(1.dp, Color(0xFFFFD60A).copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                    .clickable(onClick = onWatchAd)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("🎬", fontSize = 24.sp)
                    Column {
                        Text("광고 보고 코인 받기", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = Color.White)
                        Text("30초 광고 시청 → 🪙 30코인", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
                    }
                }
                Text("무료", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color(0xFFFFD60A))
            }
        }

        // 코인 팩
        Text("코인 충전", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.6f))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(COIN_PACKS) { pack ->
                CoinPackCard(pack = pack, onBuy = { onBuyCoinPack(pack.skuId) })
            }
        }
    }
}

@Composable
private fun CoinPackCard(pack: CoinPack, onBuy: () -> Unit) {
    Box(
        modifier = Modifier
            .width(130.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(VibeCard)
            .border(
                1.dp,
                if (pack.isPopular) VibePurple.copy(alpha = 0.5f) else VibeBorder,
                RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onBuy)
            .padding(14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (pack.isPopular) {
                Surface(shape = RoundedCornerShape(6.dp), color = VibePurple) {
                    Text("인기", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
            Text("🪙", fontSize = 28.sp)
            Text(pack.coins.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFFD60A))
            if (pack.bonus.isNotEmpty()) {
                Text(pack.bonus, style = MaterialTheme.typography.labelSmall, color = VibeGreen)
            }
            Text(pack.price, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun CategoryFilter(selected: ItemCategory?, onSelect: (ItemCategory) -> Unit) {
    val categories = listOf(
        null to "전체",
        ItemCategory.BRUSH to "🎨 브러시",
        ItemCategory.FRAME to "🖼️ 프레임",
        ItemCategory.STICKER to "🐾 스티커",
        ItemCategory.EFFECT to "🎉 효과",
    )
    LazyRow(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(categories) { (cat, label) ->
            val isSelected = selected == cat
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) VibePurple else VibeCard,
                modifier = Modifier.clickable { if (cat != null) onSelect(cat) else onSelect(selected ?: ItemCategory.BRUSH) },
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun ItemCard(
    item: StoreItem,
    isOwned: Boolean,
    isPremiumUser: Boolean,
    userCoins: Int,
    onBuy: () -> Unit,
    onGoPremium: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isPremiumLocked = item.isPremiumOnly && !isPremiumUser
    val canAfford = userCoins >= item.coinPrice

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(VibeCard)
            .border(
                1.dp,
                when {
                    isOwned -> VibeGreen.copy(alpha = 0.4f)
                    isPremiumLocked -> VibePurple.copy(alpha = 0.3f)
                    else -> VibeBorder
                },
                RoundedCornerShape(16.dp),
            )
            .clickable {
                when {
                    isOwned -> {}
                    isPremiumLocked -> onGoPremium()
                    else -> onBuy()
                }
            }
            .padding(14.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 색상 프리뷰 + 이모지
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(item.previewColor).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(item.emoji, fontSize = 28.sp)
            }

            Text(
                item.name,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Text(
                item.description,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.4f),
                textAlign = TextAlign.Center,
                maxLines = 2,
            )

            // 버튼
            when {
                isOwned -> Surface(shape = RoundedCornerShape(8.dp), color = VibeGreen.copy(alpha = 0.15f)) {
                    Text("✓ 보유", style = MaterialTheme.typography.labelMedium, color = VibeGreen, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                }
                isPremiumLocked -> Surface(shape = RoundedCornerShape(8.dp), color = VibePurple.copy(alpha = 0.15f)) {
                    Text("⭐ PRO", style = MaterialTheme.typography.labelMedium, color = VibePurple, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                }
                else -> Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (canAfford) Color(0xFFFFD60A).copy(alpha = 0.15f) else VibeBorder,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text("🪙", fontSize = 12.sp)
                        Text(
                            item.coinPrice.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (canAfford) Color(0xFFFFD60A) else Color.White.copy(alpha = 0.3f),
                        )
                    }
                }
            }
        }
    }
}
