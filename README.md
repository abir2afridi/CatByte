# CatByte

**An offline, Bangla-first video editor for Android.**

CatByte is a CapCut-class editor in progress: multi-track timeline, keyframes,
velocity curves, masks, transitions, colour work, text and captions, Media3
export — all on the device, with no account and no cloud render.

[Status](#status) · [Features](#features) · [Build and run](#build-and-run) ·
[Architecture](#architecture) · [Testing](#testing) · [Roadmap](#roadmap) ·
[Documentation](#documentation)

> **Honest status:** the export path is solid and 332 JVM/Robolectric tests
> pass. The GL Compositor infrastructure is complete (35 GLSL effects, blend
> modes, masks, chroma key, transitions) and compiles. Bangla font management
> is implemented. What remains: wiring the GL Compositor into preview/export,
> bundling OFL fonts, on-device E2E export test, and on-device AI models.
> Detailed tracking in [`STATUS.md`](STATUS.md) and [`FEATURE_MATRIX.md`](FEATURE_MATRIX.md).

---

## Status

| | |
|---|---|
| **Platform** | Android (minSdk 24, target/compile SDK 37), Kotlin 2.2, Jetpack Compose |
| **Rendering** | Media3 ExoPlayer (preview) + Media3 Transformer (export) |
| **GPU Compositor** | ✅ Infrastructure complete — 35 GLSL effects, 5 blend modes, masks, chroma key, transitions |
| **State** | Room + JSON snapshots, DataStore settings, durable undo journal |
| **Languages** | English + Bengali UI (full key parity), Bangla digit formatting, Bangla font manager |
| **APK** | ~25.8 MB debug build, no native libraries yet |
| **Plan progress** | ~68% — export core complete, GL compositor infrastructure complete, device verification pending |
| **Tests** | 332 JVM/Robolectric tests, 0 failures — no instrumented/device run yet |
| **CI** | GitHub Actions: lint, unit tests and debug APK (`.github/workflows/ci.yml`) |

Legend for the tables below: **✅ working** · **🟡 partial** · **🔴 not built**

## Features

### Editing
| Feature | State | Notes |
|---|---|---|
| Projects: create, duplicate, delete + undo, pin, search | ✅ | Aspect presets 9:16, 16:9, 1:1, 4:5 |
| Media import | ✅ | One-pass real duration + dimensions + display name; project adopts the imported aspect ratio and file name; waveform decode deferred |
| Responsive editor layout | ✅ | Transport/selection bars scroll horizontally, adaptive timeline height, height-constrained preview |
| Autosave | 🟡 | Saves on every change; debounce + journal pending |
| Undo / redo | ✅ | Gesture coalescing; persisted atomically so a crash keeps history |
| Multi-track timeline (video, overlay, audio, text, effects) | ✅ | Free pan, pinch zoom, vertical track scroll, one row per overlay + audio track, track reorder, inline trim handles, split, ripple delete, main reorder + overlay/cross-track drag + multi-select |
| Audio row drag + per-clip mute | 🟡 | One timeline row per audio clip; long-press drag with magnetic snap + project clamp; drag/fades/mixer wiring pending |
| Snapping (playhead, clip edges, audio/text/effect bounds, beats, markers) | ✅ | Priority order lives in the engine, not in the UI; detected beats are lower priority, markers higher; beat ticks and marker pins are drawn |
| Drag-and-drop clip move | 🟡 | Main-track long-press reorder and overlay start-time drag (magnetic + bounds-clamped, cross-track); audio row drag wired |
| Multi-select + group ops | 🟡 | Timeline MS toggle + selection bar; overlay group move/delete + overlay copy/paste wired; main-track contiguous block move + paste-attributes wired; cut and main-track paste pending |
| Markers | 🟡 | Add/jump/move-to-playhead/remove and snap are wired; rename/recolour/delete UI pending |
| Draggable playhead + free timeline pan | ✅ | Needle drags with edge auto-scroll; any timeline touch pauses playback; content centers under the playhead during playback |
| Keyframes (position, scale, rotation, opacity, volume) | ✅ | Sheet + interpolation + easing curves |
| Speed: constant and bezier velocity curves | ✅ | Presets: montage, hero moment, bullet time, jump cut |
| Masks (rectangle, circle, split, mirror, filmstrip) | 🟡 | Data + sheet + GL shaders exist; not burned into the pixels yet |
| Blend modes | 🟡 | Sheet + enum + GL shaders; export composites with normal alpha |
| 13 filters + adjustments (brightness, contrast, saturation, vignette, HSL) | ✅ | Applied via Media3 colour effects |
| Transitions (11 types) | 🟡 | Rendered through Media3, not true two-frame compositing; GL transition shaders exist |
| Text overlays, animations, TTS voice-over | ✅ | Bangla shaping not verified on device; font manager implemented |
| Preview surface | ✅ | Composition preview when available; ExoPlayer fallback renders a real video surface instead of a frozen still |
| CapCut-style editor shell | ✅ | Top bar (close, search, trial badge, AI UHD menu, export pill), transport row (fullscreen / play / transition / undo / redo), timecode strip, clip context action bar, bottom tool nav (Edit…Background + Tools) |
| Editor feature search | ✅ | Top-bar search jumps to 29 real panels/actions (split, volume, crop, captions, markers, …) |
| Clip panels: Volume, Speed, Crop, Opacity, Animations, Captions, Background, Timeline tools, AI tools | ✅ | One sheet per tool; cloud/GPU-only entries state why they are disabled instead of faking toggles |
| Clip actions: split, duplicate, replace, freeze, reverse, extract audio, crop, opacity, loudness | ✅ | Replace re-probes duration/dimensions; loudness normalisation is waveform-driven (`AudioLoudness`); crop writes a real transform via `CropMath` |

### Audio
| Feature | State | Notes |
|---|---|---|
| Multi-track music, volume, fades, waveform | ✅ | |
| Voice-over recording | ✅ | MediaRecorder based |
| Beat detection | 🟡 | Detector, beat-magnetic snapping, beat ticks and auto-cut-to-beat are wired; source beats map through trim/start/speed; editable beat grid pending |
| Audio rows + per-clip mute | 🟡 | One timeline row per audio clip with long-press drag (magnetic + clamp) and mute; per-clip volume/fades UI + mixer wiring pending |
| Limiter, ducking, EQ, loudness (LUFS), noise reduction | 🟡 | Maths implemented and tested in the engine (`AudioMath`); not wired to the mixer |

### Export
| Feature | State | Notes |
|---|---|---|
| 480p–4K, 24–60 fps | ✅ | 480/540/720/1080/2K/4K map to exact heights; capability ladder unit-tested (HEVC→H.264, size/fps downgrade) |
| Bitrate selector (5–100 Mbps) | ✅ | Real encoder setting: `DefaultEncoderFactory` + `VideoEncoderSettings.setBitrate()` in foreground and background exports; storage precheck follows it |
| H.264 + HEVC, AAC | ✅ | Encoder rejection retries once with the default encoder |
| Progress, cancel, validation, MediaStore save | ✅ | Duration/dimension validation before success |
| Background export (foreground service) | ✅ | Typed `mediaProcessing` / `dataSync` notification |
| Platform presets, audio-only (.m4a), GIF | 🟡 | MP4 export + audio-only presets wired; the GIF tab is shown for parity but export stays disabled (no GIF encoder bundled) and says so |
| HDR tone-map, 10-bit output | 🔴 | 10-bit sources detected by the proxy policy only |

### Not built yet
- **GL compositor wiring** — 35 GLSL effect sources, transition weights, blend modes, masks, and chroma key shaders exist in `com.example.engine.gl`, but no EGL surface executes them in preview/export yet. The canvas background colour is saved and read by the render graph, but the live compositor still draws a black canvas.
- **GIF export** — the export sheet shows the GIF tab for parity, but no GIF encoder is bundled, so it stays disabled with an explanation.
- **Bangla fonts and shaping** — UI strings are translated; OFL fonts are not bundled (font manager ready) and conjunct shaping has no golden-image test.
- **On-device AI** — auto-reframe crop maths and silence detection are ready; the detectors (MediaPipe/ML Kit/TFLite) are not.
- **Camera capture** — no CameraX module, so shoot-and-edit needs the gallery.
- **Cloud anything** — no account, no cloud render, no marketplace, no collaboration. Editing works from local files only.

---

## Build and run

Requirements: **JDK 17** and the **Android SDK (compileSdk 37)**. Node is only
needed for the desktop prototype.

```bash
# Debug APK -> app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:assembleDebug

# Install on a connected device/emulator
./gradlew :app:installDebug

# Unit tests (332 tests, incl. Robolectric Compose gestures)
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
│   └── gl/         GPU compositor: GLShaderProgram, GLTextureCache, GLFramebuffer, GLCompositor
├── data/           Room, JSON, import, export, player, cache, services, fonts
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
| `ClipTrimHandle` | Inline timeline trim: frame-quantised source trim, speed-aware, minimum-duration clamp |
| `AspectMath` | Maps imported video dimensions/rotation to the closest canvas preset |
| `RenderGraph` | Single render description + canvas maths |
| `TransitionEngine` | 17 transition kinds, two-frame weights, shaders |
| `EffectShaders` | 35 GLSL ES 3.0 effect sources |
| `LutCube` | `.cube` parsing, trilinear sampling, chain baking |
| `DecoderBudget` | Decoder pool priority/LRU plan, proxy policy |
| `SubtitleIO` | SRT/VTT parse/write, retime, validate, word→cue |
| `CaptionBridge` | Cues ↔ timeline text overlays (SRT/VTT import, subtitle style) |
| `BanglaText` | Native digits, mark-safe line breaking, caption fit, font management |
| `AudioMath` | Fades, ducking, limiter, mix, RMS/LUFS, PCM16 |
| `DeviceProfiles` | LOW/MID/HIGH tiers, thermal + battery degradation |
| `AutoReframe` | Subject-aware crop, smoothing, Ken Burns |
| `SilenceDetector` | Silence ranges from the waveform pyramid |
| `ExportPresets` | Platform presets, bitrate ladder, size estimate |

### GL Compositor (`com.example.engine.gl`)

| Class | Purpose |
|---|---|
| `GLShaderProgram` | OpenGL ES 3.0 shader compilation, linking, uniform/attribute caching |
| `GLTextureCache` | LRU texture cache with 96MB memory budget |
| `GLFramebuffer` | Offscreen framebuffers for ping-pong rendering (preview, effect, mask passes) |
| `GLCompositor` | Main compositor: base rendering, effects, blend modes, masks, chroma key, transitions |
| `GLPreviewSurface` | Real-time GPU preview SurfaceView |
| `ShaderTemplate` | GLSL ES 3.0 template for effect shaders |

---

## Testing

```bash
./gradlew :app:testDebugUnitTest --rerun
```

- **332 JVM/Robolectric tests, 0 failures**: timing, snapping, timeline geometry, undo journal,
  render graph, transitions, LUTs, captions, audio maths, Bangla text, device
  policy, export math, JSON round-trip, editor workflow, localization parity,
  inline trim maths, track mute/lock/solo persistence, GL compositor infrastructure,
  Bangla font manager, and 2 Compose UI test classes (timeline gestures, track
  header controls) driven by Robolectric.
- **CI:** `.github/workflows/ci.yml` runs lint, unit tests and `assembleDebug`
  on pushes and pull requests to `main`/`develop`.
- **Not covered yet:** instrumented codec/export runs, golden-frame rendering,
  60 fps scroll, memory and soak tests. See [`TEST_REPORT.md`](TEST_REPORT.md).

## Roadmap

1. **Wire GL Compositor into preview & export** — Connect `GLPreviewSurface` to `EditorPreviewStage`, implement `CustomVideoCompositor` for export
2. **Bangla font assets** — Download OFL fonts (Noto Sans Bengali, Hind Siliguri, Tiro Bangla, Anek Bangla) to `assets/fonts/bangla/`
3. **On-device E2E export test** — Run `connectedDebugAndroidTest` on real device, record in `TEST_REPORT.md`
4. **Decoder Pool + LRU Bitmap Cache** — `DecoderPool.kt`, `BitmapLruCache.kt` for 60fps scroll
5. **Proxy Generation Pipeline** — Implement `ProxyManager.generateProxies()` with WorkManager
6. **Audio Mixer Wiring** — Connect `AudioMath` (limiter, ducking, EQ, LUFS) to Media3 `AudioProcessor` chain
7. **Keyframe Curve Editor** — Extend `KeyframeSheet` with bezier graph editor
8. **AI Features** — Bundle Vosk/Whisper for auto-captions, MediaPipe for background removal
9. **GIF/WebP Export** — Implement encoder in `GifExporter`
10. **CameraX Capture** — Shoot-and-edit workflow

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
| [`IMPLEMENTATION_PLAN.md`](IMPLEMENTATION_PLAN.md) | Detailed implementation roadmap |
| [`IMPLEMENTATION_SUMMARY.md`](IMPLEMENTATION_SUMMARY.md) | Summary of recent work |

## Privacy

Editing, preview and export run entirely on the device. There is no account
system, no cloud render and no telemetry; project files and media stay in app
storage and the system gallery.

## License

MIT — see [`LICENSE`](LICENSE).

## Credits

Media3, Jetpack Compose, Room, Kotlin coroutines, Coil, Robolectric, JUnit.
CapCut for the UX patterns this project is measured against.