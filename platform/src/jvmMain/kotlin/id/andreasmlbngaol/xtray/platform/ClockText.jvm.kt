package id.andreasmlbngaol.xtray.platform

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)
private val dateFormatter = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH)
private val clockTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)

actual fun formatClockTime(epochMillis: Long): String =
    java.time.Instant.ofEpochMilli(epochMillis)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalTime()
        .format(clockTimeFormatter)

actual fun currentClockText(): ClockText {
    val now = LocalDateTime.now()
    return ClockText(
        time = now.format(timeFormatter),
        date = now.format(dateFormatter),
        hours = now.format(DateTimeFormatter.ofPattern("HH")),
        minutes = now.format(DateTimeFormatter.ofPattern("mm")),
        seconds = now.format(DateTimeFormatter.ofPattern("ss")),
        year = now.year,
        month = now.monthValue,
        dayOfMonth = now.dayOfMonth,
    )
}
