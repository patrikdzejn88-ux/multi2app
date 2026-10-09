package com.multi2.app.ui.proxy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.multi2.app.data.ProxyRepository
import com.multi2.app.data.SpaceRepository
import com.multi2.app.data.db.ProxyEntity
import com.multi2.app.data.db.SpaceEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProxyTestResult(
    val proxyId: Long,
    val ok: Boolean,
    val latencyMs: Long?,
    val message: String,
)

@HiltViewModel
class ProxyViewModel @Inject constructor(
    private val proxyRepository: ProxyRepository,
    spaceRepository: SpaceRepository,
) : ViewModel() {

    val proxies: StateFlow<List<ProxyEntity>> = proxyRepository.proxies
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val spaces: StateFlow<List<SpaceEntity>> = spaceRepository.spaces
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val testResult = MutableStateFlow<ProxyTestResult?>(null)

    fun save(proxy: ProxyEntity) {
        viewModelScope.launch {
            proxyRepository.upsert(proxy)
        }
    }

    fun delete(proxy: ProxyEntity) {
        viewModelScope.launch { proxyRepository.delete(proxy) }
    }

    fun toggleEnabled(proxy: ProxyEntity) {
        viewModelScope.launch { proxyRepository.upsert(proxy.copy(enabled = !proxy.enabled)) }
    }

    fun test(proxy: ProxyEntity) {
        viewModelScope.launch {
            testResult.value = ProxyTestResult(proxy.id, false, null, "Testing…")
            val result = proxyRepository.testConnectivity(proxy.host, proxy.port)
            testResult.value = result.fold(
                onSuccess = { ms -> ProxyTestResult(proxy.id, true, ms, "OK · ${ms} ms") },
                onFailure = { e -> ProxyTestResult(proxy.id, false, null, e.message ?: "Failed") },
            )
        }
    }

    fun clearTestResult() { testResult.value = null }
}
