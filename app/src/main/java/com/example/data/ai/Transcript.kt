package com.example.data.ai

import java.io.ByteArrayOutputStream

/** One timed phrase returned by a speech-to-text endpoint. */
data class TranscriptSegment(
    val startMs: Long,
    val endMs: Long,
    val text: String
)

/** A full transcription: timed segments plus the plain text. */
data class Transcript(
    val segments: List<TranscriptSegment>,
    val text: String = segments.joinToString(" ") { it.text }
)

/**
 * Builds a `multipart/form-data` body for endpoints that take a file upload
 * (Whisper-compatible `/audio/transcriptions`).
 *
 * Kept separate from the HTTP code so the exact bytes can be unit-tested: a
 * malformed boundary is the classic reason a self-hosted gateway rejects a
 * request that works against OpenAI.
 */
object MultipartForm {

    private const val CRLF = "\r\n"

    fun body(
        boundary: String,
        fields: Map<String, String>,
        fileFieldName: String,
        fileName: String,
        fileBytes: ByteArray,
        fileContentType: String = "application/octet-stream"
    ): ByteArray {
        val out = ByteArrayOutputStream()
        fields.forEach { (name, value) ->
            out.write("--$boundary$CRLF".toByteArray())
            out.write("Content-Disposition: form-data; name=\"$name\"$CRLF".toByteArray())
            out.write(CRLF.toByteArray())
            out.write(value.toByteArray(Charsets.UTF_8))
            out.write(CRLF.toByteArray())
        }
        out.write("--$boundary$CRLF".toByteArray())
        out.write(
            ("Content-Disposition: form-data; name=\"$fileFieldName\"; filename=\"$fileName\"$CRLF")
                .toByteArray()
        )
        out.write("Content-Type: $fileContentType$CRLF".toByteArray())
        out.write(CRLF.toByteArray())
        out.write(fileBytes)
        out.write(CRLF.toByteArray())
        out.write("--$boundary--$CRLF".toByteArray())
        return out.toByteArray()
    }

    /** Content-Type header value for [boundary]. */
    fun contentType(boundary: String): String = "multipart/form-data; boundary=$boundary"
}
