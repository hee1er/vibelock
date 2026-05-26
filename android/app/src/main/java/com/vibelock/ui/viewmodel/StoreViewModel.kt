package com.vibelock.ui.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibelock.ads.AdManager
import com.vibelock.billing.BillingEvent
import com.vibelock.billing.BillingManager
import com.vibelock.data.local.UserDataStore
import com.vibelock.data.local.UserState
import com.vibelock.data.model.ALL_STORE_ITEMS
import com.vibelock.data.model.StoreItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StoreUiState(
    val userState: UserState? = null,
    val toast: String? = null,
    val isLoadingPurchase: Boolean = false,
)

@HiltViewModel
class StoreViewModel @Inject constructor(
    private val userDataStore: UserDataStore,
    private val billingManager: BillingManager,
    private val adManager: AdManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StoreUiState())
    val uiState: StateFlow<StoreUiState> = _uiState.asStateFlow()

    val rewardedAdReady = adManager.rewardedAdReady
    val items = ALL_STORE_ITEMS

    init {
        viewModelScope.launch {
            userDataStore.userState.collect { userState ->
                _uiState.update { it.copy(userState = userState) }
            }
        }
        viewModelScope.launch {
            billingManager.event.collect { event ->
                when (event) {
                    is BillingEvent.PremiumActivated -> {
                        showToast("🎉 프리미엄 활성화 완료!")
                        billingManager.consumeEvent()
                    }
                    is BillingEvent.CoinsAdded -> {
                        showToast("✅ 코인 ${event.amount}개 충전 완료!")
                        billingManager.consumeEvent()
                    }
                    is BillingEvent.Error -> {
                        showToast("❌ ${event.message}")
                        billingManager.consumeEvent()
                    }
                    null -> {}
                }
            }
        }
    }

    // ── 아이템 구매 (코인) ────────────────────────────────────────

    fun buyItemWithCoins(item: StoreItem) = viewModelScope.launch {
        val user = _uiState.value.userState ?: return@launch
        if (item.isPremiumOnly && !user.isPremium) {
            showToast("⭐ 프리미엄 전용 아이템이에요")
            return@launch
        }
        if (user.unlockedItemIds.contains(item.id)) {
            showToast("이미 보유한 아이템이에요")
            return@launch
        }
        val success = userDataStore.spendCoins(item.coinPrice)
        if (success) {
            userDataStore.unlockItem(item.id)
            showToast("🎨 ${item.name} 획득!")
        } else {
            showToast("코인이 부족해요 (필요: ${item.coinPrice}코인)")
        }
    }

    // ── 구독 결제 ─────────────────────────────────────────────────

    fun subscribePremium(activity: Activity, skuId: String) = viewModelScope.launch {
        _uiState.update { it.copy(isLoadingPurchase = true) }
        billingManager.launchSubscription(activity, skuId)
        _uiState.update { it.copy(isLoadingPurchase = false) }
    }

    // ── 코인 충전 ─────────────────────────────────────────────────

    fun buyCoinPack(activity: Activity, skuId: String) = viewModelScope.launch {
        _uiState.update { it.copy(isLoadingPurchase = true) }
        billingManager.launchCoinPurchase(activity, skuId)
        _uiState.update { it.copy(isLoadingPurchase = false) }
    }

    // ── 보상형 광고 시청 → 코인 획득 ──────────────────────────────

    fun watchAdForCoins(activity: Activity) {
        adManager.showRewarded(
            activity = activity,
            onRewarded = { coins ->
                viewModelScope.launch {
                    userDataStore.addCoins(coins)
                    showToast("🎬 광고 시청 완료! 코인 ${coins}개 획득")
                }
            },
            onDismissed = {},
        )
    }

    fun dismissToast() = _uiState.update { it.copy(toast = null) }

    private fun showToast(msg: String) = _uiState.update { it.copy(toast = msg) }
}
