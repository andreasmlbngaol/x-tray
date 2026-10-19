package id.andreasmlbngaol.islanddesk.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IslandStateMachineTest {

    @Test
    fun hoverIntentMovesCollapsedToPeek() {
        val machine = IslandStateMachine()
        machine.onPointerEnter(0)
        assertEquals(IslandState.Collapsed, machine.onTick(179))
        assertEquals(IslandState.Peek, machine.onTick(180))
    }

    @Test
    fun sustainedHoverStaysInPeek() {
        val machine = IslandStateMachine()
        machine.onPointerEnter(0)
        machine.onTick(180)
        assertEquals(IslandState.Peek, machine.state)
        assertEquals(IslandState.Peek, machine.onTick(60_000))
        assertEquals(IslandState.Peek, machine.onTick(600_000))
    }

    @Test
    fun exitFromPeekCollapsesAfterDelay() {
        val machine = IslandStateMachine()
        machine.onPointerEnter(0)
        machine.onTick(180)
        machine.onPointerExit(1_000)
        assertEquals(IslandState.Peek, machine.onTick(1_179))
        assertEquals(IslandState.Collapsed, machine.onTick(1_180))
    }

    @Test
    fun reEnterCancelsPendingCollapse() {
        val machine = IslandStateMachine()
        machine.onPointerEnter(0)
        machine.onTick(180)
        machine.onPointerExit(1_000)
        machine.onPointerEnter(1_100)
        assertEquals(IslandState.Peek, machine.onTick(1_400))
    }

    @Test
    fun clickExpandsImmediately() {
        val machine = IslandStateMachine()
        machine.onPointerEnter(0)
        assertEquals(IslandState.Expanded, machine.onActivate())
    }

    @Test
    fun expandedIgnoresPointerExit() {
        val machine = IslandStateMachine()
        machine.onActivate()
        machine.onPointerExit(1_000)
        assertEquals(IslandState.Expanded, machine.onTick(10_000))
        assertFalse(machine.isTransitionPending(10_000))
    }

    @Test
    fun escapeCollapsesAndSuppressesWhilePointerStaysInside() {
        val machine = IslandStateMachine()
        machine.onPointerEnter(0)
        machine.onActivate()
        machine.onDismiss()
        assertEquals(IslandState.Collapsed, machine.state)

        machine.onTick(10_000)
        assertEquals(IslandState.Collapsed, machine.state)

        machine.onPointerExit(10_100)
        machine.onPointerEnter(10_200)
        machine.onTick(10_379)
        assertEquals(IslandState.Collapsed, machine.state)
        assertEquals(IslandState.Peek, machine.onTick(10_380))
    }

    @Test
    fun tickWithoutPointerStaysCollapsed() {
        val machine = IslandStateMachine()
        machine.onTick(0)
        machine.onTick(10_000)
        assertEquals(IslandState.Collapsed, machine.state)
    }

    @Test
    fun transitionPendingReflectsTimers() {
        val machine = IslandStateMachine()
        assertFalse(machine.isTransitionPending(0))
        machine.onPointerEnter(0)
        assertTrue(machine.isTransitionPending(0))
        machine.onTick(180)
        assertFalse(machine.isTransitionPending(180))

        machine.onPointerExit(500)
        assertTrue(machine.isTransitionPending(500))
        machine.onTick(680)
        assertFalse(machine.isTransitionPending(680))
    }
}
