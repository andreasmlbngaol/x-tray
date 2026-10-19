package id.andreasmlbngaol.xtray.platform.media

data class MediaTrack(
    val player: String,
    val busName: String = player,
    val title: String,
    val artist: String,
    val album: String,
    val artUrl: String?,
    val playing: Boolean,
    val positionMs: Long,
    val lengthMs: Long,
) {
    val hasProgress: Boolean get() = lengthMs > 0L
    val progress: Float
        get() = if (lengthMs <= 0L) 0f else (positionMs.toFloat() / lengthMs).coerceIn(0f, 1f)
}

/**
 * Read/write access to the session media player. Implementations must be safe
 * to call from a background thread and should degrade to null when no player
 * or no MPRIS support is available.
 */
interface MediaSource {
    fun snapshot(): MediaTrack?
    fun playPause()
    fun next()
    fun previous()
}
