package com.multi2.app.data

import com.multi2.app.data.db.ProxyDao
import com.multi2.app.data.db.ProxyEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProxyRepository @Inject constructor(
    private val proxyDao: ProxyDao,
) {
    val proxies: Flow<List<ProxyEntity>> = proxyDao.observeAll()

    suspend fun upsert(proxy: ProxyEntity): Long = proxyDao.upsert(proxy)

    suspend fun delete(proxy: ProxyEntity) = proxyDao.delete(proxy)

    /** Proxy chosen for a space: per-space override wins over global default. */
    suspend fun activeForSpace(spaceId: Int): ProxyEntity? = proxyDao.activeForSpace(spaceId)

    /** Real TCP connectivity check to host:port (4s timeout). */
    suspend fun testConnectivity(host: String, port: Int): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            val start = System.nanoTime()
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), 4000)
            }
            (System.nanoTime() - start) / 1_000_000
        }
    }
}
