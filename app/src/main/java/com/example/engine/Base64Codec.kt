package com.example.engine

/**
 * A tiny Base64 codec.
 *
 * `java.util.Base64` needs API 26 and `android.util.Base64` is a stub in
 * plain JVM unit tests, so the LUT payload uses this implementation: it
 * works on every supported API level and can be unit tested.
 */
internal object Base64Codec {

    private const val ALPHABET =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

    fun encode(bytes: ByteArray): String {
        if (bytes.isEmpty()) return ""
        val out = StringBuilder((bytes.size + 2) / 3 * 4)
        var index = 0
        while (index + 2 < bytes.size) {
            val chunk = ((bytes[index].toInt() and 0xFF) shl 16) or
                ((bytes[index + 1].toInt() and 0xFF) shl 8) or
                (bytes[index + 2].toInt() and 0xFF)
            out.append(ALPHABET[(chunk shr 18) and 0x3F])
            out.append(ALPHABET[(chunk shr 12) and 0x3F])
            out.append(ALPHABET[(chunk shr 6) and 0x3F])
            out.append(ALPHABET[chunk and 0x3F])
            index += 3
        }
        when (bytes.size - index) {
            1 -> {
                val chunk = (bytes[index].toInt() and 0xFF) shl 16
                out.append(ALPHABET[(chunk shr 18) and 0x3F])
                out.append(ALPHABET[(chunk shr 12) and 0x3F])
                out.append("==")
            }
            2 -> {
                val chunk = ((bytes[index].toInt() and 0xFF) shl 16) or
                    ((bytes[index + 1].toInt() and 0xFF) shl 8)
                out.append(ALPHABET[(chunk shr 18) and 0x3F])
                out.append(ALPHABET[(chunk shr 12) and 0x3F])
                out.append(ALPHABET[(chunk shr 6) and 0x3F])
                out.append('=')
            }
        }
        return out.toString()
    }

    /** Decodes a Base64 string, or returns null when the input is broken. */
    fun decode(text: String): ByteArray? {
        val clean = text.trim()
        if (clean.isEmpty()) return ByteArray(0)
        if (clean.length % 4 != 0) return null
        val out = ByteArray(clean.length / 4 * 3)
        var outIndex = 0
        var chunkIndex = 0
        while (chunkIndex < clean.length / 4) {
            val offset = chunkIndex * 4
            val c0 = clean[offset]
            val c1 = clean[offset + 1]
            val c2 = clean[offset + 2]
            val c3 = clean[offset + 3]
            val v0 = ALPHABET.indexOf(c0)
            val v1 = ALPHABET.indexOf(c1)
            if (v0 < 0 || v1 < 0) return null
            var buffer = (v0 shl 18) or (v1 shl 12)
            if (c2 == '=') {
                if (c3 != '=') return null
                out[outIndex++] = ((buffer shr 16) and 0xFF).toByte()
            } else {
                val v2 = ALPHABET.indexOf(c2)
                if (v2 < 0) return null
                buffer = buffer or (v2 shl 6)
                if (c3 == '=') {
                    out[outIndex++] = ((buffer shr 16) and 0xFF).toByte()
                    out[outIndex++] = ((buffer shr 8) and 0xFF).toByte()
                } else {
                    val v3 = ALPHABET.indexOf(c3)
                    if (v3 < 0) return null
                    buffer = buffer or v3
                    out[outIndex++] = ((buffer shr 16) and 0xFF).toByte()
                    out[outIndex++] = ((buffer shr 8) and 0xFF).toByte()
                    out[outIndex++] = (buffer and 0xFF).toByte()
                }
            }
            chunkIndex++
        }
        return if (outIndex == out.size) out else out.copyOf(outIndex)
    }
}
