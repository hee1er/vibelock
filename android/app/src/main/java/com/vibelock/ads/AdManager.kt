package com.vibelock.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.vibelock.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AdManager"

@Singleton
class AdManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null

    private val _rewardedAdReady = MutableStateFlow(false)
    val rewardedAdReady: StateFlow<Boolean> = _rewardedAdReady.asStateFlow()

    private val _interstitialReady = MutableStateFlow(false)
    val interstitialReady: StateFlow<Boolean> = _interstitialReady.asStateFlow()

    fun initialize() {
        MobileAds.initialize(context) { initStatus ->
            Log.d(TAG, "AdMob 초기화 완료: $initStatus")
            loadInterstitial()
            loadRewarded()
        }
    }

    // ── 전면 광고 (스킵할 때) ──────────────────────────────────────

    fun loadInterstitial() {
        val request = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            BuildConfig.AD_INTERSTITIAL_ID,
            request,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "전면 광고 로드 완료")
                    interstitialAd = ad
                    _interstitialReady.value = true
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "전면 광고 로드 실패: ${error.message}")
                    interstitialAd = null
                    _interstitialReady.value = false
                }
            }
        )
    }

    fun showInterstitial(activity: Activity, onDismissed: () -> Unit) {
        val ad = interstitialAd
        if (ad == null) {
            onDismissed()
            loadInterstitial()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                _interstitialReady.value = false
                onDismissed()
                loadInterstitial() // 다음 광고 미리 로드
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.w(TAG, "전면 광고 표시 실패: ${error.message}")
                onDismissed()
                loadInterstitial()
            }
        }
        ad.show(activity)
    }

    // ── 보상형 광고 (코인 획득) ────────────────────────────────────

    fun loadRewarded() {
        val request = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            BuildConfig.AD_REWARDED_ID,
            request,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "보상형 광고 로드 완료")
                    rewardedAd = ad
                    _rewardedAdReady.value = true
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "보상형 광고 로드 실패: ${error.message}")
                    rewardedAd = null
                    _rewardedAdReady.value = false
                }
            }
        )
    }

    fun showRewarded(
        activity: Activity,
        onRewarded: (coins: Int) -> Unit,
        onDismissed: () -> Unit,
    ) {
        val ad = rewardedAd
        if (ad == null) {
            onDismissed()
            loadRewarded()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                _rewardedAdReady.value = false
                onDismissed()
                loadRewarded()
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                onDismissed()
                loadRewarded()
            }
        }
        ad.show(activity) { rewardItem ->
            // 보상: 코인 30개
            onRewarded(rewardItem.amount * 30)
        }
    }
}
