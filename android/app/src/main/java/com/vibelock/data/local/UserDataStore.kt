package com.vibelock.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

val Context.userDataStore: DataStore<Preferences> by preferencesDataStore("vibelock_user")

data class UserState(
    val userId: String,
    val displayName: String,
    val isPremium: Boolean,
    val premiumExpiryMs: Long,       // 0 = 없음
    val coinBalance: Int,
    val skipsToday: Int,
    val lastSkipDate: String,        // "2026-05-26"
    val unlockedItemIds: Set<String>,
    val totalSkips: Int,
    val totalMatches: Int,
)

@Singleton
class UserDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        val KEY_USER_ID = stringPreferencesKey("user_id")
        val KEY_DISPLAY_NAME = stringPreferencesKey("display_name")
        val KEY_IS_PREMIUM = booleanPreferencesKey("is_premium")
        val KEY_PREMIUM_EXPIRY = longPreferencesKey("premium_expiry_ms")
        val KEY_COINS = intPreferencesKey("coin_balance")
        val KEY_SKIPS_TODAY = intPreferencesKey("skips_today")
        val KEY_LAST_SKIP_DATE = stringPreferencesKey("last_skip_date")
        val KEY_UNLOCKED_ITEMS = stringPreferencesKey("unlocked_items") // comma-separated
        val KEY_TOTAL_SKIPS = intPreferencesKey("total_skips")
        val KEY_TOTAL_MATCHES = intPreferencesKey("total_matches")

        const val FREE_DAILY_SKIPS = 5
        const val COINS_PER_REWARDED_AD = 30
    }

    val userState: Flow<UserState> = context.userDataStore.data.map { prefs ->
        val today = LocalDate.now().toString()
        val lastSkipDate = prefs[KEY_LAST_SKIP_DATE] ?: ""
        val skipsToday = if (lastSkipDate == today) prefs[KEY_SKIPS_TODAY] ?: 0 else 0

        UserState(
            userId = prefs[KEY_USER_ID] ?: UUID.randomUUID().toString().also { saveUserId(it) },
            displayName = prefs[KEY_DISPLAY_NAME] ?: "Anonymous",
            isPremium = (prefs[KEY_IS_PREMIUM] ?: false) &&
                    (prefs[KEY_PREMIUM_EXPIRY] ?: 0L) > System.currentTimeMillis(),
            premiumExpiryMs = prefs[KEY_PREMIUM_EXPIRY] ?: 0L,
            coinBalance = prefs[KEY_COINS] ?: 0,
            skipsToday = skipsToday,
            lastSkipDate = lastSkipDate,
            unlockedItemIds = prefs[KEY_UNLOCKED_ITEMS]
                ?.split(",")
                ?.filter { it.isNotBlank() }
                ?.toSet() ?: emptySet(),
            totalSkips = prefs[KEY_TOTAL_SKIPS] ?: 0,
            totalMatches = prefs[KEY_TOTAL_MATCHES] ?: 0,
        )
    }

    private suspend fun saveUserId(id: String) {
        context.userDataStore.edit { it[KEY_USER_ID] = id }
    }

    suspend fun setDisplayName(name: String) {
        context.userDataStore.edit { it[KEY_DISPLAY_NAME] = name.take(20) }
    }

    suspend fun setPremium(expiryMs: Long) {
        context.userDataStore.edit {
            it[KEY_IS_PREMIUM] = true
            it[KEY_PREMIUM_EXPIRY] = expiryMs
        }
    }

    suspend fun clearPremium() {
        context.userDataStore.edit {
            it[KEY_IS_PREMIUM] = false
            it[KEY_PREMIUM_EXPIRY] = 0L
        }
    }

    suspend fun addCoins(amount: Int) {
        context.userDataStore.edit {
            it[KEY_COINS] = ((it[KEY_COINS] ?: 0) + amount).coerceAtLeast(0)
        }
    }

    suspend fun spendCoins(amount: Int): Boolean {
        var success = false
        context.userDataStore.edit { prefs ->
            val current = prefs[KEY_COINS] ?: 0
            if (current >= amount) {
                prefs[KEY_COINS] = current - amount
                success = true
            }
        }
        return success
    }

    suspend fun recordSkip() {
        val today = LocalDate.now().toString()
        context.userDataStore.edit { prefs ->
            val lastDate = prefs[KEY_LAST_SKIP_DATE] ?: ""
            val currentCount = if (lastDate == today) prefs[KEY_SKIPS_TODAY] ?: 0 else 0
            prefs[KEY_SKIPS_TODAY] = currentCount + 1
            prefs[KEY_LAST_SKIP_DATE] = today
            prefs[KEY_TOTAL_SKIPS] = (prefs[KEY_TOTAL_SKIPS] ?: 0) + 1
        }
    }

    suspend fun recordMatch() {
        context.userDataStore.edit {
            it[KEY_TOTAL_MATCHES] = (it[KEY_TOTAL_MATCHES] ?: 0) + 1
        }
    }

    suspend fun unlockItem(itemId: String) {
        context.userDataStore.edit { prefs ->
            val current = prefs[KEY_UNLOCKED_ITEMS]?.split(",")?.filter { it.isNotBlank() }?.toMutableSet() ?: mutableSetOf()
            current.add(itemId)
            prefs[KEY_UNLOCKED_ITEMS] = current.joinToString(",")
        }
    }
}
