# Feature Matrix - CatByte Video Editor

**Status Legend:**
- 🟢 **VERIFIED** - Implementation complete, tests pass, preview/export parity confirmed
- 🟡 **IMPLEMENTED-NOT VERIFIED** - Code exists but not tested/verified
- 🔵 **IN PROGRESS** - Currently being implemented
- 🔴 **NOT STARTED** - Not yet implemented
- ⚫ **BLOCKED** - Blocked by dependency/decision
- ⚪ **DEFERRED** - Deferred to later phase

---

## Tier 1 — MUST (Release Blocker)

| Feature | Status | Notes |
|---------|--------|-------|
| **Media Import** | | |
| Photo Picker + SAF | 🟡 IMPLEMENTED-NOT VERIFIED | Basic picker exists, need multi-select persistable URIs |
| Multi-select | 🟡 IMPLEMENTED-NOT VERIFIED | Numbered badges, preview on long-press |
| Metadata extraction (dims, rotation, fps, duration, codec, audio) | 🔴 NOT STARTED | |
| Thumbnails | 🟡 IMPLEMENTED-NOT VERIFIED | Basic thumbnails, need LRU bitmap pool |
| Missing-media relink flow | 🔴 NOT STARTED | |
| **Projects** | | |
| Create (aspect presets: 9:16, 16:9, 1:1, 4:5, 3:4, 21:9, custom) | 🟡 IMPLEMENTED-NOT VERIFIED | 4 presets implemented, missing 3:4, 21:9, custom |
| Rename | 🔴 NOT STARTED | |
| Duplicate | 🟢 VERIFIED | Works with undo |
| Delete | 🟢 VERIFIED | Works with undo bar |
| Autosave | 🟡 IMPLEMENTED-NOT VERIFIED | Saves on every change, no debounce |
| Crash recovery | 🔴 NOT STARTED | |
| Drafts | 🟡 IMPLEMENTED-NOT VERIFIED | Auto-saves to Room |
| **Timeline** | | |
| Ruler | 🟡 IMPLEMENTED-NOT VERIFIED | Basic ruler, adaptive tick density missing |
| Centered/draggable playhead | 🔴 NOT STARTED | Plan requires fixed center playhead (CapCut-style) |
| Pinch zoom | 🟢 VERIFIED | Works |
| Frame-accurate seek | 🟡 IMPLEMENTED-NOT VERIFIED | ExoPlayer seek, frame accuracy not verified |
| Magnetic main track | 🔴 NOT STARTED | |
| Free overlay tracks | 🟡 IMPLEMENTED-NOT VERIFIED | Overlay tracks exist but not "free" |
| Snapping (playhead, clip edges, markers, beats) | 🔴 NOT STARTED | |
| Trim handles | 🟢 VERIFIED | Drag edges to trim |
| Split | 🟢 VERIFIED | Works |
| Delete | 🟢 VERIFIED | Works |
| Duplicate | 🟢 VERIFIED | Works |
| Reorder | 🟡 IMPLEMENTED-NOT VERIFIED | Drag-reorder exists |
| Ripple delete | 🔴 NOT STARTED | |
| Multi-select | 🔴 NOT STARTED | |
| Track mute/hide/lock | 🟢 VERIFIED | Works |
| Thumbnails strip | 🟡 IMPLEMENTED-NOT VERIFIED | Basic thumbnails |
| Waveform | 🟡 IMPLEMENTED-NOT VERIFIED | Static waveform, need multi-resolution pyramids |
| **Preview** | | |
| Play/pause/seek/scrub | 🟢 VERIFIED | Works |
| Loop | 🟢 VERIFIED | Works |
| Fullscreen | 🔴 NOT STARTED | |
| A/V sync | 🟡 IMPLEMENTED-NOT VERIFIED | ExoPlayer sync, drift < 1 frame not verified |
| Canvas-correct rendering | 🟡 IMPLEMENTED-NOT VERIFIED | Aspect ratio handling exists |
| Safe-area guides | 🔴 NOT STARTED | |
| **Clip Tools** | | |
| Trim | 🟢 VERIFIED | Works |
| Split | 🟢 VERIFIED | Works |
| Speed (0.1×–100×, presets + custom) | 🟡 IMPLEMENTED-NOT VERIFIED | Basic speed, no presets UI, no custom bezier |
| Volume | 🟢 VERIFIED | Works |
| Fade in/out | 🟡 IMPLEMENTED-NOT VERIFIED | Basic fade, UI missing |
| Rotate | 🟢 VERIFIED | Works |
| Flip | 🟢 VERIFIED | Works |
| Crop (free + ratios) | 🔴 NOT STARTED | |
| Scale/position/rotation via on-canvas gestures | 🔴 NOT STARTED | |
| Fit/fill/blur-fill background | 🔴 NOT STARTED | |
| Mirror | 🟢 VERIFIED | Works |
| Replace | 🔴 NOT STARTED | |
| Freeze frame | 🔴 NOT STARTED | |
| Reverse | 🔴 NOT STARTED | |
| Extract audio | 🔴 NOT STARTED | |
| Detach audio | 🔴 NOT STARTED | |
| **Text** | | |
| Multi-line | 🟡 IMPLEMENTED-NOT VERIFIED | Basic text overlay |
| Unicode incl. Bangla shaping | 🔴 NOT STARTED | CRITICAL: Need HarfBuzz verification + bundled OFL fonts |
| Fonts (bundled OFL + system) | 🔴 NOT STARTED | No bundled fonts |
| Size, color, gradient fill | 🟡 IMPLEMENTED-NOT VERIFIED | Basic color, no gradient |
| Alignment | 🟡 IMPLEMENTED-NOT VERIFIED | Basic positioning |
| Letter/line spacing | 🔴 NOT STARTED | |
| Bold/italic | 🔴 NOT STARTED | |
| Stroke, shadow, glow | 🟡 IMPLEMENTED-NOT VERIFIED | NEON_GLOW style only |
| Background box | 🟡 IMPLEMENTED-NOT VERIFIED | Semi-transparent background |
| In/out/loop animations | 🟡 IMPLEMENTED-NOT VERIFIED | POP_UP, TYPEWRITER, FADE_IN, WAVE, NEON_PULSE |
| Keyframes | 🔴 NOT STARTED | |
| **Audio** | | |
| Music import | 🟡 IMPLEMENTED-NOT VERIFIED | Audio library exists |
| Multiple tracks | 🟢 VERIFIED | Multiple audio tracks supported |
| Trim/split | 🟡 IMPLEMENTED-NOT VERIFIED | Basic trim |
| Volume | 🟢 VERIFIED | Works |
| Fades | 🟡 IMPLEMENTED-NOT VERIFIED | fadeInMs/fadeOutMs fields exist |
| Voiceover recording | 🔴 NOT STARTED | Mic permission, headphone-aware |
| Waveform | 🟡 IMPLEMENTED-NOT VERIFIED | Static waveform |
| Mixer with limiter | 🔴 NOT STARTED | |
| **Adjust** | | |
| Brightness, contrast, saturation, exposure, highlights, shadows, temperature, tint, vibrance, sharpen, vignette, grain, blur | 🟡 IMPLEMENTED-NOT VERIFIED | Basic brightness/contrast/saturation/vignette; missing exposure, highlights, shadows, temperature, tint, vibrance, sharpen, grain, blur |
| All as GLSL, keyframable | 🔴 NOT STARTED | Using Media3 ColorGradeEffect, not custom GLSL |
| **Filters** | | |
| ≥20 original LUT-based presets (.cube/PNG LUT, 33³ or 64²) | 🔴 NOT STARTED | Only 13 built-in enum filters, no LUTs |
| Intensity slider | 🔴 NOT STARTED | |
| **Transitions** | | |
| Cut, fade, dissolve, dip-to-color, slide (4 dirs), push, wipe (linear/radial), zoom, spin, blur, glitch | 🟡 IMPLEMENTED-NOT VERIFIED | 11 transition types, but rendered via Media3 not custom two-frame shader |
| Real duration and easing | 🟡 IMPLEMENTED-NOT VERIFIED | transitionDurationMs field exists |
| **Overlays** | | |
| PiP video | 🔴 NOT STARTED | |
| Images | 🔴 NOT STARTED | |
| Stickers (PNG/WebP/Lottie) | 🟡 IMPLEMENTED-NOT VERIFIED | Emoji stickers only |
| Opacity | 🟡 IMPLEMENTED-NOT VERIFIED | Opacity field exists |
| Blend modes (normal, multiply, screen, overlay, add) | 🟡 IMPLEMENTED-NOT VERIFIED | BlendMode enum exists, not wired to export |
| **Export** | | |
| 480p–4K (device-capability checked) | 🟢 VERIFIED | Ladder logic unit-tested (ExportMathTest 9/9): HEVC→H.264, size requested→1440p→1080p→720p→480p, never larger than requested; on-device probe pending instrumented run |
| 24/25/30/50/60 fps | 🟡 IMPLEMENTED-NOT VERIFIED | FPS presets exist; fps ladder 60→30→24 unit-tested in ExportMathTest |
| H.264 (baseline) + HEVC (if supported) | 🟢 VERIFIED | CodecCapabilityChecker pins resolved mime via setVideoMimeType; rejected pinned codec retries once with Media3 default (DEC-017) |
| Bitrate/quality presets | 🔴 NOT STARTED | |
| AAC audio | 🟡 IMPLEMENTED-NOT VERIFIED | Media3 default |
| Real progress | 🟢 VERIFIED | ProgressHolder polling works |
| Cancel | 🟢 VERIFIED | Cancel works |
| Temp file → validate → publish to MediaStore | 🟡 IMPLEMENTED-NOT VERIFIED | MediaMetadataRetriever validation implemented (tracks, dims, duration ±1s) + MediaStore publish; needs device verify |
| Share/open actions | 🟡 IMPLEMENTED-NOT VERIFIED | ShareHelper installs social-app intents in the completed dialog |
| Storage precheck | 🟡 IMPLEMENTED-NOT VERIFIED | Estimate uses post-downgrade resolved size × duration × 1.1 |
| Foreground service | 🟡 IMPLEMENTED-NOT VERIFIED | Typed startForeground (API 35+ mediaProcessing, API 34 dataSync), channel created, double-start guard; device run pending |
| Background export (keep editing) | 🟡 IMPLEMENTED-NOT VERIFIED | ExportDialog "Keep editing — export in background" starts FGS, closes dialog; POST_NOTIFICATIONS requested on 13+ (denial non-blocking) |

---

## Tier 2 — SHOULD (After Tier 1 Verified)

| Feature | Status | Notes |
|---------|--------|-------|
| Keyframe editor UI (diamond markers, easing) | 🔴 NOT STARTED | |
| Masks (linear, mirror, circle, rectangle, heart/star, custom path; feather, invert, keyframable) | 🔴 NOT STARTED | MaskType enum exists, UI/effects missing |
| Chroma key (YUV/HSV, spill suppression, softness) | 🔴 NOT STARTED | |
| Speed curves (bezier ramp editor) with optical-flow-free frame blending | 🔴 NOT STARTED | SpeedCurve model exists, editor missing |
| Adjustment layers and effect clips | 🔴 NOT STARTED | |
| Effects library (≥30 original GLSL effects) | 🔴 NOT STARTED | Only 6 EffectType enums, no GLSL |
| Text templates, animated captions (word-by-word, karaoke) | 🔴 NOT STARTED | |
| SRT/VTT import & export, burned-in captions | 🔴 NOT STARTED | |
| Audio: noise reduction (RNNoise) | 🔴 NOT STARTED | Check license |
| Audio: voice effects (pitch/formant/robot/echo/reverb) | 🔴 NOT STARTED | |
| Audio: beat detection + beat markers | 🔴 NOT STARTED | |
| Audio: auto-ducking | 🔴 NOT STARTED | |
| Audio: equalizer | 🔴 NOT STARTED | |
| Audio: loudness normalization (LUFS target) | 🔴 NOT STARTED | |
| Templates: save/load as reusable template with replaceable slots | 🔴 NOT STARTED | |
| Multi-project asset library | 🔴 NOT STARTED | |
| Project backup/restore (.zip with media optional) | 🔴 NOT STARTED | |

---

## Tier 3 — ADVANCED (Separate Verified Deliverables)

| Feature | Status | Notes |
|---------|--------|-------|
| On-device auto captions (Whisper.cpp/ONNX/TFLite) | 🔴 NOT STARTED | User-consented download, Bangla+English tested |
| Background removal / portrait matting (MediaPipe/ONNX) | 🔴 NOT STARTED | |
| Object tracking (mask/sticker follows subject) | 🔴 NOT STARTED | |
| Video stabilization (gyro-free or Media3/OpenCV) | 🔴 NOT STARTED | Verify license |
| Auto-reframe (subject-aware crop) | 🔴 NOT STARTED | |
| Smart cutout | 🔴 NOT STARTED | |
| Silence removal | 🔴 NOT STARTED | |
| Scene detection | 🔴 NOT STARTED | |
| AI-style auto-edit to beats | 🔴 NOT STARTED | |
| Camera capture module (CameraX) | 🔴 NOT STARTED | |
| Screen-record import | 🔴 NOT STARTED | |
| Text-to-speech voiceover (Android TTS) | 🟡 IMPLEMENTED-NOT VERIFIED | TTS_VOICEOVER tool sheet exists |

---

## V3 Additions — Tier 1

| Feature | Status | Notes |
|---------|--------|-------|
| Copy/cut/paste/paste-attributes for clips | 🔴 NOT STARTED | |
| Multi-select with group move | 🔴 NOT STARTED | |
| Clip grouping | 🔴 NOT STARTED | |
| "Select all to right" | 🔴 NOT STARTED | |
| Markers with labels/colors; jump-to-marker; marker-based snapping | 🔴 NOT STARTED | |
| Ken Burns auto pan/zoom for photos | 🔴 NOT STARTED | |
| Background options: color, gradient, blurred-copy, image | 🟡 IMPLEMENTED-NOT VERIFIED | Paper styles exist |
| Split-screen / collage layouts (2,3,4-up, grid) | 🔴 NOT STARTED | |
| Cover/thumbnail frame picker | 🔴 NOT STARTED | |
| Export presets: TikTok/Reels/Shorts, YouTube, WhatsApp, Custom | 🟡 IMPLEMENTED-NOT VERIFIED | Basic presets exist |
| Audio-only export (AAC .m4a) | 🔴 NOT STARTED | |
| GIF / animated WebP export | 🔴 NOT STARTED | |
| Optional watermark/outro toggle | 🟡 IMPLEMENTED-NOT VERIFIED | Watermark toggle exists |
| Landscape editor layout, tablet/foldable (window size classes) | 🔴 NOT STARTED | |
| External keyboard shortcuts (space=play, S=split, Ctrl+Z/Y, Del) | 🔴 NOT STARTED | |
| Onboarding: interactive first-run tour, per-tool tooltips, sample project | 🔴 NOT STARTED | |

---

## V3 Additions — Tier 2

| Feature | Status | Notes |
|---------|--------|-------|
| Compound clips (nested sequences) with expand/collapse | 🔴 NOT STARTED | |
| Reusable "presets" (saved effect/animation stacks) | 🔴 NOT STARTED | |
| Motion blur on moving layers | 🔴 NOT STARTED | |
| Speed-ramp motion blur | 🔴 NOT STARTED | |
| Text-in-video masks, curved/path text, text-follow-path, emoji | 🔴 NOT STARTED | |
| Word-level animation presets | 🔴 NOT STARTED | |
| Sound-effects library (original/CC0) | 🔴 NOT STARTED | |
| Audio ducking, voice changer, audio markers | 🔴 NOT STARTED | |
| Auto-sync cuts to beat grid | 🔴 NOT STARTED | |
| Stereo pan, loudness meter | 🔴 NOT STARTED | |
| Video-in-video freeze + "echo/trail" effects | 🔴 NOT STARTED | |
| Glitch packs, 3D-zoom/parallax from still photos | 🔴 NOT STARTED | |
| LUT import (.cube) by user | 🔴 NOT STARTED | |
| Effect/filter favorites; recently used | 🔴 NOT STARTED | |
| Project package export/import (.cfproj zip) | 🔴 NOT STARTED | |
| Local diagnostics export | 🔴 NOT STARTED | |

---

## V3 Additions — Tier 3

| Feature | Status | Notes |
|---------|--------|-------|
| Depth-based parallax photos, sky replacement | 🔴 NOT STARTED | |
| AI-based denoise/upscale (measured real results + consent) | 🔴 NOT STARTED | |
| Auto-highlight detection | 🔴 NOT STARTED | |
| Face-aware crop | 🔴 NOT STARTED | |
| Lip-sync-safe speed | 🔴 NOT STARTED | |

---

## Cross-Cutting Concerns

| Area | Status | Notes |
|------|--------|-------|
| **Bangla-first quality** | | |
| Bundled OFL Bangla fonts | 🔴 NOT STARTED | Noto Sans Bengali, Hind Siliguri, Tiro Bangla, Anek Bangla |
| Conjuncts, vowel signs, ref/ra-phala, Bangla numerals | 🔴 NOT STARTED | Golden-image tests needed |
| Banglish/Bangla input in all text fields | 🔴 NOT STARTED | IME composition handling |
| Locale files EN + BN | 🟢 VERIFIED | strings.xml + values-bn/strings.xml, 125 keys full parity; Robolectric guard test passes (LocalizationRobolectricTest, sdk 34) |
| Bangla-digit formatting for timecodes | 🔴 NOT STARTED | |
| Auto-caption language includes Bangla | 🔴 NOT STARTED | |
| **Color, HDR, Format Handling** | | |
| Working color pipeline documented | 🔴 NOT STARTED | ARCHITECTURE.md |
| 10-bit / HDR sources: tone-mapping or HDR passthrough | 🔴 NOT STARTED | |
| VFR, rotation metadata, odd dimensions | 🟡 IMPLEMENTED-NOT VERIFIED | Media3 handles |
| Non-square pixels | 🔴 NOT STARTED | |
| 10-bit alpha-less HEVC, AV1 decode | 🔴 NOT STARTED | |
| WebM/VP9 import, MKV/MOV/3GP | 🟡 IMPLEMENTED-NOT VERIFIED | Media3 handles |
| HEIC/HEIF/AVIF/WebP/GIF images | 🔴 NOT STARTED | |
| MP3/AAC/OGG/FLAC/WAV/OPUS audio | 🟡 IMPLEMENTED-NOT VERIFIED | Media3 handles |
| Unsupported combinations: clear error + proxy offer | 🔴 NOT STARTED | |
| **Android Platform Requirements** | | |
| Edge-to-edge, predictive back | 🟡 IMPLEMENTED-NOT VERIFIED | enableOnBackInvokedCallback=true |
| Photo Picker preferred | 🟢 VERIFIED | Using ActivityResultContracts.PickMultipleVisualMedia |
| Partial media access (READ_MEDIA_VISUAL_USER_SELECTED) | 🟡 IMPLEMENTED-NOT VERIFIED | Permissions in manifest |
| Foreground service correct type (mediaProcessing) | 🟡 IMPLEMENTED-NOT VERIFIED | Manifest `mediaProcessing|dataSync` + typed startForeground; channel exists; device run pending |
| Native libraries 16KB page-size compatible | 🔴 NOT STARTED | No native libs yet |
| ABI strategy: arm64-v8a primarily, AAB splits | 🔴 NOT STARTED | |
| App size budget | 🔴 NOT STARTED | |
| Scoped storage only | 🟢 VERIFIED | No MANAGE_EXTERNAL_STORAGE |
| Process death, config change, split-screen, low-memory | 🔴 NOT STARTED | |
| **Device Tiers & Graceful Degradation** | | |
| DeviceProfile probe at first launch | 🔴 NOT STARTED | |
| Low/Mid/High presets | 🔴 NOT STARTED | |
| Capability exceed: explain + propose alternative | 🔴 NOT STARTED | |
| Battery/thermal: lower preview quality, pause proxies | 🔴 NOT STARTED | |
| **Reliability & Crash Safety** | | |
| Autosave on every committed command (debounced) + journal | 🟡 IMPLEMENTED-NOT VERIFIED | Saves on every change, no debounce, no journal |
| Atomic file writes (temp → fsync → rename) | 🔴 NOT STARTED | |
| Export jobs resumable at "restart safely" level | 🔴 NOT STARTED | |
| No partial output visible in gallery | 🟡 IMPLEMENTED-NOT VERIFIED | Saves to temp then MediaStore |
| Local-only crash capture | 🔴 NOT STARTED | |
| Soak tests: random-edit fuzz, 30-min playback, 10 exports | 🔴 NOT STARTED | |
| **Code Quality Gates** | | |
| ktlint/spotless + detekt + Android Lint pass | 🔴 NOT STARTED | |
| Fail build on new warnings in :core:* and :engine:* | 🔴 NOT STARTED | No :core:*, :engine:* modules |
| Dependency verification (locking/verification metadata) | 🔴 NOT STARTED | |
| Public API in :core:* documented with KDoc | 🔴 NOT STARTED | No :core:* modules |
| Math functions have examples in tests | 🔴 NOT STARTED | |
| scripts/check.sh and check.ps1 | 🔴 NOT STARTED | |
| **Anti-Hallucination / API Verification** | | |
| Confirm APIs exist in pinned versions | 🔴 NOT STARTED | |
| Media3 unstable APIs isolated behind interfaces | 🔴 NOT STARTED | |
| Small compiling steps, build after ~100 lines | 🔴 NOT STARTED | |
| Root cause from logs before changing code | 🔴 NOT STARTED | |
| **Definition of Done (per task)** | | |
| Code compiles in all variants | 🔴 NOT STARTED | |
| Unit/instrumented tests exist and pass | 🟡 IMPLEMENTED-NOT VERIFIED | Unit 50/50 pass on JVM; instrumented compile pass, execution pending device |
| Works in preview AND export (rendering features) | 🔴 NOT STARTED | |
| State persists and survives undo/redo/save/reload | 🔴 NOT STARTED | No undo/redo |
| Loading/error/empty states exist | 🟡 IMPLEMENTED-NOT VERIFIED | Basic empty states |
| Strings are localized resources | 🟢 VERIFIED | EN + BN |
| Accessibility labels exist | 🔴 NOT STARTED | |
| FEATURE_MATRIX.md and STATUS.md updated | 🟢 VERIFIED | Creating now |
| No new lint/detekt warnings | 🔴 NOT STARTED | |
