package com.multi2.app.ui.transfer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.multi2.app.data.SpaceRepository
import com.multi2.app.data.db.ClonedAppEntity
import com.multi2.app.engine.EnginePaths
import com.multi2.app.engine.task.EngineTask
import com.multi2.app.engine.task.EngineTaskManager
import com.multi2.app.engine.task.EngineTaskType
import com.multi2.app.engine.transfer.FileTransfer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.net.ServerSocket
import javax.inject.Inject

enum class TransferPhase { IDLE, WAITING, TRANSFERRING, DONE, ERROR }

data class TransferUiState(
    val phase: TransferPhase = TransferPhase.IDLE,
    val code: String? = null,          // "ip:port" shown as QR + text on sender
    val progress: Float = 0f,
    val message: String? = null,
)

@HiltViewModel
class FileTransferViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val taskManager: EngineTaskManager,
    spaceRepository: SpaceRepository,
) : ViewModel() {

    val state = MutableStateFlow(TransferUiState())

    private val repoRef = spaceRepository

    /** All cloned apps across spaces — the transfer catalog. */
    val catalog = MutableStateFlow<List<ClonedAppEntity>>(emptyList())

    /** Public spaces as receive targets. */
    val spaces = MutableStateFlow<List<com.multi2.app.data.db.SpaceEntity>>(emptyList())

    init {
        viewModelScope.launch {
            repoRef.spaces.collect { spaceList ->
                spaces.value = spaceList.filter { !it.isPrivate }
                val all = mutableListOf<ClonedAppEntity>()
                spaceList.forEach { space -> all.addAll(repoRef.appsOfSpaceOnce(space.id)) }
                catalog.value = all
            }
        }
    }

    /** Sender: zip-serve the selected app's apks, show the connect code. */
    fun startSending(app: ClonedAppEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val apkDir = EnginePaths.apkDir(context, app.spaceId, app.pkg)
            val apks = apkDir.listFiles { f -> f.extension == "apk" }?.toList().orEmpty()
            if (apks.isEmpty()) {
                state.value = TransferUiState(TransferPhase.ERROR, message = "apk not found")
                return@launch
            }
            val ip = FileTransfer.localIpAddress()
            if (ip == null) {
                state.value = TransferUiState(TransferPhase.ERROR, message = "no network")
                return@launch
            }
            val port = ServerSocket(0).use { it.localPort }
            state.value = TransferUiState(TransferPhase.WAITING, code = "$ip:$port")
            val result = FileTransfer.serve(port, app.label, apks) { sent, total ->
                state.value = TransferUiState(
                    TransferPhase.TRANSFERRING,
                    code = "$ip:$port",
                    progress = if (total > 0) sent.toFloat() / total else 0f,
                )
            }
            state.value = result.fold(
                onSuccess = { TransferUiState(TransferPhase.DONE) },
                onFailure = { TransferUiState(TransferPhase.ERROR, message = it.message) },
            )
        }
    }

    /** Receiver: connect to "ip:port", download, import as .apks. */
    fun startReceiving(code: String, spaceId: Int) {
        val parts = code.trim().split(":")
        val ip = parts.getOrNull(0)?.trim()
        val port = parts.getOrNull(1)?.trim()?.toIntOrNull()
        if (ip.isNullOrBlank() || port == null) {
            state.value = TransferUiState(TransferPhase.ERROR, message = "invalid code")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            state.value = TransferUiState(TransferPhase.TRANSFERRING)
            val dest = File(context.cacheDir, "transfer/received_${System.currentTimeMillis()}.apks")
            val result = FileTransfer.receive(ip, port, dest) { got, total ->
                state.value = TransferUiState(
                    TransferPhase.TRANSFERRING,
                    progress = if (total > 0) got.toFloat() / total else 0f,
                )
            }
            result.onSuccess { name ->
                taskManager.submit(
                    EngineTask(
                        id = System.nanoTime(),
                        type = EngineTaskType.INSTALL,
                        pkg = "transfer",
                        spaceId = spaceId,
                        label = name,
                        sourceApkPath = dest.absolutePath,
                    ),
                )
                state.value = TransferUiState(TransferPhase.DONE, message = name)
            }.onFailure {
                state.value = TransferUiState(TransferPhase.ERROR, message = it.message)
            }
        }
    }

    fun reset() { state.value = TransferUiState() }
}

/** QR bitmap of the connect code (ZXing core). */
fun qrBitmap(content: String, size: Int = 512): Bitmap {
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
    for (x in 0 until size) {
        for (y in 0 until size) {
            bitmap.setPixel(x, y, if (matrix[x, y]) AndroidColor.BLACK else AndroidColor.WHITE)
        }
    }
    return bitmap
}
