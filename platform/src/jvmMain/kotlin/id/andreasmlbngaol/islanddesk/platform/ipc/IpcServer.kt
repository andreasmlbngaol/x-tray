package id.andreasmlbngaol.islanddesk.platform.ipc

import id.andreasmlbngaol.islanddesk.core.ipc.IpcCommand
import id.andreasmlbngaol.islanddesk.core.ipc.decodeIpcCommand
import id.andreasmlbngaol.islanddesk.platform.internal.Log
import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.nio.file.Files
import java.nio.file.Path

/**
 * One-line request/response protocol over a Unix socket. Kept deliberately
 * small so the CLI can be used from scripts and keybindings.
 */
class IpcServer(
    private val path: Path,
    private val handler: (IpcCommand) -> String,
) {
    private var server: ServerSocketChannel? = null
    private var thread: Thread? = null
    @Volatile private var running = false

    fun start(): Boolean {
        return runCatching {
            Files.createDirectories(path.parent)
            Files.deleteIfExists(path)
            val channel = ServerSocketChannel.open(StandardProtocolFamily.UNIX)
            channel.bind(UnixDomainSocketAddress.of(path))
            server = channel
            running = true
            thread = Thread({ acceptLoop(channel) }, "islanddesk-ipc").apply {
                isDaemon = true
                start()
            }
            Log.i("IPC listening on " + path)
            true
        }.onFailure { Log.w("cannot start IPC", it) }.getOrDefault(false)
    }

    fun stop() {
        running = false
        runCatching { server?.close() }
        server = null
        runCatching { Files.deleteIfExists(path) }
    }

    private fun acceptLoop(channel: ServerSocketChannel) {
        while (running) {
            val client = runCatching { channel.accept() }.getOrNull() ?: continue
            runCatching { handle(client) }.onFailure { Log.w("IPC client failed", it) }
        }
    }

    private fun handle(client: SocketChannel) {
        client.use { socket ->
            val buffer = ByteBuffer.allocate(4096)
            val read = socket.read(buffer)
            if (read <= 0) return
            val line = String(buffer.array(), 0, read).trim()
            val command = decodeIpcCommand(line)
            val reply = if (command == null) "error unknown command" else handler(command)
            socket.write(ByteBuffer.wrap((reply + "\n").toByteArray()))
        }
    }
}
