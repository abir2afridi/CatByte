# Third-Party Licenses - CatByte Video Editor

**Generated:** 2026-10-31
**Policy:** Only licenses compatible with proprietary APK distribution (Apache-2.0, MIT, BSD, CC0). NO GPL/AGPL/LGPL in shipped APK.

---

## Gradle Dependencies (from libs.versions.toml)

| Library | Version | License | Source | Notes |
|---------|---------|---------|--------|-------|
| Android Gradle Plugin (AGP) | 9.5.0 | Apache-2.0 | Google | Build tool only, not in APK |
| Kotlin Stdlib/Compiler | 2.2.10 | Apache-2.0 | JetBrains | Compiler only, stdlib in APK |
| Compose BOM | 2025.01.01 | Apache-2.0 | Google | Runtime in APK |
| AndroidX Core KTX | 1.18.0 | Apache-2.0 | Google | In APK |
| AndroidX Activity Compose | 1.10.1 | Apache-2.0 | Google | In APK |
| AndroidX Lifecycle (Runtime, ViewModel, Compose) | 2.8.7 | Apache-2.0 | Google | In APK |
| AndroidX Compose (UI, Graphics, Material3, Tooling) | 2025.01.01 | Apache-2.0 | Google | In APK |
| AndroidX Material Icons (Core, Extended) | 2025.01.01 | Apache-2.0 | Google | Fonts in APK |
| AndroidX Media3 (ExoPlayer, UI, Transformer, Effect) | 1.11.1 | Apache-2.0 | Google | In APK |
| AndroidX Navigation Compose | 2.8.9 | Apache-2.0 | Google | In APK |
| AndroidX Room (Runtime, KTX, Compiler) | 2.7.0 | Apache-2.0 | Google | Runtime in APK, Compiler build-only |
| AndroidX DataStore Preferences | 1.3.0 | Apache-2.0 | Google | In APK |
| Coil Compose | 2.7.0 | Apache-2.0 | Coil | In APK |
| Kotlinx Coroutines (Android, Core, Test) | 1.10.2 | Apache-2.0 | Kotlin | In APK |
| Robolectric | 4.16.1 | Apache-2.0 | Robolectric | Test only, not in APK |
| JUnit / AndroidX Test / Espresso | Various | Apache-2.0 / EPL-2.0 | Various | Test only, not in APK |
| Google DevTools KSP | 2.3.6 | Apache-2.0 | Google | Build tool only |
| Secrets Gradle Plugin | 2.0.1 | Apache-2.0 | Google | Build tool only |
| Foojay Toolchain Resolver | 1.0.0 | Apache-2.0 | Gradle | Build tool only |

---

## Fonts (To Be Added - P6)

| Font | License | Source | Status |
|------|---------|--------|--------|
| Noto Sans Bengali | OFL-1.1 | Google Fonts | 🔴 Not yet bundled |
| Hind Siliguri | OFL-1.1 | Google Fonts | 🔴 Not yet bundled |
| Tiro Bangla | OFL-1.1 | Google Fonts | 🔴 Not yet bundled |
| Anek Bangla | OFL-1.1 | Google Fonts | 🔴 Not yet bundled |
| Inter (UI font) | OFL-1.1 | Google Fonts | 🔴 Not yet bundled |

**OFL-1.1 Note:** Allows embedding in apps, requires license file inclusion, no modification of font name.

---

## Media/Stock Assets (To Be Added - P10)

| Asset | License | Source | Status |
|-------|---------|--------|--------|
| Sample video clips | CC0 | Pexels, Pixabay, Coverr | 🔴 Not yet added |
| Sample audio tracks | CC0 | Free Music Archive, YouTube Audio Library | 🔴 Not yet added |
| Sticker pack (emoji) | Unicode/Platform | Noto Emoji | 🔴 Not yet added |
| LUT presets (.cube) | Custom/CC0 | Self-created or CC0 | 🔴 Not yet created |

---

## Native Libraries (Future - Tier 2/3)

| Library | License | Source | Status |
|---------|---------|--------|--------|
| RNNoise (noise reduction) | BSD-3-Clause | Mozilla | 🔴 Not yet evaluated |
| whisper.cpp (auto captions) | MIT | ggerganov | 🔴 Not yet evaluated |
| MediaPipe (segmentation) | Apache-2.0 | Google | 🔴 Not yet evaluated |
| ffmpeg (transcoding proxy) | LGPL-2.1 / GPL-2.0 | FFmpeg | ⚠️ LGPL requires dynamic linking, GPL not allowed |

---

## License Compliance Checklist

- [ ] All runtime dependencies are Apache-2.0, MIT, BSD, or CC0
- [ ] No GPL/AGPL/LGPL in shipped APK (static linking)
- [ ] OFL fonts: license.txt included in APK assets
- [ ] CC0 assets: attribution not required but credited in About screen
- [ ] Native libraries: dynamic linking only, license verified
- [ ] Dependency license report generated in CI
- [ ] THIRD_PARTY_LICENSES.md updated with each new dependency

---

## Generating License Report

```bash
# Generate license report (add to CI)
./gradlew :app:generateLicenseReport

# Or use license-gradle-plugin
# Add to build.gradle.kts:
# plugins { id("com.github.hierynomus.license") version "0.16.1" }
# license { ... }
```

---

## Attribution Template (About Screen)

```
CatByte Video Editor

Built with:
- Android Jetpack (Apache-2.0)
- Kotlin (Apache-2.0)
- Jetpack Compose (Apache-2.0)
- Media3 / ExoPlayer (Apache-2.0)
- Room Database (Apache-2.0)
- Coil Image Loading (Apache-2.0)
- Kotlinx Coroutines (Apache-2.0)

Fonts:
- Noto Sans Bengali (OFL-1.1)
- Hind Siliguri (OFL-1.1)
- Tiro Bangla (OFL-1.1)
- Anek Bangla (OFL-1.1)
- Inter (OFL-1.1)

Sample Media: CC0 (Pexels, Pixabay, Free Music Archive)

Icons: Material Icons (Apache-2.0)

Full license texts: https://github.com/your-repo/CatByte/blob/main/THIRD_PARTY_LICENSES.md
```

---

## Verification Commands

```bash
# Check for GPL/AGPL in dependencies
./gradlew :app:dependencies --configuration releaseRuntimeClasspath | grep -i "gpl\|agpl\|lgpl"

# Verify all licenses in generated report
cat app/build/reports/license/licenseReport.html
```
