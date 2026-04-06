package com.vasmarfas.smsnode

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.jetbrains.compose.resources.painterResource
import smsnode.composeapp.generated.resources.Res
import smsnode.composeapp.generated.resources.logo

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "SMSNode",
        icon = painterResource(Res.drawable.logo)
    ) {
        App()
    }
}