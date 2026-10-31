'use client'

import { useState } from 'react'
import { PanelLeft, PanelRight, PanelBottom, Maximize2, Minimize2, LayoutDashboard, FileText, Layers, Zap, Music, Settings, ChevronDown } from 'lucide-react'
import { cn } from '@/utils/cn'
import { Header } from './Header'
import { Timeline } from '@/components/timeline/Timeline'
import { Preview } from '@/components/preview/Preview'
import { MediaBin } from '@/components/media/MediaBin'
import { EffectsPanel } from '@/components/effects/EffectsPanel'
import { PropertiesPanel } from '@/components/properties/PropertiesPanel'
import { AudioMeters } from '@/components/audio/AudioMeters'
import { useProjectStore } from '@/store/projectStore'

export function AppLayout() {
  const [layout, setLayout] = useState({
    leftPanelOpen: true,
    rightPanelOpen: true,
    bottomPanelOpen: true,
    leftPanelWidth: 320,
    rightPanelWidth: 320,
    bottomPanelHeight: 300,
    previewMaximized: false,
  })

  const { project } = useProjectStore()

  const leftPanelWidth = layout.previewMaximized ? 0 : layout.leftPanelWidth
  const rightPanelWidth = layout.previewMaximized ? 0 : layout.rightPanelWidth
  const bottomPanelHeight = layout.previewMaximized ? 0 : layout.bottomPanelHeight

  return (
    <div className="flex h-screen w-screen bg-background text-foreground overflow-hidden">
      {/* Header */}
      <Header projectName={project?.name || 'Untitled'} />

      <div className="flex h-full w-full overflow-hidden">
        {/* Left Sidebar */}
        {layout.leftPanelOpen && (
          <div
            className={cn(
              'flex flex-col bg-secondary border-r border-border transition-all duration-200',
              layout.previewMaximized ? 'w-0 overflow-hidden' : 'w-[320px] min-w-[200px] max-w-[500px]'
            )}
            style={{ width: leftPanelWidth }}
          >
            <div className="flex h-full overflow-hidden">
              <div className="flex w-full flex-1 flex-col overflow-hidden">
                <MediaBin />
              </div>
              <div className="separator-horizontal" />
              <EffectsPanel />
              <div className="separator-horizontal" />
              <AudioMeters />
            </div>
          </div>
        )}

        {/* Resizer Left */}
        {!layout.previewMaximized && layout.leftPanelOpen && (
          <div
            className="relative w-1 cursor-col-resize bg-border hover:bg-primary/50 transition-colors"
            onMouseDown={(e) => startResize(e, 'left')}
            aria-label="Resize left panel"
          />
        )}

        {/* Center - Preview + Timeline */}
        <div
          className={cn(
            'flex flex-1 flex-col overflow-hidden bg-background',
            layout.previewMaximized && 'absolute inset-0 z-50'
          )}
        >
          {/* Preview */}
          <div
            className={cn(
              'relative flex-1 bg-background transition-all duration-200',
              layout.previewMaximized ? 'w-full h-full' : ''
            )}
          >
            <Preview />
          </div>

          {/* Resizer Bottom */}
          {!layout.previewMaximized && layout.bottomPanelOpen && (
            <div
              className="relative h-1 w-full cursor-row-resize bg-border hover:bg-primary/50 transition-colors"
              onMouseDown={(e) => startResize(e, 'bottom')}
              aria-label="Resize timeline panel"
            />
          )}

          {/* Bottom - Timeline */}
          {layout.bottomPanelOpen && (
            <div
              className={cn(
                'flex flex-col bg-secondary border-t border-border transition-all duration-200',
                layout.previewMaximized ? 'h-0 overflow-hidden' : 'h-[300px] min-h-[150px] max-h-[600px]'
              )}
              style={{ height: bottomPanelHeight }}
            >
              <Timeline />
            </div>
          )}
        </div>

        {/* Resizer Right */}
        {!layout.previewMaximized && layout.rightPanelOpen && (
          <div
            className="relative w-1 cursor-col-resize bg-border hover:bg-primary/50 transition-colors"
            onMouseDown={(e) => startResize(e, 'right')}
            aria-label="Resize right panel"
          />
        )}

        {/* Right Sidebar */}
        {layout.rightPanelOpen && (
          <div
            className={cn(
              'flex flex-col bg-secondary border-l border-border transition-all duration-200',
              layout.previewMaximized ? 'w-0 overflow-hidden' : 'w-[320px] min-w-[200px] max-w-[500px]'
            )}
            style={{ width: rightPanelWidth }}
          >
            <div className="flex h-full overflow-hidden">
              <PropertiesPanel />
            </div>
          </div>
        )}

        {/* Maximize Preview Button */}
        <button
          className={cn(
            'fixed bottom-4 right-4 z-40 p-2 rounded-full bg-secondary/80 backdrop-blur-sm border border-border',
            'hover:bg-secondary transition-colors',
            layout.previewMaximized && 'bottom-20'
          )}
          onClick={() => setLayout((l) => ({ ...l, previewMaximized: !l.previewMaximized }))}
          aria-label={layout.previewMaximized ? 'Minimize preview' : 'Maximize preview'}
        >
          {layout.previewMaximized ? <Minimize2 className="h-5 w-5" /> : <Maximize2 className="h-5 w-5" />}
        </button>
      </div>
    </div>
  )
}

function startResize(e: React.MouseEvent, side: 'left' | 'right' | 'bottom') {
  e.preventDefault()
  e.stopPropagation()

  const startX = e.clientX
  const startY = e.clientY

  const handleMouseMove = (moveEvent: MouseEvent) => {
    const dx = moveEvent.clientX - startX
    const dy = moveEvent.clientY - startY

    // This would update layout state via a context or store
    // For now, we'll use a simple approach with CSS custom properties
    if (side === 'left') {
      document.documentElement.style.setProperty('--left-panel-width', `${Math.max(200, Math.min(500, 320 + dx))}px`)
    } else if (side === 'right') {
      document.documentElement.style.setProperty('--right-panel-width', `${Math.max(200, Math.min(500, 320 - dx))}px`)
    } else if (side === 'bottom') {
      document.documentElement.style.setProperty('--bottom-panel-height', `${Math.max(150, Math.min(600, 300 + dy))}px`)
    }
  }

  const handleMouseUp = () => {
    window.removeEventListener('mousemove', handleMouseMove)
    window.removeEventListener('mouseup', handleMouseUp)
  }

  window.addEventListener('mousemove', handleMouseMove)
  window.addEventListener('mouseup', handleMouseUp)
}