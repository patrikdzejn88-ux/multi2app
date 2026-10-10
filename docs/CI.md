# CI Readiness — first Codemagic build

The project is built only in CI (no local builds by design). This file maps
the most likely first-build failures to fixes.

## Pipeline
`codemagic.yaml` → workflow `android-build` on `mac_mini_m2`:
1. `gradle wrapper --gradle-version 8.10.2 --distribution-type bin` (system gradle on the image)
2. `./gradlew assembleRelease --no-daemon`
3. `./gradlew assembleDebug --no-daemon`
Artifacts: `app/build/outputs/apk/**/*.apk`

## Known risk map

| Symptom in CI log | Cause | Fix |
|---|---|---|
| `unresolved reference: menuAnchor` | material3 < 1.3 resolved | force `androidx.compose.material3:material3:1.3.1` |
| `e: ... HiddenApiBypass ... declaration clash` | none expected | — |
| `AAPT: error: resource string/... not found` | missed string key | add to `res/values/strings.xml` |
| `Manifest merger failed: Apps targeting Android 12+ ... exported` | all components have exported set | set exported on the named component |
| `> Cannot fit requested classes in a single dex file` | 500 stubs + libs | add `multiDexEnabled true` + `androidx.multidex:multidex:2.0.1` |
| `Duplicate class ... kotlin_module` | META-INF clash | already excluded in `packaging.resources.excludes` |
| `SDK location not found` | codemagic env | provided by image; check `android-sdk` in image spec |
| `The Linux/x86_64 ... CMake` | no NDK used | — |
| R8 `Missing class` warnings as errors | fullMode | add `-dontwarn` rules to `proguard-rules.pro` |
| Signing: release APK unsigned | no keystore yet | debug APK is signed by default; add keystore later |

## After the first build
1. Download both APKs from Codemagic artifacts.
2. Install **debug** on the target device first (signed with the debug key).
3. Smoke list: privacy dialog → home → import apps → clone → open clone
   (guest UI) → task manager → storage → private space → file transfer.
4. Paste the CI log here for fixes; iterate.
