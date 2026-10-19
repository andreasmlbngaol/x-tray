package id.andreasmlbngaol.xtray.core.ipc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IpcCommandTest {
    @Test
    fun parsesSimpleCommands() {
        assertEquals(IpcCommand.Toggle, parseIpcCommand(emptyList()))
        assertEquals(IpcCommand.Toggle, parseIpcCommand(listOf("toggle")))
        assertEquals(IpcCommand.Expand, parseIpcCommand(listOf("open")))
        assertEquals(IpcCommand.Collapse, parseIpcCommand(listOf("close")))
        assertEquals(IpcCommand.State, parseIpcCommand(listOf("status")))
    }

    @Test
    fun parsesNotifyWithSpaces() {
        assertEquals(IpcCommand.Notify("build selesai"), parseIpcCommand(listOf("notify", "build", "selesai")))
    }

    @Test
    fun parsesActivityAndDnd() {
        assertEquals(IpcCommand.StartActivity("Focus", 25), parseIpcCommand(listOf("activity", "start")))
        assertEquals(
            IpcCommand.StartActivity("Tulis laporan", 45),
            parseIpcCommand(listOf("activity", "start", "45", "Tulis", "laporan")),
        )
        assertEquals(IpcCommand.StopActivity, parseIpcCommand(listOf("activity", "stop")))
        assertEquals(IpcCommand.SetDnd(true), parseIpcCommand(listOf("dnd", "on")))
        assertEquals(IpcCommand.SetDnd(false), parseIpcCommand(listOf("dnd", "off")))
    }

    @Test
    fun rejectsUnknownInput() {
        assertNull(parseIpcCommand(listOf("nonsense")))
        assertNull(parseIpcCommand(listOf("notify")))
        assertNull(parseIpcCommand(listOf("dnd", "maybe")))
    }

    @Test
    fun roundTripsThroughWireFormat() {
        val commands = listOf(
            IpcCommand.Toggle,
            IpcCommand.Notify("hello world"),
            IpcCommand.StartActivity("Focus", 30),
            IpcCommand.SetDnd(true),
        )
        for (command in commands) {
            assertEquals(command, decodeIpcCommand(command.encode()))
        }
    }
}
