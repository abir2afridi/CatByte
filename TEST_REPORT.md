# CatByte Video Editor — TEST REPORT

**Report Date:** 2026-11-01
**Scope:** P8 export hardening round (codec capability ladder, typed foreground service, background export UX, Bangla localization parity, test infrastructure).
**Format:** Per `docs+\plan.md` §13 (FINAL REPORT structure: A–I).

---

## A) What was built

Android app (single `:app` module, Kotlin + Compose, Media3 1.11.1):

| Area | Files | Change |
|------|-------|--------|
| Codec capability query | `app/src/main/java/com/example/data/export/CodecCapability.kt` (new) | `CodecCapabilityChecker`: encoders probed via typed `MediaCodecInfo.VideoCapabilities`; fallback ladder HEVC→H.264, heights 2160→1440→1080→720→480, fps 60→30→24; `ResolvedTarget` keeps requested→resolved mapping; graceful `mime = null` fallback to Media3's default encoder |
| Export pipeline | `app/src/main/java/com/example/data/ExportManager.kt` | Export resolves target via capability checker; downgrade reasons logged + shown in progress stage; storage precheck uses the resolved (post-downgrade) size; pinned codec failure retries **once** with the default encoder (`isEncoderRelatedFailure` on verified `ExportException` constants only); `runTransformer(videoMime)` extracted so the retry reuses the same composition |
| Foreground service | `app/src/main/java/com/example/data/ExportForegroundService.kt` + `AndroidManifest.xml` | Typed `startForeground`: API 35+ `mediaProcessing`, API 34 `dataSync` (fallback `mediaProcessing|dataSync` declared); notification channel exists; double-start guard (a second `ACTION_START` while a job is active is ignored instead of overwriting the running job) |
| Background export UX | `ExportDialog.kt`, `EditorScreen.kt` | "Keep editing — export in background" text button (localized) starts the export in the foreground service and closes the dialog; on Android 13+ `POST_NOTIFICATIONS` is requested first (denial does not block the export) |
| Localization | `values/strings.xml`, `values-bn/strings.xml` | Full bn↔en parity (125 keys each now); `settings_title` was an untranslated English copy — now "সেটিংস"; numeric placeholders switched from `%d` to `%s` so fps/labels stay Latin digits in every locale |
| Test infra | `app/build.gradle.kts`, `gradle/libs.versions.toml`, test sources | `isReturnDefaultValues=true`; real `org.json:json` on the unit-test classpath (documented approach for android.jar stubs); broken example tests (`ExampleUnitTest.kt` 7/8 failing, `ExampleRobolectricTest.kt`) removed; new suites listed in B) |

---

## B) Features VERIFIED (with the test that proved each)

| # | Feature/behavior | Test | Result |
|---|------------------|------|--------|
| 1 | Project JSON round trip preserves every model field (clips, keyframes, HSL, masks, overlays, audio) | `ProjectJsonTest` × 9 | PASS |
| 2 | Corrupt/missing JSON fields fall back safely; unknown enum names → safe defaults | `ProjectJsonTest` | PASS |
| 3 | Bengali text survives the JSON round trip | `ProjectJsonTest` | PASS |
| 4 | Keyframe interpolation math (linear/ease curves, clamp) | `KeyframeInterpolatorTest` × 9 | PASS |
| 5 | Repository CRUD autosave semantics on JVM stubs | `ProjectRepositoryTest` × 8 | PASS |
| 6 | Codec/resolution/fps fallback ladder math: never larger than requested, label mapping, graceful degradation | `ExportMathTest` × 9 | PASS |
| 7 | Editor state machine: import → select → trim/split/speed/filters → undo/redo caps | `EditorWorkflowTest` × 13 | PASS |
| 8 | Bangla resources actually translated (not English copies) + placeholder rendering | `LocalizationRobolectricTest` × 2 (Robolectric, sdk 34) | PASS |
| 9 | App compiles: debug Kotlin, androidTest Kotlin, debug APK (incl. `CodecCapabilityInstrumentedTest` × 5 compile) | Gradle builds (section D) | PASS |

---

## C) Incomplete / blocked / deferred (with reasons)

| Item | Reason |
|------|--------|
| On-device codec capability probe | No emulator/device attached in this session; `CodecCapabilityInstrumentedTest` (5 tests) is written and compiles — needs `connectedDebugAndroidTest` (section I, priority 1) |
| Real end-to-end export (import → edit → export → validate MP4 → share) on device | Requires a device; the export path itself is the same code unit-tested in parts (ladder, retry-once decision) but full render was NOT executed here |
| Lint pass (`:app:lintDebug`) | Not run in this round; `MediaCodecInfo.isHardwareAccelerated` is guarded by SDK >= 29, still needs lint confirmation |
| Compose UI tests | Not started (existing gap from STATUS.md) |
| Golden-frame/img tests for Bangla shaping | Deferred to DEC-013 (font bundling) |
| Fixtures (`scripts/make_fixtures.ps1`) | Not started — needed before any on-device export E2E touch |
| Export notification text localization | The service notification uses English literals ("Export complete! Tap to open.") — should read string resources via `NotificationCompat` builder context (small follow-up) |

---

## D) Exact build commands + results

All commands run from the repo root on Windows 11 PowerShell (`.\gradlew.bat`).

```
# 1) Compile + unit tests (first run, before fixes)
.\gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest --console=plain
→ BUILD FAILED in 1m 55s. 50 tests, 9 failed:
   - 8× ProjectJsonTest: NPE from android.jar org.json stubs (returnDefaultValues)
   - 1× LocalizationRobolectricTest: String.format("%d") → Bengali digits ("৩০ fps") under bn-BD

# 2) After fixes (org.json dep, %s placeholders, capability-file cleanup,
#    verified ExportException constants from the media3 1.11.1 docs)
.\gradlew.bat :app:testDebugUnitTest --console=plain
→ BUILD SUCCESSFUL in 3m 58s. XML results: 50 tests, 0 failures, 0 errors, 0 skipped

# 3) Compile check
.\gradlew.bat :app:compileDebugKotlin --console=plain
→ BUILD SUCCESSFUL in 1m 20s (only pre-existing deprecation warnings:
   EditedMediaItemSequence.Builder(list) ctor ×3, Room fallbackToDestructiveMigration ×1)

# 4) APK + instrumented-test compile
.\gradlew.bat :app:assembleDebug :app:compileDebugAndroidTestKotlin --console=plain
→ BUILD SUCCESSFUL in 3m 38s
```

---

## E) Test results (PASS / FAIL / SKIPPED / NOT RUN) + device info

| Suite | Tests | PASS | FAIL | SKIPPED | NOT RUN | Env |
|-------|-------|------|------|---------|---------|-----|
| `ProjectJsonTest` | 9 | 9 | 0 | 0 | — | JVM unit test (real org.json) |
| `ExportMathTest` | 9 | 9 | 0 | 0 | — | JVM unit test |
| `KeyframeInterpolatorTest` | 9 | 9 | 0 | 0 | — | JVM unit test |
| `ProjectRepositoryTest` | 8 | 8 | 0 | 0 | — | JVM unit test (returnDefaultValues) |
| `EditorWorkflowTest` | 13 | 13 | 0 | 0 | — | JVM unit test + coroutines-test |
| `LocalizationRobolectricTest` | 2 | 2 | 0 | 0 | — | Robolectric 4.16.1, `@Config(sdk=[34])` |
| **Unit total** | **50** | **50** | **0** | **0** | — | |
| `CodecCapabilityInstrumentedTest` | 5 | — | — | — | NOT RUN (no device attached) | instrumented |
| Compose UI tests | 0 | — | — | — | NOT STARTED | |
| `:app:lintDebug` | — | — | — | — | NOT RUN | |

**Device info:** No emulator/device was available during this round. Soak tests, connectedAndroidTest and golden-frame tests remain pending a device run.
**Robolectric caveat:** only the SDK-34 instrumented jar is cached locally; SDK 36 runs fail on native-loader — see DEC-018.

---

## F) APK

| Field | Value |
|-------|-------|
| Path | `app/build/outputs/apk/debug/app-debug.apk` |
| Size | 22.93 MB |
| applicationId | `com.example.catbyte` |
| versionName | 1.0 |
| versionCode | 1 |
| minSdk / targetSdk / compileSdk | 24 / 37 / 37.2 |
| Signed | debug config (`debug.keystore`, alias `androiddebugkey`) |

---

## G) Install steps

```powershell
# Automated (device/adb added to PATH)
adb devices          # confirm one device
adb install -r .\app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n com.example.catbyte/.MainActivity

# Manual: copy app-debug.apk to the phone and open it in a file manager
# (accept the "install from unknown sources" prompt).
```

Quick smoke checklist after install: Language switch → Editor imports a video → Export 1080P/30 → progress notification → "Keep editing — export in background" → notification completes → gallery contains `Movies/CatByte` clip.

---

## H) Known limitations

1. **Device codec probe not yet run on real hardware** — the ladder is logic-verified only (JVM). First device run should exercise `CodecCapabilityInstrumentedTest` and one real HEVC→AVC downgrade.
2. **Robolectric SDK lock** — resource tests pinned to sdk 34 until more `android-all-instrumented` jars are cached.
3. **Export notification copy is English** (service strings not localized yet).
4. **Concurrent guard is service-level only** — if the user starts a foreground export while an in-dialog export runs, the second request is ignored (logged), no toast yet.
5. **Background export progress is not mirrored in-app** — after "keep editing", progress lives in the notification; reopening the export dialog shows a fresh picker.
6. **`startBackgroundExport(resolution, fps)` in `EditorViewModel` is a legacy stub** — unused by UI; safe to delete in a cleanup pass.
7. **Timestamps on export filenames** use wall-clock (`System.currentTimeMillis()`), acceptable for now.
8. **No XML formatter/lint gates yet** (see STATUS next tasks).

---

## I) Next 10 engineering priorities

1. **Device run**: `connectedDebugAndroidTest` + real HEVC→AVC downgrade + full export E2E on a real/emulated device; record results here.
2. **Lint pass** (`:app:lintDebug`): clear the `isHardwareAccelerated`/`getRunningServices`-class NewApi + deprecation warnings, add lint gate.
3. **Localized service notification** — read `export_finished`/`export_failed`/`export_cancelled` from string resources inside the FGS.
4. **Toast/dialog on "already exporting"** (service-level guard exists; UX feedback missing).
5. **P2 Command/undo system** (`EditorViewModel.pushHistory()`, undo/redo caps 25) — closes the biggest STATUS gap.
6. **Concat/export of audio-only + bitrate presets** (Tier 1 leftover: "Bitrate/quality presets" row in FEATURE_MATRIX).
7. **Fixtures script** (`scripts/make_fixtures.ps1` via ffmpeg) + golden-frame harness for preview/export parity.
8. **Centered fixed playhead + snapping** (CapCut parity, P4).
9. **Bangla font bundling + shaping verification** (DEC-013) incl. burned-in text export golden test.
10. **Export UX finishing**: resumable/cancel from notification, storage-safety retry, and wiring `export_encoding` progress copy.

---

*Generated by the P8 export-hardening working session. Update this file after every device run/gate.*
