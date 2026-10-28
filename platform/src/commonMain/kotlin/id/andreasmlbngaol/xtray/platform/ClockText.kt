package id.andreasmlbngaol.xtray.platform

data class ClockText(
    val time: String,
    val date: String,
    val hours: String,
    val minutes: String,
    val seconds: String,
    val year: Int,
    val month: Int,
    val dayOfMonth: Int,
)

expect fun currentClockText(): ClockText

/** Wall-clock label (HH:mm) for an absolute instant, used for focus end times. */
expect fun formatClockTime(epochMillis: Long): String
