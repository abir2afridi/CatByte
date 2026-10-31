import { create } from 'zustand'
import { subscribeWithSelector } from 'zustand/middleware'
import type {
  VideoProject,
  ProjectSettings,
  Track,
  Clip,
  MediaAsset,
  ClipTransform,
  TimelineState,
  PreviewState,
  ExportSettings,
  ExportPreset,
  Timecode,
  FrameRate,
  Keyframe,
  ClipEffect,
  TextStyle,
  Transition,
  AITask,
  CollaborationState,
  AppSettings,
  EffectType,
} from '@/types'
import {
  defaultProjectSettings,
  exportPresets,
  defaultClipTransform,
} from '@/types'

// Helper functions
const generateId = () => `${Date.now()}-${Math.random().toString(36).slice(2, 11)}`

const framesToTimecode = (frames: number, frameRate: FrameRate): Timecode => {
  const totalSeconds = Math.floor(frames / frameRate)
  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const seconds = totalSeconds % 60
  const remainingFrames = frames % frameRate
  return { hours, minutes, seconds, frames: remainingFrames, frameRate }
}

const timecodeToFrames = (tc: Timecode): number => {
  return ((tc.hours * 3600 + tc.minutes * 60 + tc.seconds) * tc.frameRate) + tc.frames
}

// Default export settings
const defaultExportSettings: ExportSettings = {
  format: 'mp4',
  codec: 'h264',
  preset: exportPresets[1], // YouTube 1080p
  width: 1920,
  height: 1080,
  frameRate: 30,
  bitrate: 12000,
  audioBitrate: 256,
  audioSampleRate: 48000,
  audioChannels: 2,
  quality: 'high',
  hardwareAcceleration: true,
  twoPass: false,
  range: 'full',
  inPoint: 0,
  outPoint: 0,
  metadata: {
    title: '',
    description: '',
    tags: [],
    author: '',
    copyright: '',
    date: new Date().toISOString(),
  },
}

const defaultTimelineState: TimelineState = {
  zoom: 100,
  playhead: 0,
  duration: 30 * 30, // 30 seconds at 30fps
  selection: [],
  scrollX: 0,
  scrollY: 0,
  snapEnabled: true,
  rippleMode: false,
}

const defaultPreviewState: PreviewState = {
  playing: false,
  currentTime: 0,
  volume: 1,
  muted: false,
  loop: false,
  quality: 'full',
  showSafeZones: false,
  showGuides: false,
  backgroundColor: '#000000',
}

const defaultAppSettings: AppSettings = {
  theme: 'dark',
  language: 'en',
  autoSave: true,
  autoSaveInterval: 30,
  proxyEnabled: false,
  proxyResolution: 'half',
  hardwareAcceleration: true,
  maxUndoHistory: 100,
  timelineSnap: true,
  playbackQuality: 'full',
  shortcuts: [],
  recentProjects: [],
  cacheSize: 2048,
  aiModelsPath: '',
}

interface ProjectStore {
  // Project
  project: VideoProject | null
  isProjectDirty: boolean
  createProject: (name?: string, settings?: Partial<ProjectSettings>) => VideoProject
  loadProject: (project: VideoProject) => void
  updateProjectSettings: (settings: Partial<ProjectSettings>) => void
  setProjectDirty: (dirty: boolean) => void
  saveProject: () => Promise<void>

  // Tracks
  addTrack: (type: Track['type'], name?: string) => Track
  removeTrack: (trackId: string) => void
  reorderTracks: (trackIds: string[]) => void
  updateTrack: (trackId: string, updates: Partial<Track>) => void
  toggleTrackEnabled: (trackId: string) => void
  toggleTrackLocked: (trackId: string) => void
  toggleTrackMuted: (trackId: string) => void
  toggleTrackSolo: (trackId: string) => void

  // Clips
  addClip: (trackId: string, assetId: string, startFrame: number) => Clip
  removeClip: (clipId: string) => void
  moveClip: (clipId: string, newStart: number, newTrackId?: string) => void
  trimClip: (clipId: string, startFrame: number, endFrame: number) => void
  splitClip: (clipId: string, frame: number) => Clip[]
  duplicateClip: (clipId: string) => Clip
  updateClip: (clipId: string, updates: Partial<Clip>) => void
  updateClipTransform: (clipId: string, transform: Partial<ClipTransform>) => void
  setClipSelection: (clipIds: string[]) => void
  addToSelection: (clipId: string) => void
  removeFromSelection: (clipId: string) => void
  clearSelection: () => void

  // Clip Effects
  addClipEffect: (clipId: string, effect: Omit<ClipEffect, 'id'>) => ClipEffect
  removeClipEffect: (clipId: string, effectId: string) => void
  updateClipEffect: (clipId: string, effectId: string, updates: Partial<ClipEffect>) => void
  reorderClipEffects: (clipId: string, effectIds: string[]) => void

  // Keyframes
  addKeyframe: (clipId: string, property: string, frame: number, value: unknown, easing?: Keyframe['easing']) => Keyframe
  removeKeyframe: (clipId: string, keyframeId: string) => void
  updateKeyframe: (clipId: string, keyframeId: string, updates: Partial<Keyframe>) => void
  getKeyframes: (clipId: string, property: string) => Keyframe[]

  // Media Pool
  addMediaAsset: (asset: Omit<MediaAsset, 'id' | 'createdAt'>) => MediaAsset
  removeMediaAsset: (assetId: string) => void
  updateMediaAsset: (assetId: string, updates: Partial<MediaAsset>) => void

  // Timeline
  timeline: TimelineState
  setTimelineZoom: (zoom: number) => void
  setPlayhead: (frame: number) => void
  setTimelineDuration: (duration: number) => void
  setTimelineScroll: (x: number, y: number) => void
  toggleSnap: () => void
  toggleRipple: () => void

  // Preview
  preview: PreviewState
  setPreviewPlaying: (playing: boolean) => void
  setPreviewTime: (time: number) => void
  setPreviewVolume: (volume: number) => void
  togglePreviewMuted: () => void
  togglePreviewLoop: () => void
  setPreviewQuality: (quality: PreviewState['quality']) => void
  toggleSafeZones: () => void
  toggleGuides: () => void

  // Export
  exportSettings: ExportSettings
  updateExportSettings: (settings: Partial<ExportSettings>) => void
  resetExportSettings: () => void

  // Undo/Redo
  history: { past: unknown[]; future: unknown[] }
  pushHistory: (action: unknown) => void
  undo: () => void
  redo: () => void
  canUndo: boolean
  canRedo: boolean
  clearHistory: () => void

  // AI Tasks
  aiTasks: AITask[]
  addAITask: (task: Omit<AITask, 'id' | 'createdAt'>) => AITask
  updateAITask: (taskId: string, updates: Partial<AITask>) => void
  removeAITask: (taskId: string) => void

  // Collaboration
  collaboration: CollaborationState
  setCollaborationConnected: (connected: boolean) => void
  updateCollaborationUser: (user: Partial<CollaborationState['currentUser']>) => void
  addCollaborationUser: (user: CollaborationState['users'][0]) => void
  removeCollaborationUser: (userId: string) => void

  // Settings
  settings: AppSettings
  updateSettings: (settings: Partial<AppSettings>) => void
  resetSettings: () => void

  // Utilities
  framesToTimecode: (frames: number) => Timecode
  timecodeToFrames: (tc: Timecode) => number
  getProjectDuration: () => number
  getClipAtPosition: (trackId: string, frame: number) => Clip | undefined
}

export const useProjectStore = create<ProjectStore>()(
  subscribeWithSelector((set, get) => ({
    // Project
    project: null,
    isProjectDirty: false,

    createProject: (name = 'Untitled Project', settings = {}) => {
      const project: VideoProject = {
        id: generateId(),
        name,
        createdAt: Date.now(),
        updatedAt: Date.now(),
        settings: { ...defaultProjectSettings, ...settings },
        tracks: [
          { id: generateId(), name: 'Video 1', type: 'video', enabled: true, locked: false, muted: false, solo: false, height: 80, order: 0, clips: [] },
          { id: generateId(), name: 'Audio 1', type: 'audio', enabled: true, locked: false, muted: false, solo: false, height: 60, order: 1, clips: [] },
        ],
        mediaPool: [],
        duration: 30 * 30,
        version: 1,
      }
      set({ project, isProjectDirty: true, timeline: { ...defaultTimelineState, duration: project.duration } })
      return project
    },

    loadProject: (project) => {
      set({ project, isProjectDirty: false, timeline: { ...defaultTimelineState, duration: project.duration } })
    },

    updateProjectSettings: (settings) =>
      set((state) => {
        if (!state.project) return state
        return {
          project: { ...state.project, settings: { ...state.project.settings, ...settings }, updatedAt: Date.now() },
          isProjectDirty: true,
        }
      }),

    setProjectDirty: (dirty) => set({ isProjectDirty: dirty }),

    saveProject: async () => {
      const { project } = get()
      if (!project) return
      // TODO: Implement actual save (Tauri FS)
      console.log('Saving project:', project)
      set({ isProjectDirty: false })
    },

    // Tracks
    addTrack: (type: Track['type'], name?: string): Track => {
      let newTrack: Track | null = null
      set((state) => {
        if (!state.project) return state
        const maxOrder = Math.max(...state.project.tracks.map((t) => t.order), -1)
        const track: Track = {
          id: generateId(),
          name: name || `${type.charAt(0).toUpperCase() + type.slice(1)} ${state.project.tracks.filter((t) => t.type === type).length + 1}`,
          type,
          enabled: true,
          locked: false,
          muted: false,
          solo: false,
          height: type === 'video' ? 80 : 60,
          order: maxOrder + 1,
          clips: [],
        }
        newTrack = track
        return {
          project: { ...state.project, tracks: [...state.project.tracks, track], updatedAt: Date.now() },
          isProjectDirty: true,
        }
      })
      return newTrack!
    },

    removeTrack: (trackId: string): void => {
      set((state) => {
        if (!state.project) return state
        return {
          project: { ...state.project, tracks: state.project.tracks.filter((t) => t.id !== trackId), updatedAt: Date.now() },
          isProjectDirty: true,
        }
      })
    },

    reorderTracks: (trackIds: string[]): void => {
      set((state) => {
        if (!state.project) return state
        const trackMap = new Map(state.project.tracks.map((t) => [t.id, t]))
        const reordered = trackIds.map((id, index) => ({ ...trackMap.get(id)!, order: index }))
        return {
          project: { ...state.project, tracks: reordered, updatedAt: Date.now() },
          isProjectDirty: true,
        }
      })
    },

    updateTrack: (trackId: string, updates: Partial<Track>): void => {
      set((state) => {
        if (!state.project) return state
        return {
          project: {
            ...state.project,
            tracks: state.project.tracks.map((t) => (t.id === trackId ? { ...t, ...updates } : t)),
            updatedAt: Date.now(),
          },
          isProjectDirty: true,
        }
      })
    },

    toggleTrackEnabled: (trackId: string): void => {
      set((state) => {
        if (!state.project) return state
        const track = state.project.tracks.find((t) => t.id === trackId)
        if (!track) return state
        return {
          project: {
            ...state.project,
            tracks: state.project.tracks.map((t) => (t.id === trackId ? { ...t, enabled: !t.enabled } : t)),
            updatedAt: Date.now(),
          },
          isProjectDirty: true,
        }
      })
    },

    toggleTrackLocked: (trackId: string): void => {
      set((state) => {
        if (!state.project) return state
        const track = state.project.tracks.find((t) => t.id === trackId)
        if (!track) return state
        return {
          project: {
            ...state.project,
            tracks: state.project.tracks.map((t) => (t.id === trackId ? { ...t, locked: !t.locked } : t)),
            updatedAt: Date.now(),
          },
          isProjectDirty: true,
        }
      })
    },

    toggleTrackMuted: (trackId: string): void => {
      set((state) => {
        if (!state.project) return state
        const track = state.project.tracks.find((t) => t.id === trackId)
        if (!track) return state
        return {
          project: {
            ...state.project,
            tracks: state.project.tracks.map((t) => (t.id === trackId ? { ...t, muted: !t.muted } : t)),
            updatedAt: Date.now(),
          },
          isProjectDirty: true,
        }
      })
    },

    toggleTrackSolo: (trackId: string): void => {
      set((state) => {
        if (!state.project) return state
        const track = state.project.tracks.find((t) => t.id === trackId)
        if (!track) return state
        const newSolo = !track.solo
        return {
          project: {
            ...state.project,
            tracks: state.project.tracks.map((t) =>
              t.id === trackId ? { ...t, solo: newSolo } : newSolo && t.type === track.type ? { ...t, solo: false } : t
            ),
            updatedAt: Date.now(),
          },
          isProjectDirty: true,
        }
      })
    },

    // Clips
    addClip: (trackId: string, assetId: string, startFrame: number): Clip => {
      let newClip: Clip | null = null
      set((state) => {
        if (!state.project) return state
        const asset = state.project.mediaPool.find((a) => a.id === assetId)
        const track = state.project.tracks.find((t) => t.id === trackId)
        if (!asset || !track) return state

        const clip: Clip = {
          id: generateId(),
          trackId,
          assetId,
          name: asset.name,
          start: startFrame,
          end: startFrame + asset.duration,
          sourceStart: 0,
          sourceEnd: asset.duration,
          transform: { ...defaultClipTransform, x: state.project.settings.width / 2, y: state.project.settings.height / 2 },
          opacity: 1,
          volume: 1,
          speed: 1,
          reverse: false,
          effects: [],
          keyframes: [],
        }
        newClip = clip

        return {
          project: {
            ...state.project,
            tracks: state.project.tracks.map((t) =>
              t.id === trackId ? { ...t, clips: [...t.clips, clip].sort((a, b) => a.start - b.start) } : t
            ),
            updatedAt: Date.now(),
          },
          isProjectDirty: true,
        }
      })
      return newClip!
    },

    removeClip: (clipId: string): void => {
      set((state) => {
        if (!state.project) return state
        return {
          project: {
            ...state.project,
            tracks: state.project.tracks.map((t) => ({
              ...t,
              clips: t.clips.filter((c) => c.id !== clipId),
            })),
            updatedAt: Date.now(),
          },
          isProjectDirty: true,
          timeline: { ...state.timeline, selection: state.timeline.selection.filter((id) => id !== clipId) },
        }
      })
    },

    moveClip: (clipId, newStart, newTrackId) =>
      set((state) => {
        if (!state.project) return state
        let clip: Clip | undefined
        let oldTrackId: string | undefined

        for (const track of state.project.tracks) {
          const found = track.clips.find((c) => c.id === clipId)
          if (found) {
            clip = found
            oldTrackId = track.id
            break
          }
        }
        if (!clip || !oldTrackId) return state

        const targetTrackId = newTrackId || oldTrackId
        const duration = clip.end - clip.start

        return {
          project: {
            ...state.project,
            tracks: state.project.tracks.map((t) => {
              if (t.id === oldTrackId) {
                return { ...t, clips: t.clips.filter((c) => c.id !== clipId) }
              }
              if (t.id === targetTrackId) {
                return { ...t, clips: [...t.clips, { ...clip!, start: newStart, end: newStart + duration }].sort((a, b) => a.start - b.start) }
              }
              return t
            }),
            updatedAt: Date.now(),
          },
          isProjectDirty: true,
        }
      }),

    trimClip: (clipId, startFrame, endFrame) =>
      set((state) => {
        if (!state.project) return state
        return {
          project: {
            ...state.project,
            tracks: state.project.tracks.map((t) => ({
              ...t,
              clips: t.clips.map((c) =>
                c.id === clipId ? { ...c, start: startFrame, end: endFrame } : c
              ),
            })),
            updatedAt: Date.now(),
          },
          isProjectDirty: true,
        }
      }),

    splitClip: (clipId, frame) => {
      const { project } = get()
      if (!project) return []

      let clip: Clip | undefined
      let track: Track | undefined

      for (const t of project.tracks) {
        const found = t.clips.find((c) => c.id === clipId)
        if (found) {
          clip = found
          track = t
          break
        }
      }
      if (!clip || !track) return []

      const asset = project.mediaPool.find((a) => a.id === clip.assetId)
      if (!asset) return []

      const sourceOffset = frame - clip.start
      if (sourceOffset <= 0 || sourceOffset >= clip.end - clip.start) return []

      const firstClip: Clip = { ...clip, id: generateId(), end: frame, sourceEnd: clip.sourceStart + sourceOffset }
      const secondClip: Clip = {
        ...clip,
        id: generateId(),
        start: frame,
        sourceStart: clip.sourceStart + sourceOffset,
      }

      set((state) => ({
        project: state.project
          ? {
              ...state.project,
              tracks: state.project.tracks.map((t) =>
                t.id === track!.id
                  ? { ...t, clips: [...t.clips.filter((c) => c.id !== clipId), firstClip, secondClip].sort((a, b) => a.start - b.start) }
                  : t
              ),
              updatedAt: Date.now(),
            }
          : state.project,
        isProjectDirty: true,
      }))

      return [firstClip, secondClip]
    },

    duplicateClip: (clipId: string): Clip => {
      let duplicate: Clip | null = null
      set((state) => {
        if (!state.project) return state
        let clip: Clip | undefined
        let trackId: string | undefined

        for (const track of state.project.tracks) {
          const found = track.clips.find((c) => c.id === clipId)
          if (found) {
            clip = found
            trackId = track.id
            break
          }
        }
        if (!clip || !trackId) return state

        const newDuplicate: Clip = {
          ...clip,
          id: generateId(),
          start: clip.end,
          end: clip.end + (clip.end - clip.start),
        }
        duplicate = newDuplicate

        return {
          project: {
            ...state.project,
            tracks: state.project.tracks.map((t) =>
              t.id === trackId ? { ...t, clips: [...t.clips, duplicate].sort((a, b) => a.start - b.start) } : t
            ),
            updatedAt: Date.now(),
          },
          isProjectDirty: true,
        }
      })
      return duplicate!
    },

    updateClip: (clipId, updates) =>
      set((state) => {
        if (!state.project) return state
        return {
          project: {
            ...state.project,
            tracks: state.project.tracks.map((t) => ({
              ...t,
              clips: t.clips.map((c) => (c.id === clipId ? { ...c, ...updates } : c)),
            })),
            updatedAt: Date.now(),
          },
          isProjectDirty: true,
        }
      }),

    updateClipTransform: (clipId, transform) =>
      set((state) => {
        if (!state.project) return state
        return {
          project: {
            ...state.project,
            tracks: state.project.tracks.map((t) => ({
              ...t,
              clips: t.clips.map((c) =>
                c.id === clipId ? { ...c, transform: { ...c.transform, ...transform } } : c
              ),
            })),
            updatedAt: Date.now(),
          },
          isProjectDirty: true,
        }
      }),

    setClipSelection: (clipIds) => set((state) => ({ timeline: { ...state.timeline, selection: clipIds } })),
    addToSelection: (clipId) =>
      set((state) => ({ timeline: { ...state.timeline, selection: [...state.timeline.selection, clipId] } })),
    removeFromSelection: (clipId) =>
      set((state) => ({ timeline: { ...state.timeline, selection: state.timeline.selection.filter((id) => id !== clipId) } })),
    clearSelection: () => set((state) => ({ timeline: { ...state.timeline, selection: [] } })),

    // Clip Effects
    addClipEffect: (clipId: string, effect: Omit<ClipEffect, 'id'>): ClipEffect => {
      let newEffect: ClipEffect | null = null
      set((state) => {
        if (!state.project) return state
        const newEffectInstance: ClipEffect = { ...effect, id: generateId(), keyframes: [] }
        newEffect = newEffectInstance
        return {
          project: {
            ...state.project,
            tracks: state.project.tracks.map((t) => ({
              ...t,
              clips: t.clips.map((c) =>
                c.id === clipId ? { ...c, effects: [...c.effects, newEffectInstance] } : c
              ),
            })),
            updatedAt: Date.now(),
          },
          isProjectDirty: true,
        }
      })
      return newEffect!
    },

    removeClipEffect: (clipId, effectId) =>
      set((state) => {
        if (!state.project) return state
        return {
          project: {
            ...state.project,
            tracks: state.project.tracks.map((t) => ({
              ...t,
              clips: t.clips.map((c) =>
                c.id === clipId ? { ...c, effects: c.effects.filter((e) => e.id !== effectId) } : c
              ),
            })),
            updatedAt: Date.now(),
          },
          isProjectDirty: true,
        }
      }),

    updateClipEffect: (clipId, effectId, updates) =>
      set((state) => {
        if (!state.project) return state
        return {
          project: {
            ...state.project,
            tracks: state.project.tracks.map((t) => ({
              ...t,
              clips: t.clips.map((c) =>
                c.id === clipId
                  ? { ...c, effects: c.effects.map((e) => (e.id === effectId ? { ...e, ...updates } : e)) }
                  : c
              ),
            })),
            updatedAt: Date.now(),
          },
          isProjectDirty: true,
        }
      }),

    reorderClipEffects: (clipId, effectIds) =>
      set((state) => {
        if (!state.project) return state
        return {
          project: {
            ...state.project,
            tracks: state.project.tracks.map((t) => ({
              ...t,
              clips: t.clips.map((c) => {
                if (c.id !== clipId) return c
                const effectMap = new Map(c.effects.map((e) => [e.id, e]))
                return { ...c, effects: effectIds.map((id) => effectMap.get(id)!).filter(Boolean) }
              }),
            })),
            updatedAt: Date.now(),
          },
          isProjectDirty: true,
        }
      }),

    // Keyframes
    addKeyframe: (clipId: string, property: string, frame: number, value: unknown, easing?: Keyframe['easing']): Keyframe => {
      let newKeyframe: Keyframe | null = null
      set((state) => {
        if (!state.project) return state
        const keyframe: Keyframe = { id: generateId(), property, frame, value, easing: easing || 'linear' }
        newKeyframe = keyframe
        return {
          project: {
            ...state.project,
            tracks: state.project.tracks.map((t) => ({
              ...t,
              clips: t.clips.map((c) =>
                c.id === clipId ? { ...c, keyframes: [...c.keyframes, keyframe].sort((a, b) => a.frame - b.frame) } : c
              ),
            })),
            updatedAt: Date.now(),
          },
          isProjectDirty: true,
        }
      })
      return newKeyframe!
    },

    removeKeyframe: (clipId, keyframeId) =>
      set((state) => {
        if (!state.project) return state
        return {
          project: {
            ...state.project,
            tracks: state.project.tracks.map((t) => ({
              ...t,
              clips: t.clips.map((c) =>
                c.id === clipId ? { ...c, keyframes: c.keyframes.filter((k) => k.id !== keyframeId) } : c
              ),
            })),
            updatedAt: Date.now(),
          },
          isProjectDirty: true,
        }
      }),

    updateKeyframe: (clipId, keyframeId, updates) =>
      set((state) => {
        if (!state.project) return state
        return {
          project: {
            ...state.project,
            tracks: state.project.tracks.map((t) => ({
              ...t,
              clips: t.clips.map((c) =>
                c.id === clipId
                  ? { ...c, keyframes: c.keyframes.map((k) => (k.id === keyframeId ? { ...k, ...updates } : k)) }
                  : c
              ),
            })),
            updatedAt: Date.now(),
          },
          isProjectDirty: true,
        }
      }),

    getKeyframes: (clipId, property) => {
      const { project } = get()
      if (!project) return []
      for (const track of project.tracks) {
        const clip = track.clips.find((c) => c.id === clipId)
        if (clip) return clip.keyframes.filter((k) => k.property === property)
      }
      return []
    },

    // Media Pool
    addMediaAsset: (asset: Omit<MediaAsset, 'id' | 'createdAt'>): MediaAsset => {
      let newAsset: MediaAsset | null = null
      set((state) => {
        if (!state.project) return state
        const newAssetInstance: MediaAsset = { ...asset, id: generateId(), createdAt: Date.now() }
        newAsset = newAssetInstance
        return {
          project: { ...state.project, mediaPool: [...state.project.mediaPool, newAssetInstance], updatedAt: Date.now() },
          isProjectDirty: true,
        }
      })
      return newAsset!
    },

    removeMediaAsset: (assetId) =>
      set((state) => {
        if (!state.project) return state
        // Also remove clips using this asset
        return {
          project: {
            ...state.project,
            mediaPool: state.project.mediaPool.filter((a) => a.id !== assetId),
            tracks: state.project.tracks.map((t) => ({
              ...t,
              clips: t.clips.filter((c) => c.assetId !== assetId),
            })),
            updatedAt: Date.now(),
          },
          isProjectDirty: true,
        }
      }),

    updateMediaAsset: (assetId, updates) =>
      set((state) => {
        if (!state.project) return state
        return {
          project: {
            ...state.project,
            mediaPool: state.project.mediaPool.map((a) => (a.id === assetId ? { ...a, ...updates } : a)),
            updatedAt: Date.now(),
          },
          isProjectDirty: true,
        }
      }),

    // Timeline
    timeline: defaultTimelineState,
    setTimelineZoom: (zoom) => set((state) => ({ timeline: { ...state.timeline, zoom: Math.max(10, Math.min(500, zoom)) } })),
    setPlayhead: (frame) => set((state) => ({ timeline: { ...state.timeline, playhead: Math.max(0, Math.min(state.timeline.duration, frame)) } })),
    setTimelineDuration: (duration) => set((state) => ({ timeline: { ...state.timeline, duration } })),
    setTimelineScroll: (x, y) => set((state) => ({ timeline: { ...state.timeline, scrollX: x, scrollY: y } })),
    toggleSnap: () => set((state) => ({ timeline: { ...state.timeline, snapEnabled: !state.timeline.snapEnabled } })),
    toggleRipple: () => set((state) => ({ timeline: { ...state.timeline, rippleMode: !state.timeline.rippleMode } })),

    // Preview
    preview: defaultPreviewState,
    setPreviewPlaying: (playing) => set((state) => ({ preview: { ...state.preview, playing } })),
    setPreviewTime: (time) => set((state) => ({ preview: { ...state.preview, currentTime: time } })),
    setPreviewVolume: (volume) => set((state) => ({ preview: { ...state.preview, volume: Math.max(0, Math.min(1, volume)) } })),
    togglePreviewMuted: () => set((state) => ({ preview: { ...state.preview, muted: !state.preview.muted } })),
    togglePreviewLoop: () => set((state) => ({ preview: { ...state.preview, loop: !state.preview.loop } })),
    setPreviewQuality: (quality) => set((state) => ({ preview: { ...state.preview, quality } })),
    toggleSafeZones: () => set((state) => ({ preview: { ...state.preview, showSafeZones: !state.preview.showSafeZones } })),
    toggleGuides: () => set((state) => ({ preview: { ...state.preview, showGuides: !state.preview.showGuides } })),

    // Export
    exportSettings: defaultExportSettings,
    updateExportSettings: (settings) => set((state) => ({ exportSettings: { ...state.exportSettings, ...settings } })),
    resetExportSettings: () => set({ exportSettings: defaultExportSettings }),

    // History
    history: { past: [], future: [] },
    pushHistory: (action) =>
      set((state) => ({
        history: {
          past: [...state.history.past, action].slice(-get().settings.maxUndoHistory),
          future: [],
        },
      })),
    undo: () =>
      set((state) => {
        if (state.history.past.length === 0) return state
        const previous = state.history.past[state.history.past.length - 1]
        return {
          history: { past: state.history.past.slice(0, -1), future: [previous, ...state.history.future] },
        }
      }),
    redo: () =>
      set((state) => {
        if (state.history.future.length === 0) return state
        const next = state.history.future[0]
        return {
          history: { past: [...state.history.past, next], future: state.history.future.slice(1) },
        }
      }),
    get canUndo() {
      return get().history.past.length > 0
    },
    get canRedo() {
      return get().history.future.length > 0
    },
    clearHistory: () => set({ history: { past: [], future: [] } }),

    // AI Tasks
    aiTasks: [],
    addAITask: (task) => {
      const newTask: AITask = { ...task, id: generateId(), createdAt: Date.now() }
      set((state) => ({ aiTasks: [...state.aiTasks, newTask] }))
      return newTask
    },
    updateAITask: (taskId, updates) =>
      set((state) => ({
        aiTasks: state.aiTasks.map((t) => (t.id === taskId ? { ...t, ...updates } : t)),
      })),
    removeAITask: (taskId) =>
      set((state) => ({ aiTasks: state.aiTasks.filter((t) => t.id !== taskId) })),

    // Collaboration
    collaboration: {
      connected: false,
      users: [],
      currentUser: { id: generateId(), name: 'User', color: '#8b5cf6', selection: [], lastActive: Date.now() },
      projectId: '',
    },
    setCollaborationConnected: (connected) =>
      set((state) => ({ collaboration: { ...state.collaboration, connected } })),
    updateCollaborationUser: (user) =>
      set((state) => ({ collaboration: { ...state.collaboration, currentUser: { ...state.collaboration.currentUser, ...user } } })),
    addCollaborationUser: (user) =>
      set((state) => ({ collaboration: { ...state.collaboration, users: [...state.collaboration.users, user] } })),
    removeCollaborationUser: (userId) =>
      set((state) => ({ collaboration: { ...state.collaboration, users: state.collaboration.users.filter((u) => u.id !== userId) } })),

    // Settings
    settings: defaultAppSettings,
    updateSettings: (settings) => set((state) => ({ settings: { ...state.settings, ...settings } })),
    resetSettings: () => set({ settings: defaultAppSettings }),

    // Utilities
    framesToTimecode: (frames) => {
      const { project } = get()
      return framesToTimecode(frames, project?.settings.frameRate || 30)
    },
    timecodeToFrames: (tc) => timecodeToFrames(tc),
    getProjectDuration: () => get().project?.duration || 0,
    getClipAtPosition: (trackId, frame) => {
      const { project } = get()
      if (!project) return undefined
      const track = project.tracks.find((t) => t.id === trackId)
      if (!track) return undefined
      return track.clips.find((c) => c.start <= frame && c.end > frame)
    },
  }))
)

// Selectors for performance
export const useProject = () => useProjectStore((state) => state.project)
export const useTracks = () => useProjectStore((state) => state.project?.tracks || [])
export const useMediaPool = () => useProjectStore((state) => state.project?.mediaPool || [])
export const useTimeline = () => useProjectStore((state) => state.timeline)
export const usePreview = () => useProjectStore((state) => state.preview)
export const useExportSettings = () => useProjectStore((state) => state.exportSettings)
export const useHistory = () => useProjectStore((state) => state.history)
export const useAITasks = () => useProjectStore((state) => state.aiTasks)
export const useCollaboration = () => useProjectStore((state) => state.collaboration)
export const useSettings = () => useProjectStore((state) => state.settings)
export const useSelection = () => useProjectStore((state) => state.timeline.selection)