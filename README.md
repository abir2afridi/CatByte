# CatByte

**An offline, Bangla-first video editor for Android.**

CatByte is a CapCut-class editor in progress: multi-track timeline, keyframes,
velocity curves, masks, transitions, colour work, text and captions, Media3
export — all on the device, with no account and no cloud render.

[Status](#status) · [Features](#features) · [Build and run](#build-and-run) ·
[Architecture](#architecture) · [Testing](#testing) · [Roadmap](#roadmap) ·
[Documentation](#documentation)

> **Honest status:** the export path is solid and 278 JVM/Robolectric tests
> pass, but the "CapCut feel" still needs a real GL compositor, bundled Bangla
> fonts and on-device models. What is done and what is not is tracked in
> [`docs/M1-M4-ENGINE.md`](docs/M1-M4-ENGINE.md) and
> [`FEATURE_MATRIX.md`](FEATURE_MATRIX.md).

---

## Status

| | |
|---|---|
| **Platform** | Android (minSdk 24, target/compile SDK 37), Kotlin 2.2, Jetpack Compose |
| **Rendering** | Media3 ExoPlayer (preview) + Media3 Transformer (export) |
| **State** | Room + JSON snapshots, DataStore settings, durable undo journal |
| **Languages** | English + Bengali UI (full key parity), Bangla digit formatting |
| **APK** | ~26 MB debug build, no native libraries yet |
| **Plan progress** | ~62% — export core complete, device verification pending (`STATUS.md`) |
| **Tests** | 278 JVM/Robolectric tests, 0 failures — no instrumented/device run yet |
| **CI** | GitHub Actions: lint, unit tests and debug APK (`.github/workflows/ci.yml`) |

Legend for the tables below: **✅ working** · **🟡 partial** · **🔴 not built**

## Features

### Editing
| Feature | State | Notes |
|---|---|---|
| Projects: create, duplicate, delete + undo, pin, search | ✅ | Aspect presets 9:16, 16:9, 1:1, 4:5 |
| Autosave | 🟡 | Saves on every change; debounce + journal pending |
| Undo / redo | ✅ | Gesture coalescing; persisted atomically so a crash keeps history |
| Multi-track timeline (video, overlay, audio, text, effects) | ✅ | Free pan, pinch zoom, trim sheet, split, ripple delete, main-track reorder + overlay drag; real per-track mute/lock/solo pending |
| Snapping (playhead, clip edges, audio/text/effect bounds, beats) | ✅ | Priority order lives in the engine, not in the UI; detected beats are the lowest-priority magnets |
| Draggable playhead + free timeline pan | ✅ | Needle drags with edge auto-scroll; any timeline touch pauses playback; content centers under the playhead during playback |
| Drag-and-drop clip move | 🟡 | Main-track long-press reorder and overlay start-time drag (magnetic + bounds-clamped); free cross-track drag pending |
| Keyframes (position, scale, rotation, opacity, volume) | ✅ | Sheet + interpolation + easing curves |
| Speed: constant and bezier velocity curves | ✅ | Presets: montage, hero moment, bullet time, jump cut |
| Masks (rectangle, circle, split, mirror, filmstrip) | 🟡 | Data + sheet exist; not burned into the pixels yet |
| Blend modes | 🟡 | Sheet + enum; export composites with normal alpha |
| 13 filters + adjustments (brightness, contrast, saturation, vignette, HSL) | ✅ | Applied via Media3 colour effects |
| Transitions (11 types) | 🟡 | Rendered through Media3, not true two-frame compositing |
| Text overlays, animations, TTS voice-over | ✅ | Bangla shaping not verified on device |

### Audio
| Feature | State | Notes |
|---|---|---|
| Multi-track music, volume, fades, waveform | ✅ | |
| Voice-over recording | ✅ | MediaRecorder based |
| Beat detection | 🟡 | Detector, beat-magnetic snapping and auto-cut-to-beat are wired; source beats map through trim/start/speed; visible beat markers pending |
| Limiter, ducking, EQ, loudness (LUFS), noise reduction | 🟡 | Maths implemented and tested in the engine; not wired to the mixer |

### Export
| Feature | State | Notes |
|---|---|---|
| 480p–4K, 24–60 fps | ✅ | Capability ladder unit-tested (HEVC→H.264, size/fps downgrade) |
| H.264 + HEVC, AAC | ✅ | Encoder rejection retries once with the default encoder |
| Progress, cancel, validation, MediaStore save | ✅ | Duration/dimension validation before success |
| Background export (foreground service) | ✅ | Typed `mediaProcessing` / `dataSync` notification |
| Platform presets, audio-only (.m4a), GIF | 🟡 | Preset chips + size estimate wired; audio-only and GIF export not built |
| HDR tone-map, 10-bit output | 🔴 | 10-bit sources detected by the proxy policy only |

### Not built yet
- **GL compositor** — 35 GLSL effect sources and the transition weights exist in
  `com.example.engine`, but no EGL surface executes them yet.
- **Bangla fonts and shaping** — UI strings are translated; OFL fonts are not
  bundled and conjunct shaping has no golden-image test.
- **On-device AI** — auto-reframe crop maths and silence detection are ready;
  the detectors (MediaPipe/ML Kit/TFLite) are not.
- **Camera capture** — no CameraX module, so shoot-and-edit needs the gallery.
- **Cloud anything** — no account, no cloud render, no marketplace, no
  collaboration. Editing works from local files only.

---

## Build and run

Requirements: **JDK 17** and the **Android SDK (compileSdk 37)**. Node is only
needed for the desktop prototype.

```bash
# Debug APK -> app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:assembleDebug

# Install on a connected device/emulator
./gradlew :app:installDebug

# Unit tests (278 tests, incl. Robolectric Compose gestures)
./gradlew :app:testDebugUnitTest

# Lint (CI runs it on every push/PR)
./gradlew :app:lintDebug
```

### Desktop prototype (legacy)

`src/` and `src-tauri/` hold an earlier Tauri 2 + React + Fabric.js + FFmpeg.wasm
prototype. It is **not** the shipping app and shares no code with the Android
build:

```bash
npm install
npm run tauri:dev     # or: npm run dev      (browser only)
npm run tauri:build
```

---

## Architecture

```
Project (Kotlin model)
        │
        ▼
RenderGraphBuilder ──► RenderGraph   immutable layout: layers, audio, text,
        │                            fps, canvas, clamped transitions
        ├──────────────► preview  (ExoPlayer + Media3 effects)
        └──────────────► export   (Media3 Transformer)
```

`ProjectCompositionFactory.plan()` consumes the same graph for both paths, so
clip order, gaps, overlay starts, audio starts and transition durations cannot
differ between what you see and what you export. A parity test
(`ProjectCompositionPlanTest`) guards that.

```
app/src/main/java/com/example/
├── model/          VideoProject, MediaClip, AudioClip, TextOverlay, keyframes
├── engine/         platform-free logic, unit tested on the JVM
├── data/           Room, JSON, import, export, player, cache, services
├── ui/             Compose screens, tool sheets, timeline, canvas, theme
└── viewmodel/      EditorViewModel (editing commands, undo, export state)
```

### Engine modules (`com.example.engine`)

| Module | Responsibility |
|---|---|
| `Timebase` | Rational fps (30000/1001), `TimeUs`, frame snapping, timecode |
| `SnapEngine` | Magnet collection/priority (clip edges, playhead, markers, beats), magnetic main-track placement |
| `TimelineEdits` | Magnetic placement, ripple vs lift delete, reorder-close-gaps, trim bounds |
| `UndoJournal` | Atomic, bounded, gesture-coalescing undo/redo persistence |
| `CenterPlayhead` | Fixed-playhead scroll maths, zoom anchoring |
| `TimelineGeometry` | dp/ms conversions, drag-anchor maths, screen x, timeline constants |
| `BeatGrid` | Onset peaks, tempo from inter-onset intervals, beat lattice, nearest beat |
| `ClipSplit` | Frame-accurate split: source-offset resolution, keyframe partition/rebase, transition/animation rules |
| `RenderGraph` | Single render description + canvas maths |
| `TransitionEngine` | 17 transition kinds, two-frame weights, shaders |
| `EffectShaders` | 35 GLSL ES 3.0 effect sources |
| `LutCube` | `.cube` parsing, trilinear sampling, chain baking |
| `DecoderBudget` | Decoder pool priority/LRU plan, proxy policy |
| `SubtitleIO` | SRT/VTT parse/write, retime, validate, word→cue |
| `CaptionBridge` | Cues ↔ timeline text overlays (SRT/VTT import, subtitle style) |
| `BanglaText` | Native digits, mark-safe line breaking, caption fit |
| `AudioMath` | Fades, ducking, limiter, mix, RMS/LUFS, PCM16 |
| `DeviceProfiles` | LOW/MID/HIGH tiers, thermal + battery degradation |
| `AutoReframe` | Subject-aware crop, smoothing, Ken Burns |
| `SilenceDetector` | Silence ranges from the waveform pyramid |
| `ExportPresets` | Platform presets, bitrate ladder, size estimate |

---

## Testing

```bash
./gradlew :app:testDebugUnitTest --rerun
```

- **278 JVM/Robolectric tests, 0 failures**: timing, snapping, timeline geometry, undo journal,
  render graph, transitions, LUTs, captions, audio maths, Bangla text, device
  policy, export math, JSON round-trip, editor workflow, localization parity,
  and 4 Compose gesture tests (ruler scrub, playhead drag, pinch zoom, snap
  threshold) driven by Robolectric.
- **CI:** `.github/workflows/ci.yml` runs lint, unit tests and `assembleDebug`
  on pushes and pull requests to `main`/`develop`.
- **Not covered yet:** instrumented codec/export runs, golden-frame rendering,
  60 fps scroll, memory and soak tests. See [`TEST_REPORT.md`](TEST_REPORT.md).

## Roadmap

1. Run the instrumented export probe on a real device and record it in
   `TEST_REPORT.md`.
2. Free drag of clips across tracks (main-track reorder and overlay start-time
   drag are wired; cross-track placement remains).
3. Execute the shader library in a GL compositor shared by preview and export.
4. Bundle Bangla OFL fonts and add golden-image shaping tests.
5. Finish captions and the export sheet (SRT/VTT import/export, readable
   auto-caption cues and keyboard shortcuts are wired; karaoke highlighting, a
   real speech model, platform-preset chips in the sheet and the audio mixer
   remain).
6. CameraX capture, then on-device cutout/reframe/tracking.

Full breakdown and device-only work: [`docs/M1-M4-ENGINE.md`](docs/M1-M4-ENGINE.md).

## Documentation

| Document | Contents |
|---|---|
| [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) | Module boundaries, timing model, render/audio data flow |
| [`docs/DECISIONS.md`](docs/DECISIONS.md) | ADR log (Media3 transformer, Room, time base, codec ladder) |
| [`docs/M1-M4-ENGINE.md`](docs/M1-M4-ENGINE.md) | What the engine milestones delivered; remaining device work |
| [`docs/SPIKE_RENDER_ENGINE.md`](docs/SPIKE_RENDER_ENGINE.md) | Render engine spike and its outcome |
| [`STATUS.md`](STATUS.md) | Phase-by-phase progress and known gaps |
| [`FEATURE_MATRIX.md`](FEATURE_MATRIX.md) | Feature-by-feature status |
| [`TEST_REPORT.md`](TEST_REPORT.md) | Test coverage and results |
| [`docs+/plan.md`](docs+/plan.md) | Original build plan |
| [`THIRD_PARTY_LICENSES.md`](THIRD_PARTY_LICENSES.md) | Dependency licences |

## Privacy

Editing, preview and export run entirely on the device. There is no account
system, no cloud render and no telemetry; project files and media stay in app
storage and the system gallery.

## License

MIT — see [`LICENSE`](LICENSE).

## Credits

Media3, Jetpack Compose, Room, Kotlin coroutines, Coil, Robolectric, JUnit.
CapCut for the UX patterns this project is measured against.
