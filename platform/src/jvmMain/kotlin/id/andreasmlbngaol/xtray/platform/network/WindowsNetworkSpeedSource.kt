package id.andreasmlbngaol.xtray.platform.network

import com.sun.jna.Memory
import com.sun.jna.ptr.IntByReference
import id.andreasmlbngaol.xtray.platform.internal.Log
import id.andreasmlbngaol.xtray.platform.system.MIB_IFROW
import id.andreasmlbngaol.xtray.platform.system.Win32Api

/**
 * Aggregate throughput from `GetIfTable`. The per-interface octet counters
 * are 32-bit, so a wrap is detected per adapter instead of on the total.
 */
class WindowsNetworkSpeedSource : NetworkSpeedSource {
    private val previous = HashMap<Int, Counters>()
    private var lastAt = 0L

    override fun sample(): NetworkSpeed {
        val rows = readRows()
        val now = System.currentTimeMillis()
        val elapsed = now - lastAt

        var down = 0L
        var up = 0L
        if (elapsed > 0L) {
            for (row in rows) {
                val before = previous[row.index]
                if (before != null) {
                    down += wrapDelta(before.inOctets, row.inOctets)
                    up += wrapDelta(before.outOctets, row.outOctets)
                }
            }
        }

        previous.clear()
        rows.forEach { previous[it.index] = Counters(it.inOctets, it.outOctets) }
        lastAt = now

        if (elapsed <= 0L) return NetworkSpeed()
        return NetworkSpeed(
            downBytesPerSecond = down * 1_000L / elapsed,
            upBytesPerSecond = up * 1_000L / elapsed,
        )
    }

    private fun wrapDelta(before: Long, now: Long): Long =
        if (now >= before) now - before else now + (1L shl 32) - before

    private fun readRows(): List<Row> {
        val api = Win32Api.iphlpapi ?: return emptyList()
        val size = IntByReference(0)
        api.GetIfTable(null, size, 0)
        if (size.value <= 0) return emptyList()

        val memory = Memory(size.value.toLong())
        if (api.GetIfTable(memory, size, 0) != 0) return emptyList()

        val count = memory.getInt(0L)
        if (count <= 0) return emptyList()

        val layout = MIB_IFROW()
        val rowSize = layout.size()
        val indexOffset = layout.offsetOf("index").toLong()
        val typeOffset = layout.offsetOf("type").toLong()
        val inOffset = layout.offsetOf("inOctets").toLong()
        val outOffset = layout.offsetOf("outOctets").toLong()

        val rows = ArrayList<Row>(count)
        for (position in 0 until count) {
            // MIB_IFTABLE is a DWORD count followed by the rows.
            val start = 4L + position.toLong() * rowSize
            if (start + rowSize > memory.size()) break
            val type = memory.getInt(start + typeOffset)
            if (type == IF_TYPE_SOFTWARE_LOOPBACK) continue
            rows.add(
                Row(
                    index = memory.getInt(start + indexOffset),
                    inOctets = memory.getInt(start + inOffset).toLong() and 0xFFFFFFFFL,
                    outOctets = memory.getInt(start + outOffset).toLong() and 0xFFFFFFFFL,
                ),
            )
        }
        Log.d("network rows=" + rows.size + " table=" + count)
        return rows
    }

    private data class Row(val index: Int, val inOctets: Long, val outOctets: Long)
    private data class Counters(val inOctets: Long, val outOctets: Long)

    private companion object {
        const val IF_TYPE_SOFTWARE_LOOPBACK = 24
    }
}
