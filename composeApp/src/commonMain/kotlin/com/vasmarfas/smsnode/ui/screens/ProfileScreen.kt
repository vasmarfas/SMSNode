package com.vasmarfas.smsnode.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vasmarfas.smsnode.data.session.SessionManager
import com.vasmarfas.smsnode.ui.viewmodel.AppViewModel
import org.jetbrains.compose.resources.stringResource
import smsnode.composeapp.generated.resources.*

import androidx.compose.material3.Surface

@Composable
fun ProfileScreen(
    sessionManager: SessionManager,
    viewModel: AppViewModel,
    onLogout: () -> Unit,
    onOpenAdmin: () -> Unit,
) {
    val user by sessionManager.user.collectAsState()
    val isAdmin = sessionManager.isAdmin

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(24.dp)) {
            Text(stringResource(Res.string.profile_tab), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))
            user?.let { u ->
                Text(stringResource(Res.string.login_colon, u.username), style = MaterialTheme.typography.bodyLarge)
                Text(stringResource(Res.string.role_colon, u.role), style = MaterialTheme.typography.bodyMedium)
                
                Spacer(Modifier.height(24.dp))
                
                        if (u.telegramId == null || u.telegramId == 0L) {
                            Text(
                                text = stringResource(Res.string.tg_not_linked),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = stringResource(Res.string.tg_link_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = stringResource(Res.string.tg_linked, u.telegramId.toString()),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
            }
            Spacer(Modifier.height(24.dp))
            if (isAdmin) {
                Button(onClick = onOpenAdmin, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(Res.string.admin_tab))
                }
                Spacer(Modifier.height(12.dp))
            }
            Button(onClick = {
                viewModel.logout()
                onLogout()
            }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.logout))
            }
        }
    }
}
