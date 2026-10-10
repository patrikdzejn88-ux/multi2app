package com.multi2.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

private val Context.lockStore by preferencesDataStore(name = "lock")

/**
 * Private-space pattern lock — parity with ui/page/lock/password.
 * Pattern stored as salted SHA-256, never in plain text.
 */
@Singleton
class LockRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val HASH = stringPreferencesKey("pattern_hash")
        val SALT = stringPreferencesKey("pattern_salt")
        val UNLOCKED = booleanPreferencesKey("session_unlocked")
    }

    val hasLock: Flow<Boolean> = context.lockStore.data.map { it[Keys.HASH] != null }
    val sessionUnlocked: Flow<Boolean> = context.lockStore.data.map { it[Keys.UNLOCKED] ?: false }

    private fun hash(pattern: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest("$salt:$pattern".toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    suspend fun setPattern(pattern: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
            .joinToString("") { "%02x".format(it) }
        context.lockStore.edit {
            it[Keys.SALT] = salt
            it[Keys.HASH] = hash(pattern, salt)
            it[Keys.UNLOCKED] = false
        }
    }

    suspend fun verify(pattern: String): Boolean {
        var ok = false
        context.lockStore.edit { p ->
            val salt = p[Keys.SALT] ?: return@edit
            val stored = p[Keys.HASH] ?: return@edit
            if (hash(pattern, salt) == stored) {
                p[Keys.UNLOCKED] = true
                ok = true
            }
        }
        return ok
    }

    suspend fun lock() {
        context.lockStore.edit { it[Keys.UNLOCKED] = false }
    }

    /** Reset after device-credential confirmation (parity: "Reset privacy space password"). */
    suspend fun reset() {
        context.lockStore.edit {
            it.remove(Keys.HASH)
            it.remove(Keys.SALT)
            it[Keys.UNLOCKED] = false
        }
    }
}
