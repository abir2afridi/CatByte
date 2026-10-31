'use client'

import { useEffect, useRef, useCallback, useState } from 'react'
import * as Fabric from 'fabric'
import { Maximize2, Minimize2, RotateCcw, RotateCw, ZoomIn, ZoomOut, Grid, Square, MousePointer2, Hand } from 'lucide-react'
import { cn } from '@/utils/cn'
import { useProjectStore } from '@/store/projectStore'
import { useTimelineUtils } from '@/hooks/useMedia'
import type { Clip, ClipTransform } from '@/types'

export function Preview() {
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const containerRef = useRef<HTMLDivElement>(null)
  const fabricCanvasRef = useRef<Fabric.Canvas | null>(null)
  const animationFrameRef = useRef<number>()

  const {
    project,
    timeline,
    preview,
    updateClipTransform,
    setPreviewTime,
  } = useProjectStore()

  const { frameToTime, formatTimecode } = useTimelineUtils()
  const [zoom, setZoom] = useState(1)
  const [showSafeZones, setShowSafeZones] = useState(false)
  const [showGuides, setShowGuides] = useState(false)
  const [tool, setTool] = useState<'select' | 'hand' | 'zoom'>('select')

  // Get video tracks only
  const videoTracks = project?.tracks.filter((t) => t.type === 'video' && t.enabled) || []

  // Initialize Fabric canvas
  useEffect(() => {
    if (!canvasRef.current || fabricCanvasRef.current) return

    const canvas = new Fabric.Canvas(canvasRef.current, {
      backgroundColor: preview.backgroundColor,
      selection: tool === 'select',
      preserveObjectStacking: true,
      perPixelTargetFind: true,
      targetFindTolerance: 5,
    })

    fabricCanvasRef.current = canvas

    // Handle object selection
    canvas.on('selection:created', (e) => {
      const selected = e.selected
      if (selected?.length === 1) {
        const obj = selected[0]
        const clipId = obj.clipId
        if (clipId) {
          useProjectStore.getState().setClipSelection([clipId])
        }
      } else if (selected && selected.length > 1) {
        const clipIds = selected.map((o) => o.clipId).filter(Boolean)
        useProjectStore.getState().setClipSelection(clipIds)
      }
    })

    canvas.on('selection:updated', (e) => {
      const selected = e.selected
      if (selected) {
        const clipIds = selected.map((o) => o.clipId).filter(Boolean)
        useProjectStore.getState().setClipSelection(clipIds)
      }
    })

    canvas.on('selection:cleared', () => {
      useProjectStore.getState().clearSelection()
    })

    // Handle object transform
    canvas.on('object:modified', (e) => {
      const obj = e.target
      if (obj?.clipId) {
        const transform: Partial<ClipTransform> = {
          x: (obj.left as number) + (obj.width as number) * (obj.originX as number),
          y: (obj.top as number) + (obj.height as number) * (obj.originY as number),
          scaleX: obj.scaleX as number,
          scaleY: obj.scaleY as number,
          rotation: Fabric.util.degreesToRadians(obj.angle as number),
        }
        updateClipTransform(obj.clipId, transform)
      }
    })

    // Handle mouse wheel zoom
    canvas.on('mouse:wheel', (opt) => {
      const delta = (opt.e as WheelEvent).deltaY
      const zoomFactor = delta > 0 ? 0.9 : 1.1
      const newZoom = Math.max(0.1, Math.min(5, zoom * zoomFactor))
      setZoom(newZoom)
      canvas.setZoom(newZoom)
      ;(opt.e as Event).preventDefault()
      ;(opt.e as Event).stopPropagation()
    })

    // Hand tool panning
    let isPanning = false
    let lastPos = { x: 0, y: 0 }

    canvas.on('mouse:down', (opt) => {
      const e = opt.e as MouseEvent
      if (tool === 'hand' && e.button === 0) {
        isPanning = true
        lastPos = { x: e.clientX, y: e.clientY }
        canvas.defaultCursor = 'grabbing'
      }
    })

    canvas.on('mouse:move', (opt) => {
      if (isPanning && tool === 'hand') {
        const e = opt.e as MouseEvent
        const dx = e.clientX - lastPos.x
        const dy = e.clientY - lastPos.y
        canvas.relativePan(new Fabric.Point(dx, dy))
        lastPos = { x: e.clientX, y: e.clientY }
      }
    })

    canvas.on('mouse:up', () => {
      isPanning = false
      canvas.defaultCursor = tool === 'hand' ? 'grab' : 'default'
    })

    // Double-click to fit
    canvas.on('mouse:dblclick', () => {
      fitToContainer()
    })

    // Initial fit
    fitToContainer()

    return () => {
      canvas.dispose()
      fabricCanvasRef.current = null
    }
  }, [])

  const fitToContainer = useCallback(() => {
    const canvas = fabricCanvasRef.current
    const container = containerRef.current
    if (!canvas || !container) return

    const containerWidth = container.clientWidth
    const containerHeight = container.clientHeight
    const projectWidth = project?.settings.width || 1920
    const projectHeight = project?.settings.height || 1080

    const scaleX = containerWidth / projectWidth
    const scaleY = containerHeight / projectHeight
    const scale = Math.min(scaleX, scaleY) * 0.95

    canvas.setZoom(scale)
    setZoom(scale)
    canvas.setViewportTransform([scale, 0, 0, scale, 0, 0])
    canvas.renderAll()
  }, [project?.settings.width, project?.settings.height])

  // Render current frame
  useEffect(() => {
    if (!fabricCanvasRef.current || !project) return

    const canvas = fabricCanvasRef.current
    const currentFrame = timeline.playhead

    // Clear canvas
    canvas.clear()
    canvas.backgroundColor = preview.backgroundColor

    // Render clips from all video tracks (bottom to top)
    for (const track of videoTracks) {
      for (const clip of track.clips) {
        if (clip.start <= currentFrame && clip.end > currentFrame) {
          renderClip(canvas, clip, currentFrame)
        }
      }
    }

    // Draw safe zones
    if (showSafeZones) {
      drawSafeZones(canvas)
    }

    // Draw guides
    if (showGuides) {
      drawGuides(canvas)
    }

    canvas.renderAll()
  }, [timeline.playhead, videoTracks, preview.backgroundColor, showSafeZones, showGuides, project])

  const renderClip = (canvas: Fabric.Canvas, clip: Clip, currentFrame: number) => {
    // In a real implementation, this would render video frames
    // For now, create a placeholder rect with clip info
    const projectWidth = project?.settings.width || 1920
    const projectHeight = project?.settings.height || 1080

    const rect = new Fabric.Rect({
      left: clip.transform.x,
      top: clip.transform.y,
      width: projectWidth * clip.transform.scaleX,
      height: projectHeight * clip.transform.scaleY,
      fill: `hsl(263, 70%, ${40 + Math.random() * 20}%)`,
      stroke: 'hsl(263, 70%, 60%)',
      strokeWidth: 2,
      angle: Fabric.util.radiansToDegrees(clip.transform.rotation),
      originX: 'center',
      originY: 'center',
      clipId: clip.id,
      selectable: tool === 'select',
      evented: tool === 'select',
      hasControls: tool === 'select',
      hasBorders: tool === 'select',
      transparentCorners: false,
      cornerColor: 'hsl(263, 70%, 60%)',
      cornerSize: 8,
      cornerStyle: 'circle',
    })

    // Add clip label
    const text = new Fabric.Text(clip.name, {
      left: clip.transform.x,
      top: clip.transform.y - (projectHeight * clip.transform.scaleY) / 2 - 15,
      fontSize: 12,
      fill: 'white',
      backgroundColor: 'rgba(0,0,0,0.7)',
      padding: 4,
      originX: 'center',
      fontFamily: 'Inter',
      clipId: clip.id,
      selectable: false,
      evented: false,
    })

    canvas.add(rect, text)
  }

  const drawSafeZones = (canvas: Fabric.Canvas) => {
    const width = project?.settings.width || 1920
    const height = project?.settings.height || 1080

    // Title safe (90%)
    const titleSafe = new Fabric.Rect({
      left: width / 2,
      top: height / 2,
      width: width * 0.9,
      height: height * 0.9,
      fill: 'transparent',
      stroke: 'rgba(255,255,0,0.8)',
      strokeWidth: 1 / zoom,
      strokeDashArray: [10 / zoom, 5 / zoom],
      originX: 'center',
      originY: 'center',
      selectable: false,
      evented: false,
    })

    // Action safe (95%)
    const actionSafe = new Fabric.Rect({
      left: width / 2,
      top: height / 2,
      width: width * 0.95,
      height: height * 0.95,
      fill: 'transparent',
      stroke: 'rgba(0,255,0,0.8)',
      strokeWidth: 1 / zoom,
      strokeDashArray: [10 / zoom, 5 / zoom],
      originX: 'center',
      originY: 'center',
      selectable: false,
      evented: false,
    })

    canvas.add(titleSafe, actionSafe)
  }

  const drawGuides = (canvas: Fabric.Canvas) => {
    const width = project?.settings.width || 1920
    const height = project?.settings.height || 1080

    // Center lines
    const hLine = new Fabric.Line([0, height / 2, width, height / 2], {
      stroke: 'rgba(255,255,255,0.3)',
      strokeWidth: 1 / zoom,
      selectable: false,
      evented: false,
    })

    const vLine = new Fabric.Line([width / 2, 0, width / 2, height], {
      stroke: 'rgba(255,255,255,0.3)',
      strokeWidth: 1 / zoom,
      selectable: false,
      evented: false,
    })

    // Rule of thirds
    const thirds = []
    for (let i = 1; i < 3; i++) {
      thirds.push(
        new Fabric.Line([0, (height / 3) * i, width, (height / 3) * i], {
          stroke: 'rgba(255,255,255,0.15)',
          strokeWidth: 1 / zoom,
          selectable: false,
          evented: false,
        }),
        new Fabric.Line([(width / 3) * i, 0, (width / 3) * i, height], {
          stroke: 'rgba(255,255,255,0.15)',
          strokeWidth: 1 / zoom,
          selectable: false,
          evented: false,
        })
      )
    }

    canvas.add(hLine, vLine, ...thirds)
  }

  // Handle container resize
  useEffect(() => {
    const handleResize = () => fitToContainer()
    window.addEventListener('resize', handleResize)
    return () => window.removeEventListener('resize', handleResize)
  }, [fitToContainer])

  // Update preview time when playhead changes
  useEffect(() => {
    setPreviewTime(frameToTime(timeline.playhead))
  }, [timeline.playhead, frameToTime, setPreviewTime])

  return (
    <div ref={containerRef} className="relative w-full h-full overflow-hidden bg-black">
      <canvas ref={canvasRef} className="w-full h-full" />

      {/* Preview Controls Overlay */}
      <div className="absolute bottom-2 left-2 right-2 flex items-center justify-between p-2 pointer-events-none">
        <div className="flex items-center gap-1 pointer-events-auto">
          <button
            className={cn('btn-ghost h-8 w-8 p-0', tool === 'select' && 'bg-primary/20')}
            onClick={() => setTool('select')}
            title="Selection Tool (V)"
          >
            <MousePointer2 className="h-4 w-4" />
          </button>
          <button
            className={cn('btn-ghost h-8 w-8 p-0', tool === 'hand' && 'bg-primary/20')}
            onClick={() => setTool('hand')}
            title="Hand Tool (H)"
          >
            <Hand className="h-4 w-4" />
          </button>
          <button
            className={cn('btn-ghost h-8 w-8 p-0', tool === 'zoom' && 'bg-primary/20')}
            onClick={() => setTool('zoom')}
            title="Zoom Tool (Z)"
          >
            <ZoomIn className="h-4 w-4" />
          </button>
          <div className="separator-vertical h-6 mx-1" />
          <button className="btn-ghost h-8 w-8 p-0" onClick={() => setZoom(Math.max(0.1, zoom - 0.1))} title="Zoom Out">
            <ZoomOut className="h-4 w-4" />
          </button>
          <span className="px-2 text-xs text-muted-foreground min-w-[45px] text-center">
            {Math.round(zoom * 100)}%
          </span>
          <button className="btn-ghost h-8 w-8 p-0" onClick={() => setZoom(Math.min(5, zoom + 0.1))} title="Zoom In">
            <ZoomIn className="h-4 w-4" />
          </button>
          <button className="btn-ghost h-8 w-8 p-0" onClick={fitToContainer} title="Fit to Window">
            <Maximize2 className="h-4 w-4" />
          </button>
        </div>

        <div className="flex items-center gap-2 pointer-events-auto">
          <button
            className={cn('btn-ghost h-8 w-8 p-0', showSafeZones && 'bg-primary/20')}
            onClick={() => setShowSafeZones(!showSafeZones)}
            title="Safe Zones"
          >
            <Square className="h-4 w-4" />
          </button>
          <button
            className={cn('btn-ghost h-8 w-8 p-0', showGuides && 'bg-primary/20')}
            onClick={() => setShowGuides(!showGuides)}
            title="Guides"
          >
            <Grid className="h-4 w-4" />
          </button>
          <button className="btn-ghost h-8 w-8 p-0" title="Rotate Left">
            <RotateCcw className="h-4 w-4" />
          </button>
          <button className="btn-ghost h-8 w-8 p-0" title="Rotate Right">
            <RotateCw className="h-4 w-4" />
          </button>
        </div>
      </div>

      {/* Timecode Display */}
      <div className="absolute top-2 left-2 pointer-events-none">
        <div className="bg-black/60 backdrop-blur-sm px-3 py-1 rounded font-mono text-xs text-white">
          {formatTimecode(timeline.playhead)} / {formatTimecode(timeline.duration)}
        </div>
      </div>

      {/* Resolution indicator */}
      <div className="absolute top-2 right-2 pointer-events-none">
        <div className="bg-black/60 backdrop-blur-sm px-2 py-1 rounded text-xs text-white/70">
          {project?.settings.width}×{project?.settings.height}
        </div>
      </div>
    </div>
  )
}