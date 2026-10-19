package id.andreasmlbngaol.islanddesk.platform

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("id-ID"))
private val dateFormatter = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.forLanguageTag("id-ID"))

actual fun currentClockText(): ClockText {
    val now = LocalDateTime.now()
    return ClockText(
        time = now.format(timeFormatter),
        date = now.format(dateFormatter),
    )
}
