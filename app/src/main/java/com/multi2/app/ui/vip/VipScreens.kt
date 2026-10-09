package com.multi2.app.ui.vip

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.multi2.app.R
import com.multi2.app.data.COINS_PER_FREE_VIP
import com.multi2.app.data.FREE_VIP_DAYS
import com.multi2.app.data.VipLevel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Shared toast wiring for VipEvent one-shots. */
@Composable
private fun VipEventEffect(viewModel: VipViewModel) {
    val context = LocalContext.current
    val event by viewModel.event.collectAsStateWithLifecycle()
    LaunchedEffect(event) {
        val e = event ?: return@LaunchedEffect
        val msg = when (e) {
            VipEvent.PAY_SUCCESS -> R.string.pay_success
            VipEvent.BILLING_UNAVAILABLE -> R.string.billing_unavailable
            VipEvent.AD_NO_FILL -> R.string.free_vip_ad_no_fill
            VipEvent.COIN_NOT_ENOUGH -> R.string.free_vip_coin_not_enough
            VipEvent.REDEEM_SUCCESS -> R.string.code_redeem_success
            VipEvent.REDEEM_FAIL -> R.string.code_redeem_fail
            VipEvent.REBIND_SUCCESS -> R.string.vip_rebind_success
            VipEvent.REBIND_LIMIT -> R.string.vip_rebind_limit
        }
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        viewModel.consumeEvent()
    }
}

@Composable
private fun VipStatusCard(level: VipLevel, expireAt: Long, coins: Int) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                when (level) {
                    VipLevel.NONE -> stringResource(R.string.user_info_normal)
                    VipLevel.FREE -> stringResource(R.string.user_info_normal_vip)
                    VipLevel.PAY -> stringResource(R.string.user_info_pay_vip)
                    VipLevel.SVIP -> stringResource(R.string.user_info_pay_svip)
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            if (expireAt > 0) {
                val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                Text(stringResource(R.string.user_info_expired_date, fmt.format(Date(expireAt))))
            }
            Text(stringResource(R.string.user_info_coin, coins))
        }
    }
}

/** Buy VIP — one-time 30/90/365 days, device-bound. Billing SDK in M4. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyVipScreen(onBack: () -> Unit, viewModel: VipViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    VipEventEffect(viewModel)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.buy_vip_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            VipStatusCard(state.level, state.expireAt, state.coins)

            Card(Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.vip_privileges),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            viewModel.plans.forEach { plan ->
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                when (plan.days) {
                                    30 -> stringResource(R.string.fee_month)
                                    90 -> stringResource(R.string.fee_quarter)
                                    else -> stringResource(R.string.fee_annual)
                                },
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(plan.priceHint, style = MaterialTheme.typography.bodySmall)
                        }
                        Button(onClick = { viewModel.buy(plan, devMode = false) }) {
                            Text(stringResource(R.string.drawer_vip_buy_txt))
                        }
                    }
                }
            }

            Text(
                stringResource(R.string.pay_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
            Text(
                stringResource(R.string.buy_vip_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
    }
}

/** Free VIP — rewarded ads → coins → redeem. Ads SDK in M4. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreeVipScreen(onBack: () -> Unit, viewModel: VipViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    VipEventEffect(viewModel)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.drawer_vip_free_txt)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            VipStatusCard(state.level, state.expireAt, state.coins)
            Button(onClick = { viewModel.watchAd() }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.free_vip_watch_ad))
            }
            OutlinedButton(
                onClick = { viewModel.redeemWithCoins() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.free_vip_redeem, COINS_PER_FREE_VIP, FREE_VIP_DAYS))
            }
        }
    }
}

/** Redeem code — backend validation in M4. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeRedeemScreen(onBack: () -> Unit, viewModel: VipViewModel = hiltViewModel()) {
    var code by remember { mutableStateOf("") }
    VipEventEffect(viewModel)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.code_redeem_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.code_redeem_note), style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                placeholder = { Text(stringResource(R.string.code_redeem_input_holder)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { viewModel.redeemCode(code.trim()) },
                enabled = code.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.code_redeem_submit)) }
        }
    }
}

/** VIP migration via Google order (GPA.****), once per day. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VipRebindScreen(onBack: () -> Unit, viewModel: VipViewModel = hiltViewModel()) {
    var orderId by remember { mutableStateOf("") }
    VipEventEffect(viewModel)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.vip_rebind_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.vip_rebind_note), style = MaterialTheme.typography.bodySmall)
            Text(
                stringResource(R.string.vip_rebind_limit),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
            OutlinedTextField(
                value = orderId,
                onValueChange = { orderId = it },
                placeholder = { Text(stringResource(R.string.vip_rebind_input_holder)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { viewModel.rebind(orderId.trim()) },
                enabled = orderId.startsWith("GPA.", ignoreCase = true) && orderId.length > 8,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.vip_rebind_submit)) }
        }
    }
}
