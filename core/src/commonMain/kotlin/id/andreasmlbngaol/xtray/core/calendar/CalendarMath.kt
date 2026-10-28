package id.andreasmlbngaol.xtray.core.calendar

/** One day in a month grid. [inMonth] is false for the spill from the neighbours. */
data class CalendarCell(
    val dayOfMonth: Int,
    val inMonth: Boolean,
)

/** A whole month laid out in weeks that start on Monday. */
data class CalendarMonth(
    val year: Int,
    val month: Int,
    val cells: List<CalendarCell>,
) {
    val weeks: List<List<CalendarCell>> get() = cells.chunked(7)
}

/**
 * Gregorian month maths for the calendar tab. Kept in core so the layout can be
 * tested without a platform clock: callers pass the year and month they want.
 */
object CalendarMath {
    val monthNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December",
    )

    /** Monday-first, matching [weekdayIndex]. */
    val weekdayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    fun isLeapYear(year: Int): Boolean =
        year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)

    fun daysInMonth(year: Int, month: Int): Int = when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (isLeapYear(year)) 29 else 28
        else -> throw IllegalArgumentException("month out of range: " + month)
    }

    /** 0 for Monday through 6 for Sunday. */
    fun weekdayIndex(year: Int, month: Int, day: Int): Int {
        // Sakamoto's method returns 0 for Sunday; shift it to a Monday week.
        val offsets = intArrayOf(0, 3, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4)
        val y = if (month < 3) year - 1 else year
        val sundayZero = (y + y / 4 - y / 100 + y / 400 + offsets[month - 1] + day) % 7
        return (sundayZero + 6) % 7
    }

    fun monthLabel(year: Int, month: Int): String =
        monthNames[month - 1] + " " + year

    /** Moves [delta] months away from the given month, carrying the year. */
    fun shift(year: Int, month: Int, delta: Int): Pair<Int, Int> {
        val zeroBased = year * 12 + (month - 1) + delta
        return zeroBased / 12 to zeroBased % 12 + 1
    }

    /**
     * Fills the leading and trailing days of the month so every row has seven
     * cells and the weekday columns line up.
     */
    fun month(year: Int, month: Int): CalendarMonth {
        val leading = weekdayIndex(year, month, 1)
        val days = daysInMonth(year, month)
        val (previousYear, previousMonth) = shift(year, month, -1)
        val previousDays = daysInMonth(previousYear, previousMonth)
        val cells = buildList {
            for (back in leading downTo 1) {
                add(CalendarCell(dayOfMonth = previousDays - back + 1, inMonth = false))
            }
            for (day in 1..days) {
                add(CalendarCell(dayOfMonth = day, inMonth = true))
            }
            val rows = (leading + days + 6) / 7
            val trailing = rows * 7 - leading - days
            for (day in 1..trailing) {
                add(CalendarCell(dayOfMonth = day, inMonth = false))
            }
        }
        return CalendarMonth(year = year, month = month, cells = cells)
    }
}
