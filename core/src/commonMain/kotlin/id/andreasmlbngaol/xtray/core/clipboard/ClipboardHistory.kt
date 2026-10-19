package id.andreasmlbngaol.xtray.core.clipboard

data class ClipboardEntry(
    val text: String,
    val capturedAtMs: Long,
)

/**
 * In-memory clipboard ring. Nothing is written to disk unless the user
 * explicitly enables persistence, which is why this class has no storage
 * dependency.
 */
class ClipboardHistory(private val maxItems: Int = 10) {
    private val entries = mutableListOf<ClipboardEntry>()

    fun items(): List<ClipboardEntry> = entries.toList()

    /** Returns true when the history changed. */
    fun add(text: String, nowMs: Long): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return false
        if (entries.firstOrNull()?.text == trimmed) return false
        entries.removeAll { it.text == trimmed }
        entries.add(0, ClipboardEntry(trimmed, nowMs))
        while (entries.size > maxItems.coerceAtLeast(1)) {
            entries.removeAt(entries.lastIndex)
        }
        return true
    }

    fun remove(text: String) {
        entries.removeAll { it.text == text }
    }

    fun clear() = entries.clear()

    companion object {
        /** Shortens long entries for display without altering the stored text. */
        fun preview(text: String, limit: Int = 140): String {
            val singleLine = text.replace('\n', ' ').replace('\r', ' ').trim()
            return if (singleLine.length <= limit) singleLine else singleLine.take(limit - 1) + "…"
        }
    }
}
