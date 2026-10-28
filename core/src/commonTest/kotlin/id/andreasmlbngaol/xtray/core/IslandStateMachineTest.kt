package id.andreasmlbngaol.xtray.core

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
    fun dragOpensPeekImmediatelyAndHoldsIt() {
        val machine = IslandStateMachine()
        assertEquals(IslandState.Peek, machine.onDragOver())
        assertEquals(IslandState.Peek, machine.onTick(10_000))

        // The resize under the pointer is reported as an exit; the drag must
        // keep peek open instead of collapsing and reopening.
        machine.onPointerExit(10_100)
        assertEquals(IslandState.Peek, machine.onDragOver())
        assertFalse(machine.isTransitionPending(10_100))
    }

    @Test
    fun dragWhileExpandedKeepsExpanded() {
        val machine = IslandStateMachine()
        machine.onActivate()
        assertEquals(IslandState.Expanded, machine.onDragOver())
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
