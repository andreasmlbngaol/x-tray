package id.andreasmlbngaol.islanddesk.core.ipc

sealed interface IpcCommand {
    data object Toggle : IpcCommand
    data object Expand : IpcCommand
    data object Collapse : IpcCommand
    data object Show : IpcCommand
    data object Hide : IpcCommand
    data object Quit : IpcCommand
    data object State : IpcCommand
    data class Notify(val message: String) : IpcCommand
    data class StartActivity(val title: String, val minutes: Int) : IpcCommand
    data object StopActivity : IpcCommand
    data class SetDnd(val enabled: Boolean) : IpcCommand
}

const val IPC_USAGE: String =
    "usage: islanddesk [toggle|expand|collapse|show|hide|state|quit|" +
        "notify <text>|activity start [minutes] [title]|activity stop|dnd on|off]"

/**
 * Parses command-line arguments shared by the CLI client and the socket
 * server. Returns null for unknown input so callers can print [IPC_USAGE].
 */
fun parseIpcCommand(args: List<String>): IpcCommand? {
    val clean = args.filter { it.isNotBlank() }
    if (clean.isEmpty()) return IpcCommand.Toggle
    return when (clean[0].lowercase()) {
        "toggle" -> IpcCommand.Toggle
        "expand", "open" -> IpcCommand.Expand
        "collapse", "close" -> IpcCommand.Collapse
        "show" -> IpcCommand.Show
        "hide" -> IpcCommand.Hide
        "quit", "exit" -> IpcCommand.Quit
        "state", "status" -> IpcCommand.State
        "notify" -> {
            val message = clean.drop(1).joinToString(" ")
            if (message.isBlank()) null else IpcCommand.Notify(message)
        }
        "activity" -> when (clean.getOrNull(1)?.lowercase()) {
            "start" -> {
                val minutes = clean.getOrNull(2)?.toIntOrNull() ?: 25
                val title = clean.drop(3).joinToString(" ").ifBlank { "Focus" }
                IpcCommand.StartActivity(title, minutes)
            }
            "stop" -> IpcCommand.StopActivity
            else -> null
        }
        "dnd" -> when (clean.getOrNull(1)?.lowercase()) {
            "on", "true", "1" -> IpcCommand.SetDnd(true)
            "off", "false", "0" -> IpcCommand.SetDnd(false)
            else -> null
        }
        else -> null
    }
}

fun IpcCommand.encode(): String = when (this) {
    IpcCommand.Toggle -> "toggle"
    IpcCommand.Expand -> "expand"
    IpcCommand.Collapse -> "collapse"
    IpcCommand.Show -> "show"
    IpcCommand.Hide -> "hide"
    IpcCommand.Quit -> "quit"
    IpcCommand.State -> "state"
    is IpcCommand.Notify -> "notify " + message.replace('\n', ' ')
    is IpcCommand.StartActivity -> "activity start " + minutes + " " + title
    IpcCommand.StopActivity -> "activity stop"
    is IpcCommand.SetDnd -> if (enabled) "dnd on" else "dnd off"
}

fun decodeIpcCommand(line: String): IpcCommand? =
    parseIpcCommand(line.trim().split(' ').filter { it.isNotEmpty() })
