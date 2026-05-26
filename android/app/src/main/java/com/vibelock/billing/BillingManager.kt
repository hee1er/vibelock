package com.vibelock.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.vibelock.BuildConfig
import com.vibelock.data.local.UserDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "BillingManager"

sealed class BillingEvent {
    data object PremiumActivated : BillingEvent()
    data class CoinsAdded(val amount: Int) : BillingEvent()
    data class Error(val message: String) : BillingEvent()
}

@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userDataStore: UserDataStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _event = MutableStateFlow<BillingEvent?>(null)
    val event: StateFlow<BillingEvent?> = _event.asStateFlow()

    private val purchasesUpdatedListener = PurchasesUpdatedListener { result, purchases ->
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { handlePurchase(it) }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "유저가 결제 취소")
            }
            else -> {
                _event.value = BillingEvent.Error("결제 오류: ${result.debugMessage}")
            }
        }
    }

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    fun connect() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing 연결 성공")
                    scope.launch { acknowledgePendingPurchases() }
                }
            }
            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing 연결 끊김 — 재연결 시도")
                connect()
            }
        })
    }

    // ── 구독 시작 ─────────────────────────────────────────────────

    suspend fun launchSubscription(activity: Activity, skuId: String) {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(skuId)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            ).build()

        val result = billingClient.queryProductDetails(params)
        val productDetails = result.productDetailsList?.firstOrNull()
        if (productDetails == null) {
            _event.value = BillingEvent.Error("상품 정보를 찾을 수 없습니다")
            return
        }

        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: run {
            _event.value = BillingEvent.Error("구독 제안 없음")
            return
        }

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .setOfferToken(offerToken)
                        .build()
                )
            ).build()

        withContext(Dispatchers.Main) {
            billingClient.launchBillingFlow(activity, flowParams)
        }
    }

    // ── 코인 팩 구매 ──────────────────────────────────────────────

    suspend fun launchCoinPurchase(activity: Activity, skuId: String) {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(skuId)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            ).build()

        val result = billingClient.queryProductDetails(params)
        val productDetails = result.productDetailsList?.firstOrNull()
        if (productDetails == null) {
            _event.value = BillingEvent.Error("상품을 찾을 수 없습니다")
            return
        }

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .build()
                )
            ).build()

        withContext(Dispatchers.Main) {
            billingClient.launchBillingFlow(activity, flowParams)
        }
    }

    // ── 구매 처리 ─────────────────────────────────────────────────

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        scope.launch {
            when {
                purchase.products.any { it.contains("premium") } -> {
                    // 구독 만료일: 30일 or 365일
                    val isYearly = purchase.products.any { it.contains("yearly") }
                    val expiryMs = System.currentTimeMillis() +
                            if (isYearly) 365L * 24 * 3600 * 1000
                            else 30L * 24 * 3600 * 1000
                    userDataStore.setPremium(expiryMs)
                    _event.value = BillingEvent.PremiumActivated
                    acknowledgePurchase(purchase)
                }
                purchase.products.any { it.contains("coins") } -> {
                    val coins = when {
                        purchase.products.any { it.contains("2000") } -> 2300
                        purchase.products.any { it.contains("500") } -> 550
                        else -> 100
                    }
                    userDataStore.addCoins(coins)
                    _event.value = BillingEvent.CoinsAdded(coins)
                    acknowledgePurchase(purchase)
                }
            }
        }
    }

    private suspend fun acknowledgePurchase(purchase: Purchase) {
        if (purchase.isAcknowledged) return
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.acknowledgePurchase(params)
    }

    private suspend fun acknowledgePendingPurchases() {
        // 구독 미확인 건 처리
        val result = billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )
        result.purchasesList.forEach { handlePurchase(it) }

        val inappResult = billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        inappResult.purchasesList.forEach { handlePurchase(it) }
    }

    fun consumeEvent() { _event.value = null }
}
