package id.andreasmlbngaol.xtray.platform.settings

import id.andreasmlbngaol.xtray.core.settings.SettingsPersistence
import id.andreasmlbngaol.xtray.platform.internal.AppPaths
import id.andreasmlbngaol.xtray.platform.internal.Log
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

class SettingsFile(private val path: Path = AppPaths.settings) : SettingsPersistence {
    override fun load(): String? {
        if (!Files.exists(path)) return null
        return runCatching { Files.readString(path) }
            .onFailure { Log.w("cannot read settings", it) }
            .getOrNull()
    }

    override fun save(json: String) {
        runCatching {
            Files.createDirectories(path.parent)
            val temp = path.resolveSibling(path.fileName.toString() + ".tmp")
            Files.writeString(temp, json)
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        }.onFailure { Log.w("cannot save settings", it) }
    }
}
