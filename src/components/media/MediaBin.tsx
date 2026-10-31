'use client'

import { useState, useRef, useCallback, useEffect, useMemo } from 'react'
import { FilePlus, Search, Grid, List, MoreVertical, Play, Image, Video, Music, Trash2, Settings } from 'lucide-react'
import { cn } from '@/utils/cn'
import { useProjectStore } from '@/store/projectStore'
import { useMedia } from '@/hooks/useMedia'
import type { MediaAsset } from '@/types'

export function MediaBin() {
  const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid')
  const [searchQuery, setSearchQuery] = useState('')
  const [filterType, setFilterType] = useState<'all' | 'video' | 'audio' | 'image'>('all')
  const [sortBy, setSortBy] = useState<'name' | 'date' | 'type' | 'duration'>('date')
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('desc')
  const [selectedAssets, setSelectedAssets] = useState<string[]>([])
  const [dragOver, setDragOver] = useState(false)
  const fileInputRef = useRef<HTMLInputElement>(null)

  const { mediaPool, importMedia, importMultiple, deleteMedia } = useMedia()
  const { project, addClip } = useProjectStore()

  // Filter and sort media
  const filteredMedia = useMemo(() => {
    return mediaPool
      .filter((asset) => {
        if (searchQuery && !asset.name.toLowerCase().includes(searchQuery.toLowerCase())) return false
        if (filterType !== 'all' && asset.type !== filterType) return false
        return true
      })
      .sort((a, b) => {
        let comparison = 0
        switch (sortBy) {
          case 'name':
            comparison = a.name.localeCompare(b.name)
            break
          case 'date':
            comparison = a.createdAt - b.createdAt
            break
          case 'type':
            comparison = a.type.localeCompare(b.type)
            break
          case 'duration':
            comparison = a.duration - b.duration
            break
        }
        return sortOrder === 'asc' ? comparison : -comparison
      })
  }, [mediaPool, searchQuery, filterType, sortBy, sortOrder])

  const handleFileDrop = useCallback(
    async (e: React.DragEvent<HTMLDivElement>) => {
      e.preventDefault()
      e.stopPropagation()
      setDragOver(false)
      if (e.dataTransfer.files) {
        await importMultiple(e.dataTransfer.files)
      }
    },
    [importMultiple]
  )

  const handleDragOver = useCallback((e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault()
    e.stopPropagation()
    setDragOver(true)
  }, [])

  const handleDragLeave = useCallback((e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault()
    e.stopPropagation()
    setDragOver(false)
  }, [])

  const handleFileSelect = useCallback(
    async (e: React.ChangeEvent<HTMLInputElement>) => {
      if (e.target.files) {
        await importMultiple(e.target.files)
        e.target.value = ''
      }
    },
    [importMultiple]
  )

  const handleAssetClick = useCallback(
    (asset: MediaAsset, e: React.MouseEvent<HTMLDivElement>) => {
      if (e.shiftKey || e.ctrlKey || e.metaKey) {
        setSelectedAssets((prev) =>
          prev.includes(asset.id) ? prev.filter((id) => id !== asset.id) : [...prev, asset.id]
        )
      } else {
        setSelectedAssets([asset.id])
      }
    },
    []
  )

  const handleAssetDoubleClick = useCallback(
    (asset: MediaAsset) => {
      // Add to timeline at playhead
      const videoTracks = project?.tracks.filter((t) => t.type === 'video' && t.enabled) || []
      if (videoTracks.length > 0) {
        const playhead = useProjectStore.getState().timeline.playhead
        addClip(videoTracks[0].id, asset.id, playhead)
      }
    },
    [project, addClip]
  )

  const handleAddToTimeline = useCallback(
    (asset: MediaAsset, trackType: 'video' | 'audio') => {
      const tracks = project?.tracks.filter((t) => t.type === trackType && t.enabled) || []
      if (tracks.length > 0) {
        const playhead = useProjectStore.getState().timeline.playhead
        addClip(tracks[0].id, asset.id, playhead)
      }
    },
    [project, addClip]
  )

  const formatDuration = (frames: number, frameRate = 30) => {
    const seconds = frames / frameRate
    const mins = Math.floor(seconds / 60)
    const secs = Math.floor(seconds % 60)
    const ms = Math.floor((seconds % 1) * 100)
    return `${mins}:${secs.toString().padStart(2, '0')}.${ms.toString().padStart(2, '0')}`
  }

  const formatFileSize = (bytes: number) => {
    if (bytes < 1024) return `${bytes} B`
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
  }

  return (
    <div
      className="flex h-full flex-col overflow-hidden"
      onDrop={handleFileDrop}
      onDragOver={handleDragOver}
      onDragLeave={handleDragLeave}
    >
      {/* Toolbar */}
      <div className="flex h-10 shrink-0 items-center gap-2 border-b border-border px-2">
        <button
          className="btn-ghost h-8 w-8 p-0"
          onClick={() => fileInputRef.current?.click()}
          title="Import Media (Ctrl+I)"
        >
          <FilePlus className="h-4 w-4" />
        </button>
        <input
          ref={fileInputRef}
          type="file"
          multiple
          accept="video/*,audio/*,image/*"
          onChange={handleFileSelect}
          className="hidden"
        />

        <div className="relative flex-1 max-w-xs">
          <Search className="absolute left-2 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-muted-foreground" />
          <input
            type="text"
            placeholder="Search media..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="input h-7 w-full pl-8 pr-2 text-xs"
          />
        </div>

        <div className="flex items-center gap-1">
          <button
            className={cn('btn-ghost h-7 w-7 p-0', viewMode === 'grid' && 'bg-primary/20')}
            onClick={() => setViewMode('grid')}
            title="Grid View"
          >
            <Grid className="h-4 w-4" />
          </button>
          <button
            className={cn('btn-ghost h-7 w-7 p-0', viewMode === 'list' && 'bg-primary/20')}
            onClick={() => setViewMode('list')}
            title="List View"
          >
            <List className="h-4 w-4" />
          </button>
        </div>

        <select
          value={filterType}
          onChange={(e) => setFilterType(e.target.value as typeof filterType)}
          className="input h-7 w-24 text-xs"
          aria-label="Filter by type"
        >
          <option value="all">All</option>
          <option value="video">Video</option>
          <option value="audio">Audio</option>
          <option value="image">Image</option>
        </select>

        <select
          value={sortBy}
          onChange={(e) => setSortBy(e.target.value as typeof sortBy)}
          className="input h-7 w-24 text-xs"
          aria-label="Sort by"
        >
          <option value="date">Date</option>
          <option value="name">Name</option>
          <option value="type">Type</option>
          <option value="duration">Duration</option>
        </select>

        <button
          className={cn('btn-ghost h-7 w-7 p-0', sortOrder === 'desc' && 'rotate-180')}
          onClick={() => setSortOrder((prev) => (prev === 'asc' ? 'desc' : 'asc'))}
          title="Toggle sort order"
        >
          <svg className="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M3 6h18" />
            <path d="M3 12h18" />
            <path d="M3 18h18" />
          </svg>
        </button>
      </div>

      {/* Media List/Grid */}
      <div
        className={cn(
          'flex-1 overflow-auto p-2',
          dragOver && 'bg-primary/10 ring-2 ring-primary ring-inset'
        )}
      >
        {filteredMedia.length === 0 ? (
          <div className="flex h-full items-center justify-center text-muted-foreground">
            <div className="text-center p-4">
              <FilePlus className="h-12 w-12 mx-auto mb-2 opacity-30" />
              <p>{mediaPool.length === 0 ? 'No media imported yet' : 'No media matches your filter'}</p>
              <p className="text-sm mt-1">Drag & drop files or click Import</p>
            </div>
          </div>
        ) : viewMode === 'grid' ? (
          <div className="grid gap-2 grid-cols-[repeat(auto-fill,minmax(140px,1fr))]">
            {filteredMedia.map((asset) => (
              <MediaGridItem
                key={asset.id}
                asset={asset}
                selected={selectedAssets.includes(asset.id)}
                onClick={(e) => handleAssetClick(asset, e)}
                onDoubleClick={() => handleAssetDoubleClick(asset)}
                onAddToTimeline={() => handleAddToTimeline(asset, asset.type === 'audio' ? 'audio' : 'video')}
                onDelete={() => deleteMedia(asset.id)}
                formatDuration={formatDuration}
                formatFileSize={formatFileSize}
              />
            ))}
          </div>
        ) : (
          <div className="space-y-1">
            {filteredMedia.map((asset) => (
              <MediaListItem
                key={asset.id}
                asset={asset}
                selected={selectedAssets.includes(asset.id)}
                onClick={(e) => handleAssetClick(asset, e)}
                onDoubleClick={() => handleAssetDoubleClick(asset)}
                onAddToTimeline={() => handleAddToTimeline(asset, asset.type === 'audio' ? 'audio' : 'video')}
                onDelete={() => deleteMedia(asset.id)}
                formatDuration={formatDuration}
                formatFileSize={formatFileSize}
              />
            ))}
          </div>
        )}
      </div>

      {/* Status Bar */}
      <div className="flex h-7 shrink-0 items-center gap-2 border-t border-border px-2 text-xs text-muted-foreground">
        <span>{filteredMedia.length} of {mediaPool.length} items</span>
        <span>•</span>
        <span>{formatFileSize(mediaPool.reduce((sum, a) => sum + a.fileSize, 0))}</span>
        <span>•</span>
        <span>{formatDuration(mediaPool.reduce((sum, a) => sum + a.duration, 0))}</span>
      </div>
    </div>
  )
}

interface MediaGridItemProps {
  asset: MediaAsset
  selected: boolean
  onClick: (e: React.MouseEvent<HTMLDivElement>) => void
  onDoubleClick: () => void
  onAddToTimeline: () => void
  onDelete: () => void
  formatDuration: (frames: number) => string
  formatFileSize: (bytes: number) => string
}

function MediaGridItem({
  asset,
  selected,
  onClick,
  onDoubleClick,
  onAddToTimeline,
  onDelete,
  formatDuration,
  formatFileSize,
}: MediaGridItemProps) {
  const [showMenu, setShowMenu] = useState(false)
  const menuRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) {
        setShowMenu(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  const getIcon = () => {
    switch (asset.type) {
      case 'video':
        return <Video className="h-8 w-8" />
      case 'audio':
        return <Music className="h-8 w-8" />
      case 'image':
        return <Image className="h-8 w-8" />
      default:
        return <FilePlus className="h-8 w-8" />
    }
  }

  const handleClick = (e: React.MouseEvent<HTMLDivElement>) => {
    if (e.shiftKey || e.ctrlKey || e.metaKey) {
      onClick(e)
    } else {
      onClick(e)
    }
  }

  const handleDoubleClick = () => {
    onDoubleClick()
  }

  const handleAddToTimeline = () => {
    onAddToTimeline()
  }

  return (
    <div
      className={cn(
        'media-item relative group flex flex-col cursor-pointer',
        selected && 'media-item-selected'
      )}
      onClick={(e: React.MouseEvent<HTMLDivElement>) => onClick(e)}
      onDoubleClick={onDoubleClick}
      onContextMenu={(e) => {
        e.preventDefault()
        setShowMenu(true)
      }}
    >
      <div className="relative aspect-video overflow-hidden bg-muted">
        {asset.thumbnailPath ? (
          <img src={asset.thumbnailPath} alt={asset.name} className="media-thumbnail" />
        ) : (
          <div className="flex h-full w-full items-center justify-center text-muted-foreground/50">{getIcon()}</div>
        )}

        {/* Duration badge */}
        <div className="media-duration">{formatDuration(asset.duration)}</div>

        {/* Type badge */}
        <div className="absolute top-1 left-1 px-1.5 py-0.5 text-[10px] bg-black/70 text-white rounded">
          {asset.type.toUpperCase()}
        </div>

        {/* Hover actions */}
        <div className="absolute inset-0 flex items-center justify-center bg-black/50 opacity-0 transition-opacity group-hover:opacity-100">
          <button
            className="btn-ghost h-8 w-8 p-0 bg-white/10 hover:bg-white/20"
            onClick={(e) => {
              e.stopPropagation()
              onAddToTimeline()
            }}
            title="Add to timeline"
          >
            <Play className="h-4 w-4" />
          </button>
        </div>
      </div>

      <div className="flex-1 min-h-0 p-1.5">
        <p className="text-sm font-medium truncate">{asset.name}</p>
        <div className="flex items-center justify-between text-[11px] text-muted-foreground mt-0.5">
          <span>{formatFileSize(asset.fileSize)}</span>
          <span>{asset.width}×{asset.height}</span>
        </div>
      </div>

      {/* Context Menu */}
      {showMenu && (
        <div
          ref={menuRef}
          className="fixed z-50 panel min-w-[160px] animate-fade-in"
          style={{
            left: 0,
            top: 0,
          }}
        >
          <button className="flex w-full items-center gap-2 px-2 py-1.5 text-sm rounded-md hover:bg-accent" onClick={() => { setShowMenu(false); onAddToTimeline() }}>
            <Video className="h-4 w-4" /> Add to Video Track
          </button>
          <button className="flex w-full items-center gap-2 px-2 py-1.5 text-sm rounded-md hover:bg-accent" onClick={() => { setShowMenu(false); onAddToTimeline() }}>
            <Music className="h-4 w-4" /> Add to Audio Track
          </button>
          <div className="separator-horizontal my-1" />
          <button className="flex w-full items-center gap-2 px-2 py-1.5 text-sm rounded-md hover:bg-accent" onClick={() => { setShowMenu(false); /* TODO: rename */ }}>
            <Settings className="h-4 w-4" /> Properties
          </button>
          <div className="separator-horizontal my-1" />
          <button className="flex w-full items-center gap-2 px-2 py-1.5 text-sm rounded-md hover:bg-accent text-destructive" onClick={() => { setShowMenu(false); onDelete() }}>
            <Trash2 className="h-4 w-4" /> Delete
          </button>
        </div>
      )}
    </div>
  )
}

interface MediaListItemProps {
  asset: MediaAsset
  selected: boolean
  onClick: (e: React.MouseEvent<HTMLDivElement>) => void
  onDoubleClick: () => void
  onAddToTimeline: () => void
  onDelete: () => void
  formatDuration: (frames: number) => string
  formatFileSize: (bytes: number) => string
}

function MediaListItem({
  asset,
  selected,
  onClick,
  onDoubleClick,
  onAddToTimeline,
  onDelete,
  formatDuration,
  formatFileSize,
}: MediaListItemProps) {
  const [showMenu, setShowMenu] = useState(false)
  const menuRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) {
        setShowMenu(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  const getIcon = () => {
    switch (asset.type) {
      case 'video':
        return <Video className="h-4 w-4" />
      case 'audio':
        return <Music className="h-4 w-4" />
      case 'image':
        return <Image className="h-4 w-4" />
      default:
        return <FilePlus className="h-4 w-4" />
    }
  }

  const handleClick = (e: React.MouseEvent<HTMLDivElement>) => {
    onClick(e)
  }

  const handleDoubleClick = () => {
    onDoubleClick()
  }

  const handleAddToTimeline = () => {
    onAddToTimeline()
  }

  return (
    <div
      className={cn(
        'media-item flex items-center gap-3 p-1.5 rounded-lg cursor-pointer hover:bg-accent transition-colors',
        selected && 'bg-accent'
      )}
      onClick={handleClick}
      onDoubleClick={handleDoubleClick}
      onContextMenu={(e) => {
        e.preventDefault()
        setShowMenu(true)
      }}
    >
      <div className="flex-shrink-0 w-12 h-12 flex items-center justify-center bg-muted rounded">
        {getIcon()}
      </div>
      <div className="flex-1 min-w-0">
        <p className="text-sm font-medium truncate">{asset.name}</p>
        <div className="flex items-center gap-3 text-[11px] text-muted-foreground">
          <span className="px-1.5 py-0.5 bg-muted rounded text-xs uppercase">{asset.type}</span>
          <span>{formatDuration(asset.duration)}</span>
          <span>{formatFileSize(asset.fileSize)}</span>
          <span>{asset.width}×{asset.height}</span>
        </div>
      </div>
      <button
        className="btn-ghost h-7 w-7 p-0 opacity-0 group-hover:opacity-100"
        onClick={(e) => {
          e.stopPropagation()
          handleAddToTimeline()
        }}
        title="Add to timeline"
      >
        <Play className="h-4 w-4" />
      </button>
      <button
        className="btn-ghost h-7 w-7 p-0 opacity-0 group-hover:opacity-100"
        onClick={(e) => {
          e.stopPropagation()
          setShowMenu(true)
        }}
      >
        <MoreVertical className="h-4 w-4" />
      </button>

      {showMenu && (
        <div
          ref={menuRef}
          className="fixed z-50 panel min-w-[160px] animate-fade-in"
        >
          <button className="flex w-full items-center gap-2 px-2 py-1.5 text-sm rounded-md hover:bg-accent" onClick={() => { setShowMenu(false); handleAddToTimeline() }}>
            <Video className="h-4 w-4" /> Add to Video Track
          </button>
          <button className="flex w-full items-center gap-2 px-2 py-1.5 text-sm rounded-md hover:bg-accent" onClick={() => { setShowMenu(false); handleAddToTimeline() }}>
            <Music className="h-4 w-4" /> Add to Audio Track
          </button>
          <div className="separator-horizontal my-1" />
          <button className="flex w-full items-center gap-2 px-2 py-1.5 text-sm rounded-md hover:bg-accent text-destructive" onClick={() => { setShowMenu(false); onDelete() }}>
            <Trash2 className="h-4 w-4" /> Delete
          </button>
        </div>
      )}
    </div>
  )
}