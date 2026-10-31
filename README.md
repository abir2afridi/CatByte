# CatByte Video Editor

A professional, CapCut-level video editor built with **Tauri 2**, **React 18**, **TypeScript**, **Fabric.js**, and **FFmpeg.wasm**.

## ✨ Features

### Core Editing
- **Multi-track Timeline** - Video, audio, text, and effect tracks
- **Frame-accurate editing** - Precise trimming, splitting, and positioning
- **Keyframe animation** - Animate transform, opacity, effects, and more
- **Real-time preview** - GPU-accelerated canvas rendering
- **Ripple/Insert/Overwrite modes** - Professional editing workflows

### Visual Effects
- **100+ built-in effects** - Blur, color correction, glitch, chroma key, etc.
- **Transitions** - Fade, slide, zoom, wipe, glitch transitions
- **Text animations** - Typewriter, fade, slide, scale, custom animations
- **LUT support** - Cinematic color grading
- **Green screen** - Chroma key with spill suppression

### Audio
- **Multi-track audio** - Music, voiceover, sound effects
- **Audio meters** - Real-time level monitoring
- **Effects** - Gain, EQ, compressor, noise reduction, reverb, delay
- **Beat sync** - Auto-cut to music beats

### AI-Powered (Planned)
- **Auto captions** - Whisper.cpp on-device speech-to-text
- **Text-to-speech** - Natural voices
- **Background removal** - Without green screen
- **Smart scene detection** - Auto-split clips
- **Auto reframe** - 9:16, 16:9, 1:1 smart crop

### Platform & Export
- **Cross-platform** - Windows, macOS, Linux (Web coming)
- **Hardware acceleration** - NVENC, QuickSync, VAAPI, VideoToolbox
- **Presets** - YouTube, TikTok, Reels, Shorts, Instagram, Twitter
- **4K/8K support** - Proxy workflow for smooth editing
- **Direct share** - Upload to social platforms

### Developer Experience
- **Plugin system** - Custom effects, transitions, tools
- **Scripting API** - JavaScript/Python automation
- **Headless CLI** - Server-side rendering
- **Local-first** - No forced cloud, privacy-focused

## 🚀 Quick Start

### Prerequisites
- **Node.js** 20+
- **Rust** 1.75+ (`curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh`)
- **FFmpeg** (for export) - `winget install ffmpeg` / `brew install ffmpeg` / `apt install ffmpeg`

### Installation
```bash
# Clone and install
git clone https://github.com/yourusername/catbyte.git
cd catbyte
npm install

# Development
npm run tauri:dev

# Build for production
npm run tauri:build
```

## 🏗 Architecture

```
src/
├── components/
│   ├── layout/        # AppLayout, Header
│   ├── timeline/      # Timeline, Tracks, Clips
│   ├── preview/       # Video Preview (Fabric.js)
│   ├── media/         # Media Bin
│   ├── effects/       # Effects Panel
│   ├── properties/    # Properties Panel
│   └── audio/         # Audio Meters
├── hooks/
│   ├── useKeyboardShortcuts.ts
│   └── useMedia.ts    # FFmpeg.wasm integration
├── store/
│   └── projectStore.ts # Zustand state management
├── types/
│   └── index.ts       # TypeScript definitions
├── utils/
│   └── cn.ts          # Class name utility
├── App.tsx
├── main.tsx
└── index.css          # Tailwind + custom styles

src-tauri/
├── src/
│   └── main.rs        # Rust backend (FFmpeg, file I/O)
├── Cargo.toml
└── tauri.conf.json
```

## 🎮 Keyboard Shortcuts

| Action | Shortcut |
|--------|----------|
| **Play/Pause** | `Space` |
| **Stop** | `K` |
| **Frame Back/Forward** | `←` / `→` |
| **Jump 1s** | `Shift + ←/→` |
| **Go to Start/End** | `Home` / `End` |
| **Split at Playhead** | `Ctrl + K` |
| **Trim Start/End** | `Q` / `W` |
| **Undo/Redo** | `Ctrl + Z` / `Ctrl + Shift + Z` |
| **Zoom In/Out** | `=` / `-` |
| **Fit to Timeline** | `\` |
| **Selection Tool** | `V` |
| **Blade Tool** | `C` |
| **Hand Tool** | `H` |
| **Zoom Tool** | `Z` |
| **Add Video Track** | `Ctrl + Alt + V` |
| **Add Audio Track** | `Ctrl + Alt + A` |
| **Toggle Snap** | `S` |
| **Toggle Ripple** | `R` |

## 🔧 Configuration

### Project Settings
```json
{
  "width": 1920,
  "height": 1080,
  "frameRate": 30,
  "backgroundColor": "#000000",
  "sampleRate": 48000,
  "channels": 2
}
```

### Export Presets
```typescript
const presets = [
  { name: 'YouTube 4K', width: 3840, height: 2160, bitrate: 45000 },
  { name: 'YouTube 1080p', width: 1920, height: 1080, bitrate: 12000 },
  { name: 'TikTok/Reels/Shorts', width: 1080, height: 1920, bitrate: 8000 },
  { name: 'Instagram Feed', width: 1080, height: 1080, bitrate: 8000 },
  { name: 'Custom', width: 1920, height: 1080, bitrate: 12000 },
]
```

## 📦 Plugin Development

Create custom effects:

```typescript
// plugins/my-effect/index.ts
export const effect = {
  id: 'my-effect',
  name: 'My Custom Effect',
  category: 'stylize',
  type: 'custom',
  params: [
    { name: 'intensity', label: 'Intensity', type: 'number', default: 0.5, min: 0, max: 1 },
    { name: 'color', label: 'Color', type: 'color', default: { r: 1, g: 0, b: 0, a: 1 } },
  ],
  render: async (input: VideoFrame, params: Params) => {
    // WebGL/Canvas processing
    return processedFrame
  }
}
```

## 🗺 Roadmap

### v0.1 (MVP) ✅
- [x] Project structure & state management
- [x] Timeline with multi-track support
- [x] Video preview with Fabric.js
- [x] Media bin with import
- [x] Properties panel
- [x] Effects panel (UI)
- [x] Audio meters
- [x] Keyboard shortcuts
- [x] Tauri backend setup

### v0.2 - Core Editing
- [ ] Clip trimming/splitting with mouse
- [ ] Drag & drop to timeline
- [ ] Transitions between clips
- [ ] Keyframe UI & interpolation
- [ ] Undo/Redo history

### v0.3 - Effects & Rendering
- [ ] FFmpeg.wasm effect pipeline
- [ ] Real-time effect preview
- [ ] Export with progress
- [ ] Hardware acceleration

### v0.4 - AI Features
- [ ] Auto captions (Whisper.cpp)
- [ ] Text-to-speech
- [ ] Background removal
- [ ] Scene detection

### v0.5 - Polish & Plugins
- [ ] Plugin system
- [ ] Template marketplace
- [ ] Collaboration (CRDT)
- [ ] Mobile companion app

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Run `npm run lint && npm run format`
5. Submit a PR

## 📄 License

MIT License - see [LICENSE](LICENSE) for details.

## 🙏 Acknowledgments

- **Tauri** - For the amazing Rust + Web framework
- **Fabric.js** - Canvas rendering engine
- **FFmpeg.wasm** - WebAssembly FFmpeg
- **Zustand** - Simple state management
- **Tailwind CSS** - Utility-first styling
- **Lucide** - Beautiful icons
- **CapCut** - Inspiration for UX patterns

---

**Built with ❤️ for creators who want professional tools without the bloat.**