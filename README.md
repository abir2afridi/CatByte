# CatByte

**An offline, Bangla-first video editor for Android.**

CatByte is a CapCut-class editor: multi-track timeline, keyframes, velocity
curves, GPU masks, chroma key and colour grading, transitions, karaoke
captions with real on-device speech recognition, text and stickers, loudness
normalisation, export queue with history — all on the device, with no account
and no mandatory cloud.

[Status](#status) · [Features](#features) · [Build and run](#build-and-run) ·
[Architecture](#architecture) · [Testing](#testing) · [Roadmap](#roadmap) ·
[Documentation](#documentation)

> **Honest status:** 1,000 JVM/Robolectric tests across 130 suites pass and the
> debug APK builds. Masks, chroma key, the per-pixel grade suite and timeline
> effects are burned into the render by GPU shaders that run in **both** the
> preview and the export. Auto-captions run fully offline through Vosk
> (consented model download, English + Bangla); an optional OpenAI-compatible
> endpoint covers the remaining AI chips. What remains: instrumented/device
> verification, ML-Kit-backed chips (remove BG, reframe, tracking), 16 KB
> page-size hygiene for the Vosk `.so`, and landscape/tablet layouts.
> Detailed tracking in [`STATUS.md`](STATUS.md) and [`FEATURE_MATRIX.md`](FEATURE_MATRIX.md).

---

## Status

| | |
|---|---|
| **Platform** | Android (minSdk 24, target/compile SDK 37), Kotlin 2.2, Jetpack Compose (BOM 2025.01, UI 1.7.7) |
| **Rendering** | One Media3 `Composition` feeds BOTH the preview (`CompositionPlayer`) and the export (`Transformer`) — what you see is what you export |
| **GPU effects** | Per-clip ES3 shaders (`GlQuadProgram`): masks, chroma key, per-pixel grade suite, motion blur; timeline effect families blend on the GPU |
| **Speech to text** | Vosk on-device (English + Bangla, consented ~45 MB downloads) + optional Whisper-compatible cloud endpoint |
| **State** | Room + JSON snapshots, DataStore settings, durable undo journal, project time machine, export queue |
| **Languages** | English + Bengali UI (150/150 key parity), Bangla numerals for timecodes, bundled OFL Bangla fonts wired into all renderers |
| **APK** | ~88 MB debug build (includes Vosk native libs for 4 ABIs and the bundled fonts) |
| **Plan progress** | ~94% — see [`STATUS.md`](STATUS.md) |
| **Tests** | 1,000 JVM/Robolectric tests across 130 suites, 0 failures — instrumented/device run pending |
| **CI** | GitHub Actions: lint, unit tests and debug APK (`.github/workflows/ci.yml`) |

Legend for the tables below: **✅ working** · **🟡 partial** · **🔴 not built**

## Features

### Editing
| Feature | State | Notes |
|---|---|---|
| Projects: create, duplicate, rename, delete + undo, pin, search, sort | ✅ | Aspect presets 9:16, 16:9, 1:1, 4:5, 3:4, 21:9…; per-project backup/restore (SAF zip) |
| Project time machine | ✅ | Named + automatic snapshots (every 50 edits) in Room; restore from Settings; survives past the 200-step undo window |
| Media import + CameraX capture | ✅ | Photo picker and in-app camera (photo + video, front/back) land straight in a new project |
| Autosave | ✅ | Trailing 400 ms debounce per project — one JSON serialisation per gesture, not per slider tick |
| Undo / redo | ✅ | Gesture coalescing; persisted atomically so a crash keeps history |
| Multi-track timeline (video, overlay, audio, text, effects) | ✅ | Free pan, pinch zoom, vertical track scroll, one row per overlay + audio track, inline trim handles, split, ripple delete, multi-select group ops |
| Effects track with overlap | ✅ | Effects may share the same seconds (both render); overlapping chips pack into visible lanes automatically; drag to move, edge handles to trim (300 ms min), tap to edit |
| Snapping (playhead, clip edges, beats, markers) | ✅ | Priority order lives in the engine; beat ticks and marker pins are drawn on the ruler |
| Draggable playhead + free timeline pan | ✅ | Needle drags with edge auto-scroll; live scrub while playing (throttled ~4 Hz) |
| Keyframes (position, scale, rotation, opacity, volume) | ✅ | Sheet + interpolation + easing curves; timeline diamond markers |
| Speed: constant and bezier velocity curves | ✅ | Presets: montage, hero moment, bullet time, jump cut |
| Masks (rectangle, circle, split, mirror, filmstrip) | ✅ | GPU-burned via `ClipMaskEffect`; overlay clips keep transparency, main-track clips fill with the canvas background |
| Chroma key | ✅ | GPU-burned via `ClipChromaKeyEffect`; eyedropper picker; mirrors `ChromaKeyMath` exactly |
| Per-pixel grade suite | ✅ | Highlights / Shadows / Sharpen / Grain / Blur sliders + Retouch and Relight chips — one ES3 shader (`ClipGradeShaderEffect`) in preview and export |
| Matrix grade (exposure, temperature, tint, vibrance) + HSL | ✅ | Folded into the shared colour matrix (`ColorAdjustMath`); Before/After compare |
| Filters | ✅ | 112 CapCut-style filters + HSL panel; one file per filter, one grade definition for preview and export |
| Timeline effects | ✅ | 8 interactive GPU families (`EffectBlendShader`) + canvas-drawn families + 6 legacy presets; effects overlap legally |
| Transitions (17 kinds) | ✅ | Rendered through Media3 with real duration clamping |
| Text overlays + karaoke captions | ✅ | Word timings from both transcription engines; the spoken word is highlighted in the burned-in export AND the preview; caption style presets |
| Text styling | ✅ | Bold, italic, letter-spacing (em), line-height, Bangla font styles, NEON_GLOW shadow — preview and export share `TextFx` maths |
| Volume-style audio meter | ✅ | Real output peak meter (L/R) above the preview, never covering the picture |
| Live cover frame | ✅ | The gutter Cover card shows the real video frame / custom cover, not a demo scene |
| Editor shell + feature search | ✅ | AI UHD chip carries the user's export settings; timecode on the transport bar; Grid next to the ratio chip — all positions locked by request |

### Audio
| Feature | State | Notes |
|---|---|---|
| Multi-track music, volume, fades, waveform | ✅ | Waveform columns map onto the trimmed source window at any zoom (240-bucket pyramid) |
| Voice-over recording | ✅ | MediaRecorder based, level meter in the mixer |
| TTS voice-over with language choice | ✅ | Presets map to BCP-47 tags (bn-IN, en-US, en-GB); warns when no voice is installed instead of mispronouncing |
| Beat detection + auto-cut-to-beat | ✅ | Onset detector, beat-magnetic snapping, beat ticks on the ruler, manual beat pinning |
| Loudness normalisation | ✅ | "Boost loudness" now measures the sounding sources (duration-weighted RMS) and drives the mix to −14 dBFS with a peak cap — cuts included, both in-app and background exports |
| Offline DSP effects | ✅ | Echo, Schroeder reverb, robot ring-mod, noise gate, voice-enhance EQ, dynamics compressor — pure-Kotlin kernels (`AudioDsp`), tested, wired into preview and export |
| Parametric EQ, ducking | ✅ | Three-band EQ + auto-ducking on the audio mixer |
| Per-clip volume envelopes | 🟡 | `VOLUME` keyframe property exists; envelope authoring UI pending |

### Captions & speech
| Feature | State | Notes |
|---|---|---|
| Offline auto captions (Vosk) | ✅ | Consent-gated model downloads (English + Bangla) in Settings → Speech models; 16 kHz mono decode; word-timed cues; runs with no network |
| Cloud auto captions | ✅ | Optional OpenAI-compatible endpoint (Settings → AI) |
| Karaoke highlight | ✅ | The word being spoken is highlighted in the burned-in export and the preview; toggle in the text sheet |
| Caption styles | ✅ | Bold Box / Typewriter / Karaoke / Neon / Clean — one tap restyles every caption |
| SRT / VTT import & export | ✅ | Parse (BOM, VTT NOTE/STYLE), retime, validate, share |
| Jump to timecode | ✅ | Timecode chip on the transport bar opens the jump dialog with project markers |

### Export
| Feature | State | Notes |
|---|---|---|
| 480p–4K, 24–60 fps | ✅ | Capability ladder unit-tested (HEVC→H.264, size/fps downgrade) |
| Platform presets | ✅ | Recommended / TikTok-Reels / Shorts 60 / YouTube / Data saver chips set resolution+fps+bitrate together |
| H.264 + HEVC, AAC, bitrate selector | ✅ | Encoder rejection retries with the default encoder |
| Export queue + history | ✅ | Background exports persist as `export_jobs` rows; the foreground service drains them FIFO with progress notifications; Settings → Export history (retry/open/delete) |
| Gallery publish + share | ✅ | Finished files land in Movies/CatByte (Music/CatByte for audio); the done screen and notification open/share them |
| Audio-only (.m4a) + per-track export | ✅ | Mixer rows can export their own track |
| GIF export | ✅ | Two-stage: render the timeline, encode GIF89a, publish to Pictures/CatByte |
| HDR tone-map, 10-bit output | 🔴 | 10-bit sources detected by the proxy policy only |

### AI (offline-first, optional cloud)
| Feature | State | Notes |
|---|---|---|
| On-device speech recognition | ✅ | Vosk, consented downloads, Bangla + English |
| AI Lab | ✅ | Script writer, titles/hooks, hashtags/SEO, translator, image generator, Lanczos image upscaler, beat finder — via the configured endpoint or pure-Kotlin engines |
| AI remix / lyrics / translate / packaging | ✅ | Text-AI chips through the user's endpoint (`AiClient`) |
| Background music generator | ✅ | Offline, royalty-free loop generation |
| ML-Kit chips (remove BG, reframe, tracking), avatars, lip sync | 🔴 | Need on-device model packs; the chips say exactly that instead of faking results |

### Not built yet
- **Instrumented/device verification** — export E2E, golden-frame rendering, 60 fps
  scroll and soak tests still need a real device run.
- **ML-Kit model packs** — background removal, auto-reframe and object tracking
  need bundled models; the chips explain this honestly.
- **16 KB page-size hygiene** — Vosk 0.3.47 ships 4 KB-aligned `.so` files; needs
  a newer Vosk release or ABI verification before Play distribution.
- **Landscape / tablet layouts and onboarding tour** — portrait-first today.
- **Proxy generation** — `ProxyManager` is still a stub; the export pipeline
  could produce low-res proxies with WorkManager.
- **Cloud anything else** — no account, no cloud render, no marketplace, no
  collaboration. Editing works from local files only (the optional AI endpoint
  is the user's own).

---

## Build and run

Requirements: **JDK 17** and the **Android SDK (compileSdk 37)**. Node is only
needed for the desktop prototype.

```bash
# Debug APK -> app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:assembleDebug

# Install on a connected device/emulator
./gradlew :app:installDebug

# Unit tests (1,000 tests, incl. Robolectric Compose gestures)
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
ProjectCompositionFactory ──► ONE Media3 Composition
        │                       (clips, overlays, GPU effects, audio chain)
        ├──────────────► preview  (CompositionPlayer; ExoPlayer fallback)
        └──────────────► export   (Media3 Transformer + canvas overlay burn-in)
```

`ProjectCompositionFactory.plan()` consumes the same render graph for both
paths, so clip order, gaps, overlay starts, audio starts, GPU effects and
transition durations cannot differ between what you see and what you export.

```
app/src/main/java/com/example/
├── model/          VideoProject, MediaClip, AudioClip, TextOverlay (karaoke), keyframes
├── engine/         platform-free logic, unit tested on the JVM
│   ├── TextFx      shared text-animation maths (preview = export)
│   ├── AudioDsp    offline audio kernels (echo, reverb, robot, gate, EQ)
│   ├── LoudnessMath RMS normalisation + soft limiter maths
│   ├── MaskMath / ChromaKeyMath / BackgroundMath / AudioMath / TextFx …
├── data/           Room, JSON, import, export, player, cache, services,
│   │               captions (Vosk, PCM), fonts, AI client
│   └── export/     render pipeline, GPU effects, overlay burn-in
├── ui/             Compose screens, tool sheets, timeline, canvas, AI Lab
└── viewmodel/      EditorViewModel (editing commands, undo, export state)
```

### GPU effects (`data/export`, ES 3.0)

Per-clip effects extend Media3 `GlEffect`/`BaseGlShaderProgram` on a shared
`GlQuadProgram` quad renderer, run inside the one composition, and therefore
appear identically in the preview and the exported file:

| Class | Purpose |
|---|---|
| `ClipMaskEffect` | Mask shapes + feather + invert, mirrors `MaskMath.coverage` |
| `ClipChromaKeyEffect` | Cb/Cr chroma distance + spill suppression, mirrors `ChromaKeyMath` |
| `ClipGradeShaderEffect` | Highlights/shadows/sharpen/grain/blur/relight per pixel |
| `MotionBlurEffect` | Directional motion blur |
| `EffectBlendShader` | Interactive timeline effect families blend into the video pixels |

The former custom GL compositor (`engine/gl/`, `GLPreviewSurface`) was dead
code with a latent NPE and has been deleted; `EffectShaders` (35 GLSL sources)
remains as a validated library.

### Engine modules (`com.example.engine`) — selected

| Module | Responsibility |
|---|---|
| `Timebase` | Rational fps (30000/1001), `TimeUs`, frame snapping, timecode |
| `SnapEngine` | Magnet collection/priority (clip edges, playhead, markers, beats) |
| `TimelineEdits` | Magnetic placement, ripple vs lift delete, reorder-close-gaps, trim bounds |
| `UndoJournal` | Atomic, bounded, gesture-coalescing undo/redo persistence |
| `TimelineGeometry` / `CenterPlayhead` | dp/ms maths, fixed-playhead scroll, zoom anchoring |
| `TextFx` | Shared text-animation curves (WYSIWYG parity between renderers) |
| `LoudnessMath` | RMS measurement, duration-weighted combine, normalisation gain, soft limiter |
| `AudioDsp` | Offline audio kernels: echo, reverb, robot, gate, EQ |
| `MaskMath` / `ChromaKeyMath` / `BackgroundMath` | Shader-mirrored coverage/key/background geometry |
| `BeatGrid` | Onset peaks, tempo, beat lattice |
| `ClipSplit` / `ClipTrimHandle` | Frame-accurate split and trim maths |
| `CaptionTimeline` / `CaptionBridge` / `SubtitleIO` | Word→cue packing, karaoke frames, SRT/VTT |
| `SpeechTranscriber` / `Resampler` | The on-device recognition contract + 16 kHz mono pipeline |
| `AudioDsp` / `AudioMath` | Offline kernels and mixer maths |
| `ExportPresets` | Platform presets, bitrate ladder, size estimate |
| `Shortcuts` | Keyboard shortcuts (space, S, M, Del, Ctrl+Z/Y, arrows) |
| `EffectOps` / `engine/effects/` | One file per canvas-drawn effect family |

---

## Testing

```bash
./gradlew :app:testDebugUnitTest --rerun
```

- **1,000 JVM/Robolectric tests across 130 suites, 0 failures**: timing,
  snapping, timeline geometry, undo journal, render graph, transitions, LUTs,
  captions (SRT, karaoke, offline pipeline), audio DSP, loudness maths, Bangla
  text and numerals, effect lanes, GPU-effect routing, export math and JSON
  round-trip, editor workflows, localization parity (150/150 keys), inline
  trim maths, track mute/lock/solo persistence, detach audio, effect move/
  resize, Compose UI tests (timeline gestures, transport, meter).
- **CI:** `.github/workflows/ci.yml` runs lint, unit tests and `assembleDebug`
  on pushes and pull requests to `main`/`develop`.
- **Not covered yet:** instrumented codec/export runs, golden-frame rendering,
  60 fps scroll, memory and soak tests. See [`TEST_REPORT.md`](TEST_REPORT.md).

## Roadmap

1. **Device verification** — `connectedDebugAndroidTest`, golden-frame rendering, TEST_REPORT refresh
2. **16 KB page-size / ABI hygiene** — verify or bump the Vosk `.so`, AAB splits, size budget
3. **ML-Kit chips** — background removal, auto-reframe, object tracking with bundled models
4. **Proxy pipeline** — low-res proxies via the existing export machinery + WorkManager
5. **Volume envelope authoring** — VOLUME keyframe handles on audio rows
6. **Landscape / tablet layouts** — window size classes, two-pane editor
7. **Onboarding tour** — interactive first-run walkthrough
8. **Caption cue editor** — per-cue text/timing editing with overlap auto-fix

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
| [`THIRD_PARTY_LICENSES.md`](THIRD_PARTY_LICENSES.md) | Dependency licences (incl. Vosk, Apache 2.0) |

## Privacy

Editing, preview and export run entirely on the device. There is no account
system, no cloud render and no telemetry; project files and media stay in app
storage and the system gallery. The optional AI endpoint and the speech-model
download are user-configured and user-consented; the Vosk models run offline
after download.

## License

MIT — see [`LICENSE`](LICENSE).

## Credits

Media3, Jetpack Compose, Room, Kotlin coroutines, Coil, Robolectric, JUnit,
Vosk (Apache 2.0), Noto Sans Bengali / Hind Siliguri / Tiro Bangla / Anek
Bangla (OFL). CapCut for the UX patterns this project is measured against.
