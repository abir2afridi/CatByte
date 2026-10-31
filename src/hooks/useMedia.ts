import { useCallback, useRef, useState, useEffect } from 'react'
import { FFmpeg } from '@ffmpeg/ffmpeg'
import { toBlobURL } from '@ffmpeg/util'
import { useProjectStore } from '@/store/projectStore'
import type { MediaAsset, ExportSettings } from '@/types'

const ffmpegRef = useRef<FFmpeg | null>(null)
const isLoadingRef = useRef(false)
const loadPromiseRef = useRef<Promise<void> | null>(null)

async function getFFmpeg() {
  if (ffmpegRef.current) return ffmpegRef.current

  if (loadPromiseRef.current) {
    await loadPromiseRef.current
    return ffmpegRef.current!
  }

  loadPromiseRef.current = (async () => {
    if (isLoadingRef.current) return
    isLoadingRef.current = true

    const baseURL = 'https://unpkg.com/@ffmpeg/core@0.12.6/dist/umd/'
    const ffmpeg = new FFmpeg()

    await ffmpeg.load({
      coreURL: await toBlobURL(`${baseURL}ffmpeg-core.js`, 'text/javascript'),
      wasmURL: await toBlobURL(`${baseURL}ffmpeg-core.wasm`, 'application/wasm'),
      workerURL: await toBlobURL(`${baseURL}ffmpeg-core.worker.js`, 'text/javascript'),
    })

    ffmpegRef.current = ffmpeg
    isLoadingRef.current = false
  })()

  await loadPromiseRef.current
  return ffmpegRef.current!
}

export function useFFmpeg() {
  const [ready, setReady] = useState(false)
  const [progress, setProgress] = useState(0)

  useEffect(() => {
    let mounted = true
    getFFmpeg().then(() => {
      if (mounted) setReady(true)
    })
    return () => { mounted = false }
  }, [])

  const writeFile = useCallback(async (name: string, data: Uint8Array | string) => {
    const ffmpeg = await getFFmpeg()
    await ffmpeg.writeFile(name, data as Uint8Array)
  }, [])

  const readFile = useCallback(async (name: string): Promise<Uint8Array> => {
    const ffmpeg = await getFFmpeg()
    const data = await ffmpeg.readFile(name)
    return data as Uint8Array
  }, [])

  const runCommand = useCallback(async (args: string[], onProgress?: (p: number) => void): Promise<void> => {
    const ffmpeg = await getFFmpeg()
    return new Promise((resolve, reject) => {
      ffmpeg.on('log', ({ type, message }) => {
        if (type === 'ffout' || type === 'fferr') {
          const match = message.match(/time=(\d+):(\d+):(\d+\.\d+)/)
          if (match && onProgress) {
            const hours = parseInt(match[1])
            const minutes = parseInt(match[2])
            const seconds = parseFloat(match[3])
            const totalSeconds = hours * 3600 + minutes * 60 + seconds
            onProgress(totalSeconds)
          }
        }
      })

      ffmpeg.exec(args)
        .then(() => resolve())
        .catch(reject)
    })
  }, [])

  const exportVideo = useCallback(async (
    inputPath: string,
    outputPath: string,
    settings: ExportSettings,
    onProgress?: (progress: number) => void
  ) => {
    const ffmpeg = await getFFmpeg()

    const args = [
      '-y',
      '-i', inputPath,
      '-c:v', settings.codec === 'h264' ? 'libx264' : settings.codec === 'hevc' ? 'libx265' : 'libvpx-vp9',
      '-preset', settings.quality === 'high' ? 'slow' : settings.quality === 'lossless' ? 'veryslow' : 'medium',
      '-crf', settings.quality === 'lossless' ? '0' : settings.quality === 'high' ? '18' : settings.quality === 'medium' ? '23' : '28',
      '-pix_fmt', 'yuv420p',
      '-c:a', 'aac',
      '-b:a', `${settings.audioBitrate}k`,
      '-ar', String(settings.audioSampleRate),
      '-ac', String(settings.audioChannels),
      '-movflags', '+faststart',
      outputPath,
    ]

    if (settings.twoPass) {
      await runCommand([
        '-y', '-i', inputPath,
        '-c:v', settings.codec === 'h264' ? 'libx264' : 'libx265',
        '-b:v', `${settings.bitrate}k`,
        '-pass', '1',
        '-f', settings.format === 'mp4' ? 'mp4' : 'webm',
        '-an', '/dev/null',
      ], onProgress)

      await runCommand([
        '-y', '-i', inputPath,
        '-c:v', settings.codec === 'h264' ? 'libx264' : 'libx265',
        '-b:v', `${settings.bitrate}k`,
        '-pass', '2',
        ...args.slice(args.indexOf('-c:a')),
      ], onProgress)
    } else {
      await runCommand(args, onProgress)
    }
  }, [runCommand])

  const generateThumbnail = useCallback(async (inputPath: string, outputPath: string, time: string = '00:00:01') => {
    const ffmpeg = await getFFmpeg()
    await ffmpeg.exec(['-y', '-ss', time, '-i', inputPath, '-vframes', '1', '-q:v', '2', outputPath])
  }, [])

  const getVideoInfo = useCallback(async (inputPath: string) => {
    const ffmpeg = await getFFmpeg()
    await ffmpeg.exec(['-i', inputPath])
  }, [])

  const concatVideos = useCallback(async (inputPaths: string[], outputPath: string) => {
    const ffmpeg = await getFFmpeg()
    const concatContent = inputPaths.map((p) => `file '${p}'`).join('\n')
    await writeFile('concat.txt', new TextEncoder().encode(concatContent))

    await runCommand([
      '-y',
      '-f', 'concat',
      '-safe', '0',
      '-i', 'concat.txt',
      '-c', 'copy',
      outputPath,
    ])
  }, [runCommand, writeFile])

  const extractAudio = useCallback(async (inputPath: string, outputPath: string) => {
    await runCommand([
      '-y',
      '-i', inputPath,
      '-vn',
      '-acodec', 'copy',
      outputPath,
    ])
  }, [runCommand])

  const createProxy = useCallback(async (inputPath: string, outputPath: string, resolution: 'half' | 'quarter' = 'half') => {
    const scale = resolution === 'half' ? 'iw/2:ih/2' : 'iw/4:ih/4'
    await runCommand([
      '-y',
      '-i', inputPath,
      '-vf', `scale=${scale}`,
      '-c:v', 'libx264',
      '-preset', 'ultrafast',
      '-crf', '28',
      '-an',
      outputPath,
    ])
  }, [runCommand])

  return {
    ready,
    progress,
    writeFile,
    readFile,
    runCommand,
    exportVideo,
    generateThumbnail,
    getVideoInfo,
    concatVideos,
    extractAudio,
    createProxy,
  }
}

// Media management hook
export function useMedia() {
  const { project, addMediaAsset, removeMediaAsset, updateMediaAsset } = useProjectStore()
  const { writeFile, runCommand, generateThumbnail } = useFFmpeg()

  const importMedia = useCallback(async (file: File): Promise<MediaAsset> => {
    const arrayBuffer = await file.arrayBuffer()
    const uint8Array = new Uint8Array(arrayBuffer)
    const fileName = file.name
    const filePath = `/media/${generateId()}_${fileName}`

    await writeFile(filePath, uint8Array)

    const isVideo = file.type.startsWith('video/')
    const isAudio = file.type.startsWith('audio/')
    const isImage = file.type.startsWith('image/')

    let duration = 0
    let width = 0
    let height = 0
    let frameRate = 30

    if (isVideo) {
      duration = 30 * 30
      width = 1920
      height = 1080
    } else if (isImage) {
      duration = project?.settings.frameRate * 5 || 150
    } else if (isAudio) {
      duration = 30 * 30
    }

    const asset = await addMediaAsset({
      name: fileName,
      type: isVideo ? 'video' : isAudio ? 'audio' : 'image',
      filePath,
      duration,
      width,
      height,
      frameRate: frameRate as 24 | 25 | 30 | 50 | 60,
      fileSize: file.size,
      metadata: { originalName: fileName, mimeType: file.type },
    })

    if (isVideo || isImage) {
      const thumbPath = `${filePath}.thumb.jpg`
      try {
        if (isVideo) {
          await generateThumbnail(filePath, thumbPath)
          updateMediaAsset(asset.id, { thumbnailPath: thumbPath })
        }
      } catch (e) {
        console.warn('Failed to generate thumbnail:', e)
      }
    }

    return asset
  }, [project, writeFile, generateThumbnail, addMediaAsset, updateMediaAsset])

  const importMultiple = useCallback(async (files: FileList | File[]) => {
    const results: MediaAsset[] = []
    for (const file of Array.from(files)) {
      try {
        const asset = await importMedia(file)
        results.push(asset)
      } catch (error) {
        console.error('Failed to import:', file.name, error)
      }
    }
    return results
  }, [importMedia])

  const deleteMedia = useCallback(async (assetId: string) => {
    const asset = project?.mediaPool.find((a) => a.id === assetId)
    if (asset) {
      removeMediaAsset(assetId)
    }
  }, [project, removeMediaAsset])

  return {
    mediaPool: project?.mediaPool || [],
    importMedia,
    importMultiple,
    deleteMedia,
  }
}

function generateId(): string {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 11)}`
}

// Timeline utilities
export function useTimelineUtils() {
  const { timeline, project } = useProjectStore()

  const frameToX = useCallback((frame: number): number => {
    return (frame / timeline.zoom) - timeline.scrollX
  }, [timeline.zoom, timeline.scrollX])

  const xToFrame = useCallback((x: number): number => {
    return Math.round((x + timeline.scrollX) * timeline.zoom)
  }, [timeline.zoom, timeline.scrollX])

  const timeToFrame = useCallback((seconds: number): number => {
    return Math.round(seconds * (project?.settings.frameRate || 30))
  }, [project?.settings.frameRate])

  const frameToTime = useCallback((frame: number): number => {
    return frame / (project?.settings.frameRate || 30)
  }, [project?.settings.frameRate])

  const formatTimecode = useCallback((frame: number): string => {
    const tc = useProjectStore.getState().framesToTimecode(frame)
    return `${tc.hours.toString().padStart(2, '0')}:${tc.minutes.toString().padStart(2, '0')}:${tc.seconds.toString().padStart(2, '0')}:${tc.frames.toString().padStart(2, '0')}`
  }, [])

  const formatTime = useCallback((frame: number): string => {
    const seconds = frameToTime(frame)
    const mins = Math.floor(seconds / 60)
    const secs = Math.floor(seconds % 60)
    const ms = Math.floor((seconds % 1) * 100)
    return `${mins}:${secs.toString().padStart(2, '0')}.${ms.toString().padStart(2, '0')}`
  }, [frameToTime])

  const snapToGrid = useCallback((frame: number): number => {
    if (!timeline.snapEnabled) return frame
    const frameRate = project?.settings.frameRate || 30
    const snapInterval = frameRate
    return Math.round(frame / snapInterval) * snapInterval
  }, [timeline.snapEnabled, project?.settings.frameRate])

  return {
    frameToX,
    xToFrame,
    timeToFrame,
    frameToTime,
    formatTimecode,
    formatTime,
    snapToGrid,
  }
}