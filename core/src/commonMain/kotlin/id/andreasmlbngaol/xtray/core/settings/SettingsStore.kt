package id.andreasmlbngaol.xtray.core.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.json.Json

/** Storage boundary so the core stays free of file APIs. */
interface SettingsPersistence {
    fun load(): String?
    fun save(json: String)
}

/**
 * Observable settings holder. Writes are synchronous and cheap: the file is a
 * few hundred bytes and changes happen on explicit user actions only.
 */
class SettingsStore(
    private val persistence: SettingsPersistence,
    initial: IslandSettings = IslandSettings(),
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    var current: IslandSettings by mutableStateOf(read(initial))
        private set

    private fun read(fallback: IslandSettings): IslandSettings {
        val raw = runCatching { persistence.load() }.getOrNull() ?: return fallback
        return runCatching { json.decodeFromString<IslandSettings>(raw).normalized() }
            .getOrElse { fallback }
    }

    fun update(transform: (IslandSettings) -> IslandSettings) {
        val next = transform(current).normalized()
        if (next == current) return
        current = next
        runCatching { persistence.save(json.encodeToString(IslandSettings.serializer(), next)) }
    }

    fun reload() {
        current = read(current)
    }
}
