'use client'

import { useState } from 'react'
import { ChevronDown, ChevronRight, Copy, Settings, Volume2, Layers, Key } from 'lucide-react'
import { cn } from '@/utils/cn'
import { useProjectStore } from '@/store/projectStore'
import type { Clip, ClipTransform, TextStyle } from '@/types'

export function PropertiesPanel() {
  const [expandedSections, setExpandedSections] = useState({
    transform: true,
    compositing: true,
    speed: true,
    audio: true,
    text: true,
    effects: true,
  })

  const { timeline, updateClip, updateClipTransform } = useProjectStore()
  const selection = timeline.selection

  if (selection.length === 0) {
    return (
      <div className="flex h-full flex-col">
        <div className="p-3 border-b border-border">
          <h3 className="font-medium">Properties</h3>
        </div>
        <div className="flex-1 flex items-center justify-center text-muted-foreground">
          <div className="text-center p-4">
            <Settings className="h-12 w-12 mx-auto mb-2 opacity-30" />
            <p>Select a clip to view properties</p>
          </div>
        </div>
      </div>
    )
  }

  if (selection.length > 1) {
    return (
      <div className="flex h-full flex-col">
        <div className="p-3 border-b border-border">
          <h3 className="font-medium">Properties</h3>
        </div>
        <div className="flex-1 flex items-center justify-center text-muted-foreground">
          <div className="text-center p-4">
            <Settings className="h-12 w-12 mx-auto mb-2 opacity-30" />
            <p>{selection.length} clips selected</p>
            <p className="text-sm mt-1">Multi-clip editing coming soon</p>
          </div>
        </div>
      </div>
    )
  }

  const clip = getSelectedClip()
  if (!clip) return null

  return (
    <div className="flex h-full flex-col">
      {/* Header */}
      <div className="p-3 border-b border-border">
        <div className="flex items-center justify-between">
          <h3 className="font-medium">Properties</h3>
          <div className="flex items-center gap-1">
            <button className="btn-ghost h-7 w-7 p-0" title="Copy properties">
              <Copy className="h-4 w-4" />
            </button>
            <button className="btn-ghost h-7 w-7 p-0" title="Paste properties">
              <Settings className="h-4 w-4" />
            </button>
          </div>
        </div>
        <div className="flex items-center gap-2 mt-2">
          <span className="px-2 py-0.5 text-xs bg-muted rounded">{clip.assetType?.toUpperCase() || 'CLIP'}</span>
          <span className="text-xs text-muted-foreground truncate flex-1">{clip.name}</span>
        </div>
      </div>

      {/* Properties Content */}
      <div className="flex-1 overflow-auto p-2 space-y-4">
        {/* Transform */}
        <PropertySection
          title="Transform"
          icon={<Settings className="h-4 w-4" />}
          expanded={expandedSections.transform}
          onToggle={() => setExpandedSections((p) => ({ ...p, transform: !p.transform }))}
        >
          <TransformControls
            clip={clip}
            onChange={(transform) => updateClipTransform(clip.id, transform)}
          />
        </PropertySection>

        {/* Compositing */}
        <PropertySection
          title="Compositing"
          icon={<Layers className="h-4 w-4" />}
          expanded={expandedSections.compositing}
          onToggle={() => setExpandedSections((p) => ({ ...p, compositing: !p.compositing }))}
        >
          <CompositingControls
            clip={clip}
            onChange={(updates) => updateClip(clip.id, updates)}
          />
        </PropertySection>

        {/* Speed */}
        <PropertySection
          title="Speed"
          icon={<Key className="h-4 w-4" />}
          expanded={expandedSections.speed}
          onToggle={() => setExpandedSections((p) => ({ ...p, speed: !p.speed }))}
        >
          <SpeedControls
            clip={clip}
            onChange={(updates) => updateClip(clip.id, updates)}
          />
        </PropertySection>

        {/* Audio */}
        {clip.assetType === 'video' || clip.assetType === 'audio' ? (
          <PropertySection
            title="Audio"
            icon={<Volume2 className="h-4 w-4" />}
            expanded={expandedSections.audio}
            onToggle={() => setExpandedSections((p) => ({ ...p, audio: !p.audio }))}
          >
            <AudioControls
              clip={clip}
              onChange={(updates) => updateClip(clip.id, updates)}
            />
          </PropertySection>
        ) : null}

        {/* Text */}
        {clip.textContent !== undefined ? (
          <PropertySection
            title="Text"
            icon={<Key className="h-4 w-4" />}
            expanded={expandedSections.text}
            onToggle={() => setExpandedSections((p) => ({ ...p, text: !p.text }))}
          >
            <TextControls
              clip={clip}
              onChange={(updates) => updateClip(clip.id, updates)}
            />
          </PropertySection>
        ) : null}
      </div>
    </div>
  )
}

function getSelectedClip(): (Clip & { assetType?: string }) | null {
  const { project, timeline } = useProjectStore.getState()
  if (!project || timeline.selection.length === 0) return null

  const clipId = timeline.selection[0]
  for (const track of project.tracks) {
    const clip = track.clips.find((c) => c.id === clipId)
    if (clip) {
      return { ...clip, assetType: track.type }
    }
  }
  return null
}

interface PropertySectionProps {
  title: string
  icon: React.ReactNode
  expanded: boolean
  onToggle: () => void
  children: React.ReactNode
}

function PropertySection({ title, icon, expanded, onToggle, children }: PropertySectionProps) {
  return (
    <div className="property-group">
      <button
        className="flex w-full items-center gap-2"
        onClick={onToggle}
      >
        {expanded ? <ChevronDown className="h-4 w-4" /> : <ChevronRight className="h-4 w-4" />}
        {icon}
        <span className="font-medium">{title}</span>
      </button>
      {expanded && <div className="pt-2 space-y-3">{children}</div>}
    </div>
  )
}

function TransformControls({ clip, onChange }: { clip: Clip & { assetType?: string }; onChange: (t: Partial<ClipTransform>) => void }) {
  const t = clip.transform

  return (
    <div className="space-y-3">
      <div className="grid grid-cols-2 gap-3">
        <NumberInput label="X" value={Math.round(t.x)} onChange={(v) => onChange({ x: v })} step={1} />
        <NumberInput label="Y" value={Math.round(t.y)} onChange={(v) => onChange({ y: v })} step={1} />
      </div>
      <div className="grid grid-cols-2 gap-3">
        <NumberInput label="Scale X" value={t.scaleX} onChange={(v) => onChange({ scaleX: v })} step={0.01} min={0.01} max={10} />
        <NumberInput label="Scale Y" value={t.scaleY} onChange={(v) => onChange({ scaleY: v })} step={0.01} min={0.01} max={10} />
      </div>
      <div className="grid grid-cols-2 gap-3">
        <NumberInput label="Rotation" value={Math.round(t.rotation * 180 / Math.PI)} onChange={(v) => onChange({ rotation: v * Math.PI / 180 })} step={1} min={-360} max={360} unit="°" />
        <div className="flex items-center gap-2">
          <label className="text-xs text-muted-foreground w-20">Uniform</label>
          <input type="checkbox" checked={Math.abs(t.scaleX - t.scaleY) < 0.01} onChange={() => onChange({ scaleX: t.scaleX, scaleY: t.scaleX })} className="h-4 w-4 accent-primary" />
        </div>
      </div>
      <div className="grid grid-cols-2 gap-3">
        <NumberInput label="Anchor X" value={t.anchorX} onChange={(v) => onChange({ anchorX: v })} step={0.01} min={0} max={1} />
        <NumberInput label="Anchor Y" value={t.anchorY} onChange={(v) => onChange({ anchorY: v })} step={0.01} min={0} max={1} />
      </div>
      <button className="btn-secondary w-full" onClick={() => onChange(defaultClipTransform)}>Reset Transform</button>
    </div>
  )
}

function CompositingControls({ clip, onChange }: { clip: Clip; onChange: (u: Partial<Clip>) => void }) {
  return (
    <div className="space-y-3">
      <div className="flex items-center gap-2">
        <label className="text-xs text-muted-foreground w-20">Opacity</label>
        <input
          type="range"
          min="0"
          max="1"
          step="0.01"
          value={clip.opacity}
          onChange={(e) => onChange({ opacity: Number(e.target.value) })}
          className="flex-1 h-1 accent-primary"
        />
        <span className="text-xs text-muted-foreground w-10 text-right">
          {Math.round(clip.opacity * 100)}%
        </span>
      </div>
    </div>
  )
}

function SpeedControls({ clip, onChange }: { clip: Clip; onChange: (u: Partial<Clip>) => void }) {
  return (
    <div className="space-y-3">
      <div className="flex items-center gap-2">
        <label className="text-xs text-muted-foreground w-20">Speed</label>
        <input
          type="range"
          min="0.01"
          max="10"
          step="0.01"
          value={clip.speed}
          onChange={(e) => onChange({ speed: Number(e.target.value) })}
          className="flex-1 h-1 accent-primary"
        />
        <span className="text-xs text-muted-foreground w-10 text-right">
          {clip.speed.toFixed(2)}x
        </span>
      </div>
      <div className="flex items-center gap-2">
        <input
          type="checkbox"
          id="reverse"
          checked={clip.reverse}
          onChange={(e) => onChange({ reverse: e.target.checked })}
          className="h-4 w-4 accent-primary"
        />
        <label htmlFor="reverse" className="text-sm">Reverse</label>
      </div>
      <div className="grid grid-cols-2 gap-2">
        <button className="btn-secondary text-xs" onClick={() => onChange({ speed: 0.5 })}>0.5x</button>
        <button className="btn-secondary text-xs" onClick={() => onChange({ speed: 1 })}>1x</button>
        <button className="btn-secondary text-xs" onClick={() => onChange({ speed: 2 })}>2x</button>
        <button className="btn-secondary text-xs" onClick={() => onChange({ speed: 4 })}>4x</button>
      </div>
    </div>
  )
}

function AudioControls({ clip, onChange }: { clip: Clip; onChange: (u: Partial<Clip>) => void }) {
  return (
    <div className="space-y-3">
      <div className="flex items-center gap-2">
        <label className="text-xs text-muted-foreground w-20">Volume</label>
        <input
          type="range"
          min="0"
          max="2"
          step="0.01"
          value={clip.volume}
          onChange={(e) => onChange({ volume: Number(e.target.value) })}
          className="flex-1 h-1 accent-primary"
        />
        <span className="text-xs text-muted-foreground w-10 text-right">
          {Math.round(clip.volume * 100)}%
        </span>
      </div>
      <div className="flex items-center gap-2">
        <input
          type="checkbox"
          id="mute"
          checked={clip.volume === 0}
          onChange={(e) => onChange({ volume: e.target.checked ? 0 : 1 })}
          className="h-4 w-4 accent-primary"
        />
        <label htmlFor="mute" className="text-sm">Mute</label>
      </div>
    </div>
  )
}

function TextControls({ clip, onChange }: { clip: Clip & { textContent?: string; textStyle?: TextStyle }; onChange: (u: Partial<Clip>) => void }) {
  const style = clip.textStyle || {
    fontFamily: 'Inter',
    fontSize: 48,
    fontWeight: 600,
    color: { r: 1, g: 1, b: 1, a: 1 },
    strokeWidth: 0,
    alignment: 'center',
    lineHeight: 1.2,
    letterSpacing: 0,
    transform: 'none',
  }

  return (
    <div className="space-y-3">
      <textarea
        value={clip.textContent || ''}
        onChange={(e) => onChange({ textContent: e.target.value })}
        placeholder="Enter text..."
        className="input h-20 w-full text-sm resize-none font-mono"
      />
      <div className="grid grid-cols-2 gap-3">
        <select
          value={style.fontFamily}
          onChange={(e) => onChange({ textStyle: { ...style, fontFamily: e.target.value } })}
          className="input h-8 text-sm"
        >
          <option value="Inter">Inter</option>
          <option value="Arial">Arial</option>
          <option value="Helvetica">Helvetica</option>
          <option value="Georgia">Georgia</option>
          <option value="Times New Roman">Times New Roman</option>
          <option value="Roboto">Roboto</option>
          <option value="Open Sans">Open Sans</option>
        </select>
        <NumberInput label="Size" value={style.fontSize} onChange={(v) => onChange({ textStyle: { ...style, fontSize: v }})} step={1} min={8} max={200} />
      </div>
      <div className="flex items-center gap-2">
        <label className="text-xs text-muted-foreground w-20">Weight</label>
        <select
          value={style.fontWeight}
          onChange={(e) => onChange({ textStyle: { ...style, fontWeight: Number(e.target.value) } })}
          className="input h-8 flex-1 text-sm"
        >
          <option value={100}>Thin</option>
          <option value={200}>Extra Light</option>
          <option value={300}>Light</option>
          <option value={400}>Regular</option>
          <option value={500}>Medium</option>
          <option value={600}>Semi Bold</option>
          <option value={700}>Bold</option>
          <option value={800}>Extra Bold</option>
          <option value={900}>Black</option>
        </select>
      </div>
      <div className="grid grid-cols-2 gap-3">
        <NumberInput label="Line Height" value={style.lineHeight} onChange={(v) => onChange({ textStyle: { ...style, lineHeight: v }})} step={0.1} min={0.5} max={3} />
        <NumberInput label="Letter Spacing" value={style.letterSpacing} onChange={(v) => onChange({ textStyle: { ...style, letterSpacing: v }})} step={0.5} min={-5} max={20} />
      </div>
      <div className="flex items-center gap-2">
        <label className="text-xs text-muted-foreground w-20">Align</label>
        <div className="flex gap-1">
          {(['left', 'center', 'right'] as const).map((align) => (
            <button
              key={align}
              className={cn('btn-ghost h-7 w-7 p-0', style.alignment === align && 'bg-primary/20')}
              onClick={() => onChange({ textStyle: { ...style, alignment: align } })}
            >
              {align === 'left' && <svg className="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><line x1="3" y1="6" x2="21" y2="6" /><line x1="3" y1="12" x2="15" y2="12" /><line x1="3" y1="18" x2="18" y2="18" /></svg>}
              {align === 'center' && <svg className="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><line x1="3" y1="6" x2="21" y2="6" /><line x1="3" y1="12" x2="21" y2="12" /><line x1="3" y1="18" x2="21" y2="18" /></svg>}
              {align === 'right' && <svg className="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><line x1="3" y1="6" x2="21" y2="6" /><line x1="9" y1="12" x2="21" y2="12" /><line x1="3" y1="18" x2="21" y2="18" /></svg>}
            </button>
          ))}
        </div>
      </div>
    </div>
  )
}

function NumberInput({ label, value, onChange, step = 1, min, max, unit = '' }: {
  label: string
  value: number
  onChange: (v: number) => void
  step?: number
  min?: number
  max?: number
  unit?: string
}) {
  return (
    <div className="flex items-center gap-2">
      <label className="text-xs text-muted-foreground w-20">{label}</label>
      <input
        type="number"
        value={value}
        onChange={(e) => {
          const v = Number(e.target.value)
          if (!isNaN(v) && (min === undefined || v >= min) && (max === undefined || v <= max)) {
            onChange(v)
          }
        }}
        step={step}
        min={min}
        max={max}
        className="input h-7 flex-1 text-sm"
      />
      {unit && <span className="text-xs text-muted-foreground">{unit}</span>}
    </div>
  )
}

const defaultClipTransform: ClipTransform = {
  x: 0,
  y: 0,
  scaleX: 1,
  scaleY: 1,
  rotation: 0,
  anchorX: 0.5,
  anchorY: 0.5,
}