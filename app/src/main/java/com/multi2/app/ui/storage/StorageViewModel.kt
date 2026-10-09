package com.multi2.app.ui.storage

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.multi2.app.data.SpaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * Sandbox layout (engine M2 populates):
 *   filesDir/virtual/spaces/<spaceId>/<pkg>/{apk,data,cache,media,obb}
 */
data class AppStorageUi(
    val pkg: String,
    val label: String,
    val spaceId: Int,
    val installBytes: Long,
    val dataBytes: Long,
    val cacheBytes: Long,
    val mediaBytes: Long,
    val obbBytes: Long,
) {
    val totalBytes: Long get() = installBytes + dataBytes + cacheBytes + mediaBytes + obbBytes
}

enum class StorageSort { BY_SIZE, BY_SPACE }

@HiltViewModel
class StorageViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val spaceRepository: SpaceRepository,
) : ViewModel() {

    val items = MutableStateFlow<List<AppStorageUi>>(emptyList())
    val sort = MutableStateFlow(StorageSort.BY_SIZE)
    val busy = MutableStateFlow(false)

    private val sandboxRoot: File get() = File(context.filesDir, "virtual/spaces")

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            busy.value = true
            items.value = withContext(Dispatchers.IO) { scan() }
            busy.value = false
        }
    }

    private fun scan(): List<AppStorageUi> {
        val root = sandboxRoot
        if (!root.exists()) return emptyList()
        val result = mutableListOf<AppStorageUi>()
        root.listFiles()?.forEach { spaceDir ->
            val spaceId = spaceDir.name.toIntOrNull() ?: return@forEach
            spaceDir.listFiles()?.forEach { pkgDir ->
                result += AppStorageUi(
                    pkg = pkgDir.name,
                    label = pkgDir.name,
                    spaceId = spaceId,
                    installBytes = dirSize(File(pkgDir, "apk")),
                    dataBytes = dirSize(File(pkgDir, "data")),
                    cacheBytes = dirSize(File(pkgDir, "cache")),
                    mediaBytes = dirSize(File(pkgDir, "media")),
                    obbBytes = dirSize(File(pkgDir, "obb")),
                )
            }
        }
        return sort(result, sort.value)
    }

    private fun sort(list: List<AppStorageUi>, by: StorageSort): List<AppStorageUi> = when (by) {
        StorageSort.BY_SIZE -> list.sortedByDescending { it.totalBytes }
        StorageSort.BY_SPACE -> list.sortedWith(compareBy({ it.spaceId }, { it.label }))
    }

    fun setSort(value: StorageSort) {
        sort.value = value
        items.value = sort(items.value, value)
    }

    fun clearCache(item: AppStorageUi) = mutate(item) { dir ->
        File(dir, "cache").deleteRecursively()
    }

    fun clearData(item: AppStorageUi) = mutate(item) { dir ->
        File(dir, "data").deleteRecursively()
        File(dir, "cache").deleteRecursively()
    }

    fun uninstall(item: AppStorageUi) {
        viewModelScope.launch {
            spaceRepository.removeApp(item.pkg, item.spaceId)
            withContext(Dispatchers.IO) {
                File(sandboxRoot, "${item.spaceId}/${item.pkg}").deleteRecursively()
            }
            refresh()
        }
    }

    private fun mutate(item: AppStorageUi, op: (File) -> Unit) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                op(File(sandboxRoot, "${item.spaceId}/${item.pkg}"))
            }
            refresh()
        }
    }

    private fun dirSize(dir: File): Long {
        if (!dir.exists()) return 0L
        var total = 0L
        dir.walkTopDown().forEach { if (it.isFile) total += it.length() }
        return total
    }
}

fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB")
    var value = bytes / 1024.0
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024.0
        unit++
    }
    return "%.1f %s".format(value, units[unit])
}
