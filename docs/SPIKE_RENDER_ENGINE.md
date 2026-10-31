# Render Engine Spike - CatByte Video Editor

**Objective:** Compare Spike A (Media3 Transformer + Composition + GlEffect) vs Spike B (Custom MediaExtractor + MediaCodec + GLSL) to decide rendering architecture.

**Status:** 🔴 NOT STARTED - Must complete before P1 gate

---

## Spike A: Media3 Transformer + Composition + Custom GlEffect

### Approach
Use Media3's `Transformer` with `Composition` API. Custom effects via `GlEffect`/`GlShaderProgram` (`@UnstableApi`). Preview uses `ExoPlayer` / `CompositionPlayer`.

### Implementation Tasks
1. [ ] Basic Composition with multiple video tracks
2. [ ] Per-clip effects (color grade, transform) via `Effect` list
3. [ ] Transitions using two-frame compositing in custom `GlEffect`
4. [ ] Text/sticker overlays via `OverlayEffect` + `TextureOverlay`
5. [ ] Speed ramps via `SpeedParameters` + custom `SpeedProvider`
6. [ ] Keyframe animation via custom `GlEffect` with uniform interpolation
7. [ ] Preview: `CompositionPlayer` or `ExoPlayer` with same effect chain
8. [ ] Export validation

### Test Cases (from plan)
| Test | Expected Result |
|------|-----------------|
| Multi-layer PiP (3+ layers) | All layers composite correctly |
| Per-layer transforms/opacity | MatrixTransformation works |
| Transitions (dissolve, slide, zoom) | Two frames blended in shader |
| Keyframes (position, scale, opacity) | Smooth interpolation |
| Speed ramps (0.1x-100x, reverse) | Frame-accurate, audio pitch preserved |
| Text/sticker overlays | Rendered at correct timeline position |
| Frame-accurate seek | < 100ms latency (proxy) |
| A/V sync | Drift < 1 frame over 60s |
| 60fps playback | No dropped frames |
| HDR/10-bit input | Correct tonemapping or passthrough |
| Memory | Bounded, no OOM on 4K |

### Pros
- Leverages Media3's battle-tested decode/encode
- Hardware acceleration automatic
- Codec selection/fallback built-in
- Less code to maintain

### Cons
- `GlEffect`/`GlShaderProgram` are `@UnstableApi` - may break
- Preview (ExoPlayer) ≠ Export (Transformer) code paths
- Two-frame transitions require custom GLSL in unstable API
- Limited to Media3's effect graph model
- Cannot easily share shaders between preview and export

---

## Spike B: Custom Pipeline (MediaExtractor + MediaCodec + GLSL)

### Approach
Custom decoder pool → `SurfaceTexture`/external OES textures → GLSL compositor → `MediaCodec` encoder (input surface) + custom audio mixer → AAC encoder → `MediaMuxer`.

### Implementation Tasks
1. [ ] `DecoderPool` with priority/eviction/pre-roll
2. [ ] `MediaExtractor` → `MediaCodec` decoder → `SurfaceTexture`
3. [ ] EGL context + GLSL shader library (compositor, transitions, effects)
4. [ ] Texture pool for OES textures
5. [ ] Custom audio mixer (PCM, resample, volume, fades, limiter)
6. [ ] `MediaCodec` encoder with input surface
7. [ ] AAC encoder (`MediaCodec` audio encoder)
8. [ ] `MediaMuxer` for MP4 output
9. [ ] Preview: Same GLSL compositor, render to `SurfaceView`/`TextureView`
10. [ ] Export validation

### Test Cases (from plan)
| Test | Expected Result |
|------|-----------------|
| Multi-layer PiP (3+ layers) | All layers composite correctly |
| Per-layer transforms/opacity | Full GLSL control |
| Transitions (dissolve, slide, zoom) | Two frames in shader |
| Keyframes | Full interpolation control |
| Speed ramps | Frame-accurate, custom frame blending |
| Text/sticker overlays | Rendered via GLSL or bitmap overlay |
| Frame-accurate seek | < 100ms (proxy) |
| A/V sync | Custom audio clock, drift < 1 frame over 60s |
| 60fps playback | No dropped frames |
| HDR/10-bit input | Manual tonemapping/passthrough |
| Memory | Explicit texture/bitmap pools |

### Pros
- **Full control** over GLSL shaders
- **Preview and export share exact same shaders** → guaranteed parity
- Two-frame transitions natural in compositor
- Can implement any effect (glitch, RGB split, VHS, etc.)
- No unstable API dependency
- Decoder pool fully controllable

### Cons
- Significant code to write/maintain (~5000+ lines)
- Must handle codec selection, capability query, fallback ladder
- 10-bit/HDR handling manual
- Audio AAC encoding + muxing plumbing
- Decoder pool edge cases (format changes, DRM, secure decoders)
- More surface for bugs

---

## Evaluation Matrix (from plan)

| Criterion | Weight | Spike A Score (1-5) | Spike B Score (1-5) | Notes |
|-----------|--------|---------------------|---------------------|-------|
| Multi-layer PiP | Critical | | | |
| Per-layer transforms/opacity | Critical | | | |
| Transitions (2-frame) | Critical | | | |
| Keyframes | High | | | |
| Speed ramps | High | | | |
| Reverse | Medium | | | |
| Text/sticker overlays | High | | | |
| Frame-accurate seek | High | | | |
| A/V sync | Critical | | | |
| 60fps | Critical | | | |
| HDR/10-bit input | Medium | | | |
| Memory management | High | | | |
| Implementation effort | Medium | | | |
| Maintenance burden | Medium | | | |
| Preview/Export Parity | **Blocker** | | | Must share shaders + RenderGraph |

### Scoring Guide
- 5 = Trivial / Native support
- 4 = Straightforward with Media3 APIs
- 3 = Possible with workarounds
- 2 = Difficult, unstable APIs, or significant custom code
- 1 = Nearly impossible / fundamental limitation

---

## Expected Outcome (from plan)

> "Expected outcome: Media3 for decode/encode plumbing where it fits, custom GL compositor for everything Media3 cannot express. Whatever you pick, preview and export must share the same GLSL shaders and the same RenderGraph."

### Hybrid Approach (Likely Decision)
- **Decode/Encode:** Media3 (`MediaExtractor`, `MediaCodec`, `MediaMuxer`, AAC encoder)
- **Compositor:** Custom GLSL (shared by preview and export)
- **RenderGraph:** Single source of truth consumed by both

---

## Spike Execution Plan

### Week 1: Spike A
- [ ] Day 1-2: Basic Composition export (3 tracks, color grade, transform)
- [ ] Day 3: Custom GlEffect for transition (dissolve)
- [ ] Day 4: OverlayEffect for text/sticker
- [ ] Day 5: CompositionPlayer preview vs Export comparison

### Week 2: Spike B
- [ ] Day 1-2: DecoderPool + basic decoder → SurfaceTexture
- [ ] Day 3: GLSL compositor (alpha blend, transform matrix)
- [ ] Day 4: Two-frame transition shader
- [ ] Day 5: Encoder + Muxer + Audio mixer

### Week 3: Evaluation
- [ ] Run all test cases on both spikes
- [ ] Measure: build time, APK size, memory, CPU, battery
- [ ] Document findings in this file
- [ ] Make decision → record in DECISIONS.md

---

## Measurement Scripts

```bash
# Build time
./gradlew :app:assembleDebug --profile

# APK size
ls -lh app/build/outputs/apk/debug/app-debug.apk

# Memory (during 4K playback)
adb shell dumpsys meminfo com.example.catbyte

# CPU/Frame time (systrace)
adb shell perfetto -c - --txt -o trace.perfetto-trace <<'EOF'
duration_ms: 10000
buffers: { size_kb: 65536 }
data_sources: { config { name: "android.surfaceflinger" } }
data_sources: { config { name: "android.gpu" } }
EOF

# Export validation
./gradlew :app:connectedDebugAndroidTest -PtestClass=ExportValidationTest
```

---

## Results (To Be Filled After Spike)

### Spike A Results
| Metric | Value |
|--------|-------|
| Multi-layer PiP | PASS/FAIL |
| Transitions (2-frame) | PASS/FAIL |
| Keyframes | PASS/FAIL |
| Speed ramps | PASS/FAIL |
| Preview/Export SSIM | X.XX |
| Memory (4K, 60s) | XX MB |
| Export time (1min 1080p30) | X.Xs (X.Xx realtime) |
| Build time | XXs |
| APK size increase | +XX MB |

### Spike B Results
| Metric | Value |
|--------|-------|
| Multi-layer PiP | PASS/FAIL |
| Transitions (2-frame) | PASS/FAIL |
| Keyframes | PASS/FAIL |
| Speed ramps | PASS/FAIL |
| Preview/Export SSIM | X.XX |
| Memory (4K, 60s) | XX MB |
| Export time (1min 1080p30) | X.Xs (X.Xx realtime) |
| Build time | XXs |
| APK size increase | +XX MB |

---

## Decision

**Chosen Approach:** TBD after spike

**Rationale:** TBD

**Recorded in:** DECISIONS.md (DEC-010)

---

## Next Steps After Decision

1. Record decision in DECISIONS.md
2. Update ARCHITECTURE.md with chosen approach
3. Begin multi-module restructure (P0)
4. Implement RenderGraph + RenderGraphBuilder
5. Start P1 vertical slice
