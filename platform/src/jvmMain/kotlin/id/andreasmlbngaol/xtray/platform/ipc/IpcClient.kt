package id.andreasmlbngaol.xtray.platform.ipc

import id.andreasmlbngaol.xtray.core.ipc.IpcCommand
import id.andreasmlbngaol.xtray.core.ipc.encode
import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.nio.file.Path

/**
 * Sends one command and returns the daemon reply, or null when no instance is
 * listening. Uses a short deadline so the CLI never hangs a script.
 */
fun sendIpc(path: Path, command: IpcCommand, timeoutMs: Long = 800L): String? {
    if (!path.toFile().exists()) return null
    return runCatching {
        SocketChannel.open(StandardProtocolFamily.UNIX).use { channel ->
            channel.connect(UnixDomainSocketAddress.of(path))
            channel.write(ByteBuffer.wrap((command.encode() + "\n").toByteArray()))

            val buffer = ByteBuffer.allocate(4096)
            val deadline = System.currentTimeMillis() + timeoutMs
            channel.configureBlocking(false)
            while (System.currentTimeMillis() < deadline) {
                val read = channel.read(buffer)
                if (read > 0) {
                    return@use String(buffer.array(), 0, read).trim()
                }
                if (read < 0) break
                Thread.sleep(10L)
            }
            null
        }
    }.getOrNull()
}
