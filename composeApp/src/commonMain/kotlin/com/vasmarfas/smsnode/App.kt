package com.vasmarfas.smsnode

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.vasmarfas.smsnode.ui.theme.SmsNodeTheme
import org.jetbrains.compose.resources.stringResource
import smsnode.composeapp.generated.resources.Res
import smsnode.composeapp.generated.resources.demo_server_banner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
    val baseUrl by viewModel.baseUrl.collectAsState()
    
    val isDemoMode = baseUrl.trimEnd('/') == "https://demo.smsnode.vasmarfas.com"

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
            Column(modifier = Modifier.fillMaxSize()) {
                if (isDemoMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(Res.string.demo_server_banner),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                Box(modifier = Modifier.weight(1f)) {
                    SmsNodeNavGraph(
                        sessionManager = sessionManager,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}
