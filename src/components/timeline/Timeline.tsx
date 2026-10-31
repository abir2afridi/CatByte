'use client'

import { useRef, useEffect, useCallback, useState, useMemo } from 'react'
import { cn } from '@/utils/cn'
import { useProjectStore } from '@/store/projectStore'
import { useTimelineUtils } from '@/hooks/useMedia'
import type { Track, Clip } from '@/types'

export function Timeline() {
  const timelineRef = useRef<HTMLDivElement>(null)
  const rulerRef = useRef<HTMLDivElement>(null)
  const tracksContainerRef = useRef<HTMLDivElement>(null)
  const playheadRef = useRef<HTMLDivElement>(null)

  const {
    project,
    timeline,
    setPlayhead,
    setTimelineZoom,
    setTimelineScroll,
    toggleSnap,
    toggleRipple,
    timeline: { snapEnabled, rippleMode },
    setClipSelection,
    addToSelection,
    removeFromSelection,
    moveClip,
    trimClip,
    splitClip,
    duplicateClip,
    removeClip,
  } = useProjectStore()

  const { frameToX, xToFrame, formatTimecode, snapToGrid } = useTimelineUtils()
  const [isScrolling, setIsScrolling] = useState(false)
  const [isDraggingPlayhead, setIsDraggingPlayhead] = useState(false)
  const [dragState, setDragState] = useState<{
    type: 'move' | 'trim-start' | 'trim-end'
    clipId: string
    trackId: string
    startX: number
    startFrame: number
    clipStart: number
    clipEnd: number
  } | null>(null)

  const allTracks = project?.tracks || []
  const videoTracks = useMemo(() => allTracks.filter((t) => t.type === 'video'), [allTracks])
  const audioTracks = useMemo(() => allTracks.filter((t) => t.type === 'audio'), [allTracks])

  // Handle horizontal scroll sync
  useEffect(() => {
    const ruler = rulerRef.current
    const tracks = tracksContainerRef.current
    if (!ruler || !tracks) return

    const handleScroll = () => {
      ruler.scrollLeft = tracks.scrollLeft
    }

    tracks.addEventListener('scroll', handleScroll)
    return () => tracks.removeEventListener('scroll', handleScroll)
  }, [])

  // Update playhead position
  useEffect(() => {
    if (playheadRef.current && !isDraggingPlayhead) {
      const x = frameToX(timeline.playhead)
      playheadRef.current.style.transform = `translateX(${x}px)`
    }
  }, [timeline.playhead, frameToX, isDraggingPlayhead])

  // Handle keyboard shortcuts for timeline
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.target instanceof HTMLInputElement) return

      const frameRate = project?.settings.frameRate || 30
      const frameStep = e.shiftKey ? frameRate : 1

      switch (e.key) {
        case 'ArrowLeft':
          e.preventDefault()
          setPlayhead(Math.max(0, timeline.playhead - frameStep))
          break
        case 'ArrowRight':
          e.preventDefault()
          setPlayhead(Math.min(timeline.duration, timeline.playhead + frameStep))
          break
        case 'Home':
          e.preventDefault()
          setPlayhead(0)
          break
        case 'End':
          e.preventDefault()
          setPlayhead(timeline.duration)
          break
      }
    }

    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [timeline.playhead, timeline.duration, project?.settings.frameRate, setPlayhead])

  const handleRulerClick = useCallback(
    (e: React.MouseEvent<HTMLDivElement>) => {
      if (e.button !== 0) return
      const rect = rulerRef.current?.getBoundingClientRect()
      if (!rect) return
      const x = e.clientX - rect.left + timeline.scrollX
      const frame = snapToGrid(xToFrame(x))
      setPlayhead(frame)
    },
    [timeline.scrollX, xToFrame, snapToGrid, setPlayhead]
  )

  const handlePlayheadMouseDown = useCallback(
    (e: React.MouseEvent<HTMLDivElement>) => {
      if (e.button !== 0) return
      e.preventDefault()
      e.stopPropagation()
      setIsDraggingPlayhead(true)
    },
    []
  )

  const handleTrackMouseDown = useCallback(
    (e: React.MouseEvent<HTMLDivElement>, track: Track) => {
      if (e.button !== 0) return
      if (track.locked) return

      const rect = tracksContainerRef.current?.getBoundingClientRect()
      if (!rect) return

      const x = e.clientX - rect.left + timeline.scrollX
      const frame = snapToGrid(xToFrame(x))

      // Check if clicking on a clip
      const clip = track.clips.find((c) => c.start <= frame && c.end > frame)
      if (clip) {
        // Check if near edges for trimming
        const clipStartX = frameToX(clip.start)
        const clipEndX = frameToX(clip.end)
        const clickX = x - timeline.scrollX

        const trimThreshold = 10 / timeline.zoom // 10px in frame units

        if (Math.abs(clickX - clipStartX) < trimThreshold) {
          setDragState({
            type: 'trim-start',
            clipId: clip.id,
            trackId: track.id,
            startX: clickX,
            startFrame: frame,
            clipStart: clip.start,
            clipEnd: clip.end,
          })
        } else if (Math.abs(clickX - clipEndX) < trimThreshold) {
          setDragState({
            type: 'trim-end',
            clipId: clip.id,
            trackId: track.id,
            startX: clickX,
            startFrame: frame,
            clipStart: clip.start,
            clipEnd: clip.end,
          })
        } else {
          // Move clip
          const isSelected = timeline.selection.includes(clip.id)
          if (!isSelected) {
            if (e.shiftKey) addToSelection(clip.id)
            else setClipSelection([clip.id])
          }

          setDragState({
            type: 'move',
            clipId: clip.id,
            trackId: track.id,
            startX: clickX,
            startFrame: frame,
            clipStart: clip.start,
            clipEnd: clip.end,
          })
        }
      } else {
        // Click on empty space - deselect
        if (!e.shiftKey && !e.ctrlKey && !e.metaKey) {
          setClipSelection([])
        }
        // Set playhead
        setPlayhead(frame)
      }
    },
    [timeline.scrollX, timeline.zoom, timeline.selection, frameToX, xToFrame, snapToGrid, setPlayhead, setClipSelection, addToSelection]
  )

  const handleMouseMove = useCallback(
    (e: MouseEvent) => {
      if (!dragState) return
      e.preventDefault()

      const rect = tracksContainerRef.current?.getBoundingClientRect()
      if (!rect) return

      const x = e.clientX - rect.left + timeline.scrollX
      const frame = snapToGrid(xToFrame(x))
      const deltaFrames = frame - dragState.startFrame

      if (dragState.type === 'move') {
        const newStart = Math.max(0, dragState.clipStart + deltaFrames)
        const newEnd = newStart + (dragState.clipEnd - dragState.clipStart)
        if (newEnd <= timeline.duration) {
          moveClip(dragState.clipId, newStart)
          if (playheadRef.current) {
            playheadRef.current.style.transform = `translateX(${frameToX(timeline.playhead)}px)`
          }
        }
      } else if (dragState.type === 'trim-start') {
        const newStart = Math.max(0, Math.min(dragState.clipEnd - 1, dragState.clipStart + deltaFrames))
        trimClip(dragState.clipId, newStart, dragState.clipEnd)
      } else if (dragState.type === 'trim-end') {
        const newEnd = Math.min(timeline.duration, Math.max(dragState.clipStart + 1, dragState.clipEnd + deltaFrames))
        trimClip(dragState.clipId, dragState.clipStart, newEnd)
      }
    },
    [dragState, timeline.scrollX, timeline.zoom, timeline.duration, xToFrame, snapToGrid, moveClip, trimClip, frameToX]
  )

  const handleMouseUp = useCallback(() => {
    setDragState(null)
    setIsDraggingPlayhead(false)
  }, [])

  useEffect(() => {
    if (dragState || isDraggingPlayhead) {
      window.addEventListener('mousemove', handleMouseMove)
      window.addEventListener('mouseup', handleMouseUp)
    }
    return () => {
      window.removeEventListener('mousemove', handleMouseMove)
      window.removeEventListener('mouseup', handleMouseUp)
    }
  }, [dragState, isDraggingPlayhead, handleMouseMove, handleMouseUp])

  // Handle playhead drag
  useEffect(() => {
    if (!isDraggingPlayhead) return

    const handleMove = (e: MouseEvent) => {
      const rect = rulerRef.current?.getBoundingClientRect()
      if (!rect) return
      const x = e.clientX - rect.left + timeline.scrollX
      const frame = snapToGrid(xToFrame(x))
      setPlayhead(frame)
    }

    window.addEventListener('mousemove', handleMove)
    return () => window.removeEventListener('mousemove', handleMove)
  }, [isDraggingPlayhead, timeline.scrollX, xToFrame, snapToGrid, setPlayhead])

  // Wheel zoom
  const handleWheel = useCallback(
    (e: React.WheelEvent) => {
      if (e.ctrlKey || e.metaKey) {
        e.preventDefault()
        const zoomFactor = e.deltaY > 0 ? 0.9 : 1.1
        const newZoom = Math.max(10, Math.min(500, timeline.zoom * zoomFactor))
        setTimelineZoom(newZoom)
      } else if (e.shiftKey) {
        e.preventDefault()
        setTimelineScroll(timeline.scrollX + e.deltaY, timeline.scrollY)
      } else {
        setTimelineScroll(timeline.scrollX + e.deltaX, timeline.scrollY + e.deltaY)
      }
    },
    [timeline.zoom, timeline.scrollX, timeline.scrollY, setTimelineZoom, setTimelineScroll]
  )

  // Render ruler
  const rulerMarks = useMemo(() => {
    const marks: { frame: number; label: string; major: boolean }[] = []
    const frameRate = project?.settings.frameRate || 30
    const secondsPerMark = Math.max(1, Math.ceil(100 / timeline.zoom))
    const framesPerMark = secondsPerMark * frameRate

    for (let frame = 0; frame <= timeline.duration; frame += framesPerMark) {
      const seconds = frame / frameRate
      const mins = Math.floor(seconds / 60)
      const secs = Math.floor(seconds % 60)
      marks.push({
        frame,
        label: `${mins}:${secs.toString().padStart(2, '0')}`,
        major: frame % (frameRate * 10) === 0,
      })
    }
    return marks
  }, [timeline.duration, timeline.zoom, project?.settings.frameRate])

  return (
    <div ref={timelineRef} className="flex h-full w-full flex-col overflow-hidden" onWheel={handleWheel}>
      {/* Ruler */}
      <div
        ref={rulerRef}
        className="flex h-8 shrink-0 items-end border-b border-border bg-secondary/50 px-2 overflow-x-auto no-scrollbar"
        style={{ minWidth: `${frameToX(timeline.duration)}px` }}
        onClick={handleRulerClick}
      >
        {rulerMarks.map((mark) => (
          <div
            key={mark.frame}
            className={cn(
              'absolute bottom-0 w-px h-4 bg-border transition-colors',
              mark.major && 'h-6 bg-border/50'
            )}
            style={{
              left: `${frameToX(mark.frame)}px`,
              transform: 'translateX(-50%)',
            }}
          >
            {mark.major && (
              <span className="absolute bottom-7 left-1/2 -translate-x-1/2 text-[10px] text-muted-foreground whitespace-nowrap">
                {mark.label}
              </span>
            )}
          </div>
        ))}

        {/* Playhead on ruler */}
        <div
          ref={playheadRef}
          className="absolute top-0 bottom-0 w-[2px] bg-timeline-playhead pointer-events-none z-10"
          style={{ transform: `translateX(${frameToX(timeline.playhead)}px)` }}
          onMouseDown={handlePlayheadMouseDown}
        >
          <div className="absolute -left-1.5 top-0 w-3 h-3 rounded-full bg-timeline-playhead border-2 border-background" />
          <div className="absolute -left-1.5 bottom-0 w-3 h-3 rounded-full bg-timeline-playhead border-2 border-background" />
        </div>
      </div>

      {/* Tracks Container */}
      <div
        ref={tracksContainerRef}
        className="flex-1 overflow-auto relative"
        style={{ minWidth: `${frameToX(timeline.duration)}px` }}
      >
        {/* Video Tracks */}
        {videoTracks.map((track, index) => (
          <TimelineTrack
            key={track.id}
            track={track}
            index={index}
            isVideo={true}
            onTrackMouseDown={handleTrackMouseDown}
            dragState={dragState}
            frameToX={frameToX}
            timeline={timeline}
          />
        ))}

        {/* Divider */}
        {videoTracks.length > 0 && audioTracks.length > 0 && (
          <div className="h-1 bg-border/50 flex-shrink-0" />
        )}

        {/* Audio Tracks */}
        {audioTracks.map((track, index) => (
          <TimelineTrack
            key={track.id}
            track={track}
            index={index}
            isVideo={false}
            onTrackMouseDown={handleTrackMouseDown}
            dragState={dragState}
            frameToX={frameToX}
            timeline={timeline}
          />
        ))}

        {/* Empty state */}
        {videoTracks.length === 0 && audioTracks.length === 0 && (
          <div className="flex h-full items-center justify-center text-muted-foreground">
            <p className="text-center p-4">
              No tracks yet. Add tracks from the toolbar or right-click in the track area.
            </p>
          </div>
        )}
      </div>

      {/* Timeline Toolbar */}
      <div className="flex h-10 shrink-0 items-center gap-2 border-t border-border bg-secondary/50 px-3">
        <div className="flex items-center gap-1">
          <button
            className={cn('btn-ghost h-7 w-7 p-0', snapEnabled && 'bg-primary/20')}
            onClick={toggleSnap}
            title="Snap (S)"
          >
            <svg className="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" />
              <path d="M6.5 7H20A2.5 2.5 0 0 1 22.5 9.5" />
            </svg>
          </button>
          <button
            className={cn('btn-ghost h-7 w-7 p-0', rippleMode && 'bg-primary/20')}
            onClick={toggleRipple}
            title="Ripple Edit (R)"
          >
            <svg className="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M4 12H3a6 6 0 0 0 6 6" />
              <path d="M20 12h1a6 6 0 0 0 6-6" />
              <path d="M12 4V3a6 6 0 0 0-6 6" />
              <path d="M12 20v1a6 6 0 0 0 6-6" />
            </svg>
          </button>
        </div>

        <div className="separator-vertical h-5 mx-1" />

        <div className="flex items-center gap-1">
          <span className="text-xs text-muted-foreground">Zoom:</span>
          <input
            type="range"
            min="10"
            max="500"
            value={timeline.zoom}
            onChange={(e) => setTimelineZoom(Number(e.target.value))}
            className="w-32 h-1 accent-primary"
            aria-label="Timeline zoom"
          />
          <span className="text-xs text-muted-foreground w-16 text-right">
            {timeline.zoom} px/f
          </span>
        </div>

        <div className="flex-1" />

        <div className="flex items-center gap-2 text-xs text-muted-foreground font-mono">
          <span>{formatTimecode(timeline.playhead)}</span>
          <span>/</span>
          <span>{formatTimecode(timeline.duration)}</span>
          <span className="text-muted-foreground/50">({Math.round(timeline.playhead / (project?.settings.frameRate || 30) * 100) / 100}s)</span>
        </div>
      </div>
    </div>
  )
}

interface TimelineTrackProps {
  track: Track
  index: number
  isVideo: boolean
  onTrackMouseDown: (e: React.MouseEvent<HTMLDivElement>, track: Track) => void
  dragState: { clipId: string; type: string } | null
  frameToX: (frame: number) => number
  timeline: { playhead: number; duration: number; zoom: number; scrollX: number; selection: string[] }
}

function TimelineTrack({
  track,
  index,
  isVideo,
  onTrackMouseDown,
  dragState,
  frameToX,
  timeline,
}: TimelineTrackProps) {
  const trackHeight = isVideo ? 80 : 60
  const isDragging = dragState?.clipId && timeline.selection.includes(dragState.clipId)

  return (
    <div
      className={cn(
        'relative border-b border-border/50 flex-shrink-0',
        'bg-timeline-track',
        track.muted && 'opacity-50',
        track.solo && 'ring-1 ring-primary',
        track.locked && 'pointer-events-none'
      )}
      style={{ height: trackHeight }}
      onMouseDown={(e) => onTrackMouseDown(e, track)}
      onDoubleClick={(e) => {
        // Add clip at position
        const rect = e.currentTarget.getBoundingClientRect()
        const x = e.clientX - rect.left + timeline.scrollX
        // TODO: Open media picker or add selected media
      }}
    >
      {/* Track Header */}
      <div className="absolute left-0 top-0 z-10 flex h-full w-32 items-center px-2 border-r border-border/50 bg-secondary/50">
        <div className="flex w-full items-center gap-2">
          <input
            type="checkbox"
            checked={track.enabled}
            onChange={(e) => {
              e.stopPropagation()
              useProjectStore.getState().updateTrack(track.id, { enabled: e.target.checked })
            }}
            className="h-4 w-4 accent-primary"
            title={track.enabled ? 'Hide track' : 'Show track'}
            aria-label={track.enabled ? 'Hide track' : 'Show track'}
          />
          {track.locked ? (
            <button
              onClick={(e) => {
                e.stopPropagation()
                useProjectStore.getState().toggleTrackLocked(track.id)
              }}
              className="p-1 text-muted-foreground hover:text-foreground"
              title="Unlock track"
            >
              <svg className="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                <path d="M7 11V7a5 5 0 0 1 10 0v4" />
              </svg>
            </button>
          ) : (
            <button
              onClick={(e) => {
                e.stopPropagation()
                useProjectStore.getState().toggleTrackLocked(track.id)
              }}
              className="p-1 text-muted-foreground hover:text-foreground"
              title="Lock track"
            >
              <svg className="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                <path d="M7 11V7a5 5 0 0 1 9.9-1" />
              </svg>
            </button>
          )}
          <span className="text-xs font-medium truncate">{track.name}</span>
          {track.solo && <span className="text-xs px-1 py-0.5 bg-primary/20 text-primary rounded">S</span>}
          {track.muted && <span className="text-xs px-1 py-0.5 bg-muted text-muted-foreground rounded">M</span>}
        </div>
      </div>

      {/* Track Content */}
      <div
        className="absolute left-32 right-0 top-0 bottom-0 relative overflow-hidden"
        style={{ minWidth: `${frameToX(timeline.duration)}px` }}
      >
        {/* Clips */}
        {track.clips.map((clip) => (
          <TimelineClip
            key={clip.id}
            clip={clip}
            track={track}
            isVideo={isVideo}
            frameToX={frameToX}
            timeline={timeline}
            isDragging={isDragging && dragState?.clipId === clip.id}
            dragType={dragState?.type}
          />
        ))}

        {/* Drop zone indicator */}
        {!track.locked && (
          <div className="absolute inset-0 border-2 border-dashed border-border/50 rounded pointer-events-none" />
        )}
      </div>
    </div>
  )
}

interface TimelineClipProps {
  clip: Clip
  track: Track
  isVideo: boolean
  frameToX: (frame: number) => number
  timeline: { playhead: number; duration: number; zoom: number; scrollX: number; selection: string[] }
  isDragging: boolean
  dragType?: string
}

function TimelineClip({ clip, track, isVideo, frameToX, timeline, isDragging, dragType }) {
  const startX = frameToX(clip.start)
  const endX = frameToX(clip.end)
  const width = Math.max(2, endX - startX)
  const isSelected = timeline.selection.includes(clip.id)
  const frameRate = 30 // Would come from project settings

  // Generate a color based on clip ID for visual distinction
  const hue = (clip.id.split('-')[0].charCodeAt(0) * 137) % 360

  return (
    <div
      className={cn(
        'timeline-clip absolute top-1 bottom-1 rounded border border-border/50 cursor-grab active:cursor-grabbing select-none transition-all',
        isSelected && 'timeline-clip-selected ring-2 ring-primary ring-offset-2 ring-offset-background',
        isDragging && 'opacity-80 z-20 shadow-lg',
        dragType === 'trim-start' && 'cursor-ew-resize',
        dragType === 'trim-end' && 'cursor-ew-resize',
        track.locked && 'cursor-not-allowed opacity-50'
      )}
      style={{
        left: `${startX}px`,
        width: `${width}px`,
        backgroundColor: `hsl(${hue}, 70%, 45%)`,
      }}
      title={`${clip.name} (${((clip.end - clip.start) / frameRate).toFixed(1)}s)`}
      onContextMenu={(e) => {
        e.preventDefault()
        // TODO: Show context menu
      }}
      onDoubleClick={(e) => {
        e.stopPropagation()
        // TODO: Open clip properties
      }}
    >
      {/* Clip thumbnail placeholder */}
      {isVideo && width > 60 && (
        <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
          <span className="text-xs text-white/70 font-mono">{clip.name}</span>
        </div>
      )}

      {/* Audio waveform placeholder */}
      {!isVideo && (
        <div className="absolute inset-y-1 left-1 right-1 h-full bg-white/10 rounded pointer-events-none" />
      )}

      {/* Selection indicator */}
      {isSelected && (
        <div className="absolute inset-0 border-2 border-primary/50 rounded pointer-events-none" />
      )}

      {/* Trim handles */}
      {(isSelected || isDragging) && width > 40 && (
        <>
          <div className="absolute left-0 top-0 bottom-0 w-2 bg-primary/50 hover:bg-primary cursor-ew-resize" />
          <div className="absolute right-0 top-0 bottom-0 w-2 bg-primary/50 hover:bg-primary cursor-ew-resize" />
        </>
      )}

      {/* Clip name */}
      {width > 80 && (
        <div className="absolute left-2 right-2 top-1 bottom-1 truncate text-xs text-white/90 pointer-events-none overflow-hidden">
          {clip.name}
        </div>
      )}

      {/* Speed indicator */}
      {clip.speed !== 1 && (
        <div className="absolute top-1 right-1 px-1 py-0.5 text-[9px] bg-black/70 text-white rounded pointer-events-none">
          {clip.speed}x
        </div>
      )}

      {/* Effects badge */}
      {clip.effects.length > 0 && (
        <div className="absolute top-1 left-1 px-1 py-0.5 text-[9px] bg-primary/80 text-primary-foreground rounded pointer-events-none">
          fx{clip.effects.length}
        </div>
      )}

      {/* Keyframe indicators */}
      {clip.keyframes.length > 0 && (
        <div className="absolute bottom-1 left-2 right-2 flex gap-1 pointer-events-none">
          {clip.keyframes.slice(0, 5).map((kf) => (
            <div
              key={kf.id}
              className="w-1.5 h-1.5 rounded-full bg-yellow-400"
              style={{ left: `${((kf.frame - clip.start) / (clip.end - clip.start)) * 100}%` }}
            />
          ))}
        </div>
      )}
    </div>
  )
}