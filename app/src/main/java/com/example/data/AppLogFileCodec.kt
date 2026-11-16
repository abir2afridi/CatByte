package com.example.data

/**
 * On-disk format for the in-app log: one entry per line, fields separated by
 * `|` and escaped so newlines and separators survive a round-trip.
 */
internal object AppLogFileCodec {

    fun serialize(entry: AppLogEntry): String =
        listOf(
            entry.timestamp.toString(),
            entry.level.name,
            escape(entry.tag),
            escape(entry.message),
            escape(entry.stackTrace.orEmpty())
        ).joinToString("|")

    fun parse(line: String): AppLogEntry? {
        val parts = line.split('|', limit = 5)
        if (parts.size != 5) return null
        return runCatching {
            AppLogEntry(
                timestamp = parts[0].toLong(),
                level = AppLogLevel.valueOf(parts[1]),
                tag = unescape(parts[2]),
                message = unescape(parts[3]),
                stackTrace = unescape(parts[4]).ifBlank { null },
                fromPreviousSession = true
            )
        }.getOrNull()
    }

    private fun escape(value: String): String {
        val result = StringBuilder(value.length)
        value.forEach { char ->
            when (char) {
                '\\' -> result.append("\\\\")
                '\n' -> result.append("\\n")
                '\r' -> result.append("\\r")
                '|' -> result.append("\\p")
                else -> result.append(char)
            }
        }
        return result.toString()
    }

    private fun unescape(value: String): String {
        val result = StringBuilder(value.length)
        var index = 0
        while (index < value.length) {
            val char = value[index]
            if (char == '\\' && index + 1 < value.length) {
                when (value[index + 1]) {
                    'n' -> {
                        result.append('\n')
                        index += 2
                    }
                    'r' -> {
                        result.append('\r')
                        index += 2
                    }
                    'p' -> {
                        result.append('|')
                        index += 2
                    }
                    '\\' -> {
                        result.append('\\')
                        index += 2
                    }
                    else -> {
                        result.append(char)
                        index++
                    }
                }
            } else {
                result.append(char)
                index++
            }
        }
        return result.toString()
    }
}
