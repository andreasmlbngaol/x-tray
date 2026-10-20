package id.andreasmlbngaol.xtray.platform.internal

import java.io.File
import java.util.concurrent.TimeUnit

object Shell {
    fun run(vararg command: String, timeoutMs: Long = 2_000L): String? {
        return try {
            val process = ProcessBuilder(*command)
                .redirectErrorStream(true)
                .start()
            if (!process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)) {
                process.destroy()
                Log.w("timeout: " + command.joinToString(" "))
                return null
            }
            val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
            if (process.exitValue() != 0) {
                Log.d("exit " + process.exitValue() + ": " + command.joinToString(" "))
                null
            } else {
                output
            }
        } catch (error: Throwable) {
            Log.w("failed: " + command.joinToString(" "), error)
            null
        }
    }

    fun exists(binary: String): Boolean {
        if (Os.isWindows) return existsOnWindows(binary)
        val path = System.getenv("PATH") ?: return false
        return path.split(File.pathSeparatorChar).any { File(it, binary).canExecute() }
    }

    private fun existsOnWindows(binary: String): Boolean {
        val path = System.getenv("PATH") ?: return false
        val extensions = (System.getenv("PATHEXT") ?: ".EXE;.BAT;.CMD")
            .split(';')
            .filter { it.isNotBlank() }
            .map { it.trim().lowercase() }
        val hasExtension = extensions.any { binary.lowercase().endsWith(it) }
        return path.split(File.pathSeparatorChar).any { dir ->
            val candidate = File(dir, binary)
            if (hasExtension) {
                candidate.canExecute()
            } else {
                extensions.any { extension -> File(dir, binary + extension).canExecute() }
            }
        }
    }
}
