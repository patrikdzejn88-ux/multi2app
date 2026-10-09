package com.multi2.app.ui.nav

/**
 * Navigation routes — 1:1 mirror of the original deep-link router:
 *   moon://main.router.ma/<route>  →  multi2://main.router/<route>
 *
 * Original routes:
 *   /apps /about /main/settings /buy/vip /ads/vip /account/code_redeem
 *   /account/vip_rebind /detail/{pkg}/{user} /vinfo /folder
 *   /launch/{pkg}/{user} /minfo /sinfo /language
 */
object Routes {
    const val LAUNCHER = "launcher"                 // home: virtual space cards
    const val APPS = "apps"                         // import apps (installed list)
    const val CHOICE = "choice"                     // pick apps to clone
    const val APP_DETAIL = "detail/{pkg}/{userId}"  // cloned app detail
    const val SETTINGS = "main/settings"
    const val ABOUT = "about"
    const val BUY_VIP = "buy/vip"
    const val FREE_VIP = "ads/vip"
    const val CODE_REDEEM = "account/code_redeem"
    const val VIP_REBIND = "account/vip_rebind"
    const val LANGUAGE = "language"
    const val MEMORY = "minfo"                      // task manager
    const val STORAGE = "sinfo"                     // storage manager
    const val SPACE_FOLDER = "folder"
    const val FILE_TRANSFER = "file_transfer"
    const val VIRTUAL_DEVICE_INFO = "vinfo"
    const val LOCK = "lock"
    const val PRIVATE_SPACE = "private_space"

    fun appDetail(pkg: String, userId: Int) = "detail/$pkg/$userId"
    fun launch(pkg: String, userId: Int) = "launch/$pkg/$userId"
}
