package id.andreasmlbngaol.xtray.platform.system

import com.sun.jna.Function
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.ptr.FloatByReference
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.PointerByReference
import id.andreasmlbngaol.xtray.platform.internal.Log
import id.andreasmlbngaol.xtray.platform.window.Win32Window
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/** Resolves one COM vtable slot into a callable function pointer. */
private fun method(comObject: Pointer, index: Int): Function {
    val address = comObject.getPointer(0L).getPointer(index.toLong() * Native.POINTER_SIZE)
    // 32-bit COM is stdcall; the x64 ABI has a single convention.
    val convention = if (Native.POINTER_SIZE == 4) Function.ALT_CONVENTION else Function.C_CONVENTION
    return Function.getFunction(address, convention)
}

/**
 * Volume through Core Audio (IAudioEndpointVolume) and brightness through
 * Dxva2. Wi-Fi and Bluetooth have no unprivileged Windows API, so those
 * controls stay hidden.
 */
class WindowsSystemControls : SystemControls {
    private val audio = WindowsAudio()
    private val brightness = WindowsBrightness()

    override val hasBrightness: Boolean get() = brightness.available
    override val hasWifi: Boolean get() = false
    override val hasBluetooth: Boolean get() = false

    override fun snapshot(): SystemState = SystemState(
        volumePercent = audio.volume(),
        muted = audio.muted(),
        micMuted = audio.micMuted(),
        brightnessPercent = brightness.percent(),
    )

    override fun volumeStep(deltaPercent: Int) {
        val current = audio.volume() ?: return
        audio.setVolume(current + deltaPercent)
    }

    override fun setVolume(percent: Int) = audio.setVolume(percent)
    override fun toggleMute() = audio.toggleMute()
    override fun toggleMic() = audio.toggleMic()

    override fun brightnessStep(deltaPercent: Int) {
        val current = brightness.percent() ?: return
        brightness.set(current + deltaPercent)
    }

    override fun setBrightness(percent: Int) = brightness.set(percent)

    override fun toggleWifi() = Unit
    override fun toggleBluetooth() = Unit
}

/**
 * COM is apartment-bound, so every call runs on one dedicated thread that
 * initialises it once. The UI thread only waits a few hundred milliseconds.
 */
private class WindowsAudio {
    private val ole32 = Win32Api.ole32
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "x-tray-audio").apply { isDaemon = true }
    }

    private var initialized = false
    private var renderEndpoint: AudioEndpoint? = null
    private var captureEndpoint: AudioEndpoint? = null

    fun volume(): Int? = onAudio { endpoint(capture = false)?.volume() }

    fun muted(): Boolean? = onAudio { endpoint(capture = false)?.muted() }

    fun micMuted(): Boolean? = onAudio { endpoint(capture = true)?.muted() }

    // Setters are fire-and-forget: dragging the slider must not queue up
    // 500 ms round trips behind each other.
    fun setVolume(percent: Int) = post {
        endpoint(capture = false)?.setVolume(percent.coerceIn(0, 100))
    }

    fun toggleMute() = post {
        val endpoint = endpoint(capture = false) ?: return@post
        val muted = endpoint.muted() ?: return@post
        endpoint.setMuted(!muted)
    }

    fun toggleMic() = post {
        val endpoint = endpoint(capture = true) ?: return@post
        val muted = endpoint.muted() ?: return@post
        endpoint.setMuted(!muted)
    }

    private fun post(block: () -> Unit) {
        if (ole32 == null) return
        runCatching {
            executor.execute {
                initialize()
                runCatching { block() }
            }
        }
    }

    private fun <T> onAudio(block: () -> T?): T? {
        if (ole32 == null) return null
        return try {
            executor.submit(Callable {
                initialize()
                block()
            }).get(500L, TimeUnit.MILLISECONDS)
        } catch (error: Throwable) {
            Log.d("audio call failed: " + error.message)
            null
        }
    }

    private fun initialize() {
        if (initialized) return
        initialized = true
        val result = ole32?.CoInitializeEx(null, COINIT_APARTMENTTHREADED) ?: return
        if (result < 0) Log.w("CoInitializeEx failed: " + result)
    }

    private fun endpoint(capture: Boolean): AudioEndpoint? {
        val cached = if (capture) captureEndpoint else renderEndpoint
        if (cached != null) return cached
        val opened = open(if (capture) E_CAPTURE else E_RENDER) ?: return null
        if (capture) captureEndpoint = opened else renderEndpoint = opened
        return opened
    }

    private fun open(flow: Int): AudioEndpoint? {
        val ole = ole32 ?: return null
        val enumeratorRef = PointerByReference()
        val created = ole.CoCreateInstance(
            CLSID_MM_DEVICE_ENUMERATOR,
            null,
            CLSCTX_ALL,
            IID_MM_DEVICE_ENUMERATOR,
            enumeratorRef,
        )
        if (created != 0) {
            Log.w("cannot create the audio device enumerator: " + created)
            return null
        }
        val enumerator = enumeratorRef.value ?: return null

        val deviceRef = PointerByReference()
        val found = call(enumerator, 4).invokeInt(arrayOf(enumerator, flow, E_CONSOLE, deviceRef))
        if (found != 0) return null
        val device = deviceRef.value ?: return null

        val interfaceRef = PointerByReference()
        val activated = call(device, 3).invokeInt(
            arrayOf(device, IID_AUDIO_ENDPOINT_VOLUME.toMemory(), CLSCTX_ALL, null, interfaceRef),
        )
        if (activated != 0) {
            Log.w("cannot activate IAudioEndpointVolume: " + activated)
            return null
        }
        return interfaceRef.value?.let { AudioEndpoint(it) }
    }

    private fun call(comObject: Pointer, index: Int): Function =
        method(comObject, index)

    private companion object {
        const val CLSCTX_ALL = 0x17
        const val COINIT_APARTMENTTHREADED = 0x2
        const val E_RENDER = 0
        const val E_CAPTURE = 1
        const val E_CONSOLE = 0

        val CLSID_MM_DEVICE_ENUMERATOR = guid("BCDE0395-E52F-467C-8E3D-C4579291692E")
        val IID_MM_DEVICE_ENUMERATOR = guid("A95664D2-9614-4F35-A746-DE8DB63617E6")
        val IID_AUDIO_ENDPOINT_VOLUME = guid("5CDF2C82-841E-4546-9722-0CF74078229A")
    }
}

private class AudioEndpoint(private val pointer: Pointer) {
    fun volume(): Int? {
        val level = FloatByReference()
        if (call(9, arrayOf(pointer, level)) != 0) return null
        return (level.value * 100f).roundToInt().coerceIn(0, 100)
    }

    fun setVolume(percent: Int) {
        call(7, arrayOf(pointer, percent / 100f, null))
    }

    fun muted(): Boolean? {
        val muted = IntByReference()
        if (call(15, arrayOf(pointer, muted)) != 0) return null
        return muted.value != 0
    }

    fun setMuted(muted: Boolean) {
        call(14, arrayOf(pointer, if (muted) 1 else 0, null))
    }

    private fun call(index: Int, args: Array<Any?>): Int =
        method(pointer, index).invokeInt(args)
}

private class WindowsBrightness {
    private val dxva2 = Win32Api.dxva2

    /** Probed once: desktops with DDC-less panels simply keep the slider hidden. */
    val available: Boolean = read() != null

    fun percent(): Int? = read()

    fun set(percent: Int) {
        withMonitor { handle -> write(handle, percent) }
    }

    private fun read(): Int? = withMonitor { handle ->
        val dx = dxva2 ?: return@withMonitor null
        val minimum = IntByReference()
        val current = IntByReference()
        val maximum = IntByReference()
        if (dx.GetMonitorBrightness(handle, minimum, current, maximum) == 0) return@withMonitor null
        val span = maximum.value - minimum.value
        if (span <= 0) null else ((current.value - minimum.value) * 100 / span).coerceIn(0, 100)
    }

    private fun write(handle: Pointer, percent: Int): Boolean {
        val dx = dxva2 ?: return false
        val minimum = IntByReference()
        val current = IntByReference()
        val maximum = IntByReference()
        if (dx.GetMonitorBrightness(handle, minimum, current, maximum) == 0) return false
        val span = maximum.value - minimum.value
        if (span <= 0) return false
        val value = minimum.value + (percent.coerceIn(1, 100) * span / 100)
        return dx.SetMonitorBrightness(handle, value) != 0
    }

    private fun <T> withMonitor(block: (Pointer) -> T?): T? {
        val dx = dxva2 ?: return null
        val u = Win32Api.user32 ?: return null
        val hwnd = Win32Window.hwnd() ?: u.GetDesktopWindow()
        val monitor = u.MonitorFromWindow(hwnd, MONITOR_DEFAULTTONEAREST)
        if (monitor == null || monitor == Pointer.NULL) return null

        val count = IntByReference()
        if (dx.GetNumberOfPhysicalMonitorsFromHMONITOR(monitor, count) == 0) return null
        if (count.value <= 0) return null

        val monitors = PHYSICAL_MONITOR().toArray(count.value).map { it as PHYSICAL_MONITOR }
        monitors.forEach { it.write() }
        if (dx.GetPhysicalMonitorsFromHMONITOR(monitor, count.value, monitors[0].pointer) == 0) return null
        try {
            monitors[0].read()
            val handle = monitors[0].hPhysicalMonitor ?: return null
            return block(handle)
        } finally {
            dx.DestroyPhysicalMonitors(count.value, monitors[0].pointer)
        }
    }

    private companion object {
        const val MONITOR_DEFAULTTONEAREST = 2
    }
}
