package id.andreasmlbngaol.xtray.core.shelf

data class ShelfItem(
    val path: String,
    val name: String,
    val addedAtMs: Long,
)

/** Files dropped on the island, kept in memory for the current session. */
class ShelfStore(private val maxItems: Int = 12) {
    private val items = mutableListOf<ShelfItem>()

    fun items(): List<ShelfItem> = items.toList()

    fun add(paths: List<String>, nowMs: Long): Int {
        var added = 0
        for (path in paths) {
            if (path.isBlank()) continue
            if (items.any { it.path == path }) continue
            items.add(0, ShelfItem(path = path, name = path.substringAfterLast('/'), addedAtMs = nowMs))
            added++
        }
        while (items.size > maxItems.coerceAtLeast(1)) {
            items.removeAt(items.lastIndex)
        }
        return added
    }

    fun remove(path: String) {
        items.removeAll { it.path == path }
    }

    fun clear() = items.clear()
}
