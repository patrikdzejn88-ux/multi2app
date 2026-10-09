# multi2-app

Clone of **Multi App** (WaxMoon `com.waxmoon.ma.gp`) — a virtual-space app cloner:
run multiple accounts of the same app in isolated sandboxes.

- **Stack:** Kotlin · Jetpack Compose (M3) · Hilt · Room · Retrofit
- **Engine:** VirtualApp-family core (admin process + per-clone proxy stubs) — see `docs/REVERSE.md`
- **Build:** Codemagic `mac_mini_m2` on every push (see `codemagic.yaml`) — no local builds required
- **SDK:** min 26 · target 35

## Docs
- `docs/REVERSE.md` — full reverse-engineering report of the original app
- `docs/PLAN.md` — milestones M0–M6

## Structure
```
app/src/main/java/com/multi2/app/
├── Multi2Application.kt      # app entry (engine bootstrap hook point)
├── MainActivity.kt           # Compose UI host (singleTop)
├── RouterActivity.kt         # multi2://main.router/* deep links
├── BridgeLauncherActivity.kt # clone launch bridge (:admin)
├── admin/                    # :admin process — engine services/providers
├── proxy/                    # per-clone stub components (:proxy.pN)
├── engine/sdk/virtual/       # engine task dialog flow
└── ui/                       # Compose screens, nav, theme
```
