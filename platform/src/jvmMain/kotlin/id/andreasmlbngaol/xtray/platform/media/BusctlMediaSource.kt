package id.andreasmlbngaol.xtray.platform.media

import id.andreasmlbngaol.xtray.platform.internal.Log
import id.andreasmlbngaol.xtray.platform.internal.Shell
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * MPRIS access through `busctl` (systemd), which emits JSON and avoids a
 * D-Bus library dependency. Every call is best-effort: a missing player or a
 * player without metadata simply yields null.
 */
class BusctlMediaSource : MediaSource {
    private var lastPlayer: String? = null

    override fun snapshot(): MediaTrack? {
        val players = playerNames()
        if (players.isEmpty()) return null

        val tracks = players.mapNotNull { player ->
            runCatching { readPlayer(player) }.getOrElse {
                Log.d("mpris read failed for " + player + ": " + it.message)
                null
            }
        }
        val chosen = tracks.firstOrNull { it.playing } ?: tracks.firstOrNull() ?: return null
        lastPlayer = chosen.busName
        return chosen
    }

    override fun playPause() = callPlayer("PlayPause")
    override fun next() = callPlayer("Next")
    override fun previous() = callPlayer("Previous")

    private fun callPlayer(method: String) {
        // The first press can arrive before any snapshot has run (peek opens
        // faster than the 1.5 s poll), so resolve the player on demand.
        val player = lastPlayer ?: snapshot()?.busName ?: return
        Shell.run(
            "busctl", "--user", "call", player,
            "/org/mpris/MediaPlayer2", "org.mpris.MediaPlayer2.Player", method,
        )
    }

    private fun playerNames(): List<String> {
        val output = Shell.run("busctl", "--user", "--json=short", "list") ?: return emptyList()
        return parsePlayerNames(output)
    }

    private fun readPlayer(player: String): MediaTrack? {
        val output = Shell.run(
            "busctl", "--user", "--json=short", "call", player,
            "/org/mpris/MediaPlayer2", "org.freedesktop.DBus.Properties",
            "GetAll", "s", "org.mpris.MediaPlayer2.Player",
        ) ?: return null
        return parseMediaTrack(output, displayName(player), player)
    }

    private fun displayName(player: String): String {
        val raw = player.removePrefix("org.mpris.MediaPlayer2.").substringBefore(".instance")
        return raw.replaceFirstChar { it.uppercaseChar() }
    }
}

internal fun parsePlayerNames(output: String): List<String> {
    val array = runCatching {
        Json.parseToJsonElement(output).jsonArray
    }.getOrNull() ?: return emptyList()
    return array.mapNotNull { element ->
        val name = element.jsonObject["name"]?.jsonPrimitive?.content ?: return@mapNotNull null
        name.takeIf { it.startsWith("org.mpris.MediaPlayer2.") && !it.endsWith(".playerctld") }
    }
}

internal fun parseMediaTrack(
    output: String,
    playerName: String,
    busName: String = playerName,
): MediaTrack? {
    val root = runCatching { Json.parseToJsonElement(output).jsonObject }.getOrNull() ?: return null
    val props = root["data"]?.jsonArray?.firstOrNull()?.jsonObject ?: return null

    fun value(name: String): JsonElement? = props[name]?.jsonObject?.get("data")

    val metadata = value("Metadata")?.jsonObject
    fun meta(name: String): JsonElement? = metadata?.get(name)?.jsonObject?.get("data")

    val title = meta("xesam:title")?.jsonPrimitive?.content?.trim().orEmpty()
    if (title.isBlank()) return null

    val artist = meta("xesam:artist")?.let { element ->
        runCatching { element.jsonArray.joinToString(", ") { it.jsonPrimitive.content } }.getOrNull()
    } ?: meta("xesam:albumArtist")?.jsonPrimitive?.content.orEmpty()

    return MediaTrack(
        player = playerName,
        busName = busName,
        title = title,
        artist = artist,
        album = meta("xesam:album")?.jsonPrimitive?.content.orEmpty(),
        artUrl = meta("mpris:artUrl")?.jsonPrimitive?.content,
        playing = value("PlaybackStatus")?.jsonPrimitive?.content == "Playing",
        positionMs = (value("Position")?.jsonPrimitive?.longOrNull ?: 0L) / 1_000L,
        lengthMs = (meta("mpris:length")?.jsonPrimitive?.longOrNull ?: 0L) / 1_000L,
    )
}
