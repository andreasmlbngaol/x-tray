package id.andreasmlbngaol.xtray.platform.internal

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object Log {
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
    private const val MAX_BYTES = 1_000_000L
    private val lock = Any()
    private var initialized = false

    var verbose: Boolean = System.getenv("XTRAY_DEBUG") == "1"

    fun i(message: String) {
        write("INFO", message)
    }

    fun d(message: String) {
        if (verbose) write("DEBUG", message)
    }

    fun w(message: String, error: Throwable? = null) {
        write("WARN", message)
        if (error != null && verbose) error.printStackTrace()
    }

    private fun write(level: String, message: String) {
        val line = "[" + LocalTime.now().format(timeFormat) + "] " + level + " " + message
        println(line)
        append(line)
    }

    private fun append(line: String) {
        synchronized(lock) {
            runCatching {
                val path = AppPaths.logFile
                Files.createDirectories(path.parent)
                if (!initialized) {
                    initialized = true
                    if (Files.exists(path) && Files.size(path) > MAX_BYTES) {
                        Files.writeString(path, "")
                    }
                }
                Files.writeString(
                    path,
                    line + System.lineSeparator(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND,
                )
            }
        }
    }
}
