# P1–P3 engine layer

Date: 2026-11-04

The P1/P2/P3 roadmap from `README.md` splits into two halves:

* **Decisions and maths** — pure Kotlin, runs on the JVM, covered by unit tests.
* **Execution** — EGL/GLES, MediaCodec, CameraX, bundled fonts and AI model
  files. None of that can run in this environment (no GPU surface, no device, no
  model binaries), so it is listed at the bottom as wiring work, not as done.

This document covers the first half: the modules added to `com.example.engine`,
the wiring that now reaches the editor UI, and the work that still needs a GPU
surface, a camera or model files. Total suite is **256 tests, 0 failures**.

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
| Free drag across tracks | Compose gesture work | Main-track long-press drag-reorder wired 2026-11-04; cross-track placement pending |
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
