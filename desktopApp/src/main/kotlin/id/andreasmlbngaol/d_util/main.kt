package id.andreasmlbngaol.d_util

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "d-util",
    ) {
        App()
    }
}