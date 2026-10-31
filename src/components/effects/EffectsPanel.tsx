'use client'

import { useState } from 'react'
import { ChevronDown, ChevronRight, Search, Zap, Sparkles, Trash2, Eye, EyeOff, Layers } from 'lucide-react'
import { cn } from '@/utils/cn'
import { useProjectStore } from '@/store/projectStore'
import type { ClipEffect, EffectType, Clip, Track } from '@/types'

// Effect categories and definitions
const effectCategories = {
  video: [
    { id: 'blur', name: 'Blur', icon: '🌫️' },
    { id: 'brightness-contrast', name: 'Brightness & Contrast', icon: '☀️' },
    { id: 'color-balance', name: 'Color Balance', icon: '🎨' },
    { id: 'curves', name: 'Curves', icon: '📈' },
    { id: 'hue-saturation', name: 'Hue/Saturation', icon: '🌈' },
    { id: 'vignette', name: 'Vignette', icon: '🔍' },
    { id: 'sharpen', name: 'Sharpen', icon: '✨' },
    { id: 'noise', name: 'Noise', icon: '📺' },
    { id: 'glitch', name: 'Glitch', icon: '📡' },
    { id: 'chroma-key', name: 'Chroma Key', icon: '🟢' },
    { id: 'crop', name: 'Crop', icon: '✂️' },
    { id: 'transform', name: 'Transform', icon: '📐' },
  ],
  audio: [
    { id: 'gain', name: 'Gain', icon: '🔊' },
    { id: 'eq', name: 'Equalizer', icon: '🎚️' },
    { id: 'compressor', name: 'Compressor', icon: '📉' },
    { id: 'noise-reduction', name: 'Noise Reduction', icon: '🔇' },
    { id: 'reverb', name: 'Reverb', icon: '🏟️' },
    { id: 'delay', name: 'Delay', icon: '⏱️' },
    { id: 'pitch-shift', name: 'Pitch Shift', icon: '🎵' },
  ],
  transition: [
    { id: 'fade', name: 'Fade', icon: '➡️' },
    { id: 'slide', name: 'Slide', icon: '↔️' },
    { id: 'zoom', name: 'Zoom', icon: '🔍' },
    { id: 'wipe', name: 'Wipe', icon: '🧹' },
    { id: 'glitch-transition', name: 'Glitch', icon: '📡' },
  ],
}

const effectDefaults: Record<EffectType, Record<string, unknown>> = {
  blur: { radius: 0 },
  'brightness-contrast': { brightness: 0, contrast: 0 },
  'color-balance': { shadows: [0, 0, 0], midtones: [0, 0, 0], highlights: [0, 0, 0] },
  curves: { master: [], red: [], green: [], blue: [] },
  'hue-saturation': { hue: 0, saturation: 0, lightness: 0 },
  vignette: { amount: 0, midpoint: 0.5, roundness: 0 },
  sharpen: { amount: 0, radius: 1, threshold: 0 },
  noise: { amount: 0, monochrome: false },
  glitch: { intensity: 0, frequency: 1 },
  'chroma-key': { color: [0, 255, 0], similarity: 0.3, blend: 0.1 },
  crop: { top: 0, right: 0, bottom: 0, left: 0 },
  transform: { x: 0, y: 0, scale: 1, rotation: 0, anchorX: 0.5, anchorY: 0.5 },
  gain: { db: 0 },
  eq: { low: 0, mid: 0, high: 0 },
  compressor: { threshold: -20, ratio: 4, attack: 0.003, release: 0.25 },
  'noise-reduction': { reduction: 0, sensitivity: 0, frequencySmoothing: 3 },
  reverb: { roomSize: 0.5, damping: 0.5, wetLevel: 0.3, dryLevel: 0.7 },
  delay: { delayTime: 0.5, feedback: 0.3, wetLevel: 0.3, dryLevel: 0.7 },
  'pitch-shift': { semitones: 0, cents: 0 },
  fade: { opacity: 1 },
  slide: { direction: 'left', distance: 100 },
  zoom: { scale: 1 },
  wipe: { direction: 'left', softness: 0 },
  'glitch-transition': { intensity: 0 },
}

export function EffectsPanel() {
  const [searchQuery, setSearchQuery] = useState('')
  const [activeCategory, setActiveCategory] = useState<'video' | 'audio' | 'transition' | 'favorites'>('video')
  const [expandedCategories, setExpandedCategories] = useState({
    video: true,
    audio: true,
    transition: true,
    favorites: true,
  })

  const { timeline } = useProjectStore()
  const selection = timeline.selection
  const selectedClip = selection.length === 1 ? getSelectedClip() : null

  function getSelectedClip() {
    const { project } = useProjectStore.getState()
    if (!project) return null
    for (const track of project.tracks) {
      const clip = track.clips.find((c) => c.id === selection[0])
      if (clip) return { clip, track }
    }
    return null
  }

  const addEffect = (type: EffectType) => {
    if (!selectedClip) return
    const { clip } = selectedClip
    const defaults = effectDefaults[type] || {}

    useProjectStore.getState().addClipEffect(clip.id, {
      type,
      enabled: true,
      params: defaults,
      keyframes: [],
    })
  }

  const removeEffect = (clipId: string, effectId: string) => {
    useProjectStore.getState().removeClipEffect(clipId, effectId)
  }

  const toggleEffect = (clipId: string, effectId: string, enabled: boolean) => {
    useProjectStore.getState().updateClipEffect(clipId, effectId, { enabled })
  }

  const updateEffectParam = (clipId: string, effectId: string, param: string, value: unknown) => {
    useProjectStore.getState().updateClipEffect(clipId, effectId, {
      params: { [param]: value },
    })
  }

  const categoryEffects = (effectCategories as Record<string, { id: string; name: string; icon: string }[]>)[activeCategory] || []
  const filteredEffects = Object.entries(categoryEffects)
    .filter(([_, effect]) =>
      effect.name.toLowerCase().includes(searchQuery.toLowerCase())
    )

  if (!selectedClip) {
    return (
      <div className="flex h-full flex-col">
        <div className="p-3 border-b border-border">
          <h3 className="font-medium">Effects</h3>
        </div>
        <div className="flex-1 flex items-center justify-center text-muted-foreground">
          <div className="text-center p-4">
            <Zap className="h-12 w-12 mx-auto mb-2 opacity-30" />
            <p>Select a clip to add effects</p>
            <p className="text-sm mt-1">Choose a clip from the timeline</p>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="flex h-full flex-col">
      {/* Header */}
      <div className="p-3 border-b border-border">
        <div className="flex items-center justify-between mb-2">
          <h3 className="font-medium">Effects</h3>
          <span className="text-xs text-muted-foreground">{selectedClip.clip.name}</span>
        </div>

        {/* Search */}
        <div className="relative">
          <Search className="absolute left-2 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-muted-foreground" />
          <input
            type="text"
            placeholder="Search effects..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="input h-7 w-full pl-8 text-sm"
          />
        </div>
      </div>

      {/* Category Tabs */}
      <div className="flex border-b border-border px-2">
        {(['video', 'audio', 'transition', 'favorites'] as const).map((cat) => (
          <button
            key={cat}
            className={cn(
              'px-3 py-1.5 text-sm font-medium border-b-2 transition-colors',
              activeCategory === cat
                ? 'border-primary text-primary'
                : 'border-transparent text-muted-foreground hover:text-foreground'
            )}
            onClick={() => setActiveCategory(cat)}
          >
            {cat.charAt(0).toUpperCase() + cat.slice(1)}
          </button>
        ))}
      </div>

      {/* Effects List */}
      <div className="flex-1 overflow-auto p-2">
        {activeCategory !== 'favorites' ? (
          <>
            <EffectCategory
              title="Video Effects"
              effects={filteredEffects}
              selectedClip={selectedClip}
              onAddEffect={addEffect}
              onRemoveEffect={removeEffect}
              onToggleEffect={toggleEffect}
              onUpdateParam={updateEffectParam}
              expanded={expandedCategories.video}
              onToggleExpanded={() => setExpandedCategories((p) => ({ ...p, video: !p.video }))}
            />
          </>
        ) : (
          <div className="text-center py-8 text-muted-foreground">
            <Sparkles className="h-8 w-8 mx-auto mb-2 opacity-30" />
            <p>No favorite effects yet</p>
            <p className="text-sm">Right-click an effect to add to favorites</p>
          </div>
        )}

        {/* Applied Effects */}
        {selectedClip.clip.effects.length > 0 && (
          <div className="mt-4 pt-4 border-t border-border">
            <h4 className="font-medium mb-2">Applied Effects</h4>
            <AppliedEffectsList
              clip={selectedClip.clip}
              onRemoveEffect={removeEffect}
              onToggleEffect={toggleEffect}
              onUpdateParam={updateEffectParam}
            />
          </div>
        )}
      </div>
    </div>
  )
}

interface EffectCategoryProps {
  title: string
  effects: [string, { name: string; icon: string }][]
  selectedClip: { clip: Clip; track: Track } | null
  onAddEffect: (type: EffectType) => void
  onRemoveEffect: (clipId: string, effectId: string) => void
  onToggleEffect: (clipId: string, effectId: string, enabled: boolean) => void
  onUpdateParam: (clipId: string, effectId: string, param: string, value: unknown) => void
  expanded: boolean
  onToggleExpanded: () => void
}

function EffectCategory({
  title,
  effects,
  selectedClip,
  onAddEffect,
  onRemoveEffect,
  onToggleEffect,
  onUpdateParam,
  expanded,
  onToggleExpanded,
}: EffectCategoryProps) {
  if (!expanded) {
    return (
      <button
        className="flex w-full items-center justify-between px-2 py-1.5 text-sm font-medium text-muted-foreground hover:text-foreground"
        onClick={onToggleExpanded}
      >
        <ChevronRight className="h-4 w-4 flex-shrink-0" />
        {title}
      </button>
    )
  }

  return (
    <div>
      <button
        className="flex w-full items-center justify-between px-2 py-1.5 text-sm font-medium text-muted-foreground hover:text-foreground"
        onClick={onToggleExpanded}
      >
        <ChevronDown className="h-4 w-4 flex-shrink-0" />
        {title}
      </button>
      <div className="space-y-1 pl-2">
        {effects.map(([id, effect]) => (
          <EffectItem
            key={id}
            effect={{ id, ...effect }}
            selectedClip={selectedClip}
            onAddEffect={onAddEffect}
            onRemoveEffect={onRemoveEffect}
            onToggleEffect={onToggleEffect}
            onUpdateParam={onUpdateParam}
          />
        ))}
      </div>
    </div>
  )
}

interface EffectItemProps {
  effect: { id: string; name: string; icon: string }
  selectedClip: { clip: Clip; track: Track } | null
  onAddEffect: (type: EffectType) => void
  onRemoveEffect: (clipId: string, effectId: string) => void
  onToggleEffect: (clipId: string, effectId: string, enabled: boolean) => void
  onUpdateParam: (clipId: string, effectId: string, param: string, value: unknown) => void
}

function EffectItem({ effect, selectedClip, onAddEffect, onRemoveEffect, onToggleEffect, onUpdateParam }: EffectItemProps) {
  const [showParams, setShowParams] = useState(false)
  const isApplied = selectedClip?.clip.effects.some((e) => e.type === effect.id) || false
  const appliedEffect = selectedClip?.clip.effects.find((e) => e.type === effect.id)

  if (isApplied && appliedEffect) {
    return (
      <div className="effect-item effect-item-active">
        <div className="flex-1 flex items-center gap-2" onClick={() => setShowParams(!showParams)}>
          <Eye className="h-4 w-4" />
          <span className="flex-1">{effect.name}</span>
          <ChevronDown className={cn('h-4 w-4 transition-transform', showParams && 'rotate-180')} />
        </div>
        <button
          className="btn-ghost h-6 w-6 p-0"
          onClick={(e) => { e.stopPropagation(); onToggleEffect(selectedClip!.clip.id, appliedEffect.id, !appliedEffect.enabled) }}
          title={appliedEffect.enabled ? 'Disable' : 'Enable'}
        >
          {appliedEffect.enabled ? <Eye className="h-4 w-4" /> : <EyeOff className="h-4 w-4" />}
        </button>
        <button
          className="btn-ghost h-6 w-6 p-0"
          onClick={(e) => { e.stopPropagation(); onRemoveEffect(selectedClip!.clip.id, appliedEffect.id) }}
          title="Remove effect"
        >
          <Trash2 className="h-4 w-4" />
        </button>

        {showParams && (
          <div className="mt-2 ml-6 space-y-2 border-l-2 border-border/50 pl-2">
            {Object.entries(appliedEffect.params).map(([param, value]) => (
              <EffectParamControl
                key={param}
                param={param}
                value={value}
                onChange={(v) => onUpdateParam(selectedClip!.clip.id, appliedEffect.id, param, v)}
              />
            ))}
          </div>
        )}
      </div>
    )
  }

  return (
    <button
      className="effect-item"
      onClick={() => onAddEffect(effect.id as EffectType)}
    >
      <span className="text-lg">{effect.icon}</span>
      <span className="flex-1">{effect.name}</span>
      <Layers className="h-4 w-4 text-muted-foreground" />
    </button>
  )
}

function AppliedEffectsList({
  clip,
  onRemoveEffect,
  onToggleEffect,
  onUpdateParam,
}: {
  clip: Clip
  onRemoveEffect: (clipId: string, effectId: string) => void
  onToggleEffect: (clipId: string, effectId: string, enabled: boolean) => void
  onUpdateParam: (clipId: string, effectId: string, param: string, value: unknown) => void
}) {
  return (
    <div className="space-y-2">
      {clip.effects.map((effect) => (
        <div key={effect.id} className="effect-item effect-item-active">
          <div className="flex-1 flex items-center gap-2">
            <Eye className={cn('h-4 w-4', !effect.enabled && 'opacity-30')} />
            <span className="flex-1">{effect.type}</span>
          </div>
          <button
            className="btn-ghost h-6 w-6 p-0"
            onClick={() => onToggleEffect(clip.id, effect.id, !effect.enabled)}
          >
            {effect.enabled ? <Eye className="h-4 w-4" /> : <EyeOff className="h-4 w-4" />}
          </button>
          <button
            className="btn-ghost h-6 w-6 p-0"
            onClick={() => onRemoveEffect(clip.id, effect.id)}
          >
            <Trash2 className="h-4 w-4" />
          </button>
        </div>
      ))}
    </div>
  )
}

function EffectParamControl({ param, value, onChange }: { param: string; value: unknown; onChange: (v: unknown) => void }) {
  if (typeof value === 'number') {
    return (
      <div className="flex items-center gap-2">
        <label className="text-xs text-muted-foreground w-20">{param}</label>
        <input
          type="range"
          min="-100"
          max="100"
          step="1"
          value={value as number}
          onChange={(e) => onChange(Number(e.target.value))}
          className="flex-1 h-1 accent-primary"
        />
        <span className="text-xs text-muted-foreground w-10 text-right">
          {(value as number).toFixed(0)}
        </span>
      </div>
    )
  }

  if (typeof value === 'boolean') {
    return (
      <div className="flex items-center gap-2">
        <label className="text-xs text-muted-foreground w-20">{param}</label>
        <input
          type="checkbox"
          checked={value as boolean}
          onChange={(e) => onChange(e.target.checked)}
          className="h-4 w-4 accent-primary"
        />
      </div>
    )
  }

  return (
    <div className="flex items-center gap-2">
      <label className="text-xs text-muted-foreground w-20">{param}</label>
      <input
        type="text"
        value={String(value)}
        onChange={(e) => onChange(e.target.value)}
        className="input h-6 flex-1 text-xs"
      />
    </div>
  )
}