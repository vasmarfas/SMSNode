package com.vasmarfas.smsnode.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vasmarfas.smsnode.ui.viewmodel.AppViewModel
import org.jetbrains.compose.resources.stringResource
import smsnode.composeapp.generated.resources.*

import com.vasmarfas.smsnode.ui.components.NetworkErrorView

@Composable
fun DialogsScreen(
    viewModel: AppViewModel,
    onOpenChat: (String) -> Unit,
) {
    val dialogs by viewModel.dialogs.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val loading by viewModel.dialogsLoading.collectAsState()
    val error by viewModel.dialogsError.collectAsState()
    val unreadCounts by viewModel.unreadCounts.collectAsState()

    var showNewChatDialog by remember { mutableStateOf(false) }
    var newChatPhone by remember { mutableStateOf("") }
    
    var filterType by remember { mutableStateOf("ALL") }

    LaunchedEffect(Unit) {
        viewModel.applyStoredToken()
        viewModel.loadDialogsAndContacts()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        if (error == "NETWORK_ERROR") {
            NetworkErrorView(onRetry = { viewModel.loadDialogsAndContacts() })
        } else {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(Res.string.dialogs_tab), style = MaterialTheme.typography.titleLarge)
                    Button(onClick = { showNewChatDialog = true }) {
                        Text(stringResource(Res.string.new_chat))
                    }
                }
                LazyRow(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = filterType == "ALL",
                            onClick = { filterType = "ALL" },
                            label = { Text(stringResource(Res.string.filter_all)) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = filterType == "UNREAD",
                            onClick = { filterType = "UNREAD" },
                            label = { Text(stringResource(Res.string.filter_unread)) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = filterType == "INCOMING",
                            onClick = { filterType = "INCOMING" },
                            label = { Text(stringResource(Res.string.filter_incoming)) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = filterType == "OUTGOING",
                            onClick = { filterType = "OUTGOING" },
                            label = { Text(stringResource(Res.string.filter_outgoing)) }
                        )
                    }
                }
                if (loading) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(Modifier.padding(24.dp))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    ) {
                        if (error != null && error != "NETWORK_ERROR") {
                            item {
                                Text(
                                    error!!,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                        
                        val filteredDialogs = dialogs.filter { d ->
                            when (filterType) {
                                "UNREAD" -> (unreadCounts[d.externalPhone] ?: 0) > 0
                                "INCOMING" -> d.lastDirection == "in"
                                "OUTGOING" -> d.lastDirection == "out"
                                else -> true
                            }
                        }

                        items(filteredDialogs) { d ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { onOpenChat(d.externalPhone) }
                            ) {
                                val contact = contacts.firstOrNull { it.phoneNumber == d.externalPhone }
                                val unread = unreadCounts[d.externalPhone] ?: 0

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            contact?.name ?: d.externalPhone,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (contact != null) {
                                            Text(
                                                d.externalPhone,
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Text(
                                            d.lastText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                        d.lastAt?.let { time ->
                                            val timeStr = if (time.length >= 16) time.substring(11, 16) else time
                                            Text(
                                                timeStr,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }
                                    }

                                    if (unread > 0) {
                                        Box(
                                            modifier = Modifier
                                                .padding(start = 8.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary)
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = unread.toString(),
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNewChatDialog) {
        AlertDialog(
            onDismissRequest = { showNewChatDialog = false },
            title = { Text(stringResource(Res.string.new_chat)) },
            text = {
                OutlinedTextField(
                    value = newChatPhone,
                    onValueChange = { newChatPhone = it },
                    label = { Text(stringResource(Res.string.phone_number)) },
                    placeholder = { Text(stringResource(Res.string.phone_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val phone = newChatPhone.trim()
                        if (phone.isNotEmpty()) {
                            showNewChatDialog = false
                            newChatPhone = ""
                            onOpenChat(phone)
                        }
                    }
                ) { Text(stringResource(Res.string.open_chat)) }
            },
            dismissButton = {
                OutlinedButton(onClick = { showNewChatDialog = false; newChatPhone = "" }) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    }
}
