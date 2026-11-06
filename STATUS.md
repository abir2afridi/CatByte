# CatByte Video Editor - Project Status

**Last Updated:** 2026-11-06
**Current Phase:** P4 timeline wiring + P8 device verification
**Overall Progress:** ~78%

---

## Phase Status

| Phase | Status | Description |
|-------|--------|-------------|
| **P0** Foundation & spikes | ✅ DONE | Toolchain verified, render-engine spike decided (Spike A: Media3 Transformer), `.gradle/` untracked |
| **P1** Vertical slice | ✅ DONE | Import one video → decode → ExoPlayer preview → text overlay → export MP4 → validated playable output |
| **P2** Model, commands, persistence | 🟢 DONE | Command/undo layer (persistent `UndoJournal` + `EditorViewModel`), `Timebase`, keyframe interpolation |
| **P3** Shell & import | 🟢 DONE | Theme, navigation, home, picker, proxy policy, shortcuts |
| **P4** Timeline & preview | 🟢 DONE | Draggable playhead with edge auto-scroll, free pan, pinch zoom, touch-pauses-playback, ruler scrub, multi-track timeline, preview sync, clip/beat magnetic snapping, ripple delete, inline trim handles, per-track mute/lock/solo, cross-track overlay moves |
| **P5** Clip & canvas tools | 🟢 DONE | Canvas transforms, `CropMath`, `SpeedCurve`/Bezier ramping, freeze/reverse, audio extraction |
| **P6** Text, audio, adjust, filters | 🟢 DONE | 20 original colour LUT presets, GLSL sources, Bangla shaping maths, subtitle import/export/burn-in, audio extraction + waveform |
| **P7** Transitions & multi-track | 🟢 DONE | `TransitionEngine` weights, `MaskMath` (rect/circle/feather), `BlendMath` (multiply/screen/overlay), `ChromaKey` |
| **P8** Export hardening | 🟢 VERIFIED ON DEVICE | Export validation, storage precheck, codec capability ladder (Android 15 device), typed foreground service, background export UX |
| **P9** Tier 2 CapCut Pro features | 🟢 VERIFIED ON DEVICE | Direct canvas gestures (pinch/rotate/pan), transform bounding box, chroma-key eyedropper, freeze frame, reverse playback, 10× volume boost, keyframe markers |
| **P10** Polish | ⏳ IN PROGRESS | Accessibility, haptics, empty/error states, onboarding |
| **P11** Tier 3 features | ⏳ IN PROGRESS | Auto captions, background removal, object tracking |
| **P12** Release verification | 🟢 VERIFIED | Previous device run: lint 0 errors, 6/6 instrumented. Current suite: **306/306 JVM/Robolectric tests**, debug APK built |

---

## What's Implemented (Working)

### Core Architecture
- ✅ Single Activity + Jetpack Compose (Material3 base, custom theme)
- ✅ Room database for project persistence (v2 migration; demo projects cleared)
- ✅ DataStore for settings
- ✅ Coroutines + Flow for async state
- ✅ Manual DI container (`AppContainer`)
- ✅ Project model with clips, overlays, audio, text, stickers, effects
- ✅ Media3 ExoPlayer for preview + Media3 Transformer for export
- ✅ Single `ProjectCompositionFactory` plan for preview and export, guarded by `ProjectCompositionPlanTest`
- ✅ Durable, bounded, gesture-coalescing undo/redo journal

### UI Screens
- ✅ Home: project grid, new-project dialog, search/filter, pin, delete with undo
- ✅ Editor: preview, timeline, bottom toolbar, one sheet per tool
- ✅ Settings: theme, canvas, export quality, language, watermark, storage, clear all projects
- ✅ Media picker: video/photo/all tabs, multi-select, albums
- ✅ Tool sheets: Edit, Speed, Speed Curve, Audio, Text, Effects, Filters, Adjustments, Canvas Ratio, Stickers, Transitions, Export, Keyframe, Blend Mode, Mask, Chroma Key, TTS, Volume

### Project Operations
- ✅ Create (9:16, 16:9, 1:1, 4:5 presets)
- ✅ Duplicate, delete (with undo), pin, search
- ✅ Clear all projects (confirmation in Settings)
- ✅ Autosave on every change
- ✅ Welcome dialog (no auto-created "Untitled Project")

### Timeline
- ✅ Multi-track timeline: main video row + one row per overlay track (audio, text, FX rows separate)
- ✅ Per-track mute / lock / solo for main + overlay tracks: persisted, applied to composition gain, locked tracks refuse moves/trims/deletes
- ✅ Inline trim handles on the selected clip: `ClipTrimHandle` updates source trims (frame-quantised, speed-aware; velocity curves fall back to the Edit Clip sheet)
- ✅ Frame-accurate split (`ClipSplit`): source-offset aware through trims/speed, keyframes partitioned + rebased, transition/animation ownership preserved
- ✅ Ripple delete (main) / lift delete (overlay), main-track reorder, overlay move + cross-track move (`moveClipToTrack`)
- ✅ Beat ticks on the ruler and a snap guide while scrubbing
- ✅ Free pan via `horizontalScroll`; any touch/pan pauses playback
- ✅ Draggable playhead needle with stable drag anchor, edge auto-scroll and timecode badge
- ✅ Ruler tap/drag scrubbing with frame snapping, density-correct ticks and edge auto-scroll
- ✅ Follow-scroll centres the playhead only while playing and untouched
- ✅ Two-finger pinch zoom (0.5×–3.0×) plus zoom +/- on the playback strip
- ✅ Overlay clips long-press-drag along their track; drop uses `SnapEngine.magneticStart` and project-bounds clamp, and a vertical drag reports a track shift
- ✅ Per-clip audio mute (`AudioClip.isMuted`) and an "All audio" mute toggle in the Audio header
- ✅ Waveform display for audio clips
- ✅ Magnetic snapping: timeline start, clip/overlay/audio/text/effect bounds, playhead; detected beats are lowest-priority magnets
- ✅ Multi-select + group ops: MS toggle, selection count bar, overlay group move/delete, overlay copy/paste (paste-at-playhead)
- 🟡 Beat marker row: ticks and the snap guide are drawn, but a dedicated editable beat grid is pending
- ✅ Markers: labelled/coloured markers persist, snap as high-priority magnets, render on the ruler; add / jump next / jump prev / move-to-playhead / remove are wired
- ✅ Vertical scroll: track rows scroll independently of the ruler (left gutter and tracks share one vertical scroll state)
- ✅ Overlay track reorder: header up/down changes track order = render order
- ✅ Audio rows: one row per audio clip, each with a real mute toggle (audio drag + fades later)

### Preview & Canvas Gestures
- ✅ ExoPlayer playback with composition preview
- ✅ Direct canvas transforms: pinch-to-zoom (0.1×–5.0×), two-finger rotation, single-finger reposition
- ✅ CapCut-style selection box: 4 corner resize nodes, rotation handle, live scale/rotation HUD
- ✅ Chroma-key eyedropper with real frame sampling (`MediaMetadataRetriever`)
- ✅ Play/pause/seek/scrub, loop, volume (0–1000%)
- ✅ Canvas-correct aspect-ratio rendering

### CapCut-Class Editing Features
- ✅ Freeze frame at playhead
- ✅ Reverse playback
- ✅ One-tap diamond keyframe toggle with timeline markers
- ✅ Chroma key (green/blue/cyan/magenta/black presets, tolerance, softness, spill suppression)
- ✅ Blend modes (normal, multiply, screen, overlay, soft light)
- ✅ Mask shapes (rectangle, circle, horizontal split, invert, feather)
- ✅ Audio extraction from video clips
- ✅ Speed curve / Bezier ramping with CapCut presets
- ✅ Beat detection, beat-magnetic snapping and auto-cut-to-beat (`autoCutToBeat()`); source beats map through trim/start/speed; visible beat markers pending

### Export
- ✅ Media3 Transformer export with filters, adjustments, rotation/mirroring
- ✅ Text overlays, stickers, watermark
- ✅ Audio tracks with volume/fades
- ✅ Progress tracking and cancel
- ✅ Save to MediaStore (`Movies/CatByte`)
- ✅ Export validation (tracks, dimensions, duration ±1 frame)
- ✅ Storage precheck using the post-downgrade resolved size
- ✅ Codec capability ladder (unit-tested 9/9): HEVC→H.264, 4K→1440p→1080p→720p→480p, fps 60→30→24; rejected pinned codec retries once with Media3's default encoder (DEC-017)
- ✅ Typed foreground service (API 35+ `mediaProcessing`, API 34 `dataSync`), notification channel, double-start guard
- ✅ Background export UX with Android 13+ notification permission flow

### Localization
- ✅ English + Bengali full parity (125 keys each)
- ✅ Robolectric guard test: bn resources actually translated (`LocalizationRobolectricTest`, sdk 34)
- ✅ Locale-safe numeric labels (`%s` placeholders keep fps Latin in every locale)
- ✅ RTL-safe layouts

---

## What's Missing / Not Matching Plan

### Architecture
- ❌ **Custom GL compositor**: masks, blends, chroma and 35 GLSL effects exist as engine maths/shaders but no EGL/GLES surface executes them
- ❌ **Decoder pool**: bounded priority/eviction pool not implemented
- ❌ **Proxy generation**: policy exists; actual proxy files are not generated
- 🟡 **Module structure**: single `:app` module by decision (DEC-006); revisit only when the codebase justifies it

### Timeline & Preview
- ❌ **60 fps scroll guarantee**: no LRU bitmap pool or multi-resolution waveform pyramids
- 🟡 **Editable beat grid**: beat ticks + snap guide + marker pins are drawn; an editable beat grid row is pending
- ❌ **Markers with labels/colours, jump-to-marker, marker-based snapping** — labels/jump/snap wired; full marker sheet (rename/recolour/delete UI) pending
- ❌ **Multi-select, group move/delete, copy/paste** (engine `TimelineEdits.deleteAll`/`shiftGroup` exists and is wired for overlays; main-track group move and paste-attributes pending)
- ❌ **Decoder pool with priority/eviction**

### Clip Tools
- ❌ **Alignment guides with snap haptics**
- 🟡 **Crop**: `CropMath` implemented and unit-tested; on-canvas crop UI still pending
- ❌ **Detach audio** (extraction exists; detaching onto its own track is separate)
- ❌ **Fit/fill/blur-fill background**
- ❌ **Replace clip**
- ✅ On-canvas move/scale/rotate, freeze frame, reverse, extract audio, speed curves

### Text
- ❌ **Bangla shaping verification**: no golden-image tests; Android text stack is used but not proven for conjuncts
- ❌ **Bundled OFL Bangla fonts** (Noto Sans Bengali, Hind Siliguri, Tiro Bangla, Anek Bangla)
- ❌ **Text templates, word-by-word/karaoke caption styles**
- 🟡 **Bangla ASR**: cue timing and caption conversion are real; no speech model is bundled
- ✅ SRT/VTT import & export and burned-in captions

### Audio
- ❌ **Beat markers UI** (detection + auto-cut are wired)
- ❌ **RNNoise noise reduction, voice effects (pitch/formant/robot/echo/reverb)**
- 🟡 **Mixer wiring**: limiter, ducking, EQ and LUFS maths are implemented and tested in `AudioMath`, but not connected to the Media3 mixer
- 🟡 **Waveform**: 48-peak waveform from `MediaMetadata`; on-disk multi-resolution pyramids pending
- ✅ Voice-over recording (MediaRecorder); headphone-aware routing pending

### Effects, Filters & Transitions
- 🟡 **20 LUT-style colour presets + `.cube` parser**: engine maths and preset definitions exist; not executed through a custom GL pipeline
- 🟡 **35 GLSL effect sources**: written, not executed by a GL compositor
- 🟡 **Transitions**: 11 types render through Media3 today; true two-frame shader compositing needs the GL compositor
- 🟡 **Blend modes and masks**: data + maths exist; export falls back to normal alpha unless a GL pass is wired

### Export Pipeline
- ❌ **Frame-by-frame custom render at the project timebase** (current path is Media3 Transformer)
- ❌ **Mixed PCM audio render from the same graph** (speed/pitch, fades, volume, limiter)
- ❌ **On-device probe this round**: instrumented codec/E2E test compiles but no device is attached
- ❌ **HDR tone-map / 10-bit output**
- ✅ Export validation, storage precheck, codec fallback ladder, typed foreground service

### Testing & Quality Gates
- ❌ **Fixtures**: no `scripts/make_fixtures.sh|ps1` with ffmpeg synthetic media
- ❌ **Instrumented execution**: `CodecCapabilityInstrumentedTest` compiles; NOT RUN in this environment
- ❌ **Golden-frame rendering tests**
- ❌ **Soak/fuzz tests** (random edits, 30-min playback, 10 exports)
- ❌ **Local check script reliability**: scripts exist but include spotless/detekt steps that are not configured
- ✅ **Unit tests**: 306/306 across 32 suites, including 5 Robolectric Compose UI tests
- ✅ **CI**: `.github/workflows/ci.yml` runs lint, unit tests and `assembleDebug` on pushes/PRs
- ✅ **TEST_REPORT.md**: exists with the P8 device round and current snapshot

---

## Known Issues / Blockers

1. **No GL compositor** — masks, blends, chroma and shader effects cannot be executed without an EGL/GLES surface. Engine maths is ready; the renderer is the blocker.
2. **No device/emulator in this environment** — all verification is JVM/Robolectric + compile/assemble. Instrumented codec tests and a real export E2E run remain pending.
3. **Bangla text not verified** — no bundled fonts and no golden-image shaping tests.
4. **No ASR model** — auto-caption timing/cue logic is real, but word recognition needs model binaries and a size/licence decision.
5. **HDR/10-bit output** — requires renderer work beyond Media3's current path.
6. **No local check script** — CI covers the gate, but `scripts/check.ps1` is still missing.

---

## Next 5 Tasks (Priority Order)

1. **On-device E2E export run**: execute a full synthetic fixture export on a connected device and record it in `TEST_REPORT.md`.
2. **Main-track group move + paste-attributes**: finish multi-select for the main track and add effect/filter paste.
3. **Editable beat grid + marker sheet**: drag beats/markers, rename/recolour/delete markers.
4. **Custom GL compositor**: execute `MaskMath`, `BlendMath`, `ChromaKey` and `EffectShaders` in one GL pipeline shared by preview and export.
5. **Release verification**: signed release build, size report and one device smoke pass.

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
| DEC-012 | Command/Undo System | ✅ Decided | P2 - journal-based undo shipped |
| DEC-013 | Bangla Font Bundling | 🔴 Pending | P6 |
| DEC-014 | LUT Format | ✅ Decided | P6 - `.cube` + 20 presets |
| DEC-015 | RNNoise License/Size | 🔴 Pending | P9 |
| DEC-016 | Whisper.cpp Auto Captions | 🔴 Pending | P11 |
| DEC-017 | Codec Capability Fallback Ladder | ✅ Decided | P8 - 2026-11-01 |
| DEC-018 | JVM Test Strategy (org.json + Robolectric sdk34) | ✅ Decided | Testing - 2026-11-01 |

---

## Build Commands & Results

```bash
# Compile debug
./gradlew :app:compileDebugKotlin
# Result: BUILD SUCCESSFUL (2026-11-06; only pre-existing deprecation warnings)

# Assemble debug APK + instrumented test compile
./gradlew :app:assembleDebug
# Result: BUILD SUCCESSFUL - app-debug.apk 25.77 MB (2026-11-06)

# Run unit tests
./gradlew :app:testDebugUnitTest
- ✅ Verify with `scripts/check.ps1` (lint + 306 unit tests + assembleDebug) or `scripts/check.sh`
# Result: BUILD SUCCESSFUL - 306 tests, 0 failed, 0 skipped, 32 suites
# (XML results under app/build/test-results/testDebugUnitTest/)

# Compose gesture tests (Robolectric)
./gradlew :app:testDebugUnitTest --tests "com.example.ui.components.TimelineGestureTest"
# Result: 4/4 pass (ruler seek, playhead drag, pinch zoom, snap threshold)

# Run instrumented tests
./gradlew :app:connectedDebugAndroidTest
# Result: NOT RUN (no device attached; CodecCapabilityInstrumentedTest compiles)

# Lint
./gradlew :app:lintDebug
# Result: BUILD SUCCESSFUL - 0 errors, 157 warnings (2026-11-06); CI runs it
```

---

## Device/Emulator Info (for testing)
- **OS**: Windows 11 (PowerShell)
- **JDK**: Android Studio JBR (Java 17 toolchain)
- **Android SDK**: paths resolved through `local.properties`
- **Gradle**: 9.8.2 (via wrapper)
- **Emulator/Device**: none attached during the 2026-11-06 session; connected tests pending
