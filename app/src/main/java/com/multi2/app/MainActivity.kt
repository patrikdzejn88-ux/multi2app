package com.multi2.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.multi2.app.data.NightModeSetting
import com.multi2.app.data.SettingsRepository
import com.multi2.app.ui.nav.AppNavHost
import com.multi2.app.ui.theme.Multi2Theme
import com.multi2.app.ui.theme.NightMode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Main UI activity — parity with org.waxmoon.play.product.ma.MainActivity
 * (singleTop, internal action com.multi2.app.main).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val nightMode = mutableStateOf(NightMode.AUTO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch {
            settingsRepository.settings.collect { settings ->
                nightMode.value = when (settings.nightMode) {
                    NightModeSetting.AUTO -> NightMode.AUTO
                    NightModeSetting.DARK -> NightMode.DARK
                    NightModeSetting.LIGHT -> NightMode.LIGHT
                }
            }
        }

        setContent {
            val mode by nightMode
            Multi2Theme(nightMode = mode) {
                val navController = rememberNavController()
                AppNavHost(navController = navController)
            }
        }
    }
}
