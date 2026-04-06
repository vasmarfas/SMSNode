package com.vasmarfas.smsnode.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vasmarfas.smsnode.data.api.ApiResult
import com.vasmarfas.smsnode.data.models.SimCardResponse
import com.vasmarfas.smsnode.ui.viewmodel.AppViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import smsnode.composeapp.generated.resources.*

@Composable
fun MySimsScreen(viewModel: AppViewModel) {
    var sims by remember { mutableStateOf<List<SimCardResponse>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var simToEdit by remember { mutableStateOf<SimCardResponse?>(null) }
    var editLabel by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.applyStoredToken()
        when (val r = viewModel.api.getMySims()) {
            is ApiResult.Success -> sims = r.value
            else -> { }
        }
        loading = false
    }

    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.padding(24.dp))
        }
        return
    }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text(stringResource(Res.string.sims_tab), style = MaterialTheme.typography.titleLarge)
            if (sims.isEmpty()) {
                Text(
                    stringResource(Res.string.no_sims_assigned),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 16.dp)
                )
            } else {
                LazyColumn(Modifier.fillMaxSize().padding(top = 8.dp)) {
                    items(sims) { s ->
                        Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    val portStr = stringResource(Res.string.port_num, s.portNumber.toString())
                                    val title = if (!s.label.isNullOrBlank()) "${s.label} (${s.phoneNumber ?: portStr})" else s.phoneNumber ?: portStr
                                    Text(title, style = MaterialTheme.typography.titleMedium)
                                    Text(stringResource(Res.string.status, s.status), style = MaterialTheme.typography.bodyMedium)
                                }
                                IconButton(onClick = {
                                    simToEdit = s
                                    editLabel = s.label ?: ""
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Добавить подпись")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    simToEdit?.let { s ->
        AlertDialog(
            onDismissRequest = { simToEdit = null },
            title = { Text(stringResource(Res.string.sim_label_title)) },
            text = {
                OutlinedTextField(
                    value = editLabel,
                    onValueChange = { editLabel = it },
                    label = { Text(stringResource(Res.string.sim_label_hint)) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val newLabel = editLabel.takeIf { it.isNotBlank() }
                        when (val r = viewModel.api.updateMySim(s.id, newLabel)) {
                            is ApiResult.Success -> {
                                sims = sims.map { if (it.id == s.id) r.value else it }
                                simToEdit = null
                            }
                            else -> { }
                        }
                    }
                }) {
                    Text(stringResource(Res.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { simToEdit = null }) { Text(stringResource(Res.string.cancel)) }
            }
        )
    }
}
