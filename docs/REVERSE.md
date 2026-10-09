# Reverse Engineering Report — "Multi App" (WaxMoon)

Source APK: `Multi App.apk` (51,355,028 bytes, sha of original kept local)
Package: `com.waxmoon.ma.gp` · sharedUserId `com.waxmoon.ma.gp`
Internal codename: **MultiApp Nova** (`app_name_multiapp`)
Application class: `org.waxmoon.play.market.MoonApplication`
Compiled against SDK 36. Decompiled: 27,005 Java classes (jadx), heavily obfuscated (R8 full-mode, single-letter packages).

---

## 1. What this app is

A **dual-space / app-cloner** ("virtual space") product: users create isolated
virtual spaces, install clones of installed apps (or APK/XAPK/APKS files) into
them, run multiple accounts in parallel, lock spaces with pattern/biometrics,
fake device identity per space, and manage storage/memory of clones.

Monetization: one-time VIP purchases (30/90/365 days, device-bound), free VIP
via rewarded ads + coins, redemption codes, VIP migration via Google order id.
Ads: **AdMob + AppLovin + Pangle (TikTok)**.

---

## 2. Process architecture (VirtualApp-family)

| Process | Purpose |
|---|---|
| `main` | UI (Compose), settings, billing, ads |
| `:admin` | Virtualization manager: `AdminService`, `AssistService`, `CompatAdminService` (all foreground specialUse), `AdminProvider`, `AssistProvider`, `ToolProvider`, `ProxyFileProvider`, `ProxyNotificationService` (BIND_NOTIFICATION_LISTENER_SERVICE), `BridgeLauncherActivity`, `StartAdminActivity`, `AssistActivity`, `ProxyActivity$I`, `ProxyReceiver$I` |
| `:proxy.p0` … `:proxy.p499` | **500 stub process slots** — each running clone gets one (`AssistActivity$P*`, `ProxyProvider$P*` per slot) |
| `:analyze.p0` … `:analyze.p19` | 20 analytics-relay provider slots (`AnalyzeProvider$P*`) |
| `:assist`, `:tool` | helper processes (strings `assist_process`, `tool_process`) |

Clone launch bridge: `com.android.virtual.bridge` intent → `BridgeLauncherActivity` (admin process).
Shortcuts: `CompatShortcutPrepareActivity` (`com.android.virtual.launcher`).
Cross-product assist: `com.waxmoon.virtual.ASSIST` (queries), 32-bit companion package `com.waxmoon.ma.gp32`.

## 3. The engine

- Native bootstrap: `System.loadLibrary("ma2023")` in `MoonApplication.<clinit>`.
- APK ships **no `lib/` folder** — native + dex payloads are **encrypted** in assets:
  - `core/engine-crypt` (3.6 MB), `core/core-crypt` (0.96 MB), `core/ext-crypt` (11 KB), `core/mnc.enc` (133 KB), `core/dic` (4 KB), `core/dexopt/`, `core/mlkit_barcode_models/`
- Java bootstrap in `attachBaseContext`: `wl.o0…oo(this)` → `iq.o0Oo` staged init (reflection-invoked `o(null, …)` version probe, then `o/o0/O/oo/OO(this)` stages), package name capture, `iq.I` process role detection.
- Binder hooking core: `wn.oo` holds a global `InvocationHandler` (`f46498l`) installed on transact — classic VirtualApp `ServiceManager` proxy interception.
- `mnc.enc` loaded via `oOoO.o(wl.o0…o(), "mnc.enc", …)` (asset → decrypt → likely DexClassLoader).
- Engine request flow UI: `VirtualRequestActivity` (`org.waxmoon.action.request`, translucent dialog) with task states: *Checking → Waiting → Copying apk → Analyzing → Installing/Uninstalling/Repairing/Clearing/Force stop/Generating* → success/fail strings (`engine_va_request_task_*`). "Google Policy Not Allow" guard.
- Crash protection: **CrashGuard** SDK (`crashguard.android.library`, project `3fc10ac2-…/8484d27a-…`), plus Firebase Crashlytics.

**Consequence for the clone:** the real engine is proprietary/encrypted; bit-for-bit
reproduction is impossible without their sources. The clone uses the same public
architecture (admin process + N proxy stub processes + Binder hooks) implemented
on an open-source VirtualApp-family core, with the **UI/UX and feature set
reproduced 1:1**.

## 4. Application components (UI layer)

- Launcher alias: `com.waxmoon.mobile.module.home.MainActivity` → `org.waxmoon.play.market.LauncherActivity` (splash → privacy/consent → main)
- Main UI: `org.waxmoon.play.product.ma.MainActivity` (singleTop, action `org.waxmoon.ma.main`) — **Jetpack Compose** (confirmed `androidx.compose` in `VirtualLauncherFragment` etc.) with in-house design system `org.waxmoon.play.core.designSystem` (components, widget, scrollbar), theme family `CoreDesignSystemTheme.Moon.Blue[.NoActionBar]`, Material 3.
- Deep-link router: `RouterActivity` + signature permission `org.waxmoon.ma.gp.protect.permission`, scheme `moon://main.router.ma`:
  `/apps` · `/about` · `/main/settings` · `/buy/vip` · `/ads/vip` · `/account/code_redeem` · `/account/vip_rebind` · `/detail/{pkg}/{user}` · `/vinfo` · `/folder` · `/launch/{pkg}/{user}` · `/minfo` · `/sinfo` · `/language`
- UI page packages (mirror of routes): `choice` (pick apps to clone), `launcher` (home/spaces), `appDetail`, `apps` (import), `setting`, `buyVip`, `freeVip`, `codeRedeem`, `vipRebind`, `language`, `memory`, `storage`, `spaceFolder`, `fileTransfer`, `virtualDeviceInfo`, `about`, `lock/password`, `privateSpace` (+dialogs).
- Other: `org.waxmoon.play.ui.LicenseActivity`, `StartAdminActivity`; OSS licenses activities.
- DI: Dagger (`product.ma.di`). DB: Room (`core.data.db`, `business.db`, sync/migration). Proto: wire/protobuf (`core.data.proto`, `business.data.proto`).

## 5. Feature inventory (from 280+ product strings)

### Home / spaces
- Virtual space cards: `Virtual Space-%1$d` / short `VP-%1$d`, custom rename ("Edit Title"), marquee border effect (animated border), extracted-colors theming
- Space card menu: Import Apps · Edit Title · Set pwd · Device Info · Install Google Suite · Remove Google Suite · Enable/Disable Google Installer
- "Stick Apps" pinned card; per-app menu: Open · Clone (one-key) · Sticky on Top · Uninstall · More
- Kill All Apps (with confirm), per-app "is running" foreground notification ("Please keep background running for receiving messages")

### Import / install
- Import from installed apps list (search, HOT badges) or **Import from file** (.apk/.xapk/.apks) — "file format cannot be parsed" guard
- Clone dialog: choose number of spaces to install into; **VIP required for >2 spaces**
- Overwrite install ("The new version will be used to overwrite…"), switch install mode (native overwrite repair)

### App detail (per clone)
Open · Stop · Uninstall · **Fix** (repair) · **Shortcut** (desktop icon + "Desktop Shortcut" permission hint flow) · Storage · **App Fake** (rename + custom icon: system icon / album) · Hidden App switch · **Support GG** (traceable process mode for crash debugging) · **Sync new version** (re-clone updated host app) · **Switch to local installation** · **Export App Data** (save app data to file manager) · **Device virtual ID** (view/reset — device id, wifi mac) · Enable google installer (Play Store inside clone) · launch mode selector (hook level `L%1$d`) · advanced hook toggles (virtual sdcard, hide developer mode, hide host path variants, deny reading phone's apps, force sdcard permission, audio/video fix, dumpable, svc scan)

### Private Space
Separate lockable hidden space: education screen ("About private space"), install apps, **Set a lock** (pattern + biometrics + backup screen lock), Secret Media, separate Google account suggestion, forget-password via device credential, "Display hidden apps / Don't show hidden apps" drawer toggles.

### Task Manager (Memory)
Running clones grouped by space, per-app PSS ("Free up %1$s"), total memory info, "Boost speed" kill action, empty state "None running App".

### Storage Manager
Per-space app list with sizes: Install Dir / Data&Cache / Cache / Media / obb; clear cache / clear data; batch select (select all filtered), "Clean all", "Uninstall all"; sort by storage/space; totals.

### File Transfer
Device-to-device app transfer over Wi-Fi/hotspot: QR scan (camera permission, MLKit barcode), link to target device, send/receive with compress step, progress states, "MultiApp compressed file" parsing on receive.

### Monetization
- **Buy VIP**: one-time purchase, periods 30/90/365 days, device-bound, notes text about non-subscription; Google Play Billing ("Billing Unavailable", product not found guards), Google Pay button
- **Free VIP**: watch rewarded ads → earn coins ("Earned one coin"), redeem VIP with coins; daily ad limit; "Ad no fill" handling
- **Redeem code**: input code → redeem VIP; "Buy Redemption Code" link
- **VIP migration (rebind)**: input Google order (GPA.****), once per day limit
- User levels: Regular User · Free Vip · Pay VIP · Supper VIP · "Vip level %1$s"; Coin balance display; expired state
- VIP privileges: remove all ads · reset device info · one-to-one support

### Settings / drawer
Drawer groups: **Account** (Buy VIP, Free VIP, Redeem, VIP migration) · **General** (Settings, Switch Language, Touch vibrator, Display hidden apps) · **Tool** (Task Manager, Storage Manager, File Transfer, Private Space, Find Help, Share app)
Settings: Ignore Battery Optimization, App Keep-Alive, Show Forget pwd, Developer Options (hidden: tap Developer 3× → verify test account → local test mode), Appearance: night mode Auto/Dark/Light, extracted colors, marquee border.

### About
Contact Us (waxmoon2023@gmail.com) · Developer: WaxMoon · Github · Telegram channel · Version · Device Id · Privacy Policy · Open source licenses · hidden developer test dialog.

### Help / support
"Find Help" — support chat ("Help Session") with photo attach, ExoPlayer video playback, loading states.

### Onboarding / privacy / consent
Privacy dialog (GDPR-style HTML: privacy/terms links, data collection disclosure incl. AdMob/AppLovin), Accept/Decline; notification permission rationale dialog; UMP "Privacy Settings"; Google Play Services missing guard; notification permission flow.

### Ads integration
AdMob app id `ca-app-pub-2058131851147808~4777722291`, AppLovin, **Pangle** (full `tt_*` string set), rewarded video reward-fail handling, "Ad playback incomplete; reward failed".

## 6. Backend / network

Retrofit + OkHttp + Gson/KotlinSerialization/Proto converters:
- `https://waxmoonma.top/` — primary API (proto)
- `https://www.waxmoon.cn` — secondary API
- `https://cdn.jsdelivr.net/gh/WaxMoon/` — config/content payloads from GitHub via jsDelivr
- Firebase project `multiapp-2e6b3` (Crashlytics, Analytics, Storage bucket `multiapp-2e6b3.firebasestorage.app`, sender id 854016456572)

## 7. Permissions (full list in manifest)

QUERY_ALL_PACKAGES, GET_INSTALLED_APPS (Meizu), POST_NOTIFICATIONS, CAMERA + FLASHLIGHT (QR), RECORD_AUDIO, location fine/coarse + media location, contacts/accounts (GET/MANAGE/AUTHENTICATE_ACCOUNTS, USE_CREDENTIALS, READ/WRITE_SOCIAL_STREAM), phone state/numbers, Bluetooth (all 5), NFC, storage (legacy + READ_MEDIA_*), network/wifi change, KILL_BACKGROUND_PROCESSES, GET_TASKS, REORDER_TASKS, DISABLE_KEYGUARD, EXPAND_STATUS_BAR, USE_BIOMETRIC/FINGERPRINT, VIBRATE, launcher INSTALL/UNINSTALL_SHORTCUT, FOREGROUND_SERVICE_SPECIAL_USE, REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, DETECT_SCREEN_RECORDING/CAPTURE (API 35), signature permission `org.waxmoon.ma.gp.protect.permission`.

## 8. Clone strategy (decided)

| Layer | Approach |
|---|---|
| UI/UX | **1:1 rebuild** — Kotlin + Jetpack Compose, same screens/routes/strings/design language (Moon.Blue → multi2 theme) |
| Virtualization engine | Open-source VirtualApp-family core (BlackBoxCore-class) adapted to the same process model (`:admin` + `:proxy.p0..N`, `:tool`), `VirtualRequestActivity` task flow reproduced |
| Native payload | our own JNI lib (`libmulti2.so`) replacing `libma2023.so` role (engine bootstrap, hooks) |
| Monetization | same SKU structure (30/90/365 one-time + coins + redeem) via Play Billing; AdMob/AppLovin mediation |
| Backend | our own API replacing `waxmoonma.top` (VIP/redeem/config), Firebase project of our own |
| Build | Gradle (KTS) + Codemagic `mac_mini_m2`, GitHub push-triggered, no local builds |
