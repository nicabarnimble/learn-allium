package dev.allium.tutor

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Allium Tutor",
    ) {
        TutorApp()
    }
}
