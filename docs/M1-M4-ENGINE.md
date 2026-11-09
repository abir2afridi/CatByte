# M1–M4 Engine Implementation

**Date:** 2026-11-04
**Scope:** CapCut-level editor foundation, cloud features excluded.
**Package:** `com.example.engine` (pure Kotlin, no Android imports, JVM-testable)

This document records what the M1–M4 milestones actually delivered, what is
wired into the app, and what still needs a device (GPU, codecs, models, fonts).

---

## Milestone status

| Milestone | Engine logic | Unit tests | Wired to UI/app | Device work left |
|---|---|---|---|---|
| **M1** Timebase, snapping, persistent undo, center playhead | ✅ | ✅ | Timeline snap + center playhead, undo journal | Magnetic drag-insertion (needs timeline DnD) |
| **M2** Single render graph, transitions, shaders, LUT, decoder budget | ✅ | ✅ | `ProjectCompositionFactory` renders from the graph | EGL/GLSL compositor execution |
| **M3** Captions, Bangla text, audio mix maths | ✅ | ✅ | Audio fades + export size estimate | Bangla font binaries + golden-image shaping tests |
| **M4** Device tiers, auto-reframe, silence detect, export presets | ✅ | ✅ | ❌ | On-device detectors (ML Kit/MediaPipe), TFLite models |

"Engine logic" means the decision-making/maths layer: real implementations, not
stubs, with unit tests that pin the behaviour. Device-only work is listed
explicitly so nothing is claimed as finished that was never executed.

---

## M1 — Foundation

### `engine/Timebase.kt`
- `Rational` fps (`30000/1001` supported) with gcd normalisation.
- `TimeUs` value class, integer-only conversions, no float drift.
- `toFrame` = nearest frame, `toFrameFloor` = sampling frame. **Fix:** the first
  implementation truncated twice, so frame 1 at 29.97 fps reported as frame 0.
  Caught by `TimebaseTest`, now regression-tested.
- `snapToFrame` with DOWN / UP / NEAREST; `Timecode` format/parse/short;
  `FrameRateLadder` (never upscales above the project rate).

### `engine/SnapEngine.kt`
- Frame-based thresholds converted from pixels once per zoom level.
- Candidate priority: playhead → marker → beat → clip edge; equal distances
  resolve by kind so the result is deterministic.
- `collect()` de-duplicates magnets at the same frame.
- `magneticStart()` pushes a clip past occupied ranges (no gaps, no overlaps).

### `engine/UndoJournal.kt`
- Snapshot undo/redo with gesture coalescing (`key` + time window).
- Bounded (`maxEntries`), versioned JSON, corrupt journal ignored instead of
  crashing.
- `FileJournalStorage`: temp file → rename, so a crash mid-write leaves the
  previous journal intact.
- **Wired:** `EditorViewModel.pushHistory` records to the journal; `undo()` /
  `redo()` fall back to the journal when the in-memory stacks are empty (process
  death) and only apply states belonging to the same project id. Divergent
  bookkeeping between the two stacks is avoided by advancing the journal on
  every in-memory undo/redo too.

### `engine/CenterPlayhead.kt`
- Offset ⇄ frame conversion for a fixed center playhead, clamped at the ends,
  pinch-zoom offset preservation, and `pinnedAtEdges()` for end padding.

---

## M2 — Render path

### `engine/RenderGraph.kt`
- One immutable `RenderGraph` consumed by preview and export: layers with
  z-order, source/timeline in-out, speed, volume, opacity, blend mode,
  transform, mask, effects, transition, plus audio, text, global effects and
  background.
- `RenderGraphBuilder` mirrors the editor's layout rules (main track sequential,
  overlays absolute), derives canvas size from resolution + aspect ratio, and
  clamps transitions to half of the shorter neighbour.
- `layersAt`, `textsAt`, `frameAt`, `totalFrames` sampling helpers.

### `engine/TransitionEngine.kt`
- 17 renderer transition kinds, including genuine two-frame ones (dissolve,
  wipe linear/radial, organic, ink, push, zoom, glitch, flip).
- `frame(kind, progress)` returns alphas/offsets/scales/fill; dip-to-black and
  flash-white expose the visible gap instead of faking an opacity fade.
- `shaderFor(kind)` returns a two-texture fragment shader for every kind.

### `engine/EffectShaders.kt`
- 35 GLSL ES 3.0 effects across glitch, retro, glow, detail, stylize, motion,
  colour. One fragment pass each, wrapped by `ShaderTemplate`.
- `forLegacyEffect()` maps the existing `EffectType` enum so old projects resolve.

### `engine/LutCube.kt`
- `.cube` parser (3D and 1D, expanded to 3D), trilinear sampling, intensity
  blend, chain baking into one table, texture budget guard (2..64).

### `engine/DecoderBudget.kt`
- Priority plan (preview → export → proxy → thumbnail) with instance and memory
  ceilings; per-tier presets.
- `ProxyPolicy`: 4K/10-bit/VFR/long-GOP/heavy-codec detection, target height,
  thumbnail stride; export always reads originals.

---

## M3 — Text and audio

### `engine/SubtitleIO.kt`
- SRT and VTT parse/write, format auto-detect, retiming, validation (overlaps,
  zero-length cues), line wrapping, `cuesFromWords` (Whisper-style) and
  `karaokeSplit` for word-by-word captions.

### `engine/BanglaText.kt`
- Native digit conversion, Bangla timecode labels.
- Mark-safe line breaking: never splits a consonant from its vowel sign, hasant
  or ZWJ/ZWNJ, and `splitLongWord` falls back to mark-safe character chunks.
- `isShapingSafe()` guard, `CaptionLayout.fit()` for the caption safe area.

### `engine/AudioMath.kt`
- Fade-in/out gain, voice-over ducking (instant + attack/release envelope),
  block limiter, multi-track mix under a ceiling, RMS dBFS / approximate LUFS,
  normalisation and peak normalisation, 16-bit PCM conversion, one-pole low-pass,
  linear resample.

---

## M4 — Capability and automation

### `engine/DeviceProfiles.kt`
- LOW/MID/HIGH tier from RAM, cores, SDK, low-RAM flag.
- Preview budget degrades on thermal state and low battery (not when charging).
- Export budget respects the decoder ceiling; live overlay limit per tier.

### `engine/AutoReframe.kt`
- Aspect-correct crop window around detected subjects, clamped to frame bounds,
  exponential smoothing, Ken Burns interpolation, subject union with padding,
  recrop detection, pixel crop with even dimensions.

### `engine/SilenceDetector.kt`
- Silence ranges from the waveform pyramid, minimum-length filter, keep ranges,
  removed duration, "mostly silent" verdict.

### `engine/ExportPresets.kt`
- Platform presets (portrait/landscape/audio-only/GIF), bits-per-pixel bitrate
  ladder, exact size estimate, device clamping (even dimensions, fps cap).

---

## Verification

```bash
./gradlew :app:testDebugUnitTest --console=plain
```

Unit tests added: `TimebaseTest`, `SnapEngineTest`, `UndoJournalTest`,
`RenderGraphTest`, `TransitionMathTest`, `LutCubeTest`, `SubtitleIOTest`,
`AudioMathTest`, `BanglaTextTest`, `EnginePolicyTest`.

---

## Remaining device-only work (not claimed as done)

1. **GL compositor execution:** shaders exist as sources and the transition/
   effect decisions are tested, but no EGL surface runs them yet. Needs an
   instrumented `GLSurfaceView`/`SurfaceTexture` harness and golden-frame
   comparison against the export path.
2. **Media3 integration:** done for layout — `ProjectCompositionFactory.plan()`
   now takes clip order, gaps, overlay starts, audio starts and clamped
   transition durations from `RenderGraphBuilder`. Per-pixel effects still go
   through the existing Media3 effect classes; moving them onto `EffectSpec`
   shaders needs the GL compositor.
3. **Bangla fonts and shaping:** font binaries are not bundled and no
   golden-image test exists. `BanglaLineBreaker` prevents mark-splitting, but
   HarfBuzz-level shaping still has to be verified on device.
4. **On-device detectors/models:** auto-reframe, silence cut and cutout consume
   data structures only; the ML Kit/MediaPipe/TFLite producers are not wired.
5. **UI wiring:** center playhead and tap-snapping are wired into
   `MultiTrackTimeline`. Multi-select and magnetic drag-insertion are still
   missing because the timeline has no drag-and-drop gesture yet.
