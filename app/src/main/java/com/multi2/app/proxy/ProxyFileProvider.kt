package com.multi2.app.proxy

import androidx.core.content.FileProvider

/**
 * FileProvider living in :admin — parity with ProxyFileProvider.
 * Shares clone files (apks, exports) across process boundary. Meta-data wired in M2.
 */
class ProxyFileProvider : FileProvider()
