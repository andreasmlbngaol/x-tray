package id.andreasmlbngaol.xtray.platform.ipc

import id.andreasmlbngaol.xtray.core.ipc.IpcCommand
import id.andreasmlbngaol.xtray.core.ipc.decodeIpcCommand
import id.andreasmlbngaol.xtray.platform.internal.AppPaths
import id.andreasmlbngaol.xtray.platform.internal.Log
import id.andreasmlbngaol.xtray.platform.internal.Os
import java.net.InetAddress
import java.net.ServerSocket
import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.nio.file.Files

/**
 * One-line request/response protocol. Linux uses a Unix socket next to the
 * runtime dir; Windows binds a loopback port and publishes it in the config
 * dir, because AF_UNIX is not available to every JVM on Windows.
 */
class IpcServer(
    private val handler: (IpcCommand) -> String,
) {
    private var unixServer: ServerSocketChannel? = null
    private var tcpServer: ServerSocket? = null
    private var thread: Thread? = null
    @Volatile private var running = false

    fun start(): Boolean = if (Os.isWindows) startTcp() else startUnix()

    fun stop() {
        running = false
        runCatching { unixServer?.close() }
        runCatching { tcpServer?.close() }
        unixServer = null
        tcpServer = null
        if (Os.isWindows) {
            runCatching { Files.deleteIfExists(AppPaths.ipcPort) }
        } else {
            runCatching { Files.deleteIfExists(AppPaths.socket) }
        }
    }

    private fun startUnix(): Boolean = runCatching {
        val path = AppPaths.socket
        Files.createDirectories(path.parent)
        Files.deleteIfExists(path)
        val channel = ServerSocketChannel.open(StandardProtocolFamily.UNIX)
        channel.bind(UnixDomainSocketAddress.of(path))
        unixServer = channel
        running = true
        thread = Thread({ acceptUnix(channel) }, "x-tray-ipc").apply {
            isDaemon = true
            start()
        }
        Log.i("IPC listening on " + path)
        true
    }.onFailure { Log.w("cannot start IPC", it) }.getOrDefault(false)

    private fun startTcp(): Boolean = runCatching {
        Files.createDirectories(AppPaths.configDir)
        val socket = ServerSocket(0, 4, InetAddress.getLoopbackAddress())
        Files.writeString(AppPaths.ipcPort, socket.localPort.toString())
        tcpServer = socket
        running = true
        thread = Thread({ acceptTcp(socket) }, "x-tray-ipc").apply {
            isDaemon = true
            start()
        }
        Log.i("IPC listening on 127.0.0.1:" + socket.localPort)
        true
    }.onFailure { Log.w("cannot start IPC", it) }.getOrDefault(false)

    private fun acceptUnix(channel: ServerSocketChannel) {
        while (running) {
            val client = runCatching { channel.accept() }.getOrNull() ?: continue
            runCatching { handleUnix(client) }.onFailure { Log.w("IPC client failed", it) }
        }
    }

    private fun acceptTcp(socket: ServerSocket) {
        while (running) {
            val client = runCatching { socket.accept() }.getOrNull() ?: continue
            runCatching { handleTcp(client) }.onFailure { Log.w("IPC client failed", it) }
        }
    }

    private fun handleUnix(client: SocketChannel) {
        client.use { socket ->
            val buffer = ByteBuffer.allocate(4096)
            val read = socket.read(buffer)
            if (read <= 0) return
            val reply = reply(String(buffer.array(), 0, read))
            socket.write(ByteBuffer.wrap((reply + "\n").toByteArray()))
        }
    }

    private fun handleTcp(client: java.net.Socket) {
        client.use { socket ->
            socket.soTimeout = 2_000
            val line = socket.getInputStream().bufferedReader().readLine() ?: return
            socket.getOutputStream().bufferedWriter().apply {
                write(reply(line) + "\n")
                flush()
            }
        }
    }

    private fun reply(line: String): String {
        val command = decodeIpcCommand(line.trim())
        return if (command == null) "error unknown command" else handler(command)
    }
}
