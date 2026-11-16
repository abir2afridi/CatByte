# CatByte: প্রতিটি offline-সম্ভব ফিচার fully functional করা + real on-device Auto Captions

## Context

CatByte-এর ১৫টা main tab-এর bottom navigation-এ অনেকগুলো chip এখনো stub — ট্যাপ করলে toast দেখায়। দুই ধরনের gap: (১) **offline-এ সম্ভব কিন্তু বাকি** (background image/blur, unlink, audio effects, enhance voice, reduce noise, fades UI, adjust-এর ৯টা parameter, masks/blend/chroma/shader render হয় না, text styling); (২) **cloud-AI লাগে** (~৩০টা chip)। Auto Captions এখন fake placeholder phrase বসায় (`AutoCaptionManager.phrasesFor`)।

ব্যবহারকারীর সিদ্ধান্ত: **offline-সম্ভব সব ফিচার functional করা হবে** (cloud-AI honest-disabled থাকবে), এবং **এখনই on-device ASR** (Vosk + Bengali/English model) integrate করতে হবে।

গুরুত্বপূর্ণ সুবিধা: preview আর export একই Media3 composition ব্যবহার করে (`PlayerCompositionLoading.kt` → `CompositionPlayer`, export → `ProjectCompositionFactory`) — তাই এক জায়গায় effect wire করলে দুই জায়গাতেই কাজ করে।

## Phase 1 — Stub chips → আসল offline ফিচার

1. **Unlink (detach audio)** — `EditorBottomActionStage.kt:187`-এর toast এর বদলে: নতুন `AudioFeature.detachAudioFromClip()` (video clip mute + aligned `AudioClip` তৈরি, undoable)। Handler: `ui/screens/EditorBottomActionStage.kt`, logic: `viewmodel/features/`-এ existing pattern অনুসরণ।
2. **Background Image + Blur** — `EditorBottomActionStage.kt:274-275`-এর দুটি chip: photo picker → `VideoProject.backgroundImageUri` + `backgroundBlurRadius` নতুন field; preview canvas (`ui/canvas/VideoCanvas.kt`) ও export (`data/export/TimelineCanvasOverlay.kt`)-এ paper style-এর পাশে bitmap/blur render; `data/db/ProjectJson.kt`-এ persistence (default দিয়ে backward-compatible)। Blur: API 31+ `RenderEffect`, নিচে stack-blur fallback।
3. **Audio Effects** — নতুন `AudioEffectsSheet`: pitch (Media3 `SonicAudioProcessor`), echo, reverb, robot (custom `AudioProcessor` — `engine/`-এ DSP math + unit test); per-clip field → `data/ProjectCompositionAudioSequence.kt`-এ wire (preview+export)।
4. **Enhance voice** — EQ preset AudioProcessor (high-pass 80Hz + presence boost)।
5. **Reduce noise** — envelope-follower noise-gate AudioProcessor (honest "basic" label)।
6. **Copyright chip** — toast-এর বদলে Sounds library tab খুলবে।

## Phase 2 — Render parity (এখনকার sheet-গুলো আসলে কাজ করবে)

7. **Masks** — `engine/`-এর `MaskMath` → custom Media3 `GlEffect` fragment shader (shape + feather + invert), `ProjectCompositionFactory`-এ per-clip; `MaskSheet` তখন preview+export দুটোতেই render করবে।
8. **Blend modes** — `BlendModeSheet`-এর selection → overlay-তে `StaticOverlaySettings` blend mode (1.11.1 API verify করে; না থাকলে custom shader)।
9. **Chroma key** — CPU bitmap path-এর (`AiCutoutManager`) বদলে GL shader effect (`ChromaKeySheet`-এর প্যারামিটার আগে থেকেই model-এ আছে)।
10. **Effects library** — `EffectShaders`-এর per-frame GLSL-গুলো `GlShaderProgram` দিয়ে execute; two-frame-লাগে এমনগুলো honest-disabled।
11. **Adjustments** — missing ৯টা parameter যোগ: exposure/temperature/tint/vibrance (`ColorGradeEffect` matrix math), highlights/shadows/sharpen/grain/blur (shader) — `AdjustmentsSheet` + `MediaClip` model + `ProjectCompositionColorEffect.kt`।

## Phase 3 — Core editing gaps

12. **Fade in/out UI** — field আছে (`model/TrackModels.kt:34-35`), UI নেই; EditClip/Volume sheet-এ slider + composition-এ audio ramp ও video alpha ramp।
13. **Text styling** — bold/italic, letter/line spacing, stroke, shadow: `TextOverlay` model + `TextEditorSheet` + preview canvas + `TimelineCanvasOverlay.drawText`-এর Paint।
14. **Project rename** (Home `ProjectCardMenu`) ও markers rename/recolor UI সম্পূর্ণকরণ।

## Phase 4 — Real on-device Auto Captions (Vosk)

15. `com.alphacephei:vosk-android` dependency; `SpeechTranscriber` interface + `VoskTranscriber` (JVM test-এর জন্য fake inject করা যাবে); model manager — user-consented download (English small + Bengali small, alphacephei.com/HuggingFace zip), progress UI, app-files-এ store।
16. Audio decode → 16kHz mono PCM (MediaCodec decode + `engine/`-এ resampler, unit-tested); existing `extractAudioFromSelectedClip` decode infra reuse।
17. `AutoCaptionManager`-এ real path: word timestamps → বর্তমান `CaptionTimeline`/`CaptionBridge` pipeline (design অনুযায়ী শুধু `phrasesFor` বদলাবে) + language picker (Bengali/English); model না থাকলে স্পষ্ট-label করা sample fallback।

## Phase 5 — Home tabs + docs

18. **Inbox/Profile** — আসল data দেখাবে (per-project clip count, duration, last-edited; aggregate stats), placeholder না।
19. `FEATURE_MATRIX.md`/`STATUS.md` status আপডেট (repo convention)।

## Verification

- প্রতি phase-এর শেষে: `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug` — repo green থাকবে (এখন ৩০৯ test pass)।
- নতুন unit test: DSP (resampler/gate/echo/EQ), detach-audio state transition, fade ramp, adjust-এর নতুন matrix math, fake transcriber দিয়ে captions cue generation, নতুন field-এর JSON round-trip।
- Emulator এই পরিবেশে নেই — instrumented/device verify pending থাকবে (unit+compile+lint হলো gate)।

## Risks

- Vosk-এর .so 16KB-page alignment (Android 15+ dev option): init fail হলে graceful fallback → sample captions + স্পষ্ট message।
- `StaticOverlaySettings` blend API 1.11.1-এ না থাকলে custom shader fallback।
- পুরো কাজটা বড় — phase অনুযায়ী ভাগ করা, প্রতিটা phase শেষে shippable; এক সেশনে না ফুরিলে পরের সেশনে phase ধরে চলবে।