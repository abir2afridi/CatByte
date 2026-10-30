package com.example.data

import android.content.Context
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors

/**
 * In-app log collector used by the System Logs screen.
 *
 * Keeps a bounded, thread-safe ring buffer in memory and mirrors entries to a
 * small text file so a crash report survives the process being killed. An
 * uncaught-exception handler is installed in [init] so real crashes are always
 * captured with their stack traces.
 */
object AppLog {

    private const val MAX_ENTRIES = 800
    private const val MAX_FILE_BYTES = 512L * 1024L
    private const val LOG_FILE_NAME = "catbyte.log"

    private val entries = CopyOnWriteArrayList<AppLogEntry>()
    private val ioExecutor = Executors.newSingleThreadExecutor()
    private val fileLock = Any()

    private var logFile: File? = null
    private var initialized = false

    /**
     * Call once from [android.app.Application.onCreate].
     * Safe to call more than once; only the first call installs the crash handler.
     */
    @Synchronized
    fun init(context: Context) {
        if (initialized) return
        initialized = true

        runCatching {
            val dir = File(context.filesDir, "logs").apply { mkdirs() }
            logFile = File(dir, LOG_FILE_NAME)
            readPersistedEntries()
            installCrashHandler()
        }
    }

    fun debug(tag: String, message: String) {
        add(AppLogEntry(System.currentTimeMillis(), AppLogLevel.DEBUG, tag, message))
    }

    fun info(tag: String, message: String) {
        add(AppLogEntry(System.currentTimeMillis(), AppLogLevel.INFO, tag, message))
    }

    fun warn(tag: String, message: String, throwable: Throwable? = null) {
        add(
            AppLogEntry(
                timestamp = System.currentTimeMillis(),
                level = AppLogLevel.WARN,
                tag = tag,
                message = message,
                stackTrace = throwable?.stackTraceString()
            )
        )
    }

    fun error(tag: String, message: String, throwable: Throwable? = null) {
        add(
            AppLogEntry(
                timestamp = System.currentTimeMillis(),
                level = AppLogLevel.ERROR,
                tag = tag,
                message = message,
                stackTrace = throwable?.stackTraceString()
            )
        )
    }

    /** Returns newest-first so the most recent crash is at the top of the screen. */
    fun snapshot(): List<AppLogEntry> = entries.sortedByDescending { it.timestamp }

    fun errorCount(): Int = entries.count { it.level == AppLogLevel.ERROR }

    fun crashCount(): Int = entries.count { it.level == AppLogLevel.ERROR && it.tag == CRASH_TAG }

    fun clear() {
        entries.clear()
        val file = logFile
        ioExecutor.execute {
            synchronized(fileLock) {
                runCatching { file?.delete() }
            }
        }
    }

    // ---------------------------------------------------------------------
    // Internals
    // ---------------------------------------------------------------------

    private const val CRASH_TAG = "Crash"

    private fun add(entry: AppLogEntry) {
        entries.add(entry)
        while (entries.size > MAX_ENTRIES) {
            entries.removeAt(0)
        }
        writeEntry(entry, synchronous = entry.level == AppLogLevel.ERROR)
        logToLogcat(entry)
    }

    private fun installCrashHandler() {
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                val entry = AppLogEntry(
                    timestamp = System.currentTimeMillis(),
                    level = AppLogLevel.ERROR,
                    tag = CRASH_TAG,
                    message = buildString {
                        append("Uncaught exception on thread \"")
                        append(thread.name)
                        append("\": ")
                        append(throwable.javaClass.name)
                        throwable.message?.let { append(": ").append(it) }
                    },
                    stackTrace = throwable.stackTraceString()
                )
                entries.add(entry)
                while (entries.size > MAX_ENTRIES) {
                    entries.removeAt(0)
                }
                writeEntry(entry, synchronous = true)
                Log.e(CRASH_TAG, entry.message, throwable)
            }
            // Let the platform show its normal "app has stopped" flow.
            previousHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun writeEntry(entry: AppLogEntry, synchronous: Boolean) {
        val file = logFile ?: return
        val task = Runnable {
            synchronized(fileLock) {
                runCatching {
                    trimIfNeeded(file)
                    file.appendText(entry.serialize() + "\n")
                }
            }
        }
        if (synchronous) task.run() else ioExecutor.execute(task)
    }

    private fun trimIfNeeded(file: File) {
        if (file.length() <= MAX_FILE_BYTES) return
        val text = file.readText()
        val keep = text.takeLast((MAX_FILE_BYTES / 2).toInt())
        val firstNewline = keep.indexOf('\n')
        file.writeText(if (firstNewline >= 0) keep.substring(firstNewline + 1) else keep)
    }

    private fun readPersistedEntries() {
        val file = logFile ?: return
        if (!file.exists()) return
        runCatching {
            file.useLines { lines ->
                lines.toList()
                    .takeLast(MAX_ENTRIES)
                    .forEach { line ->
                        parseEntry(line)?.let { entries.add(it) }
                    }
            }
        }
    }

    private fun AppLogEntry.serialize(): String =
        listOf(
            timestamp.toString(),
            level.name,
            escape(tag),
            escape(message),
            escape(stackTrace.orEmpty())
        ).joinToString("|")

    private fun parseEntry(line: String): AppLogEntry? {
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

    private fun logToLogcat(entry: AppLogEntry) {
        val message = if (entry.stackTrace.isNullOrBlank()) {
            entry.message
        } else {
            entry.message + "\n" + entry.stackTrace
        }
        when (entry.level) {
            AppLogLevel.ERROR -> Log.e(entry.tag, message)
            AppLogLevel.WARN -> Log.w(entry.tag, message)
            AppLogLevel.INFO -> Log.i(entry.tag, message)
            AppLogLevel.DEBUG -> Log.d(entry.tag, message)
        }
    }

    private fun Throwable.stackTraceString(): String {
        val writer = StringWriter()
        printStackTrace(PrintWriter(writer))
        return writer.toString()
    }
}

enum class AppLogLevel(val label: String) {
    ERROR("ERROR"),
    WARN("WARN"),
    INFO("INFO"),
    DEBUG("DEBUG")
}

data class AppLogEntry(
    val timestamp: Long,
    val level: AppLogLevel,
    val tag: String,
    val message: String,
    val stackTrace: String? = null,
    val fromPreviousSession: Boolean = false
) {
    val timeLabel: String
        get() = SimpleDateFormat("MMM d, HH:mm:ss.SSS", Locale.getDefault()).format(Date(timestamp))

    val fullText: String
        get() = buildString {
            append(timeLabel)
            append(' ')
            append(level.label)
            append(" [")
            append(tag)
            append("] ")
            append(message)
            if (!stackTrace.isNullOrBlank()) {
                append('\n')
                append(stackTrace)
            }
        }
}
