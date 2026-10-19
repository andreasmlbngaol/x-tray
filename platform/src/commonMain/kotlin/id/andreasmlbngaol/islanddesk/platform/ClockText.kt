package id.andreasmlbngaol.islanddesk.platform

data class ClockText(
    val time: String,
    val date: String,
    val hours: String,
    val minutes: String,
    val seconds: String,
)

expect fun currentClockText(): ClockText
