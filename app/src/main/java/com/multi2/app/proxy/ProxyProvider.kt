package com.multi2.app.proxy

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri

/**
 * Proxy provider — parity with org.waxmoon.play.proxy.ProxyProvider.
 * One subclass per proxy process slot; IPC endpoint inside each clone process.
 */
open class ProxyProvider : ContentProvider() {
    override fun onCreate(): Boolean = true
    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

    class P0 : ProxyProvider()
    class P1 : ProxyProvider()
    class P2 : ProxyProvider()
    class P3 : ProxyProvider()
    class P4 : ProxyProvider()
    class P5 : ProxyProvider()
    class P6 : ProxyProvider()
    class P7 : ProxyProvider()
    class P8 : ProxyProvider()
    class P9 : ProxyProvider()
}
