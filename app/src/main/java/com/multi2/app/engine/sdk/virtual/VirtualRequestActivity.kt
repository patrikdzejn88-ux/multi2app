package com.multi2.app.engine.sdk.virtual

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.multi2.app.R
import com.multi2.app.ui.theme.Multi2Theme

/**
 * Engine task-request dialog — parity with
 * org.waxmoon.play.engine.sdk.virtual.VirtualRequestActivity
 * (action com.multi2.action.request, translucent dialog theme).
 *
 * Shows engine task states: Checking → Waiting → Copying apk → Analyzing →
 * Installing/Uninstalling/Repairing/Clearing/Force stop/Generating → result.
 * Real task wiring lands in M2 together with the engine core.
 */
class VirtualRequestActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val stateText = intent.getStringExtra(EXTRA_STATE)
            ?: getString(R.string.engine_request_task_checking)
        setContent {
            Multi2Theme {
                EngineRequestDialog(stateText = stateText, onClose = { finish() })
            }
        }
    }

    companion object {
        const val EXTRA_STATE = "com.multi2.extra.REQUEST_STATE"
    }
}

@Composable
fun EngineRequestDialog(stateText: String, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose) {
        Card {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text(stateText, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onClose) {
                    Text(stringResource(R.string.engine_request_close))
                }
            }
        }
    }
}
