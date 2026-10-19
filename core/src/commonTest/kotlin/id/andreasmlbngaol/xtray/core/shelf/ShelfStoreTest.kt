package id.andreasmlbngaol.xtray.core.shelf

import kotlin.test.Test
import kotlin.test.assertEquals

class ShelfStoreTest {
    @Test
    fun addsFilesNewestFirstAndSkipsDuplicates() {
        val shelf = ShelfStore(maxItems = 3)
        assertEquals(2, shelf.add(listOf("/tmp/a.txt", "/tmp/b.txt"), 1))
        assertEquals(0, shelf.add(listOf("/tmp/a.txt"), 2))
        assertEquals(listOf("b.txt", "a.txt"), shelf.items().map { it.name })
    }

    @Test
    fun oldestItemsFallOff() {
        val shelf = ShelfStore(maxItems = 2)
        shelf.add(listOf("/a", "/b", "/c"), 1)
        assertEquals(listOf("c", "b"), shelf.items().map { it.name })
    }

    @Test
    fun removeAndClear() {
        val shelf = ShelfStore()
        shelf.add(listOf("/a", "/b"), 1)
        shelf.remove("/a")
        assertEquals(listOf("/b"), shelf.items().map { it.path })
        shelf.clear()
        assertEquals(0, shelf.items().size)
    }
}
