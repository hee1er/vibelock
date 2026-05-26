package com.vibelock.data.model

enum class ItemCategory { BRUSH, FRAME, STICKER, EFFECT }

data class StoreItem(
    val id: String,
    val name: String,
    val description: String,
    val emoji: String,
    val category: ItemCategory,
    val coinPrice: Int,
    val isPremiumOnly: Boolean = false,
    val previewColor: Long = 0xFFFFFFFF,
)

val ALL_STORE_ITEMS = listOf(
    // ── 브러시 ────────────────────────────────────────────────────
    StoreItem(
        id = "brush_watercolor",
        name = "수채화 브러시",
        description = "부드럽고 번지는 수채화 효과",
        emoji = "🎨",
        category = ItemCategory.BRUSH,
        coinPrice = 100,
        previewColor = 0xFF64D2FF,
    ),
    StoreItem(
        id = "brush_glow",
        name = "형광 브러시",
        description = "네온처럼 빛나는 글로우 효과",
        emoji = "✨",
        category = ItemCategory.BRUSH,
        coinPrice = 120,
        previewColor = 0xFFBF5AF2,
    ),
    StoreItem(
        id = "brush_chalk",
        name = "분필 브러시",
        description = "거칠고 자연스러운 분필 질감",
        emoji = "🖊️",
        category = ItemCategory.BRUSH,
        coinPrice = 80,
        previewColor = 0xFFFFD60A,
    ),
    StoreItem(
        id = "brush_pixel",
        name = "픽셀 브러시",
        description = "레트로 픽셀 스타일 그리기",
        emoji = "👾",
        category = ItemCategory.BRUSH,
        coinPrice = 90,
        previewColor = 0xFF32D74B,
    ),
    StoreItem(
        id = "brush_rainbow",
        name = "무지개 브러시",
        description = "그릴수록 색상이 변하는 마법 브러시",
        emoji = "🌈",
        category = ItemCategory.BRUSH,
        coinPrice = 200,
        isPremiumOnly = true,
        previewColor = 0xFFFF375F,
    ),

    // ── 락스크린 프레임 ────────────────────────────────────────────
    StoreItem(
        id = "frame_cherry",
        name = "벚꽃 프레임",
        description = "봄 벚꽃으로 꾸민 락스크린 테두리",
        emoji = "🌸",
        category = ItemCategory.FRAME,
        coinPrice = 80,
        previewColor = 0xFFFF9F9F,
    ),
    StoreItem(
        id = "frame_galaxy",
        name = "은하수 프레임",
        description = "우주를 담은 신비로운 프레임",
        emoji = "🌌",
        category = ItemCategory.FRAME,
        coinPrice = 120,
        previewColor = 0xFF1C1C4E,
    ),
    StoreItem(
        id = "frame_neon",
        name = "네온 프레임",
        description = "사이버펑크 네온 테두리",
        emoji = "💜",
        category = ItemCategory.FRAME,
        coinPrice = 100,
        previewColor = 0xFF9C6FFF,
    ),
    StoreItem(
        id = "frame_nature",
        name = "자연 프레임",
        description = "싱그러운 나뭇잎과 꽃 프레임",
        emoji = "🌿",
        category = ItemCategory.FRAME,
        coinPrice = 70,
        previewColor = 0xFF32D74B,
    ),

    // ── 스티커 팩 ─────────────────────────────────────────────────
    StoreItem(
        id = "sticker_animals",
        name = "동물 스티커 팩",
        description = "귀여운 동물 스티커 20종",
        emoji = "🐾",
        category = ItemCategory.STICKER,
        coinPrice = 150,
        previewColor = 0xFFF59E0B,
    ),
    StoreItem(
        id = "sticker_food",
        name = "음식 스티커 팩",
        description = "맛있는 음식 스티커 20종",
        emoji = "🍕",
        category = ItemCategory.STICKER,
        coinPrice = 150,
        previewColor = 0xFFEF4444,
    ),
    StoreItem(
        id = "sticker_kpop",
        name = "K-POP 스티커 팩",
        description = "K-POP 스타일 스티커 30종",
        emoji = "⭐",
        category = ItemCategory.STICKER,
        coinPrice = 200,
        isPremiumOnly = true,
        previewColor = 0xFFFF6FBF,
    ),

    // ── 특수 효과 ─────────────────────────────────────────────────
    StoreItem(
        id = "effect_confetti",
        name = "폭죽 효과",
        description = "정답 맞힐 때 화려한 폭죽 터짐",
        emoji = "🎉",
        category = ItemCategory.EFFECT,
        coinPrice = 100,
        previewColor = 0xFFFFD60A,
    ),
    StoreItem(
        id = "effect_firework",
        name = "불꽃 효과",
        description = "정답 시 불꽃놀이 애니메이션",
        emoji = "🎆",
        category = ItemCategory.EFFECT,
        coinPrice = 120,
        isPremiumOnly = true,
        previewColor = 0xFFFF453A,
    ),
)

// 코인 충전 패키지
data class CoinPack(
    val skuId: String,
    val coins: Int,
    val price: String,
    val bonus: String = "",
    val isPopular: Boolean = false,
)

val COIN_PACKS = listOf(
    CoinPack(skuId = "vibelock_coins_100", coins = 100, price = "₩1,200"),
    CoinPack(skuId = "vibelock_coins_500", coins = 500, price = "₩4,900", bonus = "+50 보너스", isPopular = true),
    CoinPack(skuId = "vibelock_coins_2000", coins = 2_000, price = "₩14,900", bonus = "+300 보너스"),
)

// 구독 플랜
data class PremiumPlan(
    val skuId: String,
    val title: String,
    val price: String,
    val period: String,
    val perMonth: String,
    val savings: String = "",
    val isRecommended: Boolean = false,
)

val PREMIUM_PLANS = listOf(
    PremiumPlan(
        skuId = "vibelock_premium_monthly",
        title = "월간 플랜",
        price = "₩3,900",
        period = "/ 월",
        perMonth = "₩3,900/월",
    ),
    PremiumPlan(
        skuId = "vibelock_premium_yearly",
        title = "연간 플랜",
        price = "₩29,900",
        period = "/ 년",
        perMonth = "₩2,492/월",
        savings = "36% 절약",
        isRecommended = true,
    ),
)
