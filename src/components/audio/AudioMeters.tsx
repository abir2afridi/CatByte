'use client'

import { useEffect, useRef, useState } from 'react'
import { useProjectStore } from '@/store/projectStore'

export function AudioMeters() {
  const [levels, setLevels] = useState({ left: 0, right: 0 })
  const [peaks, setPeaks] = useState({ left: 0, right: 0 })
  const animationRef = useRef<number>()
  const audioContextRef = useRef<AudioContext>()
  const analyserRef = useRef<AnalyserNode>()

  const { preview, project } = useProjectStore()

  // Initialize audio context
  useEffect(() => {
    if (typeof window !== 'undefined' && !audioContextRef.current) {
      audioContextRef.current = new (window.AudioContext || (window as any).webkitAudioContext)()
      analyserRef.current = audioContextRef.current.createAnalyser()
      analyserRef.current.fftSize = 256
      analyserRef.current.smoothingTimeConstant = 0.8
    }
  }, [])

  // Simulate audio levels (in real app, connect to actual audio)
  useEffect(() => {
    if (!preview.playing) {
      setLevels({ left: 0, right: 0 })
      return
    }

    const animate = () => {
      // Simulate audio levels based on playhead position
      // In real implementation, this would come from actual audio analysis
      const baseLevel = Math.random() * 0.3 + 0.1
      const variation = Math.sin(Date.now() / 100) * 0.2

      const left = Math.max(0, Math.min(1, baseLevel + variation + (Math.random() - 0.5) * 0.1))
      const right = Math.max(0, Math.min(1, baseLevel - variation + (Math.random() - 0.5) * 0.1))

      setLevels({ left, right })
      setPeaks((prev) => ({
        left: Math.max(prev.left * 0.95, left),
        right: Math.max(prev.right * 0.95, right),
      }))

      animationRef.current = requestAnimationFrame(animate)
    }

    animate()
    return () => {
      if (animationRef.current) cancelAnimationFrame(animationRef.current)
    }
  }, [preview.playing])

  const audioTracks = project?.tracks.filter((t) => t.type === 'audio' && t.enabled && !t.muted) || []

  return (
    <div className="p-3 border-t border-border bg-secondary/50">
      <div className="flex items-center justify-between mb-2">
        <h4 className="font-medium text-sm">Audio Meters</h4>
        <div className="flex items-center gap-2">
          <span className="text-xs text-muted-foreground">
            {preview.playing ? '● LIVE' : '○ STOPPED'}
          </span>
        </div>
      </div>

      {/* Master Meter */}
      <div className="mb-3">
        <div className="flex items-center justify-between text-xs mb-1">
          <span className="font-medium">MASTER</span>
          <span className="text-muted-foreground font-mono">
            L: {levelToDb(levels.left)} R: {levelToDb(levels.right)}
          </span>
        </div>
        <MeterBar level={Math.max(levels.left, levels.right)} peak={Math.max(peaks.left, peaks.right)} />
      </div>

      {/* Channel Meters */}
      <div className="grid grid-cols-2 gap-2">
        <ChannelMeter label="L" level={levels.left} peak={peaks.left} />
        <ChannelMeter label="R" level={levels.right} peak={peaks.right} />
      </div>

      {/* Track Meters */}
      {audioTracks.length > 0 && (
        <div className="mt-3 pt-3 border-t border-border">
          <h5 className="text-xs font-medium text-muted-foreground mb-2">Tracks</h5>
          <div className="space-y-1 max-h-32 overflow-auto">
            {audioTracks.slice(0, 4).map((track) => (
              <TrackMeter key={track.id} track={track} />
            ))}
          </div>
        </div>
      )}
    </div>
  )
}

function MeterBar({ level, peak }: { level: number; peak: number }) {
  const percentage = level * 100
  const peakPercentage = peak * 100

  return (
    <div className="relative h-6 bg-muted rounded overflow-hidden">
      <div
        className="absolute inset-y-0 left-0 bg-gradient-to-r from-green-500 via-yellow-500 to-red-500 transition-all duration-50"
        style={{ width: `${percentage}%` }}
      />
      {peakPercentage > 0 && (
        <div
          className="absolute top-0 bottom-0 w-0.5 bg-white/80 drop-shadow"
          style={{ left: `${peakPercentage}%` }}
        />
      )}
      <div className="absolute inset-0 grid grid-cols-20 gap-[1px] pointer-events-none">
        {Array.from({ length: 20 }).map((_, i) => (
          <div key={i} className="bg-white/10" />
        ))}
      </div>
      {/* dB markers */}
      <div className="absolute inset-0 flex items-end justify-between px-1 pointer-events-none">
        {[-60, -30, -12, -6, -3, 0].map((db) => (
          <span key={db} className="text-[8px] text-white/50 font-mono">{db}</span>
        ))}
      </div>
    </div>
  )
}

function ChannelMeter({ label, level, peak }: { label: string; level: number; peak: number }) {
  const percentage = level * 100
  const peakPercentage = peak * 100

  return (
    <div className="flex flex-col items-center gap-1">
      <span className="text-xs font-medium text-muted-foreground">{label}</span>
      <div className="relative w-8 h-32 bg-muted rounded overflow-hidden flex-1">
        <div
          className="absolute bottom-0 left-0 right-0 bg-gradient-to-t from-green-500 via-yellow-500 to-red-500 transition-all duration-50"
          style={{ height: `${percentage}%` }}
        />
        {peakPercentage > 0 && (
          <div
            className="absolute left-0 right-0 h-0.5 bg-white/80 drop-shadow"
            style={{ bottom: `${peakPercentage}%` }}
          />
        )}
      </div>
      <span className="text-[10px] font-mono text-muted-foreground">{levelToDb(level)}</span>
    </div>
  )
}

function TrackMeter({ track }: { track: { id: string; name: string; clips: any[]; muted: boolean; solo: boolean } }) {
  const [level, setLevel] = useState({ left: 0, right: 0 })

  useEffect(() => {
    if (!track.muted) {
      const interval = setInterval(() => {
        setLevel({
          left: Math.random() * 0.5,
          right: Math.random() * 0.5,
        })
      }, 100)
      return () => clearInterval(interval)
    }
  }, [track.muted])

  return (
    <div className="flex items-center gap-2 text-xs">
      <span className="w-16 truncate text-muted-foreground">{track.name}</span>
      <div className="flex-1 h-4 bg-muted rounded overflow-hidden relative">
        <div
          className="absolute inset-y-0 left-0 bg-primary/80 transition-all duration-50"
          style={{ width: `${Math.max(level.left, level.right) * 100}%` }}
        />
      </div>
      <span className="w-12 text-right font-mono text-muted-foreground">
        {levelToDb(Math.max(level.left, level.right))}
      </span>
    </div>
  )
}

function levelToDb(level: number): string {
  if (level <= 0) return '-∞'
  const db = 20 * Math.log10(level)
  return db > -1 ? '0' : Math.round(db).toString()
}