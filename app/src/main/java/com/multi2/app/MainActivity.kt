package com.multi2.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.multi2.app.ui.nav.AppNavHost
import com.multi2.app.ui.theme.Multi2Theme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main UI activity — parity with org.waxmoon.play.product.ma.MainActivity
 * (singleTop, internal action com.multi2.app.main).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Multi2Theme {
                val navController = rememberNavController()
                AppNavHost(navController = navController)
            }
        }
    }
}
