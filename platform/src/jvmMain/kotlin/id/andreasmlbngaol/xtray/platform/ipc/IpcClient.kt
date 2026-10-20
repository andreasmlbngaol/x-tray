package id.andreasmlbngaol.xtray.platform.ipc

import id.andreasmlbngaol.xtray.core.ipc.IpcCommand
import id.andreasmlbngaol.xtray.core.ipc.encode
import id.andreasmlbngaol.xtray.platform.internal.AppPaths
import id.andreasmlbngaol.xtray.platform.internal.Os
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.nio.file.Files

/**
 * Sends one command and returns the daemon reply, or null when no instance is
 * listening. Uses a short deadline so the CLI never hangs a script.
 */
fun sendIpc(command: IpcCommand, timeoutMs: Long = 800L): String? =
    if (Os.isWindows) sendTcp(command, timeoutMs) else sendUnix(command, timeoutMs)

private fun sendUnix(command: IpcCommand, timeoutMs: Long): String? {
    val path = AppPaths.socket
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

private fun sendTcp(command: IpcCommand, timeoutMs: Long): String? {
    val portFile = AppPaths.ipcPort
    if (!Files.exists(portFile)) return null
    val port = runCatching { Files.readString(portFile).trim().toInt() }.getOrNull() ?: return null
    return runCatching {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(InetAddress.getLoopbackAddress(), port), timeoutMs.toInt())
            socket.soTimeout = timeoutMs.toInt()
            socket.getOutputStream().bufferedWriter().apply {
                write(command.encode() + "\n")
                flush()
            }
            socket.getInputStream().bufferedReader().readLine()
        }
    }.getOrNull()
}
