package id.andreasmlbngaol.xtray.core.notification

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NotificationCenterTest {
    @Test
    fun pushKeepsHistoryNewestFirst() {
        val center = NotificationCenter(clock = { 1_000L })
        center.push("Chat", "Hi", "There")
        center.push("Mail", "Subject", "Body")
        assertEquals(listOf("Mail", "Chat"), center.history.map { it.appName })
        assertEquals("Subject", center.current?.title)
    }

    @Test
    fun dismissingTheCardKeepsItInTheHistory() {
        val center = NotificationCenter(clock = { 1_000L })
        val notification = center.push("Chat", "Hi", "There")
        center.dismiss(notification.id)
        assertNull(center.current)
        assertEquals(1, center.history.size)
    }

    @Test
    fun historyIsBounded() {
        val center = NotificationCenter(clock = { 1_000L }, maxHistory = 3)
        repeat(5) { index -> center.push("App", "T" + index, "B" + index) }
        assertEquals(3, center.history.size)
        assertEquals("T4", center.history.first().title)
    }

    @Test
    fun groupsKeepOneSenderTogetherAndOrderByRecency() {
        var now = 0L
        val center = NotificationCenter(clock = { now })
        center.push("Chat", "a", "")
        now = 1L
        center.push("Mail", "b", "")
        now = 2L
        center.push("Chat", "c", "")

        val groups = center.grouped()
        assertEquals(listOf("Chat", "Mail"), groups.map { it.appName })
        assertEquals(listOf("c", "a"), groups.first().notifications.map { it.title })
        assertEquals("c", groups.first().latest.title)
    }

    @Test
    fun removeDropsOneMessageAndItsCard() {
        val center = NotificationCenter(clock = { 1_000L })
        val first = center.push("Chat", "a", "")
        val second = center.push("Chat", "b", "")

        center.remove(first.id)

        assertEquals(listOf(second.id), center.history.map { it.id })
        assertEquals(second.id, center.current?.id)

        center.remove(second.id)
        assertNull(center.current)
        assertEquals(0, center.history.size)
    }

    @Test
    fun removeKeepsACardThatIsNotTheRemovedMessage() {
        val center = NotificationCenter(clock = { 1_000L })
        val older = center.push("Chat", "a", "")
        center.push("Mail", "b", "")

        center.remove(older.id)

        assertEquals("b", center.current?.title)
        assertEquals(1, center.history.size)
    }

    @Test
    fun removeGroupDropsEveryMessageFromThatSender() {
        val center = NotificationCenter(clock = { 1_000L })
        center.push("Chat", "a", "")
        center.push("Mail", "b", "")
        center.push("Chat", "c", "")

        center.removeGroup("Chat")

        assertEquals(listOf("b"), center.history.map { it.title })
        // The card on screen was one of the removed messages.
        assertNull(center.current)
        assertEquals(listOf("Mail"), center.grouped().map { it.appName })
    }

    @Test
    fun clearAllDropsHistoryAndTheCard() {
        val center = NotificationCenter(clock = { 1_000L })
        center.push("Chat", "Hi", "There")
        center.clearAll()
        assertNull(center.current)
        assertEquals(0, center.history.size)
        assertEquals(0, center.grouped().size)
    }
}
