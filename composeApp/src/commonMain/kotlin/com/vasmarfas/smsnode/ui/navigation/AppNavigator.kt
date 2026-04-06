package com.vasmarfas.smsnode.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList

sealed class Screen {
    data object Login : Screen()
    data object Register : Screen()
    data object Dialogs : Screen()
    data class Chat(val phone: String) : Screen()
    data object Contacts : Screen()
    data object MySims : Screen()
    data object Templates : Screen()
    data object Profile : Screen()
    data object Admin : Screen()
    data object AdminGateways : Screen()
    data class AdminGatewayDetail(val gatewayId: Int) : Screen()
    data object AdminUsers : Screen()
    data class AdminUserDetail(val userId: Int) : Screen()
    data object AdminPending : Screen()
    data object AdminRegMode : Screen()
    data object AdminMessages : Screen()
}

class AppNavigator(initial: Screen) {
    private val stack: SnapshotStateList<Screen> = mutableStateListOf(initial)

    val current: Screen get() = stack.last()
    val canGoBack: Boolean get() = stack.size > 1

    fun navigate(screen: Screen) {
        stack.add(screen)
    }

    fun back() {
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    fun replaceAll(screen: Screen) {
        stack.clear()
        stack.add(screen)
    }

    fun popUpToAndNavigate(popUpTo: Screen, inclusive: Boolean, navigateTo: Screen) {
        val idx = stack.indexOfLast { it == popUpTo }
        if (idx < 0) {
            navigate(navigateTo)
            return
        }
        val removeFrom = if (inclusive) idx else idx + 1
        while (stack.size > removeFrom) stack.removeAt(stack.lastIndex)
        stack.add(navigateTo)
    }

    fun navigateToMain(screen: Screen) {
        val mainScreens = setOf(Screen.Dialogs, Screen.Contacts, Screen.MySims, Screen.Templates, Screen.Profile)
        if (screen !in mainScreens) return
        val idx = stack.indexOfFirst { it in mainScreens }
        if (idx >= 0) {
            while (stack.size > idx + 1) stack.removeAt(stack.lastIndex)
        }
        if (stack.last() != screen) stack.add(screen)
    }
}

@Composable
fun rememberAppNavigator(initial: Screen): AppNavigator = remember(initial) { AppNavigator(initial) }
