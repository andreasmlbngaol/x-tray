package id.andreasmlbngaol.xtray.core.clipboard

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClipboardHistoryTest {
    @Test
    fun newestEntryComesFirstAndDuplicatesMoveToTop() {
        val history = ClipboardHistory(maxItems = 3)
        assertTrue(history.add("one", 1))
        assertTrue(history.add("two", 2))
        assertTrue(history.add("one", 3))

        assertEquals(listOf("one", "two"), history.items().map { it.text })
        assertEquals(3L, history.items().first().capturedAtMs)
    }

    @Test
    fun blankEntriesAreIgnored() {
        val history = ClipboardHistory()
        assertFalse(history.add("   ", 1))
        assertTrue(history.items().isEmpty())
    }

    @Test
    fun historyIsBounded() {
        val history = ClipboardHistory(maxItems = 2)
        history.add("a", 1)
        history.add("b", 2)
        history.add("c", 3)
        assertEquals(listOf("c", "b"), history.items().map { it.text })
    }

    @Test
    fun previewIsSingleLineAndShortened() {
        val preview = ClipboardHistory.preview("line one\nline two", limit = 12)
        assertFalse(preview.contains('\n'))
        assertTrue(preview.endsWith("…"))
    }
}
