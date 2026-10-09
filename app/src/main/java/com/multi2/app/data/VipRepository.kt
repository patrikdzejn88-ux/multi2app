package com.multi2.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.vipStore by preferencesDataStore(name = "vip")

enum class VipLevel { NONE, FREE, PAY, SVIP }

data class VipState(
    val level: VipLevel = VipLevel.NONE,
    val coins: Int = 0,
    val expireAt: Long = 0L,          // epoch millis; 0 = no vip
    val lastRebindAt: Long = 0L,
) {
    val isVip: Boolean get() = level != VipLevel.NONE && expireAt > System.currentTimeMillis()
}

/** VIP plans — parity: 30 / 90 / 365 days, one-time purchase, device-bound. */
data class VipPlan(val days: Int, val sku: String, val priceHint: String)

val VIP_PLANS = listOf(
    VipPlan(30, "vip_30d", "$4.99"),
    VipPlan(90, "vip_90d", "$9.99"),
    VipPlan(365, "vip_365d", "$19.99"),
)

/** Free-VIP economy: watch rewarded ad → 1 coin; N coins → 30 days. */
const val COINS_PER_FREE_VIP = 30
const val FREE_VIP_DAYS = 30
const val REBIND_INTERVAL_MS = 24L * 60 * 60 * 1000 // once a day

@Singleton
class VipRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val LEVEL = stringPreferencesKey("vip_level")
        val COINS = intPreferencesKey("coins")
        val EXPIRE_AT = longPreferencesKey("vip_expire_at")
        val LAST_REBIND = longPreferencesKey("last_rebind_at")
    }

    val state: Flow<VipState> = context.vipStore.data.map { p ->
        VipState(
            level = p[Keys.LEVEL]?.let { runCatching { VipLevel.valueOf(it) }.getOrNull() } ?: VipLevel.NONE,
            coins = p[Keys.COINS] ?: 0,
            expireAt = p[Keys.EXPIRE_AT] ?: 0L,
            lastRebindAt = p[Keys.LAST_REBIND] ?: 0L,
        )
    }

    suspend fun addCoins(count: Int) {
        context.vipStore.edit { it[Keys.COINS] = (it[Keys.COINS] ?: 0) + count }
    }

    /** Redeem free VIP with coins; returns false when not enough. */
    suspend fun redeemFreeVip(): Boolean {
        var ok = false
        context.vipStore.edit { p ->
            val coins = p[Keys.COINS] ?: 0
            if (coins >= COINS_PER_FREE_VIP) {
                p[Keys.COINS] = coins - COINS_PER_FREE_VIP
                p[Keys.LEVEL] = VipLevel.FREE.name
                p[Keys.EXPIRE_AT] = System.currentTimeMillis() + FREE_VIP_DAYS * 86400_000L
                ok = true
            }
        }
        return ok
    }

    /** M4: called after successful Play Billing purchase. */
    suspend fun activatePaidVip(days: Int) {
        context.vipStore.edit {
            it[Keys.LEVEL] = VipLevel.PAY.name
            it[Keys.EXPIRE_AT] = System.currentTimeMillis() + days * 86400_000L
        }
    }

    /** M4: called after successful redeem-code validation on backend. */
    suspend fun activateByCode(days: Int) {
        context.vipStore.edit {
            it[Keys.LEVEL] = VipLevel.PAY.name
            it[Keys.EXPIRE_AT] = System.currentTimeMillis() + days * 86400_000L
        }
    }

    /** VIP migration: allowed once per day; returns remaining wait ms or 0 on success. */
    suspend fun rebind(orderId: String): Long {
        val now = System.currentTimeMillis()
        var wait = 0L
        context.vipStore.edit { p ->
            val last = p[Keys.LAST_REBIND] ?: 0L
            val elapsed = now - last
            if (elapsed < REBIND_INTERVAL_MS) {
                wait = REBIND_INTERVAL_MS - elapsed
            } else {
                p[Keys.LAST_REBIND] = now
                // M4: validate orderId (GPA.****) on backend, then activatePaidVip
            }
        }
        return wait
    }
}
