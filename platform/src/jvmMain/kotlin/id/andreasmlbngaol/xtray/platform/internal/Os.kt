package id.andreasmlbngaol.xtray.platform.internal

/** Host operating system, resolved once so platform branches stay cheap. */
object Os {
    private val name: String = System.getProperty("os.name").orEmpty().lowercase()

    val isWindows: Boolean = name.startsWith("windows")
    val isLinux: Boolean = name.contains("linux")
    val isMac: Boolean = name.contains("mac") || name.contains("darwin")
}
