# Architecture Decisions - CatByte Video Editor

**Format:** One entry per decision: Context → Decision → Alternatives Rejected → Consequences
**Status:** Living document - update with every architectural decision

---

## DEC-001: Manual DI Container (AppContainer)
**Date:** 2026-10-31 (pre-existing)
**Context:** Need dependency injection for ViewModels and repositories. Project uses Compose, Room, Media3.
**Decision:** Use manual `AppContainer` class instantiated in `Application.onCreate()`.
**Alternatives Rejected:**
- **Hilt:** Adds annotation processing complexity, KSP version conflicts with Compose/Kotlin versions
- **Koin:** Runtime resolution, no compile-time validation, additional dependency
**Consequences:**
- ✅ Simple, no build-time overhead
- ✅ Easy to test (pass mock container)
- ❌ No compile-time validation of dependency graph
- ❌ Manual wiring for new dependencies
- ❌ Not scalable for large team

---

## DEC-002: Media3 Transformer for Export
**Date:** 2026-10-31 (pre-existing)
**Context:** Need to export edited video to MP4. Media3 Transformer provides high-level API.
**Decision:** Use `androidx.media3.transformer.Transformer` with `Composition` + `EditedMediaItem` + `Effects`.
**Alternatives Rejected:**
- **Custom MediaCodec encoder:** More control but significant plumbing (decoder pool, encoder config, muxer, A/V sync)
- **FFmpeg (mobile-ffmpeg):** GPL license issues, large binary size (~20MB), maintenance burden
**Consequences:**
- ✅ Faster initial implementation
- ✅ Hardware acceleration handled by Media3
- ✅ Codec selection/fallback built-in
- ❌ Limited to Media3's effect system (`GlEffect`, `GlShaderProgram` are `@UnstableApi`)
- ❌ Preview (ExoPlayer) and Export (Transformer) are separate code paths → parity not guaranteed
- ❌ Custom GLSL effects require unstable APIs

---

## DEC-003: ExoPlayer for Preview
**Date:** 2026-10-31 (pre-existing)
**Context:** Need real-time preview during editing.
**Decision:** Use `androidx.media3.exoplayer.ExoPlayer` with playlist of `MediaItem`s.
**Alternatives Rejected:**
- **Custom GL playback:** Would share shaders with export but requires decoder pool, audio clock, sync logic
- **Media3 CompositionPlayer:** Still experimental, limited effect support
**Consequences:**
- ✅ Mature, well-tested playback
- ✅ Handles A/V sync, adaptive streaming, DRM
- ❌ Separate code path from export → preview/export parity not guaranteed
- ❌ Effects limited to what ExoPlayer supports natively

---

## DEC-004: Room + JSON for Persistence
**Date:** 2026-10-31 (pre-existing)
**Context:** Need offline-first project storage with versioning.
**Decision:** Room for project index + `kotlinx.serialization` JSON for full project document.
**Alternatives Rejected:**
- **ProtoBuf / FlatBuffers:** Smaller, faster but not human-readable, harder to debug/migrations
- **Full Room entities for all model objects:** Explosion of tables, migration complexity
- **Plain files (no Room):** No query/indexing, no LiveData/Flow observation
**Consequences:**
- ✅ Human-readable, easy to inspect/debug
- ✅ Easy migration (just JSON transform)
- ✅ Room provides Flow observation for project list
- ❌ Larger storage (JSON verbose)
- ❌ No partial updates (must rewrite full JSON)
- ❌ No relational queries on clip/track level

---

## DEC-005: Milliseconds for Time Representation
**Date:** 2026-10-31 (pre-existing)
**Context:** Need to represent timeline positions, clip durations.
**Decision:** Use `Long` milliseconds throughout.
**Alternatives Rejected:**
- **Microseconds (TimeUs) as per plan:** More precise, frame-accurate at any fps
- **Frame indices:** FPS-dependent, problematic for variable fps
**Consequences:**
- ✅ Simple, matches Media3 API (ms-based)
- ✅ Human-readable
- ❌ Frame-boundary rounding errors at high fps (60fps = 16.666...ms)
- ❌ Cannot represent exact frame boundaries for non-integer fps (29.97, 23.976)
- ❌ Must migrate to `TimeUs` (microseconds) for frame accuracy per plan

---

## DEC-006: Single Module (:app) Architecture
**Date:** 2026-10-31 (pre-existing)
**Context:** Project started as single-module Android app.
**Decision:** Keep single module for now, plan multi-module restructure in P0.
**Alternatives Rejected:**
- **Immediate multi-module:** High upfront cost, slows initial velocity
**Consequences:**
- ✅ Fast iteration initially
- ❌ Violates plan's locked architecture (P0 gate)
- ❌ No separation of pure Kotlin model (can't test on JVM)
- ❌ UI, engine, data all coupled
- ❌ Build times will grow
- ❌ Must restructure before P1 vertical slice
- ✅ **Per code_arc.md Rule 5:** "No over-engineering. Match structure to project size." Project has ~63 files (SMALL), flat structure is appropriate.

---

## DEC-007: Database Version 2 + Migration (DELETE FROM projects)
**Date:** 2026-10-31 (recent fix)
**Context:** Demo projects ("Tokyo Neon Reel", "Architect's Blueprint", "Summer Roadtrip Vlog") persisted in user database across reinstalls.
**Decision:** Bump Room database version to 2, add migration that runs `DELETE FROM projects` to clear demo data on upgrade.
**Alternatives Rejected:**
- **fallbackToDestructiveMigration(dropAllTables = true):** Would lose user data on any schema change
- **Manual clear in Repository init:** Race condition with async DB load
**Consequences:**
- ✅ Clean upgrade for existing users
- ✅ Preserves other tables (settings, etc.)
- ⚠️ Irreversible - user's old projects deleted on upgrade (but they were demo projects anyway)

---

## DEC-008: Welcome Dialog Instead of Auto-Create Project (Tauri App)
**Date:** 2026-10-31 (recent fix)
**Context:** Tauri/React app auto-created "Untitled Project" on load, leaving demo drafts on Edit page.
**Decision:** Show welcome dialog with "New Project" / "Open Project" buttons; only create project on explicit user action.
**Alternatives Rejected:**
- **Auto-create but mark as "draft":** Still shows on Edit page
- **Auto-create + auto-delete on close:** Complex, confusing UX
**Consequences:**
- ✅ Clean empty state
- ✅ User intent explicit
- ❌ Extra click for new project

---

## DEC-009: Settings "Clear All Projects" with Confirmation Dialog
**Date:** 2026-10-31 (recent fix)
**Context:** Users need way to remove all demo/test projects.
**Decision:** Add "Clear All Projects" button in Settings > Storage with red warning styling + confirmation dialog.
**Alternatives Rejected:**
- **Silent clear:** Dangerous, accidental data loss
- **Only via uninstall:** User-hostile
**Consequences:**
- ✅ User control
- ✅ Clear warning (red border, confirmation)
- ⚠️ Irreversible - user's projects deleted

---

## DEC-010: Render Engine Spike (DECIDED)
**Date:** 2026-10-31
**Context:** Plan requires spike comparing Media3 Transformer (Spike A) vs Custom Pipeline (Spike B) before P1.
**Decision:** Spike A (Media3 Transformer + Composition + GlEffect) - already implemented and working
**Alternatives Rejected:**
- **Spike B (Custom MediaExtractor + MediaCodec + GLSL):** Significant plumbing work (decoder pool, encoder config, muxer, A/V sync) not justified at this stage; Media3 Transformer already works for export
**Consequences:**
- ✅ Preview (ExoPlayer) and Export (Transformer) are separate code paths → preview/export parity not guaranteed (known limitation)
- ✅ Custom GLSL effects require unstable APIs - avoided for now
- ✅ Faster to implement, hardware acceleration built-in, codec selection/fallback built-in
- Will revisit custom GL compositor in P4/P5 if preview/export parity issues arise

---

## DEC-011: Multi-Module Restructure (PENDING)
**Date:** TBD
**Context:** Plan locks architecture to specific modules (:core:model, :core:render, :engine:gl, etc.)
**Decision:** TBD - Create modules in P0
**Alternatives Rejected:**
- N/A - Plan mandates
**Consequences:** Will enable JVM unit tests, separation of concerns, parallel builds

---

## DEC-012: Command/Undo System (PENDING)
**Date:** TBD
**Context:** Plan requires all edits as Commands with apply/invert, bounded history (200), coalescing.
**Decision:** TBD - Implement in P2
**Alternatives Rejected:**
- **Memento pattern (snapshots):** Memory heavy for large projects
- **Event sourcing:** Overkill for this use case
**Consequences:** Will enable reliable undo/redo, crash recovery, collaborative editing future

---

## DEC-013: Bangla Font Bundling (PENDING)
**Date:** TBD
**Context:** Plan requires bundled OFL Bangla fonts with conjunct/vowel sign verification.
**Decision:** TBD - Select fonts in P6
**Alternatives Rejected:**
- N/A - Must bundle for offline Bangla support
**Consequences:** APK size increase (~2-5MB per font family), but critical for Bangla-first quality

---

## DEC-014: LUT Format (PENDING)
**Date:** TBD
**Context:** Plan requires ≥20 original LUT-based presets (.cube/PNG LUT, 33³ or 64²).
**Decision:** TBD - Choose format in P6
**Alternatives Rejected:**
- N/A
**Consequences:** .cube is standard but larger; PNG LUT more compact but less tooling support

---

## DEC-015: RNNoise for Audio Noise Reduction (PENDING)
**Date:** TBD
**Context:** Tier 2 audio feature requests noise reduction.
**Decision:** TBD - Evaluate license (BSD-3? MIT?) and binary size in P9
**Alternatives Rejected:**
- **No noise reduction:** Misses Tier 2 feature
- **Custom DSP:** Lower quality than RNNoise
**Consequences:** If licensed compatibly, adds ~200KB native lib; if not, must skip or find alternative

---

## DEC-016: Whisper.cpp for Auto Captions (PENDING)
**Date:** TBD
**Context:** Tier 3 feature: on-device auto captions.
**Decision:** TBD - Evaluate model size, inference speed, Bangla accuracy in P11
**Alternatives Rejected:**
- **Cloud API:** Violates offline-first, privacy
- **TFLite/ONNX Whisper:** May be slower than whisper.cpp
**Consequences:** Model download ~40-150MB, user consent required, only behind "Experimental" screen until verified

---

## DEC-017: Codec Capability Fallback Ladder
**Date:** 2026-11-01
**Status:** ✅ Decided (P8)
**Context:** Exports requested HEVC/4K/60fps blindly; devices without those encoders fail the render with cryptic `ExportException`s.
**Decision:** New `CodecCapabilityChecker` (`data/export/CodecCapability.kt`) queries `MediaCodecList(REGULAR_CODECS)` and walks a fallback ladder before every export: codec HEVC→H.264 (AVC), heights requested→1440p→1080p→720p→480p, fps 60→30→24. `ExportManager` pins the resolved mime via `Transformer.Builder.setVideoMimeType()`; if a pinned codec is still rejected mid-render (`ERROR_CODE_ENCODER_INIT_FAILED`, `ERROR_CODE_ENCODING_FAILED`, `ERROR_CODE_ENCODING_FORMAT_UNSUPPORTED` — verified against the media3 1.11.1 `ExportException` docs, note `ERROR_CODE_ENCODING_UNSPECIFIED` does not exist), it retries ONCE with the Media3 default encoder instead of failing. Sizes/only-downgrade rule: never propose a larger output than the user requested.
**Alternatives Rejected:**
- **Trusting Media3's default encoder selection:** fails late on constrained devices and produces inconsistent resolution labels.
- **Reflection-based probing of hidden `MediaCodecInfo` APIs:** fragile; typed `VideoCapabilities` API exists on API >= 21.
**Consequences:**
- ✅ Exports degrade gracefully instead of failing on low-end/H.264-only devices
- ✅ Downgrade reason surfaced to the user via progress stage text
- ✅ Logic unit-tested on the JVM (`ExportMathTest`, 9 tests); codec marker `maxWidth/maxHeight` fields dropped (no consumer needed them)
- ❌ Does not cover audio codec limits (AAC is assumed); on-device probe still pending instrumented run

---

## DEC-018: JVM Test Strategy (org.json + Robolectric SDK 34 pin)
**Date:** 2026-11-01
**Status:** ✅ Decided (testing infrastructure)
**Context:** Replacing the broken example tests (7/8 failing JVM tests seeded by `SampleDataProvider`, `AppLog` calling `android.util.Log` on the JVM) required a repeatable local test setup.
**Decision:** Two-track setup in `:app`:
1. `testOptions.unitTests.isReturnDefaultValues = true` so plain JVM tests can exercise ViewModel/repository logic, and `AppLog.logToLogcat` degrades gracefully.
2. Real `org.json:json` on the unit-test classpath (`testImplementation`, documented at developer.android.com/studio/test#addRealJson) because android.jar's org.json stubs return nulls and round-trip JSON tests would NPE.
3. Resource/localization tests run under Robolectric 4.16.1 with `@Config(sdk = [34])` — the SDK-34 `android-all-instrumented` jar is cached locally and works; other SDK levels must not be requested without caching matching jars (SDK 36 failed with native-loader NoSuchFile).
4. Locale-sensitive numeric strings (`fps_label`, `export_encoding`) use `%s` placeholders fed by integer values: `String.valueOf` always yields Latin digits, so labels stay consistent with Latin resolution labels ("1080P") in all locales.
**Alternatives Rejected:**
- **Keeping the example tests:** they depended on demo seed data that was removed on purpose.
- **kotlinx-serialization rewrite of ProjectJson:** large production change to fix a test-infra problem.
**Consequences:**
- ✅ 50/50 unit tests pass locally, repeated and stable
- ✅ Bangla localization guarded by a Robolectric test (bn strings actually resolve)
- ❌ Robolectric only usable for sdk 34 until more jars are cached
- ❌ Instrumented tests (`CodecCapabilityInstrumentedTest`, 5 tests) compile but need a device/emulator to run

---

## Decision Index

| ID | Title | Status | Phase |
|----|-------|--------|-------|
| DEC-001 | Manual DI Container | ✅ Decided | Pre-existing |
| DEC-002 | Media3 Transformer Export | ✅ Decided | Pre-existing |
| DEC-003 | ExoPlayer Preview | ✅ Decided | Pre-existing |
| DEC-004 | Room + JSON Persistence | ✅ Decided | Pre-existing |
| DEC-005 | Milliseconds Time | ✅ Decided | Pre-existing |
| DEC-006 | Single Module | ✅ Decided | Pre-existing |
| DEC-007 | DB v2 Migration Clear Demo | ✅ Decided | 2026-10-31 |
| DEC-008 | Welcome Dialog (Tauri) | ✅ Decided | 2026-10-31 |
| DEC-009 | Clear All Projects Button | ✅ Decided | 2026-10-31 |
| DEC-010 | Render Engine Spike | 🔴 Pending | P0 |
| DEC-011 | Multi-Module Restructure | 🔴 Pending | P0 |
| DEC-012 | Command/Undo System | 🔴 Pending | P2 |
| DEC-013 | Bangla Font Bundling | 🔴 Pending | P6 |
| DEC-014 | LUT Format | 🔴 Pending | P6 |
| DEC-015 | RNNoise License/Size | 🔴 Pending | P9 |
| DEC-016 | Whisper.cpp Auto Captions | 🔴 Pending | P11 |
| DEC-017 | Codec Capability Fallback Ladder | ✅ Decided | P8 |
| DEC-018 | JVM Test Strategy (org.json + Robolectric sdk34) | ✅ Decided | Testing |
