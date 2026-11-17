# CatByte Comprehensive Software Improvement Plan

This implementation plan addresses the 5 core software improvement areas requested for CatByte:
1. AI & Advanced Features (Auto Captions & AI tools framework)
2. Audio & Beat Grid (Editable beat grid and advanced audio fades)
3. UI/UX Polish (Haptic feedback, accessibility descriptions, and onboarding guide)
4. Performance & Background Export Robustness
5. Test Coverage & Instrumented UI Testing

## User Review Required

> [!IMPORTANT]
> - **AI Features:** Fully local AI features (Auto Captions, AI cutout) require heavy native/on-device ML libraries (e.g., LiteRT / ML Kit). We will provide robust architectural structures and UI integration with clear status handling.
> - **Haptics:** Haptic feedback uses standard Android `HapticFeedbackConstants` and will respect user system settings.

## Proposed Changes

### 1. AI & Advanced Features
- **CaptionsSheet (`app/src/main/java/com/example/ui/components/CaptionsSheet.kt`):** Enhance auto-caption generation flow with progress indicators and SRT/VTT import parser validation.
- **AiToolsSheet (`app/src/main/java/com/example/ui/components/AiToolsSheet.kt`):** Wire AI cutout and smart slow-mo status handlers.

### 2. Audio & Beat Grid
- **TimelineBeatGrid (`app/src/main/java/com/example/ui/components/TimelineBeatGrid.kt`):** [NEW] Dedicated editable beat grid overlay on the timeline ruler for precise beat synchronization.
- **AudioSheet (`app/src/main/java/com/example/ui/audio/AudioSheet.kt`):** Add per-clip audio fade-in/fade-out sliders and advanced mixer controls.

### 3. UI/UX Polish
- **Haptics Integration (`app/src/main/java/com/example/ui/screens/EditorScreen.kt` & timeline components):** Add haptic feedback (vibration) on clip split, trim, and magnetic snap.
- **Onboarding Guide (`app/src/main/java/com/example/ui/screens/OnboardingDialog.kt`):** [NEW] First-time interactive guide for multi-track timeline, trim handles, and keyframes.
- **Accessibility:** Add meaningful `contentDescription` attributes across main editing controls.

### 4. Performance & Export Robustness
- **ExportForegroundService (`app/src/main/java/com/example/data/ExportForegroundService.kt`):** Add battery/storage state verification before background export.

### 5. Testing
- **Test Suite (`app/src/test/java/com/example/...`):** Add unit tests for beat grid calculations and audio fade curves.

## Verification Plan

### Automated Tests
- Run `./gradlew :app:testDebugUnitTest` to ensure all JVM/Robolectric tests pass.

### Manual Verification
- Deploy to device/emulator, test haptics, beat grid snapping, and audio fades.
