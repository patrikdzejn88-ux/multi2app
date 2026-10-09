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

### M1 — Home & navigation
Routes mirroring `moon://main.router.ma/*` → `multi2://main.router/*`:
launcher (space cards grid), apps (import list), appDetail, setting, buyVip,
freeVip, codeRedeem, vipRebind, language, memory, storage, spaceFolder,
fileTransfer, virtualDeviceInfo, about, lock, privateSpace, choice.
Drawer with Account/General/Tool groups. All strings 1:1 (EN base).

### M2 — Engine integration
Open VirtualApp-family core (BlackBoxCore-class) as `engine/` module:
`:admin` process services/providers, `:proxy.p0..N` stub slots (manifest already
declares pattern), install/launch/uninstall/repair/clear flows behind
`VirtualRequestActivity`-style task dialog with the original state strings.

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
