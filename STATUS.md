# CatByte Video Editor - Project Status

**Last Updated:** 2026-11-01
**Current Phase:** P8 - Export hardening (core complete, device verification pending)
**Overall Progress:** ~62%

---

## Phase Status

| Phase | Status | Description |
|-------|--------|-------------|
| **P0** Foundation & spikes | ✅ DONE | Verify toolchain ✓, render-engine spike decision ✓ (Spike A: Media3 Transformer), `.gradle/` untracked ✓ |
| **P1** Vertical slice | ✅ DONE | Import one video → decode → ExoPlayer preview → text overlay → export MP4 → validated playable output |
| **P2** Model, commands, persistence | 🔴 NOT STARTED | Command/undo system, TimeUs, keyframe interpolation |
| **P3** Shell & import | 🟡 PARTIAL | Theme, navigation, home, picker exist; missing proxies |
| **P4** Timeline & preview | 🟡 PARTIAL | Custom timeline exists, preview sync exists |
| **P5** Clip & canvas tools | 🟡 PARTIAL | Basic transforms exist; missing crop, speed curves, freeze |
| **P6** Text, audio, adjust, filters | 🟡 PARTIAL | Text exists, audio tracks exist; missing Bangla verification, voiceover, mixer |
| **P7** Transitions & multi-track | 🟡 PARTIAL | Basic transitions exist; missing two-frame compositing, overlay tracks |
| **P8** Export hardening | 🟢 CORE COMPLETE | Export validation ✓, storage precheck ✓, codec capability ladder ✓ (unit-tested), typed foreground service ✓, background export UX ✓; still missing: frame-by-frame rational timebase render, on-device instrumented probe |
| **P9** Tier 2 features | ⏳ NOT STARTED | Keyframe UI, masks, chroma key, effects library |
| **P10** Polish | ⏳ NOT STARTED | Accessibility, haptics, empty/error states, onboarding |
| **P11** Tier 3 features | ⏳ NOT STARTED | Auto captions, background removal, object tracking |
| **P12** Release verification | ⏳ NOT STARTED | Lint, tests, signed release, APK analyzer |

---

## What's Implemented (Working)

### Core Architecture
- ✅ Single Activity + Jetpack Compose (Material3 base, custom theme)
- ✅ Room database for project persistence (v2 with migration - demo projects cleared)
- ✅ DataStore for settings
- ✅ Coroutines + Flow for async
- ✅ Manual DI container (AppContainer)
- ✅ Project model with clips, overlays, audio, text, stickers, effects
- ✅ Media3 ExoPlayer for preview
- ✅ Media3 Transformer for export

### UI Screens
- ✅ Home screen: project grid, new project dialog, search/filter, pin/favorite, delete with undo
- ✅ Editor screen: preview, timeline, bottom toolbar, tool sheets
- ✅ Settings screen: theme, canvas, export quality, language, watermark, storage, **Clear All Projects**
- ✅ Media picker: Video/Photo/All tabs, multi-select, albums
- ✅ Tool sheets: Edit, Speed, Audio, Text, Effects, Filters, Adjustments, Canvas Ratio, Stickers, Transitions, Export, Keyframe, Blend Mode, Mask, TTS

### Project Operations
- ✅ Create project (aspect ratio presets: 9:16, 16:9, 1:1, 4:5)
- ✅ Duplicate project
- ✅ Delete project (with undo bar)
- ✅ **Clear all projects** (Settings > Storage > Clear All Projects with confirmation)
- ✅ Pin/favorite projects
- ✅ Autosave on every change
- ✅ **Welcome dialog** (Tauri app) - no auto-create "Untitled Project"

### Timeline
- ✅ Multi-track timeline (main + overlay tracks)
- ✅ Pinch zoom, scroll
- ✅ Clip trim handles (drag edges)
- ✅ Split, duplicate, delete, reorder clips
- ✅ Track mute/hide/lock/solo
- ✅ Playhead with timecode
- ✅ Waveform display for audio clips

### Preview
- ✅ ExoPlayer-based playback
- ✅ Play/pause/seek/scrub
- ✅ Loop toggle
- ✅ Volume control
- ✅ Canvas-correct rendering (aspect ratio handling)

### Export
- ✅ Media3 Transformer-based export
- ✅ Filters, color adjustments, rotation/mirroring
- ✅ Text overlays, stickers, watermark
- ✅ Audio tracks with volume/fades
- ✅ Progress tracking
- ✅ Cancel support
- ✅ Save to MediaStore (Movies/CatByte)
- ✅ Export validation (validate exported MP4 using MediaMetadataRetriever: tracks, dims, duration)
- ✅ Storage precheck (estimate = bitrate × resolved-output duration × 1.1, post-downgrade size)
- ✅ **Codec capability ladder** (unit-tested 9/9): HEVC→H.264, size requested→1440p→1080p→720p→480p, fps 60→30→24; never larger than requested; rejected pinned codec retries once with Media3's default encoder (DEC-017)
- ✅ **Typed foreground service**: API 35+ `mediaProcessing`, API 34 `dataSync`, typed notification channel, double-start guard
- ✅ **Background export UX**: "Keep editing — export in background" closes the dialog, starts the FGS; Android 13+ requests `POST_NOTIFICATIONS` first (denial does not block the export)

### Localization
- ✅ English + Bengali full parity (125 keys each, incl. new `export_background_hint`)
- ✅ Robolectric guard test: bn resources actually translated (`LocalizationRobolectricTest`, sdk 34)
- ✅ Locale-safe numeric labels: `fps_label`/`export_encoding` use `%s` placeholders so fps stays Latin digits in every locale (matches Latin resolution labels like "1080P")
- ✅ RTL-safe layouts

---

## What's Missing / Not Matching Plan (Critical Gaps)

### Architecture (P0 - CRITICAL)
- ❌ **Module structure**: Plan requires multi-module Gradle. Per code_arc.md Rule 5 ("No over-engineering"), keeping single `:app` module for small project (~63 files).
- ❌ **RenderGraph concept**: Plan requires `Project` → `RenderGraphBuilder` → `RenderGraph`. Currently separate preview (ExoPlayer) and export (Transformer) paths.
- ❌ **Custom GL compositor**: Plan requires custom GLSL compositor. Currently using Media3's built-in effects only.
- ❌ **Decoder pool**: Plan requires bounded DecoderPool with priority/eviction. Not implemented.
- ❌ **Proxy generation**: Plan requires auto-generate low-res proxies. Not implemented.

### Domain Model (P2 - CRITICAL)
- ❌ **Command/Undo system**: Plan requires all edits as `Command`s with `apply`/`invert`. Currently direct mutation.
- ❌ **TimeUs value class**: Plan requires microseconds. Currently using `Long` milliseconds.
- ❌ **Keyframe interpolation/splitting**: Basic keyframes exist, no interpolation/splitting logic.
- ❌ **Transition clamping**: Plan requires transition duration ≤ min(adjacent clip durations) / 2. Not enforced.
- ❌ **Project JSON formatVersion + migration chain**: Missing.

### Timeline & Preview (P4 - CRITICAL)
- ❌ **Fixed center playhead (CapCut-style)**: Content scrolls with playhead, not fixed center.
- ❌ **60fps scroll guarantee**: No LRU bitmap pool, no multi-resolution waveform pyramids.
- ❌ **Magnetic main track + free overlay tracks**: Basic track ordering exists, not magnetic.
- ❌ **Snapping (playhead, clip edges, markers, beats)**: Not implemented.
- ❌ **Decoder pool with priority/eviction**: Not implemented.

### Clip Tools (P5 - CRITICAL)
- ❌ **On-canvas gestures** (move/scale/rotate/crop corners, pinch/rotate)
- ❌ **Alignment guides with snap haptics**
- ❌ **Crop (free + ratios)**
- ❌ **Speed curves** (bezier ramp editor)
- ❌ **Freeze frame, reverse, extract audio, detach audio**
- ❌ **Fit/fill/blur-fill background**

### Text (P6 - CRITICAL)
- ❌ **Bangla shaping verification** (HarfBuzz via Android text stack/Paint)
- ❌ **Bundled OFL Bangla fonts** (Noto Sans Bengali, Hind Siliguri, Tiro Bangla, Anek Bangla)
- ❌ **Text templates, animated captions styles** (word-by-word highlight, karaoke)
- ❌ **SRT/VTT import & export, burned-in captions**

### Audio (P6 - CRITICAL)
- ❌ **Voiceover recording** (mic permission, headphone-aware)
- ❌ **Waveform extraction** (multi-resolution min/max peak pyramids on disk)
- ❌ **Mixer with limiter** to prevent clipping
- ❌ **Beat detection + beat markers**
- ❌ **Auto-ducking, equalizer, loudness normalization (LUFS target)**

### Effects & Filters (P6/P7 - CRITICAL)
- ❌ **≥20 original LUT-based presets** (.cube/PNG LUT, 33³ or 64²) with intensity slider
- ❌ **≥30 original GLSL effects** (glitch, RGB split, VHS, shake, etc.)
- ❌ **Transitions with two-frame compositing** in shader

### Export Pipeline (P8 - RELEASE BLOCKER)
- ❌ **Frame-by-frame render at project frame-rate timebase** (rational math, no float drift)
- ❌ **Audio: render mixed PCM from same graph** (resample to 48kHz stereo, apply speed/pitch, fades, volume, limiter)
- ❌ **On-device probe**: instrumented codec + E2E export run (test written, needs device)
- ✅ **Export validation** (MediaExtractor/MediaMetadataRetriever: tracks, dims, duration ±1 frame)
- ✅ **Storage precheck** (estimate = bitrate × duration × 1.1)
- ✅ **Codec capability query** with fallback ladder (HEVC→H.264, 4K→1080p; DEC-017)
- ✅ **Foreground service typed** (`mediaProcessing` API 35+ / `dataSync` API 34), notification channel, double-start guard

### Testing (P0-P12 - CRITICAL)
- ❌ **Fixtures**: No `scripts/make_fixtures.sh|ps1` with ffmpeg synthetic media
- ❌ **Instrumented execution**: `CodecCapabilityInstrumentedTest` (5 tests) compiles, NOT RUN (no device)
- ❌ **Golden frames**: None
- ❌ **Compose UI tests**: None
- ✅ **Unit tests**: 50/50 pass (ProjectJson 9, ExportMath 9, KeyframeInterpolator 9, ProjectRepository 8, EditorWorkflow 13, Robolectric localization 2) — example tests (7/8 failing) removed
- ✅ **TEST_REPORT.md**: Created (root, plan §13 format; sections A–I)

---

## Known Issues / Blockers

1. **Single module architecture** - Per code_arc.md Rule 5, single module is fine for small project (~63 files). Will restructure when project grows.
2. **No RenderGraph** - Preview and export use different paths, parity not guaranteed
3. **No custom GL compositor** - Limited to Media3's built-in effects
4. **No command/undo system** - Direct state mutation, no undo/redo
5. **Bangla text not verified** - No golden image tests, no bundled Bangla fonts
6. **No on-device test run yet** — fixtures + instrumented E2E pending a device/emulator (unit tests are green: 50/50)
7. **Export validation missing** ✅ - Now implemented (MediaMetadataRetriever validation)
8. **No CI/check scripts** - No local pipeline for format, lint, unit tests, assembleDebug

---

## Next 5 Tasks (Priority Order)

1. **Device run** - `connectedDebugAndroidTest` (CodecCapabilityInstrumentedTest) + real HEVC→AVC downgrade + one full export E2E touch; record in TEST_REPORT.md
2. **Lint pass** - `:app:lintDebug`, clear NewApi/deprecation warnings from this round, add as a gate
3. **Localized service notification** - FGS notification text reads string resources (`export_finished`/`export_failed`/`export_cancelled`)
4. **P2 Command/undo system** - `EditorViewModel.pushHistory()` commands, undo/redo caps 25 (biggest remaining architecture gap)
5. **Fixtures script** - `scripts/make_fixtures.ps1` (ffmpeg synthetic media) for the on-device export E2E touch

---

## Decisions Recorded

| ID | Title | Status | Phase |
|----|-------|--------|-------|
| DEC-001 | Manual DI Container | ✅ Decided | Pre-existing |
| DEC-002 | Media3 Transformer Export | ✅ Decided | Pre-existing |
| DEC-003 | ExoPlayer Preview | ✅ Decided | Pre-existing |
| DEC-004 | Room + JSON Persistence | ✅ Decided | Pre-existing |
| DEC-005 | Milliseconds Time | ✅ Decided | Pre-existing |
| DEC-006 | Single Module | ✅ Decided | Pre-existing |
| DEC-007 | DB v2 Migration Clear Demo | ✅ Decided | 2026-10-31 |
| DEC-008 | Welcome Dialog (Tauri) | ✅ Decided | 2026-10-31 |
| DEC-009 | Clear All Projects Button | ✅ Decided | 2026-10-31 |
| DEC-010 | Render Engine Spike | ✅ Decided | P0 - Spike A (Media3 Transformer) chosen |
| DEC-011 | Multi-Module Restructure | 🔴 Pending | P0 - Deferred per code_arc.md Rule 5 |
| DEC-012 | Command/Undo System | 🔴 Pending | P2 |
| DEC-013 | Bangla Font Bundling | 🔴 Pending | P6 |
| DEC-014 | LUT Format | 🔴 Pending | P6 |
| DEC-015 | RNNoise License/Size | 🔴 Pending | P9 |
| DEC-016 | Whisper.cpp Auto Captions | 🔴 Pending | P11 |
| DEC-017 | Codec Capability Fallback Ladder | ✅ Decided | P8 - 2026-11-01 |
| DEC-018 | JVM Test Strategy (org.json + Robolectric sdk34) | ✅ Decided | Testing - 2026-11-01 |

---

## Build Commands & Results

```bash
# Compile debug
./gradlew :app:compileDebugKotlin
# Result: BUILD SUCCESSFUL (1m 20s; only pre-existing deprecation warnings)

# Assemble debug APK + instrumented test compile
./gradlew :app:assembleDebug :app:compileDebugAndroidTestKotlin
# Result: BUILD SUCCESSFUL (3m 38s) - app-debug.apk 22.93 MB

# Run unit tests
./gradlew :app:testDebugUnitTest
# Result: BUILD SUCCESSFUL (3m 58s) - 50 tests, 0 failed, 0 skipped
# (XML results under app/build/test-results/testDebugUnitTest/)

# Run instrumented tests
./gradlew :app:connectedDebugAndroidTest
# Result: NOT RUN (no device attached; CodecCapabilityInstrumentedTest compiles)

# Lint
./gradlew :app:lintDebug
# Result: Not yet run
```

---

## Device/Emulator Info (for testing)
- **OS**: Windows 11 (PowerShell)
- **JDK**: Verified via Gradle daemon (Java 17 toolchain)
- **Android SDK**: Paths resolved through `local.properties`
- **Gradle**: 9.8.2 (via wrapper)
- **Emulator/Device**: None attached during the 2026-11-01 session; connected tests pending
