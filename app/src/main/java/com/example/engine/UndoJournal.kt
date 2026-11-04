package com.example.engine

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Byte-level store the journal persists through. */
interface JournalStorage {
    fun read(): String?
    fun write(text: String)
    fun clear()
}

/**
 * Atomic file store: write a sibling temp file, flush it to disk, then rename.
 * A crash mid-write therefore leaves the previous journal intact instead of a
 * truncated one.
 */
class FileJournalStorage(private val file: File) : JournalStorage {

    override fun read(): String? = runCatching {
        if (!file.exists()) null else file.readText()
    }.getOrNull()

    override fun write(text: String) {
        runCatching {
            file.parentFile?.mkdirs()
            val temp = File(file.parentFile, "${file.name}.tmp")
            temp.writeText(text)
            if (!temp.renameTo(file)) {
                file.delete()
                temp.renameTo(file)
            }
        }
    }

    override fun clear() {
        runCatching { file.delete() }
    }
}

/**
 * Persistent undo/redo history.
 *
 * The in-memory stacks in the editor die with the process, which is why a crash
 * used to lose every undo step. This journal stores the *pre-edit* project JSON
 * for each step, coalesces a drag/slider stream into a single step, and restores
 * both stacks on the next launch.
 */
class UndoJournal(
    private val storage: JournalStorage,
    private val maxEntries: Int = 200,
    private val coalesceWindowMs: Long = 500L,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {

    private data class Entry(val projectJson: String, val key: String?, val atMs: Long)

    private val undoStack = mutableListOf<Entry>()
    private val redoStack = mutableListOf<Entry>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()
    val undoDepth: Int get() = undoStack.size
    val redoDepth: Int get() = redoStack.size

    /** Returns true when a previous session's history was restored. */
    fun load(): Boolean {
        val raw = storage.read() ?: return false
        return runCatching {
            val root = JSONObject(raw)
            if (root.optInt("version", 0) != FORMAT_VERSION) return false
            undoStack.clear()
            redoStack.clear()
            readStack(root.optJSONArray("undo"), undoStack)
            readStack(root.optJSONArray("redo"), redoStack)
            undoStack.isNotEmpty()
        }.getOrDefault(false)
    }

    /**
     * Records the snapshot taken *before* an edit. Repeated calls with the same
     * [key] inside [coalesceWindowMs] collapse into one undo step so one gesture
     * is one undo.
     */
    fun record(before: String, key: String? = null) {
        val now = clock()
        val last = undoStack.lastOrNull()
        if (key != null && last != null && last.key == key && now - last.atMs < coalesceWindowMs) {
            undoStack[undoStack.lastIndex] = last.copy(atMs = now)
            persist()
            return
        }
        undoStack.add(Entry(before, key, now))
        while (undoStack.size > maxEntries) {
            undoStack.removeAt(0)
        }
        redoStack.clear()
        persist()
    }

    /** Returns the project JSON to restore, or null when nothing is left. */
    fun undo(current: String): String? {
        if (undoStack.isEmpty()) return null
        val previous = undoStack.removeAt(undoStack.lastIndex)
        redoStack.add(Entry(current, null, clock()))
        persist()
        return previous.projectJson
    }

    fun redo(current: String): String? {
        if (redoStack.isEmpty()) return null
        val next = redoStack.removeAt(redoStack.lastIndex)
        undoStack.add(Entry(current, null, clock()))
        persist()
        return next.projectJson
    }

    /**
     * Clears the stacks. The persisted journal is only dropped when [deleteFile]
     * is set, so reopening the same project can recover history after a crash.
     */
    fun clear(deleteFile: Boolean = false) {
        undoStack.clear()
        redoStack.clear()
        if (deleteFile) {
            storage.clear()
        } else {
            persist()
        }
    }

    private fun persist() {
        runCatching { storage.write(serialise()) }
    }

    private fun serialise(): String {
        val root = JSONObject()
        root.put("version", FORMAT_VERSION)
        root.put("undo", writeStack(undoStack))
        root.put("redo", writeStack(redoStack))
        return root.toString()
    }

    private fun writeStack(entries: List<Entry>): JSONArray = JSONArray().also { array ->
        entries.forEach { entry ->
            array.put(
                JSONObject().apply {
                    put("project", entry.projectJson)
                    put("key", entry.key ?: JSONObject.NULL)
                    put("at", entry.atMs)
                }
            )
        }
    }

    private fun readStack(array: JSONArray?, target: MutableList<Entry>) {
        if (array == null) return
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val project = item.optString("project", "")
            if (project.isEmpty()) continue
            target.add(
                Entry(
                    projectJson = project,
                    key = if (item.isNull("key")) null else item.optString("key"),
                    atMs = item.optLong("at", 0L)
                )
            )
        }
    }

    private companion object {
        const val FORMAT_VERSION = 1
    }
}
