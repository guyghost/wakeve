package com.guyghost.wakeve

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Wakeve",
    ) {
        App()
    }
}