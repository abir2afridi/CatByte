package com.example.data

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.util.UUID

/**
 * Extracts the audio track of a video clip into a small `.m4a` file.
 *
 * AI auto captions upload this file to the user's speech-to-text endpoint.
 * Sending the whole video would blow through a Whisper-class 25 MB request
 * limit after twenty seconds of 1080p, so only the audio track (AAC passthrough,
 * no re-encode) is muxed out.
 */
object AiAudioExtractor {

    sealed interface Result {
        data class Ok(val file: File, val sourceStartMs: Long, val durationMs: Long) : Result
        /** [noAudio] lets a multi-clip caption run skip silent clips instead of aborting. */
        data class Failure(val message: String, val noAudio: Boolean = false) : Result
    }

    suspend fun extract(
        context: Context,
        uriString: String,
        /** Source offset the clip starts at. */
        startMs: Long,
        /** How much source audio to include. */
        durationMs: Long
    ): Result = withContext(Dispatchers.IO) {
        if (durationMs <= 0L) return@withContext Result.Failure("The clip has nothing to transcribe")
        val output = File(
            context.cacheDir,
            "ai_caption_${UUID.randomUUID().toString().take(8)}.m4a"
        )

        var extractor: MediaExtractor? = null
        var muxer: MediaMuxer? = null
        var started = false
        try {
            extractor = MediaExtractor().apply {
                setDataSource(context, Uri.parse(uriString), null)
            }
            val audioTrack = (0 until extractor.trackCount).firstOrNull { index ->
                extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)
                    ?.startsWith("audio/") == true
            } ?: return@withContext Result.Failure("This clip has no audio track to transcribe", noAudio = true)

            val format = extractor.getTrackFormat(audioTrack)
            extractor.selectTrack(audioTrack)
            val startUs = startMs.coerceAtLeast(0L) * 1_000L
            val endUs = startUs + durationMs.coerceAtLeast(1L) * 1_000L
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            muxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val outTrack = muxer.addTrack(format)
            muxer.start()
            started = true

            val maxInput = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
                .takeIf { it > 0 } ?: (256 * 1024)
            val buffer = ByteBuffer.allocateDirect(maxInput)
            val info = MediaCodec.BufferInfo()
            var written = 0
            var firstPtsUs = -1L

            while (true) {
                val size = extractor.readSampleData(buffer, 0)
                if (size < 0) break
                val ptsUs = extractor.sampleTime
                if (ptsUs < 0L) break
                if (ptsUs > endUs) break
                if (ptsUs >= startUs) {
                    if (firstPtsUs < 0L) firstPtsUs = ptsUs
                    info.offset = 0
                    info.size = size
                    info.presentationTimeUs = (ptsUs - firstPtsUs).coerceAtLeast(0L)
                    info.flags = if (extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) {
                        MediaCodec.BUFFER_FLAG_KEY_FRAME
                    } else {
                        0
                    }
                    muxer.writeSampleData(outTrack, buffer, info)
                    written++
                }
                extractor.advance()
            }

            if (written == 0) {
                Result.Failure("No audio in the selected range", noAudio = true)
            } else {
                Result.Ok(
                    file = output,
                    sourceStartMs = (firstPtsUs / 1_000L).coerceAtLeast(0L),
                    durationMs = durationMs
                )
            }
        } catch (error: Throwable) {
            AppLog.warn("AiAudioExtractor", "Could not extract audio for transcription", error)
            Result.Failure("Could not read the clip's audio: ${error.message ?: error.javaClass.simpleName}")
        } finally {
            if (started) runCatching { muxer?.stop() }
            runCatching { muxer?.release() }
            runCatching { extractor?.release() }
        }
    }
}
