package com.vasmarfas.smsnode

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import com.vasmarfas.smsnode.ui.theme.SmsNodeTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import com.vasmarfas.smsnode.data.settings.ServerUrlStorage
import com.vasmarfas.smsnode.data.session.SessionManager
import com.vasmarfas.smsnode.ui.navigation.SmsNodeNavGraph
import com.vasmarfas.smsnode.ui.viewmodel.AppViewModel

@Composable
fun App() {
    val sessionManager = remember { SessionManager() }
    val viewModel = remember {
        AppViewModel(
            sessionManager,
            baseUrl = ServerUrlStorage.getBaseUrl() ?: "http://localhost:8007"
        )
    }
    viewModel.applyStoredToken()
    LaunchedEffect(Unit) { viewModel.tryRestoreSessionFromCredentials() }
    
    val isRestoring by viewModel.isSessionRestoring.collectAsState()

    val telegramInitData = getTelegramInitData()
    if (telegramInitData != null) {
        LaunchedEffect(Unit) {
            delay(500)
            if (sessionManager.user.value == null) {
                viewModel.tryTelegramLogin(telegramInitData)
            }
        }
    }

    SmsNodeTheme {
        if (isRestoring) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            SmsNodeNavGraph(
                sessionManager = sessionManager,
                viewModel = viewModel
            )
        }
    }
}
