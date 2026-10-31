// Fabric.js type extensions
import 'fabric'

declare module 'fabric' {
  interface FabricObject {
    clipId?: string
  }
}

declare module '@ffmpeg/ffmpeg' {
  interface FFmpeg {
    FS: {
      writeFile: (name: string, data: Uint8Array | string) => void
      readFile: (name: string) => Uint8Array
    }
    setLogger: (logger: (msg: { type: string; message: string }) => void) => void
    run: (...args: string[]) => Promise<void>
  }
}