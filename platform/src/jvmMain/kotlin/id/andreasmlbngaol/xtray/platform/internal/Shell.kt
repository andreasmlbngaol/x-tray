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

    /**
     * Starts a program and returns immediately. For launchers such as
     * gnome-control-center that keep running until the user closes them.
     */
    fun launch(vararg command: String): Boolean = try {
        ProcessBuilder(*command).start()
        true
    } catch (error: Throwable) {
        Log.w("failed to launch: " + command.joinToString(" "), error)
        false
    }

    fun exists(binary: String): Boolean {
        val path = System.getenv("PATH") ?: return false
        return path.split(File.pathSeparatorChar).any { File(it, binary).canExecute() }
    }
}
