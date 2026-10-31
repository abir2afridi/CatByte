// Core types for CatByte Video Editor

export interface Timecode {
  hours: number
  minutes: number
  seconds: number
  frames: number
  frameRate: number
}

export const frameRates = [24, 25, 30, 50, 60] as const
export type FrameRate = (typeof frameRates)[number]

export interface VideoProject {
  id: string
  name: string
  createdAt: number
  updatedAt: number
  settings: ProjectSettings
  tracks: Track[]
  mediaPool: MediaAsset[]
  duration: number // in frames
  version: number
}

export interface ProjectSettings {
  width: number
  height: number
  frameRate: FrameRate
  backgroundColor: string
  sampleRate: number
  channels: number
}

export const defaultProjectSettings: ProjectSettings = {
  width: 1920,
  height: 1080,
  frameRate: 30,
  backgroundColor: '#000000',
  sampleRate: 48000,
  channels: 2,
}

export type TrackType = 'video' | 'audio' | 'text' | 'effect'

export interface Track {
  id: string
  name: string
  type: TrackType
  enabled: boolean
  locked: boolean
  muted: boolean
  solo: boolean
  height: number
  order: number
  clips: Clip[]
}

export interface Clip {
  id: string
  trackId: string
  assetId: string
  name: string
  // Timeline position (in frames)
  start: number
  end: number
  // Source position (in frames)
  sourceStart: number
  sourceEnd: number
  // Transform
  transform: ClipTransform
  // Opacity/Volume
  opacity: number
  volume: number
  // Speed
  speed: number
  reverse: boolean
  // Effects
  effects: ClipEffect[]
  // Keyframes
  keyframes: Keyframe[]
  // For text clips
  textContent?: string
  textStyle?: TextStyle
}

export interface ClipTransform {
  x: number
  y: number
  scaleX: number
  scaleY: number
  rotation: number
  anchorX: number
  anchorY: number
}

export const defaultClipTransform: ClipTransform = {
  x: 0,
  y: 0,
  scaleX: 1,
  scaleY: 1,
  rotation: 0,
  anchorX: 0.5,
  anchorY: 0.5,
}

export interface ClipEffect {
  id: string
  type: EffectType
  enabled: boolean
  params: Record<string, unknown>
  keyframes: Keyframe[]
}

export type EffectType =
  // Video effects
  | 'blur'
  | 'brightness-contrast'
  | 'color-balance'
  | 'curves'
  | 'hue-saturation'
  | 'vignette'
  | 'sharpen'
  | 'noise'
  | 'glitch'
  | 'chroma-key'
  | 'crop'
  | 'transform'
  // Audio effects
  | 'gain'
  | 'eq'
  | 'compressor'
  | 'noise-reduction'
  | 'reverb'
  | 'delay'
  | 'pitch-shift'
  // Transitions
  | 'fade'
  | 'slide'
  | 'zoom'
  | 'wipe'
  | 'glitch-transition'

export type EffectParamValue = number | string | boolean | number[] | ColorValue

export interface ColorValue {
  r: number
  g: number
  b: number
  a: number
}

export interface Keyframe {
  id: string
  property: string
  frame: number
  value: unknown
  easing: EasingType
}

export type EasingType =
  | 'linear'
  | 'ease-in'
  | 'ease-out'
  | 'ease-in-out'
  | 'ease-out-in'
  | 'bounce'
  | 'elastic'
  | 'custom'

export interface MediaAsset {
  id: string
  name: string
  type: 'video' | 'audio' | 'image' | 'font'
  filePath: string
  thumbnailPath?: string
  duration: number // in frames
  width: number
  height: number
  frameRate: FrameRate
  sampleRate?: number
  channels?: number
  fileSize: number
  metadata: Record<string, unknown>
  createdAt: number
}

export interface TextStyle {
  fontFamily: string
  fontSize: number
  fontWeight: number
  color: ColorValue
  strokeColor?: ColorValue
  strokeWidth: number
  alignment: 'left' | 'center' | 'right'
  lineHeight: number
  letterSpacing: number
  transform: 'none' | 'uppercase' | 'lowercase' | 'capitalize'
  // Animation
  animationIn?: TextAnimation
  animationOut?: TextAnimation
}

export interface TextAnimation {
  type: 'fade' | 'slide' | 'typewriter' | 'scale' | 'rotate' | 'custom'
  duration: number
  easing: EasingType
  params: Record<string, EffectParamValue>
}

export interface Transition {
  id: string
  type: EffectType
  duration: number // in frames
  easing: EasingType
  params: Record<string, EffectParamValue>
  alignment: 'center' | 'start' | 'end'
}

// Timeline state
export interface TimelineState {
  zoom: number // frames per pixel
  playhead: number // current frame
  duration: number // total frames
  selection: string[] // clip IDs
  hoveredClip?: string
  scrollX: number
  scrollY: number
  snapEnabled: boolean
  rippleMode: boolean
}

// Preview state
export interface PreviewState {
  playing: boolean
  currentTime: number
  volume: number
  muted: boolean
  loop: boolean
  quality: 'full' | 'half' | 'quarter' | 'proxy'
  showSafeZones: boolean
  showGuides: boolean
  backgroundColor: string
}

// Export settings
export interface ExportSettings {
  format: 'mp4' | 'webm' | 'mov' | 'gif' | 'image-sequence'
  codec: 'h264' | 'hevc' | 'vp9' | 'av1' | 'prores'
  preset: ExportPreset
  width: number
  height: number
  frameRate: FrameRate
  bitrate: number // kbps
  audioBitrate: number // kbps
  audioSampleRate: number
  audioChannels: number
  quality: 'low' | 'medium' | 'high' | 'lossless'
  hardwareAcceleration: boolean
  twoPass: boolean
  range: 'full' | 'in-out' | 'selection'
  inPoint: number
  outPoint: number
  metadata: ExportMetadata
}

export interface ExportPreset {
  name: string
  platform: 'youtube' | 'tiktok' | 'instagram' | 'twitter' | 'custom'
  width: number
  height: number
  frameRate: FrameRate
  bitrate: number
  audioBitrate: number
}

export const exportPresets: ExportPreset[] = [
  { name: 'YouTube 4K', platform: 'youtube', width: 3840, height: 2160, frameRate: 30, bitrate: 45000, audioBitrate: 320 },
  { name: 'YouTube 1080p', platform: 'youtube', width: 1920, height: 1080, frameRate: 30, bitrate: 12000, audioBitrate: 256 },
  { name: 'TikTok/Reels/Shorts', platform: 'tiktok', width: 1080, height: 1920, frameRate: 30, bitrate: 8000, audioBitrate: 128 },
  { name: 'Instagram Feed', platform: 'instagram', width: 1080, height: 1080, frameRate: 30, bitrate: 8000, audioBitrate: 128 },
  { name: 'Instagram Story', platform: 'instagram', width: 1080, height: 1920, frameRate: 30, bitrate: 8000, audioBitrate: 128 },
  { name: 'Twitter/X', platform: 'twitter', width: 1280, height: 720, frameRate: 30, bitrate: 8000, audioBitrate: 128 },
  { name: 'Custom', platform: 'custom', width: 1920, height: 1080, frameRate: 30, bitrate: 12000, audioBitrate: 256 },
]

export interface ExportMetadata {
  title: string
  description: string
  tags: string[]
  author: string
  copyright: string
  date: string
}

// Undo/Redo
export interface HistoryAction {
  id: string
  type: string
  description: string
  timestamp: number
  data: unknown
  inverse: unknown
}

// Keyboard shortcuts
export interface Shortcut {
  id: string
  keys: string[]
  action: string
  description: string
  category: 'general' | 'timeline' | 'playback' | 'editing' | 'view'
}

// Plugin system
export interface Plugin {
  id: string
  name: string
  version: string
  description: string
  author: string
  entryPoint: string
  permissions: PluginPermission[]
  effects: EffectDefinition[]
  transitions: TransitionDefinition[]
  tools: ToolDefinition[]
}

export type PluginPermission =
  | 'read-files'
  | 'write-files'
  | 'network'
  | 'clipboard'
  | 'media-devices'
  | 'gpu'

export interface EffectDefinition {
  id: string
  name: string
  category: string
  type: EffectType
  params: EffectParamDefinition[]
  render: string // WASM module path or JS function
}

export interface EffectParamDefinition {
  name: string
  label: string
  type: 'number' | 'string' | 'boolean' | 'color' | 'curve' | 'gradient'
  default: EffectParamValue
  min?: number
  max?: number
  step?: number
  options?: { label: string; value: EffectParamValue }[]
}

export interface TransitionDefinition {
  id: string
  name: string
  category: string
  type: EffectType
  params: EffectParamDefinition[]
  render: string
}

export interface ToolDefinition {
  id: string
  name: string
  icon: string
  shortcut?: string
  cursor: string
  onActivate: string
  onDeactivate: string
  onPointerDown: string
  onPointerMove: string
  onPointerUp: string
}

// AI Features
export interface AITask {
  id: string
  type: 'captions' | 'translate' | 'voiceover' | 'background-removal' | 'scene-detection' | 'reframe' | 'script-to-video'
  status: 'pending' | 'processing' | 'completed' | 'failed'
  progress: number
  input: Record<string, unknown>
  output?: Record<string, unknown>
  error?: string
  createdAt: number
  completedAt?: number
}

export interface CaptionSegment {
  id: string
  start: number
  end: number
  text: string
  confidence: number
  speaker?: string
}

// Collaboration
export interface CollaborationUser {
  id: string
  name: string
  color: string
  cursor?: { x: number; y: number }
  selection: string[]
  lastActive: number
}

export interface CollaborationState {
  connected: boolean
  users: CollaborationUser[]
  currentUser: CollaborationUser
  projectId: string
}

// Settings
export interface AppSettings {
  theme: 'dark' | 'light' | 'system'
  language: string
  autoSave: boolean
  autoSaveInterval: number // seconds
  proxyEnabled: boolean
  proxyResolution: 'half' | 'quarter'
  hardwareAcceleration: boolean
  maxUndoHistory: number
  timelineSnap: boolean
  playbackQuality: 'full' | 'half' | 'quarter' | 'proxy'
  shortcuts: Shortcut[]
  recentProjects: string[]
  cacheSize: number // MB
  aiModelsPath: string
}