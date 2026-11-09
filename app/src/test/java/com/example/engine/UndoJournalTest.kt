package com.example.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class UndoJournalTest {

    private class MemoryStorage : JournalStorage {
        var text: String? = null
        var writes = 0
        override fun read(): String? = text
        override fun write(text: String) {
            this.text = text
            writes++
        }

        override fun clear() {
            text = null
        }
    }

    @Test
    fun `undo walks back and redo walks forward`() {
        val journal = UndoJournal(MemoryStorage())
        journal.record("A")
        journal.record("B")
        assertEquals(2, journal.undoDepth)

        assertEquals("B", journal.undo("C"))
        assertEquals("A", journal.undo("B"))
        assertNull(journal.undo("A"))
        assertTrue(journal.canRedo)

        assertEquals("B", journal.redo("A"))
        assertEquals("C", journal.redo("B"))
        assertNull(journal.redo("C"))
    }

    @Test
    fun `one drag gesture is one undo step`() {
        val journal = UndoJournal(MemoryStorage(), coalesceWindowMs = 500L, clock = { 1_000L })
        journal.record("S0", key = "slider:brightness")
        journal.record("S1", key = "slider:brightness")
        journal.record("S2", key = "slider:brightness")
        assertEquals(1, journal.undoDepth)

        journal.record("S3", key = "slider:contrast")
        assertEquals(2, journal.undoDepth)
    }

    @Test
    fun `edits outside the window do not coalesce`() {
        var now = 0L
        val journal = UndoJournal(MemoryStorage(), coalesceWindowMs = 500L, clock = { now })
        journal.record("S0", key = "drag")
        now = 5_000L
        journal.record("S1", key = "drag")
        assertEquals(2, journal.undoDepth)
    }

    @Test
    fun `a new edit clears the redo branch`() {
        val journal = UndoJournal(MemoryStorage())
        journal.record("A")
        journal.undo("B")
        assertTrue(journal.canRedo)
        journal.record("B2")
        assertFalse(journal.canRedo)
    }

    @Test
    fun `history survives a process restart`() {
        val storage = MemoryStorage()
        val first = UndoJournal(storage)
        first.record("A")
        first.record("B")

        val second = UndoJournal(storage)
        assertTrue(second.load())
        assertEquals(2, second.undoDepth)
        assertEquals("B", second.undo("C"))
    }

    @Test
    fun `history is bounded`() {
        val journal = UndoJournal(MemoryStorage(), maxEntries = 3)
        repeat(10) { index -> journal.record("state$index") }
        assertEquals(3, journal.undoDepth)
        assertEquals("state9", journal.undo("current"))
    }

    @Test
    fun `corrupt journal is ignored instead of crashing`() {
        val storage = MemoryStorage()
        storage.text = "{not json"
        val journal = UndoJournal(storage)
        assertFalse(journal.load())
        assertFalse(journal.canUndo)
    }

    @Test
    fun `clear can drop the persisted file`() {
        val storage = MemoryStorage()
        val journal = UndoJournal(storage)
        journal.record("A")
        journal.clear(deleteFile = true)
        assertNull(storage.text)
        assertFalse(journal.canUndo)
    }

    @Test
    fun `file storage writes atomically and reads back`() {
        val directory = File(System.getProperty("java.io.tmpdir"), "catbyte-journal-${System.nanoTime()}")
        val file = File(directory, "undo.json")
        val storage = FileJournalStorage(file)
        storage.write("first")
        assertEquals("first", storage.read())
        storage.write("second")
        assertEquals("second", storage.read())
        assertFalse(File(directory, "undo.json.tmp").exists())
        storage.clear()
        assertFalse(file.exists())
    }
}
