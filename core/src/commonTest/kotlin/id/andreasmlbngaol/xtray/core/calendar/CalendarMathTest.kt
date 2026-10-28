package id.andreasmlbngaol.xtray.core.calendar

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CalendarMathTest {
    @Test
    fun leapYearsFollowTheGregorianRule() {
        assertTrue(CalendarMath.isLeapYear(2024))
        assertFalse(CalendarMath.isLeapYear(2100))
        assertTrue(CalendarMath.isLeapYear(2000))
    }

    @Test
    fun monthLengths() {
        assertEquals(29, CalendarMath.daysInMonth(2024, 2))
        assertEquals(28, CalendarMath.daysInMonth(2025, 2))
        assertEquals(31, CalendarMath.daysInMonth(2026, 10))
        assertEquals(30, CalendarMath.daysInMonth(2026, 11))
    }

    @Test
    fun weekdayIsMondayFirst() {
        // 2026-10-28 is a Wednesday: index 2 in a Monday-first week.
        assertEquals(2, CalendarMath.weekdayIndex(2026, 10, 28))
        assertEquals(3, CalendarMath.weekdayIndex(2026, 10, 1))
        assertEquals(6, CalendarMath.weekdayIndex(2026, 10, 4))
    }

    @Test
    fun gridPadsTheMonthOnBothSides() {
        val month = CalendarMath.month(2026, 10)
        assertEquals(0, month.cells.size % 7)
        assertEquals(listOf(28, 29, 30), month.cells.take(3).map { it.dayOfMonth })
        assertTrue(month.cells.take(3).none { it.inMonth })
        assertEquals(1, month.cells[3].dayOfMonth)
        assertTrue(month.cells[3].inMonth)
        assertEquals(35, month.cells.size)
        assertFalse(month.cells.last().inMonth)
    }

    @Test
    fun shiftCarriesAcrossYears() {
        assertEquals(2026 to 12, CalendarMath.shift(2026, 11, 1))
        assertEquals(2027 to 1, CalendarMath.shift(2026, 12, 1))
        assertEquals(2025 to 12, CalendarMath.shift(2026, 1, -1))
    }

    @Test
    fun monthLabelUsesTheMonthName() {
        assertEquals("October 2026", CalendarMath.monthLabel(2026, 10))
    }
}
