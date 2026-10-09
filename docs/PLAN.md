# multi2-app — Development Plan

Clone of "Multi App" (WaxMoon, com.waxmoon.ma.gp) — virtual-space app cloner.
Reverse basis: [REVERSE.md](REVERSE.md). Target: feature-parity 1:1 UI/UX,
functionally equivalent virtualization on an open engine core.

## Stack

- **Native Android · Kotlin · Jetpack Compose (Material 3)** — same stack as the original
- minSdk 26 / targetSdk 35 / compileSdk 35, JDK 17
- Gradle KTS, AGP 8.7.x, Kotlin 2.1.x, Compose BOM 2024.12.01
- DI: Hilt (original uses Dagger) · DB: Room · Net: Retrofit/OkHttp
- Build: Codemagic `mac_mini_m2` on GitHub push (no local builds)
- Package: `com.multi2.app` · app label "Multi2 App"

## Modules / milestones

### M0 — Scaffold (this commit)
Gradle project, manifest skeleton (processes, permissions, router scheme),
Compose theme ("Multi2 Blue" ≈ Moon.Blue), app class, placeholder home.

### M1 — Home & navigation  [DONE core]
Routes mirroring `moon://main.router.ma/*` → `multi2://main.router/*`.
DONE: Room (spaces/cloned_apps/proxies), DataStore settings, MVVM+Hilt,
Home (real space cards, rename/delete, hidden filter), Import Apps
(search/select/clone dialog with 2-space free limit), App Detail (local
options persisted, engine actions stubbed), Settings (battery opt intent,
night mode Auto/Dark/Light, vibrator, hidden apps, marquee, dev-mode 3-tap),
NEW Proxy manager (SOCKS5/HTTP(S), per-space scope, TCP test), night-mode
theme wiring. TODO later: about, memory, storage, fileTransfer, lock,
privateSpace, buyVip screens.

### M2 — Engine integration  [DONE core]
Own engine core implemented (no external VA fork): sandbox layout
(filesDir/virtual/spaces/<id>/<pkg>/{apk,data,cache,media,obb,dexopt,lib}),
ApkInstaller (host clone + .apk/.xapk/.apks import, native lib extraction by
ABI), EngineTaskManager serial queue with original task-state strings,
CloneLauncher slot allocator (p0..p9, expandable to 500), ProxyProcessRuntime,
GuestRuntime (DexClassLoader + AssetManager.addAssetPath + sandboxed
GuestContext + guest Application attach), HiddenApiBypass (meta-reflection,
pure Java). VirtualRequestActivity shows LIVE task state.
NEXT ITERATION: guest Activity UI attach (per-API-level Activity.attach),
system-service hooks, expand slots to p499.

### M3 — Space & app management
Room schema (spaces, cloned apps, pins, hidden, per-app hook options),
import from installed list + file (.apk/.xapk/.apks), clone dialog (VIP >2),
app fake (rename/icon), shortcuts, virtual device id, storage/memory managers.

### M4 — Monetization
Play Billing one-time 30/90/365, coins + rewarded ads (AdMob; AppLovin later),
redeem codes + VIP rebind via own backend API (replaces waxmoonma.top),
VIP levels & privileges gating.

### M5 — Private space, file transfer, help
Pattern lock + biometrics, hidden space; WiFi QR file transfer; help chat;
onboarding/privacy consent; keep-alive foreground notifications.

### M6 — Polish & store
Night mode Auto/Dark/Light, extracted-colors theming, marquee border,
kill-all, boost, CrashGuard-equivalent (Crashlytics), release signing,
Codemagic release pipeline.

## Verification
Every milestone: parallel review agents cross-check screens/strings/routes
against `reverse/apktool` + `reverse/jadx` before merge to `main`.
