package com.multi2.app.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "spaces")
data class SpaceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val locked: Boolean = false,
    val sortOrder: Int = 0,
)

@Entity(
    tableName = "cloned_apps",
    primaryKeys = ["pkg", "spaceId"],
    indices = [Index("pkg"), Index("spaceId")],
)
data class ClonedAppEntity(
    val pkg: String,
    val spaceId: Int,
    val label: String,
    val pinned: Boolean = false,
    val hidden: Boolean = false,
    val launchMode: Int = 0,
    val fakeLabel: String? = null,
    val customIconPath: String? = null,
    val traceable: Boolean = false,        // "Support GG" toggle
    val googleInstaller: Boolean = false,
    val virtualSdcard: Boolean = false,
    val hideDevMode: Boolean = false,
    val denyReadHostApps: Boolean = false,
    val installState: Int = INSTALL_STATE_PENDING,
    val createdAt: Long = System.currentTimeMillis(),
) {
    companion object {
        const val INSTALL_STATE_PENDING = 0
        const val INSTALL_STATE_INSTALLED = 1
        const val INSTALL_STATE_FAILED = 2
    }
}

@Entity(tableName = "proxies")
data class ProxyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val protocol: String,          // SOCKS5 | HTTP | HTTPS
    val host: String,
    val port: Int,
    val username: String? = null,
    val password: String? = null,
    val enabled: Boolean = true,
    val spaceId: Int? = null,      // null = global default for all spaces
) {
    companion object {
        const val PROTOCOL_SOCKS5 = "SOCKS5"
        const val PROTOCOL_HTTP = "HTTP"
        const val PROTOCOL_HTTPS = "HTTPS"
        val PROTOCOLS = listOf(PROTOCOL_SOCKS5, PROTOCOL_HTTP, PROTOCOL_HTTPS)
    }
}
