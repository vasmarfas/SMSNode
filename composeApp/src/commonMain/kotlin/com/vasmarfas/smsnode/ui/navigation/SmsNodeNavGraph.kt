package com.vasmarfas.smsnode.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vasmarfas.smsnode.NotificationRouteHolder
import com.vasmarfas.smsnode.data.session.SessionManager
import com.vasmarfas.smsnode.ui.screens.AdminGatewayDetailScreen
import com.vasmarfas.smsnode.ui.screens.AdminGatewaysScreen
import com.vasmarfas.smsnode.ui.screens.AdminMenuScreen
import com.vasmarfas.smsnode.ui.screens.AdminMessagesScreen
import com.vasmarfas.smsnode.ui.screens.AdminPendingScreen
import com.vasmarfas.smsnode.ui.screens.AdminRegModeScreen
import com.vasmarfas.smsnode.ui.screens.AdminUserDetailScreen
import com.vasmarfas.smsnode.ui.screens.AdminUsersScreen
import com.vasmarfas.smsnode.ui.screens.ChatScreen
import com.vasmarfas.smsnode.ui.screens.ContactsScreen
import com.vasmarfas.smsnode.ui.screens.DialogsScreen
import com.vasmarfas.smsnode.ui.screens.LoginScreen
import com.vasmarfas.smsnode.ui.screens.MySimsScreen
import com.vasmarfas.smsnode.ui.screens.ProfileScreen
import com.vasmarfas.smsnode.ui.screens.RegisterScreen
import com.vasmarfas.smsnode.ui.screens.TemplatesScreen
import com.vasmarfas.smsnode.ui.viewmodel.AppViewModel
import org.jetbrains.compose.resources.stringResource
import smsnode.composeapp.generated.resources.*

private val MAIN_SCREENS =
    setOf(Screen.Dialogs, Screen.Contacts, Screen.MySims, Screen.Templates, Screen.Profile)

@Composable
fun SmsNodeNavGraph(
    sessionManager: SessionManager,
    viewModel: AppViewModel,
    modifier: Modifier = Modifier,
) {
    val user by sessionManager.user.collectAsState()
    val loginResult by viewModel.loginResult.collectAsState()
    val registerResult by viewModel.registerResult.collectAsState()
    val initialScreen =
        remember { if (sessionManager.user.value != null) Screen.Dialogs else Screen.Login }
    val navigator = rememberAppNavigator(initialScreen)
    val current = navigator.current
    val pendingChatPhone by NotificationRouteHolder.pendingChatPhone.collectAsState()

    val showBottomBar = user != null
    val isTopLevel = current in MAIN_SCREENS
    val isMainOrAdmin = current in MAIN_SCREENS || current is Screen.Chat ||
            current == Screen.Admin || current == Screen.AdminGateways || current == Screen.AdminUsers ||
            current is Screen.AdminGatewayDetail || current is Screen.AdminUserDetail ||
            current == Screen.AdminPending || current == Screen.AdminRegMode || current == Screen.AdminMessages

    LaunchedEffect(user) {
        if (user != null && current == Screen.Login) {
            navigator.replaceAll(Screen.Dialogs)
        } else if (user == null && current != Screen.Login && current != Screen.Register) {
            navigator.replaceAll(Screen.Login)
        }
    }

    LaunchedEffect(user, pendingChatPhone) {
        if (user == null) return@LaunchedEffect
        val phone = NotificationRouteHolder.consumePendingChatPhone() ?: return@LaunchedEffect
        viewModel.markDialogRead(phone)
        navigator.replaceAll(Screen.Dialogs)
        navigator.navigate(Screen.Chat(phone))
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideScreen = maxWidth > 600.dp

        Row(Modifier.fillMaxSize()) {
            if (isWideScreen && showBottomBar && isTopLevel) {
                NavigationRail {
                    listOf(
                        Triple(stringResource(Res.string.dialogs_tab), Icons.AutoMirrored.Filled.Chat, Screen.Dialogs),
                        Triple(stringResource(Res.string.contacts_tab), Icons.Filled.Contacts, Screen.Contacts),
                        Triple(stringResource(Res.string.sims_tab), Icons.Filled.SimCard, Screen.MySims),
                        Triple(stringResource(Res.string.templates_tab), Icons.Filled.List, Screen.Templates),
                        Triple(stringResource(Res.string.profile_tab), Icons.Filled.Person, Screen.Profile),
                    ).forEach { (label, icon, screen) ->
                        NavigationRailItem(
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) },
                            selected = current == screen,
                            onClick = { navigator.navigateToMain(screen) }
                        )
                    }
                }
            }

            Scaffold(
                modifier = Modifier.weight(1f),
                bottomBar = {
                    if (!isWideScreen && showBottomBar && isTopLevel) {
                        NavigationBar {
                            listOf(
                                Triple(stringResource(Res.string.dialogs_tab), Icons.AutoMirrored.Filled.Chat, Screen.Dialogs),
                                Triple(stringResource(Res.string.contacts_tab), Icons.Filled.Contacts, Screen.Contacts),
                                Triple(stringResource(Res.string.sims_tab), Icons.Filled.SimCard, Screen.MySims),
                                Triple(stringResource(Res.string.templates_tab), Icons.Filled.List, Screen.Templates),
                                Triple(stringResource(Res.string.profile_tab), Icons.Filled.Person, Screen.Profile),
                            ).forEach { (label, icon, screen) ->
                                NavigationBarItem(
                                    icon = { Icon(icon, contentDescription = label) },
                                    label = { Text(label) },
                                    selected = current == screen,
                                    onClick = { navigator.navigateToMain(screen) }
                                )
                            }
                        }
                    }
                }
            ) { paddingValues ->
                Box(
                    Modifier.padding(paddingValues).fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(Modifier.widthIn(max = 840.dp).fillMaxSize()) {
                        val screen = navigator.current
                        if (user == null) {
                            when (screen) {
                                Screen.Login -> LoginScreen(
                                    viewModel = viewModel,
                                    loginResult = loginResult,
                                    onClearLoginResult = viewModel::clearLoginResult,
                                    onLoginSuccess = { navigator.replaceAll(Screen.Dialogs) },
                                    onNavigateToRegister = { navigator.navigate(Screen.Register) }
                                )

                                Screen.Register -> RegisterScreen(
                                    viewModel = viewModel,
                                    registerResult = registerResult,
                                    onClearRegisterResult = viewModel::clearRegisterResult,
                                    onRegisterCreated = { navigator.replaceAll(Screen.Dialogs) },
                                    onBack = { navigator.back() }
                                )

                                else -> LoginScreen(
                                    viewModel = viewModel,
                                    loginResult = loginResult,
                                    onClearLoginResult = viewModel::clearLoginResult,
                                    onLoginSuccess = { navigator.replaceAll(Screen.Dialogs) },
                                    onNavigateToRegister = { navigator.navigate(Screen.Register) }
                                )
                            }
                        } else {
                            when (screen) {
                                Screen.Login -> LoginScreen(
                                    viewModel = viewModel,
                                    loginResult = loginResult,
                                    onClearLoginResult = viewModel::clearLoginResult,
                                    onLoginSuccess = { navigator.replaceAll(Screen.Dialogs) },
                                    onNavigateToRegister = { navigator.navigate(Screen.Register) }
                                )

                                Screen.Register -> RegisterScreen(
                                    viewModel = viewModel,
                                    registerResult = registerResult,
                                    onClearRegisterResult = viewModel::clearRegisterResult,
                                    onRegisterCreated = { navigator.replaceAll(Screen.Dialogs) },
                                    onBack = { navigator.back() }
                                )

                                Screen.Dialogs -> DialogsScreen(
                                    viewModel = viewModel,
                                    onOpenChat = { phone ->
                                        viewModel.markDialogRead(phone)
                                        navigator.navigate(Screen.Chat(phone))
                                    }
                                )

                                is Screen.Chat -> ChatScreen(
                                    phone = screen.phone,
                                    viewModel = viewModel,
                                    onBack = { navigator.back() }
                                )

                                Screen.Contacts -> ContactsScreen(
                                    viewModel = viewModel,
                                    onOpenChat = { phone ->
                                        viewModel.markDialogRead(phone)
                                        navigator.navigate(Screen.Chat(phone))
                                    }
                                )

                                Screen.MySims -> MySimsScreen(viewModel = viewModel)
                                Screen.Templates -> TemplatesScreen(
                                    viewModel = viewModel,
                                    isAdmin = user?.role == "admin"
                                )

                                Screen.Profile -> ProfileScreen(
                                    sessionManager = sessionManager,
                                    viewModel = viewModel,
                                    onLogout = {
                                        viewModel.logout()
                                        navigator.replaceAll(Screen.Login)
                                    },
                                    onOpenAdmin = { navigator.navigate(Screen.Admin) }
                                )

                                Screen.Admin -> AdminMenuScreen(
                                    onNavigateTo = { route ->
                                        val s = when (route) {
                                            "admin/gateways" -> Screen.AdminGateways
                                            "admin/users" -> Screen.AdminUsers
                                            "admin/pending" -> Screen.AdminPending
                                            "admin/regmode" -> Screen.AdminRegMode
                                            "admin/messages" -> Screen.AdminMessages
                                            else -> return@AdminMenuScreen
                                        }
                                        navigator.navigate(s)
                                    },
                                    onBack = { navigator.back() }
                                )

                                Screen.AdminGateways -> AdminGatewaysScreen(
                                    viewModel = viewModel,
                                    onBack = { navigator.back() },
                                    onOpenGatewayDetail = {
                                        navigator.navigate(
                                            Screen.AdminGatewayDetail(
                                                it
                                            )
                                        )
                                    }
                                )

                                is Screen.AdminGatewayDetail -> AdminGatewayDetailScreen(
                                    gatewayId = screen.gatewayId,
                                    viewModel = viewModel,
                                    onBack = { navigator.back() }
                                )

                                Screen.AdminUsers -> AdminUsersScreen(
                                    viewModel = viewModel,
                                    onBack = { navigator.back() },
                                    onOpenUserDetail = {
                                        navigator.navigate(
                                            Screen.AdminUserDetail(
                                                it
                                            )
                                        )
                                    }
                                )

                                is Screen.AdminUserDetail -> AdminUserDetailScreen(
                                    userId = screen.userId,
                                    viewModel = viewModel,
                                    onBack = { navigator.back() }
                                )

                                Screen.AdminPending -> AdminPendingScreen(
                                    viewModel = viewModel,
                                    onBack = { navigator.back() }
                                )

                                Screen.AdminRegMode -> AdminRegModeScreen(
                                    viewModel = viewModel,
                                    onBack = { navigator.back() }
                                )

                                Screen.AdminMessages -> AdminMessagesScreen(
                                    viewModel = viewModel,
                                    onBack = { navigator.back() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
