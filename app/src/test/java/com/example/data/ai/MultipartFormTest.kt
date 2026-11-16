package com.example.data.ai

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The multipart envelope must be byte-exact for speech-to-text gateways. */
class MultipartFormTest {

    private val boundary = "CatByteBoundaryTEST"
    private val fileBytes = byteArrayOf(0, 1, 2, 3, 127, -1)

    private fun body() = MultipartForm.body(
        boundary = boundary,
        fields = mapOf("model" to "whisper-1", "response_format" to "verbose_json"),
        fileFieldName = "file",
        fileName = "audio.m4a",
        fileBytes = fileBytes
    )

    @Test
    fun `fields are written with the boundary`() {
        val text = body().toString(Charsets.UTF_8)
        assertTrue(text.contains("--$boundary\r\n"))
        assertTrue(text.contains("Content-Disposition: form-data; name=\"model\"\r\n\r\nwhisper-1\r\n"))
        assertTrue(text.contains("name=\"response_format\"\r\n\r\nverbose_json\r\n"))
    }

    @Test
    fun `file part keeps the bytes intact`() {
        val bytes = body()
        val text = bytes.toString(Charsets.ISO_8859_1)
        assertTrue(text.contains("name=\"file\"; filename=\"audio.m4a\""))
        assertTrue(text.contains("Content-Type: application/octet-stream"))
        // The binary payload appears verbatim between the headers and CRLF.
        var offset = -1
        for (i in 0..bytes.size - fileBytes.size) {
            if (bytes.copyOfRange(i, i + fileBytes.size).contentEquals(fileBytes)) {
                offset = i
                break
            }
        }
        assertTrue("file bytes not found", offset >= 0)
        assertArrayEquals(fileBytes, bytes.copyOfRange(offset, offset + fileBytes.size))
    }

    @Test
    fun `body ends with the closing boundary`() {
        val text = body().toString(Charsets.UTF_8)
        assertTrue(text.endsWith("--$boundary--\r\n"))
    }

    @Test
    fun `content type carries the boundary`() {
        assertEquals(
            "multipart/form-data; boundary=$boundary",
            MultipartForm.contentType(boundary)
        )
    }
}
