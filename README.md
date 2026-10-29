# CatByte — Pencil & Paper Sketch Edition

A video editor built with **Jetpack Compose** and **Kotlin**, featuring a hand-drawn sketchbook aesthetic. Designed for short-form creators (Reels, Shorts, TikTok) who want a tactile, pencil-on-paper editing experience.

---

## ✨ Features

### Core Editing
- **Multi-track timeline** with video, audio, text, stickers, and effects
- **Aspect ratios**: 9:16 (Shorts/Reels), 16:9 (YouTube), 1:1 (Square), 4:5 (Portrait)
- **Paper textures**: Drafting Grid, Ruled Notebook, Blank Parchment, Sepia Kraft
- **13 artistic filters**: Graphite, Blueprint, Sepia, Charcoal, Manga Dots, Cyberpunk, Vintage, Golden Hour, Noir, Teal/Orange, Moody, Glitch Art
- **Transitions**: Cut, Fade, Slide, Push, Wipe, Spin, Glitch, Ink Bleed
- **Text animations**: Typewriter, Pop-up, Neon Pulse, Fade, Slide, Bounce
- **Speed ramping** (velocity curves) and clip adjustments (brightness, contrast, saturation, vignette)

### P1 UX & Functionality
- **Home screen**: Saveable tab/filter/dialog state, delete confirmation with timed Undo bar, honest quick-tool Toasts, real profile stats
- **Settings dialog**: Language selector (System / English / বাংলা), real cache size/clear, version from `BuildConfig`, watermark toggle
- **Export dialog**: Saveable resolution/FPS, default from settings, watermark preview, Cancel during export, honest share messaging
- **Editor**: Adaptive timeline height, runtime `RECORD_AUDIO` permission flow

### P2 Code Quality
- **DataStore Preferences** for persistent settings (theme, resolution, FPS, grid, watermark, language)
- **Manual DI**: `CatByteApplication` → `AppContainer` → `EditorViewModelFactory`
- **Locale handling** via `attachBaseContext` + `LocaleUtils` (works on all API levels)
- **CacheManager** for real cache reporting/clearing (includes Coil disk cache)
- **Test compatibility**: `EditorViewModel(repository)` constructor preserved for existing unit tests

### P3 Polish
- **Bengali (bn) localization** for key settings/home strings
- **Platform splash screen** (API 31+) with sketchbook branding
- **Adaptive icon** with monochrome support
- **Manifest**: `enableOnBackInvokedCallback`, `localeConfig`, `configChanges="...|locale"`

---

## 🏗 Architecture

```
app/
├── data/
│   ├── AppLanguage.kt          # SYSTEM | ENGLISH | BENGALI
│   ├── AppContainer.kt         # Manual DI container
│   ├── CacheManager.kt         # Cache size + clear
│   ├── LocaleUtils.kt          # Per-app locale via attachBaseContext
│   ├── ProjectRepository.kt    # In-memory project CRUD + restore
│   └── SettingsRepository.kt   # DataStore persistence
├── ui/
│   ├── components/
│   │   ├── VideoCanvas.kt      # Preview with pulse, watermark, placeholders
│   │   ├── ExportDialog.kt     # Standalone export sheet
│   │   ├── SettingsDialog.kt
│   │   └── MultiTrackTimeline.kt
│   └── screens/
│       ├── HomeScreen.kt       # Drafts, templates, inbox, profile
│       └── EditorScreen.kt     # Timeline + canvas + tool sheets
├── viewmodel/
│   ├── EditorViewModel.kt      # Core editing state + settings bridge
│   └── EditorViewModelFactory.kt
├── CatByteApplication.kt       # App entry + DI init
└── MainActivity.kt             # Screen nav, locale wrap, lifecycle
```

---

## 🚀 Build & Run

```bash
# Requires JDK 17+ and Android SDK 36
./gradlew :app:assembleDebug --offline

# Run unit tests
./gradlew :app:testDebugUnitTest --tests "com.example.ExampleUnitTest" --offline
```

**Output**: `app/build/outputs/apk/debug/app-debug.apk`

---

## 📦 Dependencies (Key)

| Library | Version |
|---------|---------|
| Compose BOM | 2024.09.00 |
| Kotlin | 2.2.10 |
| DataStore Preferences | 1.1.7 |
| Coil Compose | 2.7.0 |
| Room | 2.7.0 (configured, unused for P1-P3) |
| Lifecycle/ViewModel/Compose | 2.8.7 |
| Navigation Compose | 2.8.9 |

---

## 🎯 Scope Notes

**Implemented (P1–P3):**
- All UX flows, settings, export dialog, localization, splash, DI, DataStore, cache, tests

**Out of Scope (P0 – not attempted):**
- Real video playback/rendering (still simulated with honest messaging)
- Release signing cleanup (keystore env vars remain)
- Room persistence for projects (in-memory only)
- Full string localization (~300 hardcoded strings)

---

## 📄 License

MIT — see [LICENSE](LICENSE)