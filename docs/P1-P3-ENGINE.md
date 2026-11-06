# P1–P3 engine layer

Date: 2026-11-04

The P1/P2/P3 roadmap from `README.md` splits into two halves:

* **Decisions and maths** — pure Kotlin, runs on the JVM, covered by unit tests.
* **Execution** — EGL/GLES, MediaCodec, CameraX, bundled fonts and AI model
  files. None of that can run in this environment (no GPU surface, no device, no
  model binaries), so it is listed at the bottom as wiring work, not as done.

This document covers the first half: the modules added to `com.example.engine`,
the wiring that now reaches the editor UI, and the work that still needs a GPU
surface, a camera or model files. Total suite is **294 tests, 0 failures**.

## What was added

| Module | P | Covers |
|---|---|---|
| `TimelineEdits` | P1 | Magnetic placement (start *or* end edge snaps), reorder-close-gaps, ripple vs lift delete, multi-select group shift, trim bounded by neighbours + minimum duration |
| `CanvasMath` + `CropMath` | P1 | Focal-point pinch/rotate, drag, on-canvas clamp, centre snap, contain/cover scale; crop by aspect, coverage, rotation swap, even-aligned pixel rects |
| `MaskMath` | P1 | Rectangle / circle / split / mirror / filmstrip coverage, feather ramp (smoothstep), invert |
| `BlendMath` | P1 | Multiply, screen, overlay, add, subtract, difference — channel maths plus straight-alpha ARGB composite |
| `ChromaKey` | P1 | Cb/Cr distance keying with tolerance + softness, green-spill suppression, ARGB apply |
| `CaptionTimeline` | P1 | Cue grouping by characters / words / pause / duration, karaoke windows, active word, reading speed, readability extension capped by the next cue |
| `FilterPresets` | P1 | 20 original bilingual colour looks as `ColorGrade`, linear blending, intensity slider |
| `PlatformPresets` | P1 | TikTok, Reels, Shorts, YouTube, YouTube 4K, WhatsApp, square, portrait, original — never upscales the source short edge, even dimensions, bitrate from a documented bits-per-pixel budget |
| `Shortcuts` | P2 | Tablets/Chromebooks/DeX keyboard mapping and hint strings |
| `SceneMath` | P3 | Luma histograms, half-L1 frame distance, cut detection with min-gap suppression |
| `BeatGrid` | P3 | Onset peaks from an energy envelope, tempo from median IOI with octave folding, beat lattice, nearest beat |
| `SubjectTrack` | P3 | Box clamp, exponential smoothing with deadband (anti-jitter), aspect-correct crop window for auto-reframe |

Tests: `TimelineEditsTest`, `CanvasCropTest`, `MaskBlendChromaTest`,
`CaptionFilterTest`, `PolicyPresetAutoEditTest`, `AutoCaptionManagerTest`.

**No separate preview-policy module.** `DeviceProfiles` already owns the
tier/thermal/battery ladder (`previewBudget`, `exportBudget`,
`shouldGenerateProxies`), so an earlier `PreviewPolicy` was deleted rather than
shipped as a second source of truth.

## Wired into the app

| Wiring | Path |
|---|---|
| Offline auto-captions | `AutoCaptionManager` splits each phrase into ≤32-char / ≤5-word cues with `CaptionTimeline`, stretches them to 17 cps, converts them with `CaptionBridge`, and `EditorViewModel.addAutoCaptions()` appends them as subtitle overlays |
| Auto Captions button | `EditorScreen` now passes `onAutoCaptions` to `TextEditorSheet`, so the button that was hidden (`null` callback) is live |
| Keyboard shortcuts | `EditorScreen` takes focus on open and dispatches `Shortcuts.resolve` to play/pause, split, delete, undo/redo, zoom and frame stepping |

## One real bug the tests caught

`CropMath.toPixels` truncated floats: `0.9f - 0.1f` is `0.79999995`, so a
full-frame crop produced a **1534 px** wide rect instead of 1536 for a 1920 source.
Now rounded before even alignment, which is what encoders expect.

## Still device- or asset-bound (not done)

| Item | Blocker | First wiring point |
|---|---|---|
| GL compositor executing masks / blends / chroma / 35 shaders | Needs EGL + GLES context; no GPU surface here | `MaskMath`, `BlendMath`, `ChromaKey`, `EffectShaders` are already the shader contract |
| True two-frame transitions | Needs the compositor (Media3 Transformer cannot sample two layers) | `TransitionEngine.weights()` + `RenderGraph` |
| Bundle OFL Bangla fonts + shaping golden images | Font binaries are not in the repo | `BanglaText` + `assets/fonts` |
| Speech-recognition model (real auto captions) | No ASR model in the APK | `AutoCaptionManager.phrasesFor` is the only thing to replace; cue timing is already real |
| Karaoke word highlighting | Needs a per-word render pass | `CaptionTimeline.karaoke` is ready |
| CameraX capture | No CameraX dependency, no camera | New module; `Project` import path already accepts URIs |
| On-device captions / cutout / tracking models | Model files + licence + size budget | `CaptionTimeline`, `AiCutoutManager`, `SubjectTrack` |
| Free drag across tracks | Compose gesture work | Main-track long-press reorder + overlay start-time drag wired; cross-track placement pending |
| Export sheet presets | UI | `PlatformPresets` + `ExportPresets.estimateBytes` |
| Real-device verification | No device/emulator attached | `connectedDebugAndroidTest` + the codec ladder |

The rule for the GL milestone: preview and export must call the *same* functions
in this package, and no shader may implement rules that this Kotlin code states
differently — otherwise the parity that `ProjectCompositionPlanTest` protects
starts to drift again.

## Addendum — captions and export sheet wired (later on 2026-11-04)

`CaptionBridge` (new module) plus `CaptionBridgeTest` (12 tests) join the
package: pure conversion between parsed SRT/VTT cues and `TextOverlay`s, so the
import path, the share-sheet export and the burned-in render never disagree.

- **Import**: the text sheet opens a document picker (`*/*`, max 2 MB), parses
  via `SubtitleIO.parse` (both SRT and VTT), places cues as bottom-centred,
  subtitle-styled text overlays and toasts the count plus the first validation
  issue (overlaps, cues starting past the end).
- **Export**: the same sheet shares the timeline text as an SRT file through
  the system share sheet (FileProvider, `application/x-subrip`).
- **Burn-in**: imported captions render exactly like typed text because preview
  and export share `TimelineCanvasOverlay`; no separate caption path exists.
- **Export sheet**: canvas/preset chips (9:16 TikTok/Reels, 16:9 YouTube, 1:1
  Square, 4:5 Portrait) update the project aspect ratio with the same rebuild
  the canvas sheet triggers; the misleading "2K/4K" button is now a real "4K";
  the dead watermark note is a real Switch wired to preview + export; an
  estimated file size is shown using the engine bitrate ladder.

- **Drag-reorder**: long-press + drag on main-track clips reorders the clip
  list through the same `moveClip` path as before; tap-to-select is untouched.
  The drop target is the final insertion index computed from on-screen slot
  geometry (clip widths + the 26 dp transition buttons), so the drop position
  matches what the user sees. Free cross-track drag is still pending.

### Auto captions and keyboard shortcuts (same day)

- **Auto captions**: `AutoCaptionManager` no longer emits one 3-second card per
  phrase. Each phrase is sliced into per-word timings, regrouped into cues of
  ≤32 characters / ≤5 words by `CaptionTimeline.cues`, stretched towards 17
  characters per second (never past the next cue or the project end) and
  converted with `CaptionBridge`; `EditorViewModel.addAutoCaptions()` appends
  them as subtitle overlays and selects the first one. The **Auto Captions**
  button in the text sheet was hidden because nothing passed `onAutoCaptions` —
  it is now wired. `AutoCaptionManagerTest` covers ordering, overlap, project
  bounds and short projects.
- **Keyboard shortcuts**: `EditorScreen` takes focus on open and routes
  key-down events through `Shortcuts.resolve` to play/pause, split, delete,
  undo/redo, timeline zoom and single-frame stepping. Only `TOGGLE_MUTE` is left
  unhandled because the editor has no mute action yet; that key passes through.
- **Deleted `PreviewPolicy`**: `DeviceProfiles.previewBudget` already answers the
  same tier/thermal/battery question, so the duplicate module and its five tests
  were removed instead of shipping two ladders that can drift.

Total suite after this addendum: **256 tests, 0 failures**.

## Addendum — timeline gestures, frame-accurate split, feature-file split (2026-11-06)

### Two new engine modules

| Module | Tests | Covers |
|---|---|---|
| `TimelineGeometry` | `TimelineGeometryTest` (6) | The one dp/ms conversion (`DP_PER_SECOND = 65`, zoom clamp), content width/padding, drag-anchor maths (`positionMsAfterDrag`), screen-x from scroll offset, snap/hit constants (`SNAP_THRESHOLD_DP`, `PLAYHEAD_HIT_HALF_DP`) |
| `ClipSplit` | `ClipSplitTest` (6) | `MIN_SPLIT_MS = 100` edge guard, source-offset resolution through trims + constant speed, keyframe partition + rebase, outgoing transition stays on the second half, entrance animation stays on the first |

`ClipTiming.sourceOffsetForOutputMs` joins `ClipTiming`: it converts a timeline
offset back into a *source* timestamp, integrating the velocity curve at 20 ms
steps when one is active. Splitting a 2× clip at output 1000 ms therefore cuts
at source 3000 ms, not at 1000 ms — the two halves replay exactly the frames
the preview showed.

### Timeline interaction redesign (the three real bugs)

1. **Free pan restored** — the old code wrapped the whole timeline in
   `detectDragGestures` + `detectTransformGestures`, which consumed every drag
   and starved `horizontalScroll`. Those detectors are gone; the viewport pans
   freely and gestures now live where they belong:
   - ruler: tap + drag scrub (consumes only inside the ruler),
   - main track: tap selects, long-press drag reorders,
   - playhead needle: drag seeks.
2. **Playhead drag fixed** — the needle previously recomputed its position from
   its own moving screen position, so the cursor stuck and jumped. Deltas are
   now accumulated from a fixed `dragAnchorMs` and converted through
   `TimelineGeometry.positionMsAfterDrag`. The needle renders *outside* the
   scroll container (positioned from `scrollState.value`), drags get edge
   auto-scroll (`dispatchRawDelta`, 48 dp edge / 24 dp step) and snapping via
   `SnapEngine` (never to itself). Scroll reads moved inside the needle so
   scroll-driven recomposition stays scoped to it.
3. **Playback no longer fights the timeline** — any press on the viewport
   (`PointerEventPass.Initial`) and any scroll drag pauses playback; the
   follow-scroll effect only runs while playing *and* untouched. Splitting
   pauses first and re-syncs the exact player position
   (`PlayerManager.syncPosition()` now also runs inside `pausePlayback()`), so
   the cut lands on the frame the user is looking at.

`splitClipAtPlayhead()` returns `Boolean` now: a selected overlay splits first,
then the main-track clip; a playhead outside a splittable clip toasts
"Move the playhead inside a clip to split it" instead of doing nothing.

### Feature-file split

`CapCutToolSheets.kt` (2,459 lines, 17 sheets) is gone, replaced by one file per
tool: `EditClipSheet`, `SpeedSheet`, `SpeedCurveEditorSheet`, `AudioSheet`,
`TextEditorSheet`, `EffectsSheet`, `FiltersSheet`, `AdjustmentsSheet`,
`CanvasRatioSheet`, `StickersSheet`, `TransitionsPickerSheet`, `BlendModeSheet`,
`MaskSheet`, `KeyframeSheet`, `ChromaKeySheet`, `VolumeSheet`. The timeline
split into `TimelinePlayhead`, `TimelineRuler`, `TimelineVideoTrack`,
`TimelineOverlayTrack`, `TimelineAudioTrack`, `TimelineTextTrack`,
`TimelineEffectsTrack`, `TimelineTrackHeader` + the `MultiTrackTimeline`
coordinator, and `EditorScreen` shed `EditorTopBar`, `CapCutBottomActionBar`,
`EditorKeys`, `PlaybackControlsStrip` and `CaptionActions` (1,017 → 774 lines).
Every extracted composable is `internal`; only `EditorScreen` stays public.

Total suite after this addendum: **268 tests, 0 failures**.

## Addendum — engine→UI wiring, beat sync, ripple delete, overlay drag, pinch (2026-11-06, later)

The engine modules above were already tested, but several had no production
caller. This pass wired them into the editor and added gesture-level tests.

### Wired

| Wiring | Path |
|---|---|
| Beat sync | `EditorViewModel.snapBeats` re-detects beats whenever the first audio clip changes (`collectLatest`). `PlaybackControlsStrip`'s beat button calls `autoCutToBeat()`, and the timeline feeds the same beats into `SnapEngine.collect` as the lowest-priority magnets (clip edges still win). |
| Beat mapping | `BeatGrid.timelineBeats` converts source-file timestamps to project time through the audio clip's timeline start, trim and speed; beats outside the played window are dropped, so magnets stay aligned after trims or speed changes. |
| Ripple delete | `deleteSelectedClip()` now goes through `TimelineEdits.delete`: main track `ripple = true` closes the gap, overlay tracks `ripple = false` leave the remaining overlays in place. |
| Overlay drag | `TimelineOverlayTrack` long-press-drag reports a new start time; `EditorViewModel.moveOverlayClip` runs `SnapEngine.magneticStart` against the other overlays and clamps to `0..totalDuration - duration`. |
| Pinch zoom | Two-finger pinch is handled on `PointerEventPass.Initial` (2+ pointers only, changes consumed); a single finger still pans. Clamp 0.5×–3.0×, matching `setTimelineZoom`. |
| Edge auto-scroll | `TimelineAutoScroll` is shared by the playhead needle and the ruler scrub, so both can walk past the viewport edge during a drag. |

### Tests

- `EditorWorkflowTest` +3: main-track ripple delete, overlay lift delete, overlay move (magnetic push-out + clamp).
- `PolicyPresetAutoEditTest` +3: `BeatGrid.timelineBeats` maps timeline start/trim/speed, half-speed spread, and empty/zero-length guards.
- `TimelineGestureTest` (new, Robolectric + Compose UI test): 4/4 pass — ruler tap seeks, playhead drag seeks forward, two-finger pinch reports a zoom, snap threshold is 6 frames at the ruler density.

Total suite after this addendum: **278 tests, 0 failures** (29 suites).
CI (`.github/workflows/ci.yml`) now runs lint, unit tests and `assembleDebug`
without `--offline`, so a fresh runner can resolve dependencies.

Still environment-blocked: no device/emulator for instrumented runs, no EGL/GLES
surface for the GL compositor, no Bangla font binaries or ASR model.

## Addendum — real multi-track timeline, inline trim, track controls (2026-11-06, evening)

### Model
- `VideoTrack` gained `isMuted`, `isLocked`, `isSolo`; `MediaClip` gained `isMuted`;
  `VideoProject` gained main-track `isMainTrackMuted/isLocked/isSolo` plus
  `TimelineTrackIds.MAIN`, `trackIdForClip`, `isTrackLocked*` helpers.
- `ProjectJson` persists every new field, so a saved project reopens with the
  same track state (round-trip test covers it).

### Engine
| Module | Covers |
|---|---|
| `ClipTrimHandle` | Inline edge trims: frame-quantised source trim for constant-speed clips; velocity curves are refused (the Edit Clip sheet owns them); minimum duration clamp. |
| `TimelineGeometry.OVERLAY_TRACK_HEIGHT_DP` | The one row height used for cross-track drop maths. |
| `BeatGrid.cutOffsets` | Which local beats are safe to cut at: both edges need `ClipSplit.MIN_SPLIT_MS`, dense beat lists abort instead of shredding a clip. |

### UI
- The timeline is genuinely multi-track now: ruler → main video row → **one row per overlay track** → audio → text → FX. The old flattened `flatMap` overlay row is gone.
- The fixed left gutter (`TimelineTrackHeaders`) is one real row per track: mute, lock, solo for main/overlay, mute-all for audio. Buttons call back into the project, not a local `remember`.
- Inline trim handles: the selected clip gets draggable start/end handles; the delta converts to milliseconds and `EditorViewModel.trimSelectedClipEdge` applies `ClipTrimHandle`.
- Overlay long-press drag now reports a **track shift** (one overlay row = one track change), which `EditorViewModel.moveClipToTrack` resolves in the target track.
- The ruler draws detected beats as ticks, and a snap guide line appears while scrubbing.

### Audio routing
- `ProjectCompositionFactory.videoVoiceGain` resolves the real gain: clip mute, track mute and video-track solo (main + overlay) all collapse to one decision, so preview and export cannot disagree.

### Auto-cut
- `autoCutToBeat()` reuses the detected/mapped beats and then cuts through `ClipSplit` instead of hand-rolled `trimStartMs` arithmetic, so speed, keyframes and transition ownership survive.

### Tests
- `ClipTrimHandleTest` (7): end/start handle, frame quantisation, 2× speed source distance, minimum clamp, no-op, velocity-curve refusal.
- `PolicyPresetAutoEditTest` +2: `BeatGrid.cutOffsets` edge/inside filtering and dense-beat abort.
- `EditorWorkflowTest` +5: track mute/lock/solo persistence, locked-track guards, cross-track move, inline trim end result, locked-main split refusal.
- `ProjectJsonTest` +1: track flags and clip mute survive the JSON round trip.
- `TimelineTrackControlsTest` (new, Robolectric Compose): mute/lock/solo header taps report the right control.

Total suite after this addendum: **294 tests, 0 failures** (31 suites). Debug APK rebuilt (26.22 MB).

Still environment-blocked: no device/emulator for instrumented runs, no EGL/GLES
surface for the GL compositor, no Bangla font binaries or ASR model.
