package id.andreasmlbngaol.islanddesk.platform.instance

import id.andreasmlbngaol.islanddesk.platform.internal.Log
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * Advisory file lock that guarantees a single daemon. The second process
 * fails to acquire and can forward its command over IPC instead.
 */
class SingleInstance(private val path: Path) {
    private var channel: FileChannel? = null
    private var lock: FileLock? = null

    fun tryAcquire(): Boolean {
        return runCatching {
            Files.createDirectories(path.parent)
            val opened = FileChannel.open(
                path,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
            )
            val acquired = opened.tryLock()
            if (acquired == null) {
                opened.close()
                false
            } else {
                channel = opened
                lock = acquired
                true
            }
        }.onFailure { Log.w("cannot acquire instance lock", it) }.getOrDefault(false)
    }

    fun release() {
        runCatching { lock?.release() }
        runCatching { channel?.close() }
        lock = null
        channel = null
    }
}
