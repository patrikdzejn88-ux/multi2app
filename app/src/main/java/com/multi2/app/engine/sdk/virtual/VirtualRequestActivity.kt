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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.multi2.app.R
import com.multi2.app.engine.task.EngineTask
import com.multi2.app.engine.task.EngineTaskManager
import com.multi2.app.ui.theme.Multi2Theme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Engine task-request dialog — parity with
 * org.waxmoon.play.engine.sdk.virtual.VirtualRequestActivity
 * (action com.multi2.action.request, translucent dialog theme).
 *
 * Shows the LIVE current engine task state (Checking → Copying → Analyzing →
 * Installing/… → result) with the original's strings.
 */
@AndroidEntryPoint
class VirtualRequestActivity : ComponentActivity() {

    @Inject lateinit var taskManager: EngineTaskManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Multi2Theme {
                val task by taskManager.current.collectAsStateWithLifecycle()
                EngineRequestDialog(task = task, onClose = { finish() })
            }
        }
    }

    companion object {
        const val EXTRA_STATE = "com.multi2.extra.REQUEST_STATE"
    }
}

@Composable
fun EngineRequestDialog(task: EngineTask?, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose) {
        Card {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (task == null) {
                    Text(
                        stringResource(R.string.engine_request_task_in_queue),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                } else {
                    val isRunning = task.state in RUNNING_STATES
                    if (isRunning) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                    }
                    Text(task.label, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(task.state.stringRes), style = MaterialTheme.typography.bodyLarge)
                    task.error?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onClose) {
                    Text(stringResource(R.string.engine_request_close))
                }
            }
        }
    }
}

private val RUNNING_STATES = setOf(
    com.multi2.app.engine.task.EngineTaskState.CHECKING,
    com.multi2.app.engine.task.EngineTaskState.QUEUED,
    com.multi2.app.engine.task.EngineTaskState.COPYING,
    com.multi2.app.engine.task.EngineTaskState.ANALYZING,
    com.multi2.app.engine.task.EngineTaskState.INSTALLING,
    com.multi2.app.engine.task.EngineTaskState.UNINSTALLING,
    com.multi2.app.engine.task.EngineTaskState.REPAIRING,
    com.multi2.app.engine.task.EngineTaskState.CLEARING_CACHE,
    com.multi2.app.engine.task.EngineTaskState.CLEARING_DATA,
    com.multi2.app.engine.task.EngineTaskState.FORCE_STOPPING,
    com.multi2.app.engine.task.EngineTaskState.GENERATING,
)
