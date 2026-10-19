package id.andreasmlbngaol.islanddesk.platform

data class ClockText(
    val time: String,
    val date: String,
)

expect fun currentClockText(): ClockText
