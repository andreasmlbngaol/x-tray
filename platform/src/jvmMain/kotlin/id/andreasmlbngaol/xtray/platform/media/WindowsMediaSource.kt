package id.andreasmlbngaol.xtray.platform.media

import id.andreasmlbngaol.xtray.platform.internal.AppPaths
import id.andreasmlbngaol.xtray.platform.internal.Log
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.nio.file.Files
import java.util.Base64

/**
 * Windows media through the System Media Transport Controls (the API behind
 * the volume-flyout card). WinRT has no CLI, so a single PowerShell helper
 * polls the current session once a second and prints one JSON line per tick.
 *
 * Commands are handed over as a tiny file the helper consumes on its next
 * tick; that keeps the helper process long-lived instead of spawning a
 * PowerShell window for every button press.
 */
class WindowsMediaSource : MediaSource {
    private val commandFile = AppPaths.configDir.resolve("media.command")
    private val lock = Any()

    @Volatile private var process: Process? = null
    @Volatile private var track: MediaTrack? = null
    private var lastStart = 0L
    private var hookInstalled = false

    override fun snapshot(): MediaTrack? {
        ensureStarted()
        return track
    }

    override fun playPause() = send("playpause")
    override fun next() = send("next")
    override fun previous() = send("previous")

    private fun send(command: String) {
        ensureStarted()
        runCatching {
            Files.createDirectories(commandFile.parent)
            Files.writeString(commandFile, command)
        }.onFailure { Log.d("cannot queue a media command: " + it.message) }
    }

    private fun ensureStarted() {
        synchronized(lock) {
            val running = process
            if (running != null && running.isAlive) return
            val now = System.currentTimeMillis()
            if (now - lastStart < RESTART_DELAY_MS) return
            lastStart = now

            runCatching {
                Files.createDirectories(commandFile.parent)
                Files.deleteIfExists(commandFile)
                val started = ProcessBuilder(
                    "powershell.exe",
                    "-NoProfile",
                    "-NonInteractive",
                    "-ExecutionPolicy", "Bypass",
                    "-WindowStyle", "Hidden",
                    "-EncodedCommand", encode(script()),
                )
                    .redirectErrorStream(true)
                    .start()
                process = started
                Thread({ readLoop(started) }, "x-tray-media").apply {
                    isDaemon = true
                    start()
                }
                installShutdownHook()
                Log.i("media helper started")
            }.onFailure {
                Log.w("cannot start the media helper", it)
            }
        }
    }

    private fun readLoop(started: Process) {
        runCatching {
            started.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                while (true) {
                    val line = reader.readLine() ?: break
                    if (!line.trimStart().startsWith("{")) continue
                    track = parse(line)
                }
            }
        }.onFailure { Log.d("media helper stopped: " + it.message) }
        track = null
    }

    private fun installShutdownHook() {
        if (hookInstalled) return
        hookInstalled = true
        runCatching {
            Runtime.getRuntime().addShutdownHook(
                Thread {
                    runCatching { process?.destroy() }
                },
            )
        }
    }

    private fun parse(line: String): MediaTrack? {
        val root = runCatching { Json.parseToJsonElement(line).jsonObject }.getOrNull() ?: return null
        if (root.containsKey("none") || root.containsKey("error")) return null

        val title = root["title"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        if (title.isBlank()) return null
        val app = root["app"]?.jsonPrimitive?.contentOrNull.orEmpty()

        return MediaTrack(
            player = friendlyName(app),
            busName = app,
            title = title,
            artist = root["artist"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            album = root["album"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            artUrl = null,
            playing = root["playing"]?.jsonPrimitive?.booleanOrNull ?: false,
            positionMs = root["position"]?.jsonPrimitive?.longOrNull ?: 0L,
            lengthMs = root["length"]?.jsonPrimitive?.longOrNull ?: 0L,
        )
    }

    private fun friendlyName(app: String): String {
        val base = app.substringAfterLast('!').substringAfterLast('\\').substringAfterLast('/')
        val name = base.removeSuffix(".exe").ifBlank { "Media" }
        return name.replaceFirstChar { it.uppercaseChar() }
    }

    private fun encode(script: String): String =
        Base64.getEncoder().encodeToString(script.toByteArray(Charsets.UTF_16LE))

    private fun script(): String {
        val path = commandFile.toAbsolutePath().toString().replace("'", "''")
        return SCRIPT
            .replace("§", "$")
            .replace("__COMMAND_FILE__", path)
    }

    private companion object {
        const val RESTART_DELAY_MS = 10_000L

        val SCRIPT = """
            §ErrorActionPreference = 'SilentlyContinue'
            [Console]::OutputEncoding = [System.Text.Encoding]::UTF8
            Add-Type -AssemblyName System.Runtime.WindowsRuntime
            §asTask = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object { §_.Name -eq 'AsTask' -and §_.GetParameters().Count -eq 1 -and §_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1' })[0]
            function Await(§operation, §type) {
              §task = §asTask.MakeGenericMethod(§type).Invoke(§null, @(§operation))
              §task.Wait(-1) | Out-Null
              §task.Result
            }
            §null = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager,Windows.Media.Control,ContentType=WindowsRuntime]
            §commandFile = '__COMMAND_FILE__'
            while (§true) {
              §command = §null
              if (Test-Path -LiteralPath §commandFile) {
                try {
                  §command = (Get-Content -LiteralPath §commandFile -Raw).Trim()
                  Remove-Item -LiteralPath §commandFile -Force
                } catch { }
              }
              try {
                §manager = Await ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager])
                §session = §manager.GetCurrentSession()
                if (§command -and §session) {
                  if (§command -eq 'playpause') { §null = Await (§session.TryTogglePlayPauseAsync()) ([bool]) }
                  elseif (§command -eq 'next') { §null = Await (§session.TrySkipNextAsync()) ([bool]) }
                  elseif (§command -eq 'previous') { §null = Await (§session.TrySkipPreviousAsync()) ([bool]) }
                }
                if (§session) {
                  §properties = Await (§session.TryGetMediaPropertiesAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties])
                  §timeline = §session.GetTimelineProperties()
                  §playback = §session.GetPlaybackInfo()
                  §info = [ordered]@{
                    app = §session.SourceAppUserModelId
                    title = §properties.Title
                    artist = §properties.Artist
                    album = §properties.AlbumTitle
                    playing = (§playback.PlaybackStatus -eq 4)
                    position = [int64]§timeline.Position.TotalMilliseconds
                    length = [int64](§timeline.EndTime - §timeline.StartTime).TotalMilliseconds
                  }
                  Write-Output (§info | ConvertTo-Json -Compress)
                } else {
                  Write-Output '{"none":true}'
                }
              } catch {
                Write-Output '{"error":true}'
              }
              Start-Sleep -Milliseconds 1000
            }
        """.trimIndent()
    }
}
