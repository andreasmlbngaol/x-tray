package id.andreasmlbngaol.xtray.platform.network

import java.io.File

/** Aggregate throughput across physical interfaces, sampled from sysfs. */
data class NetworkSpeed(
    val downBytesPerSecond: Long = 0L,
    val upBytesPerSecond: Long = 0L,
) {
    val isIdle: Boolean get() = downBytesPerSecond < 1024L && upBytesPerSecond < 1024L
}

interface NetworkSpeedSource {
    fun sample(): NetworkSpeed
}

class SysfsNetworkSpeedSource : NetworkSpeedSource {
    private var lastRx = -1L
    private var lastTx = -1L
    private var lastAt = 0L

    override fun sample(): NetworkSpeed {
        val rx = total("rx_bytes")
        val tx = total("tx_bytes")
        val now = System.currentTimeMillis()
        val elapsed = now - lastAt
        val speed = if (lastRx < 0L || elapsed <= 0L) {
            NetworkSpeed()
        } else {
            NetworkSpeed(
                downBytesPerSecond = ((rx - lastRx).coerceAtLeast(0L) * 1_000L) / elapsed,
                upBytesPerSecond = ((tx - lastTx).coerceAtLeast(0L) * 1_000L) / elapsed,
            )
        }
        lastRx = rx
        lastTx = tx
        lastAt = now
        return speed
    }

    private fun total(stat: String): Long {
        val root = File("/sys/class/net")
        val interfaces = root.listFiles() ?: return 0L
        return interfaces
            .filter { it.name != "lo" }
            .sumOf { iface ->
                runCatching {
                    File(iface, "statistics/" + stat).readText().trim().toLong()
                }.getOrDefault(0L)
            }
    }
}
