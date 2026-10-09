package com.multi2.app.ui.vip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.multi2.app.data.VIP_PLANS
import com.multi2.app.data.VipPlan
import com.multi2.app.data.VipRepository
import com.multi2.app.data.VipState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VipViewModel @Inject constructor(
    private val vipRepository: VipRepository,
) : ViewModel() {

    val state: StateFlow<VipState> = vipRepository.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VipState())

    val plans: List<VipPlan> = VIP_PLANS

    /** One-shot UI events surfaced as toasts. */
    val event = MutableStateFlow<VipEvent?>(null)

    fun consumeEvent() { event.value = null }

    /** M4: Play Billing launch; simulated activation in dev mode only. */
    fun buy(plan: VipPlan, devMode: Boolean) {
        viewModelScope.launch {
            if (devMode) {
                vipRepository.activatePaidVip(plan.days)
                event.value = VipEvent.PAY_SUCCESS
            } else {
                event.value = VipEvent.BILLING_UNAVAILABLE
            }
        }
    }

    /** M4: rewarded ad; returns coin when ad completes. */
    fun watchAd() {
        viewModelScope.launch {
            // Ads SDK lands in M4 — report no-fill like the original does
            event.value = VipEvent.AD_NO_FILL
        }
    }

    fun redeemWithCoins() {
        viewModelScope.launch {
            event.value = if (vipRepository.redeemFreeVip()) VipEvent.REDEEM_SUCCESS
            else VipEvent.COIN_NOT_ENOUGH
        }
    }

    fun redeemCode(code: String) {
        viewModelScope.launch {
            // M4: backend validation
            event.value = VipEvent.REDEEM_FAIL
        }
    }

    fun rebind(orderId: String) {
        viewModelScope.launch {
            val wait = vipRepository.rebind(orderId)
            event.value = if (wait > 0) VipEvent.REBIND_LIMIT else VipEvent.REBIND_SUCCESS
        }
    }
}

enum class VipEvent {
    PAY_SUCCESS, BILLING_UNAVAILABLE, AD_NO_FILL,
    COIN_NOT_ENOUGH, REDEEM_SUCCESS, REDEEM_FAIL,
    REBIND_SUCCESS, REBIND_LIMIT,
}
