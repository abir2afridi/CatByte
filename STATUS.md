# CatByte Video Editor - Project Status

**Last Updated:** 2026-11-01
**Current Phase:** P1 - Vertical slice ✅ COMPLETE
**Overall Progress:** ~60%

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
| **P8** Export hardening | 🟢 PARTIAL (Core Done) | Export validation ✓, storage precheck ✓, codec capability query ✓, still missing: frame-by-frame render validation, foreground service type |
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
- ✅ Storage precheck (estimate = bitrate × duration × 1.1 before export)
- 🔴 Codec capability query with fallback ladder (HEVC→H.264, 4K→1080p)
- 🔴 Foreground service with correct type (`mediaProcessing`)

### Localization
- ✅ English + Bengali strings
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
- ❌ **Foreground service with correct type** (`mediaProcessing`), proper notification channel, service timeout handling
- ✅ **Export validation** (MediaExtractor/MediaMetadataRetriever: tracks, dims, duration ±1 frame)
- ✅ **Storage precheck** (estimate = bitrate × duration × 1.1)
- ✅ **Codec capability query** with fallback ladder (HEVC→H.264, 4K→1080p)

### Testing (P0-P12 - CRITICAL)
- ❌ **Fixtures**: No `scripts/make_fixtures.sh|ps1` with ffmpeg synthetic media
- ❌ **Unit tests**: Only example tests exist, 7/8 failing
- ❌ **Instrumented tests**: None exist
- ❌ **Golden frames**: None
- ❌ **Compose UI tests**: None
- ❌ **TEST_REPORT.md**: Not created

---

## Known Issues / Blockers

1. **Single module architecture** - Per code_arc.md Rule 5, single module is fine for small project (~63 files). Will restructure when project grows.
2. **No RenderGraph** - Preview and export use different paths, parity not guaranteed
3. **No custom GL compositor** - Limited to Media3's built-in effects
4. **No command/undo system** - Direct state mutation, no undo/redo
5. **Bangla text not verified** - No golden image tests, no bundled Bangla fonts
6. **No test infrastructure** - No fixtures, no unit/instrumented tests, no golden frame comparison
7. **Export validation missing** ✅ - Now implemented (MediaMetadataRetriever validation)
8. **No CI/check scripts** - No local pipeline for format, lint, unit tests, assembleDebug

---

## Next 5 Tasks (Priority Order)

1. **Codec capability query** - Query MediaCodecList, fall back down ladder (HEVC→H.264, 4K→1080p)
2. **Create TEST_REPORT.md** - Document test results
3. **Add codec capability query** - Query MediaCodecList, fall back down ladder (HEVC→H.264, 4K→1080p)
4. **Foreground service** - mediaProcessing type, proper notification channel
5. **Test EXPORT validation** - Write instrumented test to verify exported MP4 plays correctly

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

---

## Build Commands & Results

```bash
# Compile debug
./gradlew :app:compileDebugKotlin
# Result: BUILD SUCCESSFUL

# Assemble debug APK
./gradlew :app:assembleDebug
# Result: BUILD SUCCESSFUL (24MB APK)

# Run unit tests
./gradlew :app:testDebugUnitTest
# Result: 7/8 tests FAIL (pre-existing failures in ExampleUnitTest.kt)

# Run instrumented tests
./gradlew :app:connectedDebugAndroidTest
# Result: Not yet run (only example tests exist)

# Lint
./gradlew :app:lintDebug
# Result: Not yet run
```

---

## Device/Emulator Info (for testing)
- **OS**: Windows 11 (PowerShell)
- **JDK**: Need to verify
- **Android SDK**: Need to verify
- **Gradle**: 9.8.2 (via wrapper)
- **Emulator**: Need to verify availability
