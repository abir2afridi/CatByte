# Build a CapCut-Class Android Video Editor (Original Implementation)

## 0. HOW YOU MUST WORK (READ FIRST)

You are an autonomous senior Android engineering team operating inside a real repository with a shell, Gradle, and Android SDK. You do NOT answer with plans or sample snippets. You write code, build, run tests, read the real errors, fix them, and repeat.

**Working loop (never break it):**
1. Read `STATUS.md` (create it if missing). It is your memory across sessions.
2. Pick the next unfinished task in the current phase.
3. Implement → `./gradlew assembleDebug` → run tests → fix → repeat until green.
4. Commit locally with a focused message (never push).
5. Update `STATUS.md` (done / in progress / blocked / next 5 tasks / known bugs / decisions).
6. Continue to the next task. Stop only at a phase gate or a genuine blocker.

**Context discipline:** The codebase will be large. Keep files small and single-purpose, keep module boundaries strict, and record every architectural decision in `docs/DECISIONS.md` (one short entry each: context, decision, alternatives rejected, consequences) so later sessions do not re-derive them.

**Honesty rules:** No fake buttons, fake progress, fake AI, fake benchmarks, fake test results. A feature is VERIFIED only after its acceptance test actually ran and passed. If something is impossible in this environment, say exactly why, then continue with everything else.

**Legal/identity rule:** "CapCut-class" means comparable capability and workflow quality, not copying. Use an original name (default: **ClipForge**), original icons, original fonts/stickers/LUTs/music (only CC0 or properly licensed), original UI code. Do not decompile, reference, or reuse any proprietary app's code or assets.

---

## 1. MISSION

Ship a real, offline-first Android video editor where a user can:

**Import → create project → multi-track edit → real-time preview → autosave → reopen → export validated MP4 → play/share.**

Priority order when trade-offs appear: (1) export correctness, (2) preview/export parity, (3) timeline responsiveness, (4) UI polish, (5) breadth of effects, (6) AI features.

The vertical slice (one video → trim → text → export MP4 that plays) MUST work before any breadth work begins.

---

## 2. LOCKED ARCHITECTURE (do not re-litigate; deviate only with a DECISIONS.md entry and evidence)

**Language/UI:** Kotlin, Jetpack Compose (Material3 as base, heavily custom theme), single-Activity, Navigation Compose.
**DI:** Hilt (or Koin if Hilt+KSP causes version conflict; record decision).
**Async:** Coroutines + Flow. No blocking on main thread.
**Persistence:** Room (project index, media library, caches) + `kotlinx.serialization` JSON for the versioned project document. DataStore for settings.
**Min SDK 26, target/compile = current stable.** Verify AGP/Kotlin/Compose/Media3 compatibility from official release notes before pinning versions (use a version catalog `libs.versions.toml`).

**Modules (Gradle):**
- `:app` — navigation, DI wiring, theme
- `:core:model` — pure Kotlin project/timeline model, math, commands (no Android deps → fast JVM unit tests)
- `:core:render` — RenderGraph builder (shared by preview and export)
- `:engine:gl` — EGL/GLSL compositor, shader library, texture pool
- `:engine:playback` — preview player, decoder pool, audio clock
- `:engine:export` — export pipeline, muxer, validation
- `:engine:audio` — mixer, waveform extraction, effects
- `:feature:home`, `:feature:picker`, `:feature:editor`, `:feature:text`, `:feature:audio`, `:feature:export`, `:feature:settings`
- `:data` — Room, repositories, media import, thumbnail/waveform caches
- `:testing` — fixtures, golden-frame utilities

**Single source of truth:** One `Project` document → `RenderGraphBuilder` → `RenderGraph` (immutable description of what every frame and audio sample must contain at time T). BOTH preview and export consume the same RenderGraph. No separate "export logic".

**Rendering strategy (decide by spike in Phase 0, document results):**
- Spike A: Media3 `Transformer` + `Composition` + custom `GlEffect`/`GlShaderProgram` for export; Media3 `CompositionPlayer` for preview.
- Spike B: Custom pipeline — `MediaExtractor` + `MediaCodec` decoders → `SurfaceTexture`/external OES textures → own GLSL compositor (multi-layer, blend, masks, LUT, transitions) → `MediaCodec` encoder via input surface (+ own audio mixer → AAC encoder → `MediaMuxer`).
- Evaluate each on: multi-layer PiP, per-layer transforms/opacity, transitions (needs two simultaneous decoded frames), keyframes, speed ramps, reverse, text/sticker overlays, frame-accurate seek, A/V sync, 60fps, HDR/10-bit input handling, memory, effort.
- Expected outcome: Media3 for decode/encode plumbing where it fits, custom GL compositor for everything Media3 cannot express. Whatever you pick, preview and export must share the same GLSL shaders and the same RenderGraph.

**Decoder pool:** Hardware decoders are limited (often 4–8 concurrent instances; fewer for 4K). Implement a bounded `DecoderPool` with priority/eviction, pre-roll of the next clip, and graceful fallback (software decoder / proxy) when allocation fails.

**Proxy/preview media:** Auto-generate low-res, short-GOP (all-intra or 1-second GOP) proxies in the background for clips that are 4K, HEVC/10-bit, VFR, or have long GOPs, to give smooth scrubbing. Export always uses originals.

---

## 3. CORE DOMAIN MODEL (implement exactly this shape, extend carefully)

All times are `Long` **microseconds**. Provide a `TimeUs` value class and frame-snapping helpers `snapToFrame(t, fps)`.

```kotlin
@Serializable data class Project(
  val id: String, val formatVersion: Int, val name: String,
  val createdAt: Long, val modifiedAt: Long,
  val canvas: Canvas, val fps: Rational, val background: Background,
  val tracks: List<Track>, val markers: List<Marker>, val assets: Map<String, MediaAsset>
)
@Serializable data class Track(val id: String, val type: TrackType, val clips: List<Clip>,
  val muted: Boolean, val hidden: Boolean, val locked: Boolean, val zOrder: Int)
sealed interface Clip { val id: String; val startUs: Long; val durationUs: Long /* on timeline */ }
// VideoClip, ImageClip, AudioClip, TextClip, StickerClip, EffectClip(adjustment layer), CaptionClip
data class MediaRef(val assetId: String, val srcInUs: Long, val srcOutUs: Long)
data class Transform(val pos: Vec2, val scale: Vec2, val rotationDeg: Float, val anchor: Vec2,
  val flipH: Boolean, val flipV: Boolean, val crop: Rect, val fit: FitMode)
data class Speed(val constant: Double?, val curve: List<SpeedPoint>?, val preservePitch: Boolean, val reverse: Boolean)
data class Keyframe<T>(val timeUs: Long /* clip-local */, val value: T, val easing: Easing)
data class Transition(val type: TransitionType, val durationUs: Long, val params: Map<String, Float>)
```

**Invariants (enforce in code + property-based tests):**
- `srcOut > srcIn`, `duration > 0`, clips on one track never overlap (except designated overlay tracks).
- Timeline duration of a constant-speed clip = `(srcOut − srcIn) / speed`. Speed curves integrate piecewise.
- Keyframe times are clip-local, so moving a clip never breaks them; trimming/splitting re-bases or splits keyframe curves correctly (split must create an interpolated boundary keyframe).
- Transition duration ≤ min(adjacent clip durations) / 2 unless overlapping tracks define otherwise; clamp and document.
- All edits are `Command`s (`apply`/`invert`) or immutable snapshots with structural sharing; undo/redo bounded (e.g. 200), coalescing for drags/sliders.
- Project JSON has `formatVersion` and a tested migration chain.

---

## 4. FEATURE TIERS (CapCut-class scope)

### Tier 1 — MUST (release blocker)
- Media import: Photo Picker + SAF, multi-select, persistable URIs, metadata (dims, rotation, fps, duration, codec, audio presence), thumbnails, missing-media relink flow.
- Projects: create (aspect presets 9:16, 16:9, 1:1, 4:5, 3:4, 21:9, custom), rename, duplicate, delete, autosave, crash recovery, drafts.
- Timeline: ruler, centered/draggable playhead, pinch zoom, frame-accurate seek, magnetic main track, free overlay tracks, snapping (playhead, clip edges, markers, beats), trim handles, split, delete, duplicate, reorder, ripple delete, multi-select, track mute/hide/lock, thumbnails strip, waveform.
- Preview: play/pause/seek/scrub, loop, fullscreen, A/V sync, canvas-correct rendering, safe-area guides.
- Clip tools: trim, split, speed (0.1×–100×, presets + custom), volume, fade in/out, rotate, flip, crop (free + ratios), scale/position/rotation via on-canvas gestures, fit/fill/blur-fill background, mirror, replace, freeze frame, reverse, extract audio, detach audio.
- Text: multi-line, Unicode incl. **Bangla shaping (verify with HarfBuzz via Android text stack/Paint — test with real Bangla strings)**, fonts (bundled OFL fonts + system), size, color, gradient fill, alignment, letter/line spacing, bold/italic, stroke, shadow, glow, background box, in/out/loop animations, keyframes.
- Audio: music import, multiple tracks, trim/split, volume, fades, voiceover recording (mic permission, headphone-aware), waveform, mixer with limiter to prevent clipping.
- Adjust: brightness, contrast, saturation, exposure, highlights, shadows, temperature, tint, vibrance, sharpen, vignette, grain, blur — all as GLSL, keyframable.
- Filters: ≥ 20 original LUT-based presets (.cube/PNG LUT, 33³ or 64²), intensity slider.
- Transitions: cut, fade, dissolve, dip-to-color, slide (4 dirs), push, wipe (linear/radial), zoom, spin, blur, glitch, with real duration and easing — rendered via two-frame compositing in the shader.
- Overlays: PiP video, images, stickers (PNG/WebP/Lottie where licensed), opacity, blend modes (normal, multiply, screen, overlay, add).
- Export: 480p–4K (device-capability checked), 24/25/30/50/60 fps, H.264 (baseline) + HEVC (if supported), bitrate/quality presets, AAC audio, real progress, cancel, temp file → validate → publish to MediaStore (`Movies/ClipForge`), share/open actions, storage precheck.

### Tier 2 — SHOULD (after Tier 1 is VERIFIED)
- Keyframe editor UI (diamond markers, easing: linear, ease-in/out, cubic-bezier, hold) for position/scale/rotation/opacity/adjustments/mask.
- Masks: linear, mirror, circle, rectangle, heart/star, custom path; feather, invert, keyframable.
- Chroma key (YUV/HSV based, spill suppression, softness).
- Speed curves (bezier ramp editor) with optical-flow-free frame blending as the baseline.
- Adjustment layers and effect clips spanning multiple tracks.
- Effects library (≥ 30 original GLSL effects: glitch, RGB split, VHS, shake, zoom pulse, film burn overlays, light leaks, blur variants, mosaic, mirror, kaleidoscope, etc.).
- Text templates, animated captions styles (word-by-word highlight, karaoke), SRT/VTT import & export, burned-in captions.
- Audio: noise reduction (RNNoise or similar — check license), voice effects (pitch/formant/robot/echo/reverb), beat detection + beat markers, auto-ducking, equalizer, loudness normalization (LUFS target).
- Templates: save/load project as reusable template with replaceable slots.
- Multi-project asset library, project backup/restore (zip with media optional).

### Tier 3 — ADVANCED (each is a separate verified deliverable)
- On-device auto captions: Whisper-family model via whisper.cpp/ONNX/TFLite (user-consented download, show size, language list, Bangla + English tested; report real WER-ish sanity checks on fixtures).
- Background removal / portrait matting: MediaPipe / ONNX segmentation (real mask video, temporal smoothing, edge refinement).
- Object tracking (mask/sticker follows subject), video stabilization (gyro-free feature-tracking or Media3/OpenCV-class approach — verify license), auto-reframe (subject-aware crop), smart cutout, silence removal, scene detection, AI-style auto-edit to beats.
- Camera capture module (CameraX), screen-record import.
- Text-to-speech voiceover (Android TTS; offline engines if available).
Never present a Tier 3 feature in the main UI unless it genuinely works; otherwise put it behind a labeled "Experimental" screen.

---

## 5. UI/UX SPECIFICATION (CapCut-class workflow, original design)

**Design language:** Near-black (#0B0B0E) base, layered charcoal surfaces, one accent (default electric cyan or mint; configurable), 1px hairline separators, 8dp grid, consistent 12dp radius, 48dp minimum touch targets, haptics on snap/trim/split.

**Editor layout (portrait):**
- Top bar: back/close, project name, undo, redo, resolution chip, **Export** button.
- Preview: maximum possible height, canvas-accurate, on-canvas handles (move/scale/rotate/crop corners), pinch/rotate gestures, alignment guides with snap haptics.
- Transport row: timecode (current / total), play/pause, fullscreen, "fit timeline".
- Timeline: **fixed center playhead, content scrolls under it** (CapCut-style) with an optional free-playhead mode; ruler with adaptive tick density; main track with thumbnails; overlay/text/audio tracks collapsed by default and expandable; "+" at end of main track; long-press to drag-reorder; edge drag to trim with live preview frame; two-finger pinch zoom anchored at playhead.
- Bottom toolbar: horizontally scrolling, **context-sensitive**. With nothing selected: Edit, Audio, Text, Stickers, Overlay, Effects, Captions, Filters, Adjust, Canvas, Templates. With a clip selected: Split, Speed, Volume, Animation, Crop, Transform, Mask, Filters, Adjust, Opacity, Blend, Fade, Reverse, Freeze, Replace, Duplicate, Delete.
- Property panels slide up as bottom sheets that never cover the preview entirely; sliders show numeric value and support tap-to-type and reset.

**Home:** greeting-free, project grid with thumbnails, duration, date; New Project card; search; multi-select delete; storage usage; settings.
**Picker:** tabs for Video / Photo / All / Audio, albums, multi-select with numbered badges, preview on long-press, duration badges.

**Accessibility/i18n:** all strings in resources; content descriptions; TalkBack-usable timeline actions (via custom actions); contrast ≥ 4.5:1; RTL-safe layouts; Bangla + English locale files.

---

## 6. PERFORMANCE BUDGETS (measure on a real/emulated device and record it; do not invent numbers)

- Cold start to home: target < 1.5 s on mid-range device.
- Timeline scroll/zoom: 60 fps (no per-frame allocations in `onDraw`/`DrawScope`; cache thumbnails in an LRU bitmap pool; waveform stored as multi-resolution min/max peak pyramids on disk).
- Scrub latency (proxy): target < 100 ms to first rendered frame.
- Play start: target < 300 ms.
- Memory: never decode whole videos into RAM; bounded texture/bitmap pools; track via `Debug.MemoryInfo` + LeakCanary (debug).
- Export: report real realtime-multiple for a defined fixture project on the test device; handle thermal throttling and low storage; foreground service + notification so export survives backgrounding.
- Use Baseline Profiles + R8; add Macrobenchmark for startup and timeline jank when possible.

---

## 7. EXPORT PIPELINE (RELEASE BLOCKER)

1. Validate project, assets, settings; check storage (estimate = bitrate × duration × 1.1).
2. Build RenderGraph; choose codec by `MediaCodecList` capability query (size, fps, profile/level); fall back down a documented ladder (e.g. HEVC→H.264, 4K→1440p→1080p, 60→30 fps) and tell the user.
3. Render frame-by-frame at the project frame-rate timebase (frame N at `N × 1e6 / fps` µs, rational math, no float drift). Encoder input surface, presentation timestamps from the graph, not wall clock.
4. Audio: render mixed PCM from the same graph (resample to 48 kHz stereo, apply speed/pitch, fades, volume, limiter), encode AAC-LC, mux in A/V sync.
5. Progress = frames_rendered / total_frames (real). Cancellation via coroutine cancel → release codecs/EGL/muxer → delete temp file.
6. Write to app-private temp → **validate** (`MediaExtractor`/`MediaMetadataRetriever`: tracks, dims, duration ±1 frame ×N tolerance, audio track present if expected, decode first/middle/last frame, compare checksum/SSIM vs golden) → publish via MediaStore → success UI.
7. Run as a foreground service; survive process death where possible (resume = restart safely, never leave partial files visible).

**Parity test:** For each effect/transition/transform, render a fixture frame through the preview path and the export path with the same RenderGraph and assert SSIM ≥ 0.98 (or exact match for deterministic shaders).

---

## 8. TESTING & VERIFICATION PROTOCOL

**Fixtures:** generate synthetic media (do not commit large binaries): use `ffmpeg -f lavfi` (testsrc2, sine, color bars with burned-in frame counters and timecode, VFR, rotated, 10-bit HEVC, 4K, portrait, audio-only, video-only) in a `scripts/make_fixtures.sh|ps1` script, or generate in-app with `MediaCodec` in instrumented tests.

**Unit (JVM, fast):** timeline math, split/trim/ripple, speed integration, keyframe interpolation/splitting, transition clamping, command undo/redo (property-based fuzz: random edit sequences, undo-all must equal original), serialization round-trip + migrations, snapping, canvas math, export-setting validation.

**Instrumented (emulator/device):** import + metadata, thumbnail generation, decoder pool stress, playback sync (audio clock vs video PTS drift < 1 frame over 60 s), project save/reopen, full export of fixtures, cancel mid-export, low-storage simulation, process-death recovery, Bangla text render golden image.

**Golden frames:** render known frames (text, transition mid-point, blend modes, LUT) and compare with stored goldens.

**Compose UI tests:** home flow, picker multi-select, editor tool panels, export dialog states.

**Reporting:** `TEST_REPORT.md` lists each test as PASS / FAIL / SKIPPED / NOT RUN with the command, date, and device/emulator model. Never mark PASS without running.

---

## 9. PHASED EXECUTION PLAN WITH GATES

Complete each phase fully (build green, tests green, STATUS.md updated) before starting the next.

**P0 — Foundation & spikes:** inspect repo, verify toolchain, version catalog, module skeleton, CI-style `./gradlew` check script, **render-engine spike A vs B with measured results**, DECISIONS.md. *Gate: debug APK builds and launches; spike decision recorded.*

**P1 — Vertical slice:** import one video → decode → GL compositor draws it on a canvas → add one text overlay → export MP4 via the chosen pipeline → validated playable output. *Gate: real MP4 with correct video + text + audio, validated by automated test.*

**P2 — Model, commands, persistence:** full domain model, command/undo system, Room + JSON, autosave, recovery, migrations, 90%+ coverage on core math. *Gate: fuzz tests pass; save/reopen restores exact state.*

**P3 — Shell & import:** theme, navigation, home, picker, media metadata, thumbnails, proxies, missing-media flow. *Gate: Test A passes.*

**P4 — Timeline & preview:** custom-drawn timeline, playhead, zoom, selection, trim/split/move/ripple/snap, preview sync, decoder pool. *Gate: Test B passes; 60 fps scroll on test device.*

**P5 — Clip & canvas tools:** transforms with on-canvas gestures, crop, speed, reverse, freeze, fades, volume, canvas ratios, blur-fill. *Gate: preview/export parity tests pass.*

**P6 — Text, audio, adjust, filters:** full text engine (Bangla verified), audio tracks + voiceover + waveform + mixer, adjustments, LUT filters. *Gate: Test C passes.*

**P7 — Transitions & multi-track compositing:** two-frame transitions, overlay tracks, PiP, blend modes, opacity, stickers. *Gate: transition mid-frame goldens match in preview and export.*

**P8 — Export hardening:** presets, capability ladder, foreground service, cancel, failure recovery, validation suite. *Gate: Test D and Test E pass.*

**P9 — Tier 2 features** in this order: keyframe UI → masks → chroma key → effects library → captions/SRT → audio FX/beat detection → speed curves → templates. Each feature ships with UI, shader/engine code, persistence, preview, export, and tests.

**P10 — Polish:** accessibility, haptics, empty/error states, onboarding, localization (EN + BN), Baseline Profile, R8, leak/jank fixes, crash-free soak test (30-minute editing script + 3 long exports).

**P11 — Tier 3 features** one at a time with consent flows, model licenses, size disclosure, real measured quality.

**P12 — Release verification:** lint, unit + instrumented suites, debug APK, signed release via local keystore *provided by the user through environment variables or `local.properties`* (never invent or commit keys), APK analyzer check, manifest/permissions audit (only: READ_MEDIA_* via picker where possible, RECORD_AUDIO, POST_NOTIFICATIONS, FOREGROUND_SERVICE_*), third-party license file, final report.

---

## 10. DOCUMENTS TO MAINTAIN (short, accurate, always in sync)

`STATUS.md` (live), `PLAN.md`, `FEATURE_MATRIX.md` (NOT STARTED / IN PROGRESS / IMPLEMENTED-NOT VERIFIED / VERIFIED / BLOCKED / DEFERRED), `docs/ARCHITECTURE.md` (with RenderGraph diagram + timing model), `docs/DECISIONS.md`, `docs/SPIKE_RENDER_ENGINE.md`, `TEST_REPORT.md`, `README.md`, `THIRD_PARTY_LICENSES.md`.

---

## 11. ENGINEERING RULES

- Inspect before editing; preserve existing working code; never destructive git commands; never push; no secrets in the repo.
- Every dependency: justify, check license (avoid GPL/AGPL in the shipped APK unless explicitly accepted), maintenance, size, ABI support (arm64-v8a, armeabi-v7a if needed, x86_64 for emulator).
- No `catch (e: Exception) {}`; typed errors with user-actionable messages; structured logging with no personal paths.
- All GL/codec/audio resources have explicit lifecycles and are released in `finally`/`use`.
- Compose performance: stable/immutable state, `derivedStateOf`, lambda deferral for scroll state, no heavy work in composition.
- When a build breaks, read the actual error, fix the root cause, and never disable checks, delete tests, or stub out features to get green.
- If stuck on one task for more than ~3 attempts, write down what you tried in STATUS.md, choose the next-best approach, or move to an independent task — do not stop the whole project.

---

## 12. V3 ADDITIONS (these extend and override earlier sections where they conflict)

### 12.1 Extra features (assign to tiers, track each in FEATURE_MATRIX.md)

**Tier 1 additions**
- Copy / cut / paste / paste-attributes for clips; multi-select with group move; clip grouping; "select all to right".
- Markers with labels/colors; jump-to-marker; marker-based snapping.
- Ken Burns auto pan/zoom for photos (editable start/end rects), default photo duration setting.
- Background options: color, gradient, blurred-copy of clip, image.
- Split-screen / collage layouts (2, 3, 4-up, grid presets) built on the same overlay compositor.
- Cover/thumbnail frame picker for the exported video and project card.
- Export presets: TikTok/Reels/Shorts (9:16 1080p30), YouTube (16:9 1080p/4K), WhatsApp Status (small bitrate), Custom; audio-only export (AAC .m4a); optional GIF / animated WebP export (short clips, size warning).
- Optional original watermark/outro toggle (off by default, user-controlled).
- Landscape editor layout, tablet and foldable layouts (window size classes), external keyboard shortcuts (space=play, S=split, Ctrl+Z/Y, Del).
- Onboarding: short interactive first-run tour; per-tool tooltips; sample project built from bundled CC0 media.

**Tier 2 additions**
- Compound clips (nested sequences) with expand/collapse; reusable "presets" (saved effect/animation stacks).
- Motion blur on moving layers (sub-frame accumulation, quality-selectable), speed-ramp motion blur.
- Text-in-video masks (video visible through text), curved/path text, text-follow-path, emoji (color font support), word-level animation presets.
- Sound-effects library (original/CC0), audio ducking, voice changer, audio markers, auto-sync cuts to beat grid, stereo pan, loudness meter.
- Video-in-video freeze + "echo/trail" effects, glitch packs, 3D-zoom/parallax from still photos (depth estimation = Tier 3).
- LUT import (.cube) by user; effect/filter favorites; recently used.
- Project package export/import (`.cfproj` zip: JSON + optional media + thumbnails) with integrity check.
- Local diagnostics export (user-initiated log bundle, no network, no personal paths).

**Tier 3 additions**
- Depth-based parallax photos, sky replacement, AI-based denoise/upscale (only with measured real results and consent flow), auto-highlight detection, face-aware crop, lip-sync-safe speed.

### 12.2 Bangla-first quality (differentiator)
- Bundle OFL-licensed Bangla fonts (e.g., Noto Sans Bengali, Hind Siliguri, Tiro Bangla, Anek Bangla) and verify conjuncts (যুক্তাক্ষর), vowel signs (কার), ref/ra-phala, and Bangla numerals render identically in the editor, caption styles, and exported video (golden-image tests).
- Banglish/Bangla input must work in all text fields including on-canvas text editing with third-party keyboards (IME composition handled correctly: no lost composing text, correct cursor/selection).
- Locale files for English and Bangla; Bangla-digit formatting option for timecodes.
- Auto-caption language list must include Bangla; report honest accuracy limits on fixtures.

### 12.3 Color, HDR and format handling
- Define the working color pipeline in ARCHITECTURE.md: input color space/transfer detection (BT.601/709/2020, SDR/HLG/PQ), linear-light vs gamma-space blending decision, and output tagging.
- 10-bit / HDR sources: either correct tone-mapping to SDR export (default) or HDR passthrough where the device and codec support it; never show washed-out or oversaturated output. Test with HLG and Dolby-Vision-profile-8 fallback fixtures if available.
- Handle VFR, rotation metadata, odd dimensions, non-square pixels, 10-bit alpha-less HEVC, AV1 decode where device supports, WebM/VP9 import, MKV/MOV/3GP, HEIC/HEIF/AVIF/WebP/GIF images, MP3/AAC/OGG/FLAC/WAV/OPUS audio. Unsupported combinations produce a clear error with an offer to transcode to a proxy.

### 12.4 Android platform requirements (verify against current official docs when implementing)
- Edge-to-edge and predictive back support; Photo Picker preferred; handle "partial media access" (`READ_MEDIA_VISUAL_USER_SELECTED`) correctly.
- Foreground service with the correct type for export (`mediaProcessing` where available on the target API) and proper notification channel; handle service timeout callbacks.
- Native libraries (if any) must be 16 KB page-size compatible and built with a current NDK; verify APK/AAB alignment.
- ABI strategy: ship arm64-v8a primarily; use ABI splits/AAB to keep download size reasonable; set an app-size budget and report real sizes.
- Scoped storage only; no `MANAGE_EXTERNAL_STORAGE`.
- Handle process death, config change, split-screen, multi-window, and low-memory kill without losing edits.

### 12.5 Device tiers and graceful degradation
- At first launch, probe `MediaCodecList`, max decoder instances, GL ES version, RAM class, and thermal API; store a `DeviceProfile`.
- Define Low / Mid / High presets that control proxy usage, preview resolution (e.g., 360p / 540p / 720p), max simultaneous video layers, effect quality, and default export resolution.
- If a requested combination exceeds device capability, the UI explains why and proposes the nearest supported alternative, never crashes or silently drops layers.
- Battery/thermal: lower preview quality or pause background proxy generation when thermal status is severe; warn before long 4K exports on battery saver.

### 12.6 Reliability and crash safety
- Autosave on every committed command (debounced) plus journal file; on restart, offer "Recover unsaved changes".
- Atomic file writes (write temp → fsync → rename) for project files.
- Export jobs are resumable at the "restart safely" level and never leave partial output visible in the gallery.
- Local-only crash capture (uncaught exception handler writes a sanitized report; user may share manually).
- Soak tests: random-edit fuzz script performing thousands of commands with periodic save/reload equality checks; 30-minute playback/scrub loop with memory growth assertion; 10 consecutive exports with no leak (GL/codec handle counts stable).

### 12.7 Code quality gates
- ktlint/spotless + detekt + Android Lint must pass; fail the build on new warnings in `:core:*` and `:engine:*`.
- Dependency verification: Gradle dependency locking or verification metadata, `dependencyInsight` check for duplicate Media3/Compose versions, license report generated automatically.
- Public API in `:core:*` documented with KDoc; math functions have examples in tests.
- `scripts/check.sh` and `scripts/check.ps1` run the full local pipeline (format, lint, unit tests, assembleDebug) so it works on Linux/macOS and Windows PowerShell.

### 12.8 Anti-hallucination and API-verification protocol (important for the coding agent)
- Never invent an Android, Media3, CameraX, Room or Compose API. Before using any non-trivial API, confirm it exists in the pinned version by reading its source/Javadoc from the Gradle cache or official docs. If unsure, write a 20-line probe test first.
- Media3 transformer/effect/composition APIs change between releases and parts are `@UnstableApi`/experimental: pin exact versions, isolate usage behind your own interfaces (`VideoEncoderPort`, `PreviewPlayerPort`) so a future API change touches one file.
- Prefer small, compiling steps. After every ~100 lines of new engine code, build. After every feature, run its test.
- When asked to "make it work", find the root cause from logs (`adb logcat`, Gradle `--stacktrace`, codec error codes) before changing code. Record non-obvious fixes in DECISIONS.md.

### 12.9 Definition of Done (per task)
A task is done only when: (1) code compiles in all variants, (2) relevant unit/instrumented tests exist and pass, (3) the behavior works in preview AND export if it affects rendering, (4) state persists and survives undo/redo/save/reload, (5) loading/error/empty states exist, (6) strings are localized resources, (7) accessibility labels exist, (8) FEATURE_MATRIX.md and STATUS.md are updated, (9) no new lint/detekt warnings.

### 12.10 Self-review checklist before every commit
Threading correct? Resources released? Cancellation handled? Units in microseconds? Frame-boundary rounding safe? Preview/export parity preserved? Undo works? Large-file/4K behavior bounded? No debug code, TODO-stubs or hardcoded paths? Docs updated?

### 12.11 Optional online layer (strictly separate module, default OFF)
If ever added (template store, cloud backup, stock media): separate `:optional:online` module, explicit opt-in, privacy notice, no analytics by default, core editor fully functional without it, no media uploaded without per-action consent.

---

## 13. FINAL REPORT (when you reach a gate or the end)

A) What was built (architecture + modules). B) Features VERIFIED (with the test that proved each). C) Incomplete/blocked/deferred (with reasons). D) Exact build commands + results. E) Test results split PASS/FAIL/SKIPPED/NOT RUN with device info. F) APK path, size, applicationId, versionName/Code. G) Install steps. H) Known limitations (codecs, devices, effects). I) Next 10 engineering priorities.

---

## 14. BEGIN NOW

1. Inspect the repository and environment (OS, JDK, Android SDK, Gradle, emulator/device availability, git state).
2. Create `STATUS.md` and the docs skeleton.
3. Execute **P0**, then continue phase by phase under the working loop in Section 0, honoring the V3 additions in Section 12.

Do not stop after planning. Do not stop after the UI looks good. The project is successful only when an imported, edited, saved, reopened project exports to a validated, playable MP4 — and then keeps growing toward the Tier 2 and Tier 3 feature sets.