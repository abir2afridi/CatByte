# CatByte Architecture Documentation

**Version:** 1.0
**Last Updated:** 2026-10-31
**Status:** DRAFT - Needs to be updated after render-engine spike decision

---

## 1. High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              CATBYTE VIDEO EDITOR                           │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐    ┌──────────┐  │
│  │   :feature:* │    │   :feature:* │    │   :feature:* │    │  :data   │  │
│  │   (UI)       │    │   (UI)       │    │   (UI)       │    │ (Room,   │  │
│  └──────┬───────┘    └──────┬───────┘    └──────┬───────┘    └────┬─────┘  │
│         │                   │                   │            │            │
│         └───────────────────┼───────────────────┘            │            │
│                             ▼                                │            │
│  ┌─────────────────────────────────────────────────────────────┐            │
│  │                    :core:model (Pure Kotlin)                 │            │
│  │  Project, Timeline, Clip, Transform, Keyframe,               │            │
│  │  Commands, Math, Serialization, TimeUs, Rational             │            │
│  └────────────────────────────┬────────────────────────────────┘            │
│                               │                                             │
│              ┌────────────────┼────────────────┐                          │
│              ▼                ▼                ▼                          │
│  ┌──────────────────┐ ┌──────────────┐ ┌──────────────┐                  │
│  │  :core:render    │ │ :engine:gl   │ │ :engine:audio  │                  │
│  │  RenderGraph,    │ │ EGL/GLSL     │ │ Mixer,         │                  │
│  │  RenderGraphBuilder,                    │ Waveform,    │                  │
│  │  Commands        │ │ Compositor,    │ │ Effects        │                  │
│  │                  │ │ Shader Library │ │                │                  │
│  └────────┬─────────┘ └──────┬─────────┘ └──────┬───────┘                  │
│           │                  │                  │                          │
│           └──────────────────┼──────────────────┘                          │
│                              ▼                                             │
│  ┌─────────────────────────────────────────────────────────────┐           │
│  │  :engine:playback (Preview)          :engine:export (Export)│           │
│  │  - DecoderPool                    - MediaCodec Encoder      │           │
│  │  - Proxy Generator                - MediaMuxer              │           │
│  │  - CompositionPlayer              - Validation              │           │
│  │  - Audio Clock                    - Foreground Service      │           │
│  └─────────────────────────────────────────────────────────────┘           │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Single Source of Truth: RenderGraph

### Core Principle
> **One `Project` document → `RenderGraphBuilder` → `RenderGraph` (immutable description of what every frame and audio sample must contain at time T). BOTH preview and export consume the same RenderGraph. No separate "export logic".**

### RenderGraph Structure
```kotlin
@Serializable
data class RenderGraph(
    val formatVersion: Int = 1,
    val projectId: String,
    val canvas: Canvas,
    val fps: Rational,
    val durationUs: Long,
    val videoTracks: List<VideoTrackGraph>,
    val audioTracks: List<AudioTrackGraph>,
    val globalEffects: List<EffectGraph>,
    val metadata: RenderMetadata
)

@Serializable
data class VideoTrackGraph(
    val trackId: String,
    val zOrder: Int,
    val clips: List<ClipGraph>,
    val transform: TrackTransform?
)

@Serializable
data class ClipGraph(
    val clipId: String,
    val assetId: String,
    val timelineInUs: Long,
    val timelineOutUs: Long,
    val sourceInUs: Long,
    val sourceOutUs: Long,
    val transform: TransformGraph,
    val opacity: Float,
    val blendMode: BlendMode,
    val speed: SpeedGraph,
    val effects: List<EffectGraph>,
    val keyframes: List<KeyframeGraph>,
    val mask: MaskGraph?
)

@Serializable
data class TransformGraph(
    val position: Vec2,
    val scale: Vec2,
    val rotationDeg: Float,
    val anchor: Vec2,
    val flipH: Boolean,
    val flipV: Boolean,
    val crop: Rect,
    val fit: FitMode
)

@Serializable
sealed class EffectGraph {
    @Serializable data class ColorGrade(
        val brightness: Float,
        val contrast: Float,
        val saturation: Float,
        val redGain: Float,
        val greenGain: Float,
        val blueGain: Float
    ) : EffectGraph()
    @Serializable data class LUT(
        val lutData: ByteArray,
        val intensity: Float
    ) : EffectGraph()
    @Serializable data class Transition(
        val type: TransitionType,
        val durationUs: Long,
        val params: Map<String, Float>,
        val incomingClipId: String,
        val outgoingClipId: String
    ) : EffectGraph()
    @Serializable data class Overlay(
        val assetId: String,
        val timelineInUs: Long,
        val timelineOutUs: Long,
        val transform: TransformGraph,
        val blendMode: BlendMode
    ) : EffectGraph()
    @Serializable data class CustomGlsl(
        val shaderName: String,
        val uniforms: Map<String, Float>
    ) : EffectGraph()
}
```

### RenderGraphBuilder
```kotlin
class RenderGraphBuilder {
    fun build(project: Project, assets: Map<String, MediaAsset>): Result<RenderGraph> {
        // 1. Validate project invariants
        // 2. Compute timeline duration
        // 3. Build video tracks (resolve overlaps, transitions)
        // 4. Build audio tracks (mixing graph)
        // 5. Build global effects (watermark, canvas background)
        // 6. Serialize to immutable RenderGraph
    }
}
```

---

## 3. Timing Model

### Time Representation
- **All internal times: `Long` microseconds (`TimeUs`)**
- **Frame-accurate: `TimeUs = frameIndex × (1_000_000 / fps)`**
- **Rational fps: `Rational(numerator, denominator)` e.g., 30000/1001 for 29.97fps**

```kotlin
@JvmInline
value class TimeUs(private val value: Long) : Comparable<TimeUs> {
    companion object {
        fun fromFrames(frame: Long, fps: Rational): TimeUs =
            TimeUs((frame * 1_000_000 * fps.denominator) / fps.numerator)
        fun fromMs(ms: Long): TimeUs = TimeUs(ms * 1000)
    }
    
    fun toFrames(fps: Rational): Long =
        (value * fps.numerator) / (1_000_000 * fps.denominator)
    
    fun toMs(): Long = value / 1000
    
    fun snapToFrame(fps: Rational): TimeUs = 
        TimeUs.fromFrames(toFrames(fps), fps)
}

@Serializable
data class Rational(
    val numerator: Int,
    val denominator: Int = 1
) {
    fun toFloat(): Float = numerator.toFloat() / denominator
    fun invert(): Rational = Rational(denominator, numerator)
}
```

### Frame Snapping
```kotlin
fun TimeUs.snapToFrame(fps: Rational): TimeUs {
    val frame = toFrames(fps)
    return TimeUs.fromFrames(frame, fps)
}

fun TimeUs.alignToFrameBoundary(fps: Rational, direction: RoundingDirection): TimeUs {
    return when (direction) {
        RoundingDirection.DOWN -> snapToFrame(fps)
        RoundingDirection.UP -> {
            val snapped = snapToFrame(fps)
            if (snapped == this) this else snapped + TimeUs.fromFrames(1, fps)
        }
        RoundingDirection.NEAREST -> {
            val frame = toFrames(fps)
            val remainder = value % (1_000_000 * fps.denominator / fps.numerator)
            if (remainder < (1_000_000 * fps.denominator / fps.numerator) / 2) {
                snapToFrame(fps)
            } else {
                snapToFrame(fps) + TimeUs.fromFrames(1, fps)
            }
        }
    }
}
```

---

## 4. Rendering Strategy (To Be Decided by Spike)

### Spike A: Media3 Transformer + Composition + Custom GlEffect
**Pros:**
- Leverages Media3's decode/encode plumbing
- Hardware acceleration built-in
- Handles codec selection, fallback

**Cons:**
- Limited to Media3's effect system
- Custom GLSL requires `GlEffect`/`GlShaderProgram` which is `@UnstableApi`
- Preview uses ExoPlayer (separate path from export)
- Two-frame transitions difficult

### Spike B: Custom Pipeline (MediaExtractor + MediaCodec + GLSL)
**Pros:**
- Full control over GLSL shaders
- Preview and export share exact same shaders
- Two-frame transitions natural in compositor
- Can implement any effect

**Cons:**
- More code to write/maintain
- Must handle codec selection, fallback, 10-bit/HDR
- Decoder pool management complex
- Audio mixer + AAC encoder + MediaMuxer plumbing

### Evaluation Criteria (from plan)
| Criterion | Spike A | Spike B |
|-----------|---------|---------|
| Multi-layer PiP | Limited | Full |
| Per-layer transforms/opacity | Via MatrixTransformation | Full |
| Transitions (2 frames) | Difficult | Natural |
| Keyframes | Media3 keyframes | Full control |
| Speed ramps | SpeedParameters | Full control |
| Reverse | Media3 reverse | Full control |
| Text/sticker overlays | OverlayEffect | Full control |
| Frame-accurate seek | ExoPlayer | Custom |
| A/V sync | ExoPlayer | Custom audio clock |
| 60fps | Yes | Yes |
| HDR/10-bit input | Media3 support | Manual |
| Memory | Media3 managed | Manual pool |
| Effort | Lower | Higher |

**Expected Outcome:** Media3 for decode/encode plumbing where it fits, custom GL compositor for everything Media3 cannot express. Whatever picked, preview and export must share same GLSL shaders and same RenderGraph.

---

## 5. Decoder Pool

```kotlin
class DecoderPool(
    private val maxInstances: Int = 4,
    private val context: Context
) {
    private val available = mutableListOf<MediaCodec>()
    private val inUse = mutableMapOf<Int, MediaCodec>()
    private val priorityQueue = PriorityQueue<DecodeRequest>()
    
    suspend fun acquire(request: DecodeRequest): MediaCodec {
        // Priority: preview real-time > export > proxy generation
        // Eviction: LRU within same priority
        // Fallback: software decoder if hardware exhausted
    }
    
    fun release(codec: MediaCodec) { ... }
    
    fun preRoll(nextClip: MediaClip) { ... }
}
```

---

## 6. Proxy/Preview Media

```kotlin
class ProxyGenerator(
    private val context: Context,
    private val decoderPool: DecoderPool
) {
    // Auto-generate for clips that are:
    // - 4K (width >= 3840)
    // - HEVC/10-bit
    // - VFR (variable frame rate)
    // - Long GOP (> 1 second GOP)
    
    // Output: low-res (720p max), short-GOP (1s or all-intra), H.264 baseline
    // Store in app cache with LRU eviction
    // Export ALWAYS uses originals
}
```

---

## 7. Color Pipeline (Working Color Space)

### Input Detection
```kotlin
data class ColorMetadata(
    val primaries: ColorPrimaries,
    val transfer: TransferFunction,
    val matrix: MatrixCoefficients,
    val range: ColorRange,
    val bitDepth: Int
)
```

### Working Pipeline Decision
- **Option 1: Linear-light blending** (physically correct, requires tonemapping for SDR output)
- **Option 2: Gamma-space blending** (simpler, matches most LUTs, but incorrect for cross-dissolves)

**Decision TBD after spike.** Document in DECISIONS.md.

### Output Tagging
- SDR export: BT.709 + sRGB transfer + FULL range
- HDR export (if supported): BT.2020 + HLG/PQ + FULL range
- Always write correct SEI/colr/MDCV boxes

---

## 8. Module Boundaries

| Module | Dependencies | Responsibility |
|--------|--------------|----------------|
| `:core:model` | None (pure Kotlin) | Domain model, math, commands, serialization |
| `:core:render` | `:core:model` | RenderGraph, RenderGraphBuilder |
| `:engine:gl` | `:core:model`, `:core:render` | EGL context, GLSL shaders, texture pool, compositor |
| `:engine:playback` | `:engine:gl`, `:core:render` | DecoderPool, proxy gen, CompositionPlayer, audio clock |
| `:engine:export` | `:engine:gl`, `:core:render` | Encoder, muxer, validation, foreground service |
| `:engine:audio` | `:core:model` | Mixer, waveform, effects, RNNoise (if licensed) |
| `:data` | `:core:model` | Room, repositories, media import, thumbnail/waveform cache |
| `:feature:*` | `:core:model`, `:data`, `:engine:playback` | UI screens, tool sheets |
| `:testing` | All | Fixtures, golden-frame utilities |

---

## 9. Data Flow

### Preview Path
```
Project + Assets
      │
      ▼
RenderGraphBuilder → RenderGraph
      │
      ▼
CompositionPlayer (consumes RenderGraph)
      │
      ├──► DecoderPool → Video Frames → GL Compositor → SurfaceTexture
      │
      └──► Audio Mixer → AudioTrack / OpenSL ES
```

### Export Path
```
Project + Assets
      │
      ▼
RenderGraphBuilder → RenderGraph
      │
      ▼
ExportPipeline (consumes SAME RenderGraph)
      │
      ├──► Frame Renderer (GL Compositor) → MediaCodec Encoder (input surface)
      │
      └──► Audio Mixer → PCM → AAC Encoder → MediaMuxer
      │
      ▼
Validation (MediaExtractor) → MediaStore
```

---

## 10. Persistence Schema

### Room Entities
```kotlin
@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val title: String,
    val payload: String,  // JSON serialized Project
    val aspectRatio: String,
    val resolution: String,
    val fps: Int,
    val paperStyle: String,
    val isFavorite: Boolean,
    val lastModified: Long
)

@Entity(tableName = "media_assets")
data class MediaAssetEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val uriString: String,
    val mimeType: String,
    val width: Int,
    val height: Int,
    val durationUs: Long,
    val rotationDeg: Int,
    val hasAudio: Boolean,
    val thumbnailPath: String?,
    val waveformPath: String?,
    val proxyPath: String?
)
```

### Project JSON Format
```json
{
  "formatVersion": 1,
  "id": "proj_abc123",
  "name": "My Video",
  "createdAt": 1699999999999,
  "modifiedAt": 1699999999999,
  "canvas": { "width": 1080, "height": 1920, "background": { "type": "color", "color": "#000000" } },
  "fps": { "numerator": 30, "denominator": 1 },
  "tracks": [
    { "id": "track_main", "type": "video", "clips": [...], "zOrder": 0 },
    { "id": "track_overlay1", "type": "video", "clips": [...], "zOrder": 1 }
  ],
  "markers": [],
  "assets": { "asset_1": { "id": "asset_1", "uri": "content://...", "width": 1920, ... } }
}
```

---

## 11. Testing Architecture

### Test Pyramid
```
                    ┌─────────────┐
                    │  E2E / UI   │  ← Instrumented (emulator/device)
                    │  (Compose)  │
               ┌────┴────────────┴────┐
               │   Integration        │  ← Instrumented (export, playback, import)
               │   (DecoderPool,      │
               │    RenderGraph,      │
               │    ExportPipeline)   │
          ┌────┴──────────────────────┴────┐
          │        Unit (JVM)              │  ← Fast, no Android deps
          │  - Timeline math               │
          │  - Command undo/redo           │
          │  - Keyframe interpolation      │
          │  - Serialization round-trip    │
          │  - RenderGraphBuilder          │
          │  - Color math                  │
          └────────────────────────────────┘
```

### Golden Frame Tests
```kotlin
@Test
fun `transition mid-frame matches golden`() {
    val renderGraph = createTransitionGraph(TransitionType.DISSOLVE, 0.5f)
    val previewFrame = previewRenderer.renderFrame(renderGraph, 500_000)  // 500ms
    val exportFrame = exportRenderer.renderFrame(renderGraph, 500_000)
    
    assertSsim(previewFrame, goldenFrame) > 0.98
    assertSsim(exportFrame, goldenFrame) > 0.98
    assertSsim(previewFrame, exportFrame) > 0.99
}
```

---

## 12. Open Questions / Decisions Needed

1. **Render Engine:** Spike A vs B decision (see SPIKE_RENDER_ENGINE.md)
2. **Color Pipeline:** Linear-light vs gamma-space blending
3. **Undo System:** Command pattern vs immutable snapshots with structural sharing
4. **Keyframe Storage:** Clip-local vs timeline-global
5. **Proxy Format:** H.264 all-intra vs 1s GOP, resolution ladder
6. **Audio Mixer:** Custom vs Media3's AudioProcessor chain
7. **Bangla Fonts:** Which OFL fonts to bundle (size vs coverage)
8. **LUT Format:** .cube only or also PNG LUT (64²)

---

## 13. Version History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-10-31 | Assistant | Initial draft based on PLAN.md |
