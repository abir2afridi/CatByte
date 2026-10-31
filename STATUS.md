# CatByte Video Editor - Project Status

**Last Updated:** 2026-10-31
**Current Phase:** P0 - Foundation & spikes
**Overall Progress:** ~40%

---

## Phase Status

| Phase | Status | Description |
|-------|--------|-------------|
| **P0** Foundation & spikes | 🟡 IN PROGRESS | Verify toolchain, create docs skeleton, run render-engine spike A vs B, record DECISIONS.md |
| **P1** Vertical slice | ⏳ NOT STARTED | Import one video → decode → GL compositor → add text overlay → export MP4 |
| **P2** Model, commands, persistence | 🟡 PARTIAL | Model exists, Room+JSON exists, but no command/undo system, no migrations |
| **P3** Shell & import | 🟡 PARTIAL | Theme, navigation, home, picker exist; missing proxies, missing-media flow |
| **P4** Timeline & preview | 🟡 PARTIAL | Custom timeline exists, preview sync exists; missing 60fps scroll guarantee, decoder pool |
| **P5** Clip & canvas tools | 🟡 PARTIAL | Basic transforms exist; missing on-canvas gestures, crop, speed, freeze, ratios |
| **P6** Text, audio, adjust, filters | 🟡 PARTIAL | Text exists, audio tracks exist; missing Bangla verification, voiceover, waveform, mixer |
| **P7** Transitions & multi-track | 🟡 PARTIAL | Basic transitions exist; missing two-frame compositing, overlay tracks, PiP, blend modes |
| **P8** Export hardening | 🟡 PARTIAL | Export exists; missing presets, capability ladder, validation suite |
| **P9** Tier 2 features | ⏳ NOT STARTED | Keyframe UI, masks, chroma key, effects library, captions/SRT, audio FX, speed curves, templates |
| **P10** Polish | ⏳ NOT STARTED | Accessibility, haptics, empty/error states, onboarding, localization |
| **P11** Tier 3 features | ⏳ NOT STARTED | Auto captions, background removal, object tracking |
| **P12** Release verification | ⏳ NOT STARTED | Lint, tests, signed release, APK analyzer, manifest audit |

---

## What's Implemented (Working)

### Core Architecture
- ✅ Single Activity + Jetpack Compose (Material3 base, custom theme)
- ✅ Room database for project persistence (v2 with migration)
- ✅ DataStore for settings
- ✅ Coroutines + Flow for async
- ✅ Manual DI container (AppContainer)
- ✅ Project model with clips, overlays, audio, text, stickers, effects
- ✅ Media3 ExoPlayer for preview
- ✅ Media3 Transformer for export

### UI Screens
- ✅ Home screen: project grid, new project dialog, search/filter, pin/favorite, delete with undo
- ✅ Editor screen: preview, timeline, bottom toolbar, tool sheets
- ✅ Settings screen: theme, canvas, export quality, language, watermark, storage, clear cache/projects
- ✅ Media picker: Video/Photo/All tabs, multi-select, albums
- ✅ Tool sheets: Edit, Speed, Audio, Text, Effects, Filters, Adjustments, Canvas Ratio, Stickers, Transitions, Export, Keyframe, Blend Mode, Mask, TTS

### Project Operations
- ✅ Create project (aspect ratio presets: 9:16, 16:9, 1:1, 4:5)
- ✅ Duplicate project
- ✅ Delete project (with undo bar)
- ✅ **Clear all projects** (Settings > Storage > Clear All Projects) - NEW
- ✅ Pin/favorite projects
- ✅ Autosave on every change

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

### Localization
- ✅ English + Bengali strings
- ✅ RTL-safe layouts

---

## What's Missing / Not Matching Plan (Critical Gaps)

### Architecture (P0 - CRITICAL)
- ❌ **Module structure**: Plan requires multi-module Gradle (`:core:model`, `:core:render`, `:engine:gl`, etc.). Currently single `:app` module only. Per code_arc.md Rule 5 ("No over-engineering"), keeping single module for small project size (~63 files).
- ❌ **RenderGraph concept**: Plan requires single source of truth `Project` → `RenderGraphBuilder` → `RenderGraph`. Currently separate preview (ExoPlayer) and export (Transformer) paths.
- ❌ **Custom GL compositor**: Plan requires custom GLSL compositor. Currently using Media3's built-in effects only.
- ❌ **Decoder pool**: Plan requires bounded DecoderPool with priority/eviction. Not implemented.
- ❌ **Proxy generation**: Plan requires auto-generate low-res proxies. Not implemented.

### Domain Model (P2 - CRITICAL)
- ❌ **Command/Undo system**: Plan requires all edits as `Command`s with `apply`/`invert`. Currently direct mutation.
- ❌ **TimeUs value class**: Plan requires microseconds with `TimeUs`. Currently using `Long` milliseconds.
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
- ❌ **Storage precheck** (estimate = bitrate × duration × 1.1)
- ❌ **Codec capability query** with documented fallback ladder
- ❌ **Frame-by-frame render at project frame-rate timebase** (rational math, no float drift)
- ❌ **Audio: render mixed PCM from same graph** (resample to 48kHz stereo, apply speed/pitch, fades, volume, limiter)
- ❌ **Validation** (MediaExtractor/MediaMetadataRetriever: tracks, dims, duration ±1 frame, audio track, decode first/middle/last frame, compare checksum/SSIM vs golden)
- ❌ **Foreground service with correct type** (`mediaProcessing`), proper notification channel, service timeout handling

### Testing (P0-P12 - CRITICAL)
- ❌ **Fixtures**: No `scripts/make_fixtures.sh|ps1` with ffmpeg synthetic media
- ❌ **Unit tests**: Only example tests exist, 7/8 failing
- ❌ **Instrumented tests**: None exist
- ❌ **Golden frames**: None
- ❌ **Compose UI tests**: None
- ❌ **TEST_REPORT.md**: Not created

### Documentation (P0-P12)
- ❌ **STATUS.md** - Creating now
- ❌ **FEATURE_MATRIX.md** - Not created
- ❌ **docs/ARCHITECTURE.md** - Not created
- ❌ **docs/DECISIONS.md** - Not created
- ❌ **docs/SPIKE_RENDER_ENGINE.md** - Not created
- ❌ **README.md** - Exists but minimal
- ❌ **THIRD_PARTY_LICENSES.md** - Not created
- ❌ **scripts/check.sh / check.ps1** - Not created

---

## Known Issues / Blockers

1. **Single module architecture** - Must restructure to match plan's multi-module design before P1
2. **No RenderGraph** - Preview and export use different paths, parity not guaranteed
3. **No custom GL compositor** - Limited to Media3's built-in effects
4. **No command/undo system** - Direct state mutation, no undo/redo
5. **Bangla text not verified** - No golden image tests, no bundled Bangla fonts
6. **No test infrastructure** - No fixtures, no unit/instrumented tests, no golden frame comparison
7. **Export validation missing** - No automated validation of exported MP4
8. **No CI/check scripts** - No local pipeline for format, lint, unit tests, assembleDebug

---

## Next 5 Tasks (Priority Order)

1. **Create docs skeleton**: FEATURE_MATRIX.md, docs/ARCHITECTURE.md, docs/DECISIONS.md, docs/SPIKE_RENDER_ENGINE.md, THIRD_PARTY_LICENSES.md, scripts/check.sh, scripts/check.ps1
2. **Run render-engine spike A vs B**: Compare Media3 Transformer+Composition+GlEffect (Spike A) vs Custom pipeline with MediaExtractor+MediaCodec+GLSL (Spike B)
3. **Restructure to multi-module Gradle**: Create `:core:model`, `:core:render`, `:engine:gl`, `:engine:playback`, `:engine:export`, `:engine:audio`, `:feature:*`, `:data`, `:testing`
4. **Implement RenderGraph + RenderGraphBuilder**: Single source of truth for preview and export
5. **Implement Command/Undo system**: All edits as Commands with apply/invert, bounded history

---

## Decisions Recorded (to be moved to DECISIONS.md)

| Decision | Context | Alternatives Rejected | Consequences |
|----------|---------|----------------------|--------------|
| Manual DI container (AppContainer) | Avoid Hilt/Koin complexity | Hilt, Koin | Simple but manual wiring; no compile-time validation |
| Media3 Transformer for export | Leverage existing Media3 pipeline | Custom MediaCodec encoder | Faster to implement but limited to Media3's effects; preview/export parity not guaranteed |
| ExoPlayer for preview | Standard Android playback | Custom GL playback | Good sync but separate code path from export |
| Room + JSON for persistence | Offline-first, versioned | ProtoBuf, flatbuffers | Human-readable, easy migration, but larger storage |
| Milliseconds for time | Simpler than microseconds | Microseconds (TimeUs) as per plan | Frame-boundary rounding issues at high fps; must migrate to TimeUs |
| Single module architecture | Project is small (~63 files) | Multi-module Gradle | Per code_arc.md Rule 5, single module is appropriate for small project |

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
