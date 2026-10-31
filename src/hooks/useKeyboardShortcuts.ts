import { useEffect, useCallback, useRef } from 'react'
import { useProjectStore } from '@/store/projectStore'
import type { Shortcut } from '@/types'

const defaultShortcuts: Shortcut[] = [
  // General
  { id: 'save', keys: ['ctrl', 's'], action: 'save', description: 'Save project', category: 'general' },
  { id: 'undo', keys: ['ctrl', 'z'], action: 'undo', description: 'Undo', category: 'general' },
  { id: 'redo', keys: ['ctrl', 'shift', 'z'], action: 'redo', description: 'Redo', category: 'general' },
  { id: 'copy', keys: ['ctrl', 'c'], action: 'copy', description: 'Copy', category: 'general' },
  { id: 'paste', keys: ['ctrl', 'v'], action: 'paste', description: 'Paste', category: 'general' },
  { id: 'duplicate', keys: ['ctrl', 'd'], action: 'duplicate', description: 'Duplicate', category: 'general' },
  { id: 'delete', keys: ['delete', 'backspace'], action: 'delete', description: 'Delete', category: 'general' },
  { id: 'select-all', keys: ['ctrl', 'a'], action: 'selectAll', description: 'Select all', category: 'general' },
  { id: 'deselect', keys: ['escape'], action: 'deselect', description: 'Deselect', category: 'general' },
  { id: 'preferences', keys: ['ctrl', ','], action: 'preferences', description: 'Preferences', category: 'general' },

  // Playback
  { id: 'play-pause', keys: ['space'], action: 'playPause', description: 'Play/Pause', category: 'playback' },
  { id: 'stop', keys: ['k'], action: 'stop', description: 'Stop', category: 'playback' },
  { id: 'frame-back', keys: ['arrowleft'], action: 'frameBack', description: 'Previous frame', category: 'playback' },
  { id: 'frame-forward', keys: ['arrowright'], action: 'frameForward', description: 'Next frame', category: 'playback' },
  { id: 'jump-back', keys: ['shift', 'arrowleft'], action: 'jumpBack', description: 'Jump back 1s', category: 'playback' },
  { id: 'jump-forward', keys: ['shift', 'arrowright'], action: 'jumpForward', description: 'Jump forward 1s', category: 'playback' },
  { id: 'go-to-start', keys: ['home'], action: 'goToStart', description: 'Go to start', category: 'playback' },
  { id: 'go-to-end', keys: ['end'], action: 'goToEnd', description: 'Go to end', category: 'playback' },
  { id: 'toggle-loop', keys: ['ctrl', 'l'], action: 'toggleLoop', description: 'Toggle loop', category: 'playback' },
  { id: 'toggle-fullscreen', keys: ['f'], action: 'toggleFullscreen', description: 'Toggle fullscreen', category: 'playback' },

  // Timeline
  { id: 'zoom-in', keys: ['=', '+'], action: 'zoomIn', description: 'Zoom in', category: 'timeline' },
  { id: 'zoom-out', keys: ['-', '_'], action: 'zoomOut', description: 'Zoom out', category: 'timeline' },
  { id: 'zoom-fit', keys: ['\\'], action: 'zoomFit', description: 'Zoom to fit', category: 'timeline' },
  { id: 'scroll-left', keys: ['shift', 'arrowleft'], action: 'scrollLeft', description: 'Scroll left', category: 'timeline' },
  { id: 'scroll-right', keys: ['shift', 'arrowright'], action: 'scrollRight', description: 'Scroll right', category: 'timeline' },
  { id: 'scroll-up', keys: ['shift', 'arrowup'], action: 'scrollUp', description: 'Scroll up', category: 'timeline' },
  { id: 'scroll-down', keys: ['shift', 'arrowdown'], action: 'scrollDown', description: 'Scroll down', category: 'timeline' },
  { id: 'snap-toggle', keys: ['s'], action: 'toggleSnap', description: 'Toggle snap', category: 'timeline' },
  { id: 'ripple-toggle', keys: ['r'], action: 'toggleRipple', description: 'Toggle ripple', category: 'timeline' },
  { id: 'add-video-track', keys: ['ctrl', 'alt', 'v'], action: 'addVideoTrack', description: 'Add video track', category: 'timeline' },
  { id: 'add-audio-track', keys: ['ctrl', 'alt', 'a'], action: 'addAudioTrack', description: 'Add audio track', category: 'timeline' },

  // Editing
  { id: 'split', keys: ['ctrl', 'k'], action: 'split', description: 'Split at playhead', category: 'editing' },
  { id: 'trim-start', keys: ['q'], action: 'trimStart', description: 'Trim start to playhead', category: 'editing' },
  { id: 'trim-end', keys: ['w'], action: 'trimEnd', description: 'Trim end to playhead', category: 'editing' },
  { id: 'ripple-delete', keys: ['shift', 'delete'], action: 'rippleDelete', description: 'Ripple delete', category: 'editing' },
  { id: 'lift', keys: ['ctrl', 'shift', 'x'], action: 'lift', description: 'Lift', category: 'editing' },
  { id: 'extract', keys: ['ctrl', 'x'], action: 'extract', description: 'Extract', category: 'editing' },
  { id: 'overwrite', keys: ['ctrl', 'shift', 'v'], action: 'overwrite', description: 'Overwrite', category: 'editing' },
  { id: 'add-transition', keys: ['ctrl', 't'], action: 'addTransition', description: 'Add default transition', category: 'editing' },

  // Tools
  { id: 'tool-select', keys: ['v'], action: 'toolSelect', description: 'Selection tool', category: 'editing' },
  { id: 'tool-blade', keys: ['c'], action: 'toolBlade', description: 'Blade tool', category: 'editing' },
  { id: 'tool-hand', keys: ['h'], action: 'toolHand', description: 'Hand tool', category: 'editing' },
  { id: 'tool-zoom', keys: ['z'], action: 'toolZoom', description: 'Zoom tool', category: 'editing' },
  { id: 'tool-text', keys: ['t'], action: 'toolText', description: 'Text tool', category: 'editing' },
  { id: 'tool-pen', keys: ['p'], action: 'toolPen', description: 'Pen tool', category: 'editing' },

  // View
  { id: 'toggle-media-panel', keys: ['ctrl', '1'], action: 'toggleMediaPanel', description: 'Toggle media panel', category: 'view' },
  { id: 'toggle-effects-panel', keys: ['ctrl', '2'], action: 'toggleEffectsPanel', description: 'Toggle effects panel', category: 'view' },
  { id: 'toggle-properties-panel', keys: ['ctrl', '3'], action: 'togglePropertiesPanel', description: 'Toggle properties panel', category: 'view' },
  { id: 'toggle-timeline-panel', keys: ['ctrl', '4'], action: 'toggleTimelinePanel', description: 'Toggle timeline panel', category: 'view' },
  { id: 'toggle-audio-meters', keys: ['ctrl', 'shift', 'a'], action: 'toggleAudioMeters', description: 'Toggle audio meters', category: 'view' },
  { id: 'toggle-keyframes', keys: ['ctrl', 'shift', 'k'], action: 'toggleKeyframes', description: 'Toggle keyframes', category: 'view' },
]

function keysMatch(event: KeyboardEvent, keys: string[]): boolean {
  const pressed = new Set<string>()
  if (event.ctrlKey || event.metaKey) pressed.add('ctrl')
  if (event.shiftKey) pressed.add('shift')
  if (event.altKey) pressed.add('alt')
  if (event.metaKey) pressed.add('meta')

  const key = event.key.toLowerCase()
  if (key === ' ') pressed.add('space')
  else if (key === 'escape') pressed.add('escape')
  else if (key === 'arrowleft') pressed.add('arrowleft')
  else if (key === 'arrowright') pressed.add('arrowright')
  else if (key === 'arrowup') pressed.add('arrowup')
  else if (key === 'arrowdown') pressed.add('arrowdown')
  else if (key === 'home') pressed.add('home')
  else if (key === 'end') pressed.add('end')
  else if (key === 'delete') pressed.add('delete')
  else if (key === 'backspace') pressed.add('backspace')
  else if (key === 'enter') pressed.add('enter')
  else if (key === 'tab') pressed.add('tab')
  else pressed.add(key)

  return keys.every((k) => pressed.has(k)) && pressed.size === keys.length
}

export function useKeyboardShortcuts() {
  const {
    project,
    saveProject,
    pushHistory,
    undo,
    redo,
    timeline,
    setPlayhead,
    setTimelineZoom,
    toggleSnap,
    toggleRipple,
    addTrack,
    splitClip,
    timeline: { duration, playhead, zoom },
    preview,
    setPreviewPlaying,
    setPreviewTime,
    togglePreviewLoop,
    clearSelection,
  } = useProjectStore()

  const shortcutsRef = useRef<Shortcut[]>(defaultShortcuts)
  const actionHandlersRef = useRef<Record<string, () => void>>({})

  // Initialize action handlers
  useEffect(() => {
    actionHandlersRef.current = {
      save: () => saveProject(),
      undo: () => undo(),
      redo: () => redo(),
      copy: () => {}, // TODO: implement clipboard
      paste: () => {}, // TODO: implement clipboard
      duplicate: () => {
        const selection = useProjectStore.getState().timeline.selection
        selection.forEach((id) => useProjectStore.getState().duplicateClip(id))
      },
      delete: () => {
        const selection = useProjectStore.getState().timeline.selection
        selection.forEach((id) => useProjectStore.getState().removeClip(id))
      },
      selectAll: () => {
        if (!project) return
        const allClips = project.tracks.flatMap((t) => t.clips.map((c) => c.id))
        useProjectStore.getState().setClipSelection(allClips)
      },
      deselect: () => clearSelection(),
      preferences: () => {}, // TODO: open preferences

      playPause: () => setPreviewPlaying(!preview.playing),
      stop: () => { setPreviewPlaying(false); setPreviewTime(0) },
      frameBack: () => setPlayhead(playhead - 1),
      frameForward: () => setPlayhead(playhead + 1),
      jumpBack: () => setPlayhead(playhead - (project?.settings.frameRate || 30)),
      jumpForward: () => setPlayhead(playhead + (project?.settings.frameRate || 30)),
      goToStart: () => setPlayhead(0),
      goToEnd: () => setPlayhead(duration),
      toggleLoop: () => togglePreviewLoop(),
      toggleFullscreen: () => {}, // TODO: implement fullscreen

      zoomIn: () => setTimelineZoom(zoom * 1.2),
      zoomOut: () => setTimelineZoom(zoom / 1.2),
      zoomFit: () => setTimelineZoom(100),
      scrollLeft: () => useProjectStore.getState().setTimelineScroll(timeline.scrollX - 200, timeline.scrollY),
      scrollRight: () => useProjectStore.getState().setTimelineScroll(timeline.scrollX + 200, timeline.scrollY),
      scrollUp: () => useProjectStore.getState().setTimelineScroll(timeline.scrollX, timeline.scrollY - 100),
      scrollDown: () => useProjectStore.getState().setTimelineScroll(timeline.scrollX, timeline.scrollY + 100),
      toggleSnap: () => toggleSnap(),
      toggleRipple: () => toggleRipple(),
      addVideoTrack: () => addTrack('video'),
      addAudioTrack: () => addTrack('audio'),

      split: () => {
        const selection = useProjectStore.getState().timeline.selection
        selection.forEach((id) => splitClip(id, playhead))
      },
      trimStart: () => {
        const selection = useProjectStore.getState().timeline.selection
        selection.forEach((id) => {
          const clip = getClipById(id)
          if (clip && playhead > clip.start && playhead < clip.end) {
            useProjectStore.getState().trimClip(id, playhead, clip.end)
          }
        })
      },
      trimEnd: () => {
        const selection = useProjectStore.getState().timeline.selection
        selection.forEach((id) => {
          const clip = getClipById(id)
          if (clip && playhead > clip.start && playhead < clip.end) {
            useProjectStore.getState().trimClip(id, clip.start, playhead)
          }
        })
      },
      rippleDelete: () => {}, // TODO: implement
      lift: () => {}, // TODO: implement
      extract: () => {}, // TODO: implement
      overwrite: () => {}, // TODO: implement
      addTransition: () => {}, // TODO: implement

      toolSelect: () => {}, // TODO: set tool
      toolBlade: () => {}, // TODO: set tool
      toolHand: () => {}, // TODO: set tool
      toolZoom: () => {}, // TODO: set tool
      toolText: () => {}, // TODO: set tool
      toolPen: () => {}, // TODO: set tool

      toggleMediaPanel: () => {}, // TODO: toggle panel
      toggleEffectsPanel: () => {}, // TODO: toggle panel
      togglePropertiesPanel: () => {}, // TODO: toggle panel
      toggleTimelinePanel: () => {}, // TODO: toggle panel
      toggleAudioMeters: () => {}, // TODO: toggle panel
      toggleKeyframes: () => {}, // TODO: toggle panel
    }
  }, [
    project,
    saveProject,
    pushHistory,
    undo,
    redo,
    timeline,
    setPlayhead,
    setTimelineZoom,
    toggleSnap,
    toggleRipple,
    addTrack,
    splitClip,
    duration,
    playhead,
    zoom,
    preview,
    setPreviewPlaying,
    setPreviewTime,
    togglePreviewLoop,
    clearSelection,
  ])

  function getClipById(clipId: string) {
    if (!project) return null
    for (const track of project.tracks) {
      const clip = track.clips.find((c) => c.id === clipId)
      if (clip) return clip
    }
    return null
  }

  const handleKeyDown = useCallback(
    (event: KeyboardEvent) => {
      // Don't trigger shortcuts when typing in inputs
      const target = event.target as HTMLElement
      if (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA' || target.isContentEditable) {
        return
      }

      for (const shortcut of shortcutsRef.current) {
        if (keysMatch(event, shortcut.keys)) {
          event.preventDefault()
          event.stopPropagation()
          const handler = actionHandlersRef.current[shortcut.action]
          if (handler) handler()
          break
        }
      }
    },
    []
  )

  useEffect(() => {
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [handleKeyDown])

  return {
    shortcuts: shortcutsRef.current,
    registerShortcut: (shortcut: Shortcut) => {
      shortcutsRef.current = [...shortcutsRef.current.filter((s) => s.id !== shortcut.id), shortcut]
    },
    unregisterShortcut: (id: string) => {
      shortcutsRef.current = shortcutsRef.current.filter((s) => s.id !== id)
    },
  }
}