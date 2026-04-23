package com.vasmarfas.smsnode.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vasmarfas.smsnode.data.api.ApiResult
import com.vasmarfas.smsnode.data.models.MessageResponse
import com.vasmarfas.smsnode.data.models.SimCardResponse
import com.vasmarfas.smsnode.ui.viewmodel.AppViewModel
import org.jetbrains.compose.resources.stringResource
import smsnode.composeapp.generated.resources.*
import com.vasmarfas.smsnode.ui.components.NetworkErrorView

private data class PendingMessage(
    val localId: Long,
    val text: String,
    val simCardId: Int?,
)


@Composable
fun ChatScreen(
    phone: String,
    viewModel: AppViewModel,
    onBack: () -> Unit,
) {
    var messages by remember { mutableStateOf<List<MessageResponse>>(emptyList()) }
    var pendingMessages by remember { mutableStateOf<List<PendingMessage>>(emptyList()) }
    var knownMessageIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var nextLocalId by remember { mutableStateOf(1L) }
    var mySims by remember { mutableStateOf<List<SimCardResponse>>(emptyList()) }
    var selectedSimId by remember { mutableStateOf<Int?>(null) }
    var contactName by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var text by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableStateOf(0) }
    var showTemplates by remember { mutableStateOf(false) }
    val templates by viewModel.templates.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(phone, refresh) {
        viewModel.applyStoredToken()
        when (val r = viewModel.api.getDialogMessages(phone)) {
            is ApiResult.Success -> {
                val newMessages = r.value.reversed()
                messages = newMessages
                knownMessageIds = newMessages.map { it.id }.toSet()
                error = null
            }
            is ApiResult.NetworkError -> error = "NETWORK_ERROR"
            is ApiResult.Error -> error = "Ошибка: ${r.message}"
            else -> error = "Не удалось загрузить сообщения"
        }
        if (error == null || error != "NETWORK_ERROR") {
            when (val r = viewModel.api.getMySims()) {
                is ApiResult.Success -> {
                    mySims = r.value
                    if (selectedSimId == null && r.value.isNotEmpty())
                        selectedSimId = r.value.first().id
                }
                else -> { }
            }
            when (val r = viewModel.api.getContacts()) {
                is ApiResult.Success -> {
                    contactName = r.value.firstOrNull { it.phoneNumber == phone }?.name
                }
                else -> { }
            }
            viewModel.loadTemplates()
        }
        loading = false
        if (error == null) {
            while (true) {
                delay(5000)
                when (val r = viewModel.api.getDialogMessages(phone)) {
                    is ApiResult.Success -> {
                        error = null
                        val prevIds = knownMessageIds
                        val newMessages = r.value.reversed()
                        messages = newMessages
                        knownMessageIds = newMessages.map { it.id }.toSet()
                        if (pendingMessages.isNotEmpty()) {
                            val serverOut = newMessages.filter { it.direction == "out" }
                            pendingMessages = pendingMessages.filter { pending ->
                                serverOut.none { m ->
                                    m.id !in prevIds &&
                                        m.externalPhone == phone &&
                                        m.text == pending.text &&
                                        m.simCardId == pending.simCardId
                                }
                            }
                        }
                    }
                    is ApiResult.NetworkError -> {
                        // ignore polling errors to not show full-screen error
                    }
                    else -> { }
                }
            }
        }
    }

    LaunchedEffect(messages.size, pendingMessages.size) {
        val total = messages.size + pendingMessages.size
        if (total > 0) listState.animateScrollToItem(total - 1)
    }

    Column(Modifier.fillMaxSize()) {
        if (error == "NETWORK_ERROR") {
            NetworkErrorView(onRetry = { refresh++ })
        } else {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
            TextButton(onClick = onBack) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад"
                )
                Text(
                    text = "Назад",
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
            if (contactName != null) {
                Column(Modifier.weight(1f)) {
                    Text(contactName!!, style = MaterialTheme.typography.titleMedium)
                    Text(phone, style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Text(phone, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            }
            
            if (error != null) {
                Text(
                    text = "!",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
        
        if (error != null) {
            Text(
                text = error ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { msg ->
                    val isOut = msg.direction == "out"
                    val simLabel = msg.simCardId?.let { id -> mySims.find { it.id == id }?.let { it.phoneNumber ?: "Порт ${it.portNumber}" } ?: "№$id" } ?: ""
                    val timeStr = msg.createdAt?.let { s ->
                        if (s.length >= 16) s.substring(11, 16) else if (s.length >= 10) s.takeLast(5) else s
                    } ?: ""
                    Box(Modifier.fillMaxWidth()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .align(if (isOut) Alignment.CenterEnd else Alignment.CenterStart),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isOut) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(
                                    msg.text,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = if (isOut) Arrangement.End else Arrangement.Start
                                ) {
                                    if (simLabel.isNotEmpty()) {
                                        Text(
                                            simLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(end = 6.dp)
                                        )
                                    }
                                    if (timeStr.isNotEmpty()) {
                                        Text(
                                            timeStr,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                if (pendingMessages.isNotEmpty()) {
                    items(pendingMessages, key = { it.localId }) { pending ->
                        val simLabel = pending.simCardId?.let { id ->
                            mySims.find { it.id == id }?.let { it.phoneNumber ?: "Порт ${it.portNumber}" } ?: "№$id"
                        } ?: ""
                        Box(Modifier.fillMaxWidth()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .align(Alignment.CenterEnd),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(
                                        pending.text,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (simLabel.isNotEmpty()) {
                                            Text(
                                                simLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(end = 6.dp)
                                            )
                                        }
                                        CircularProgressIndicator(
                                            strokeWidth = 1.5.dp,
                                            modifier = Modifier
                                                .padding(start = 4.dp)
                                                .size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (mySims.size > 1) {
                Text(stringResource(Res.string.send_from_number), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                LazyRow(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(mySims) { sim ->
                        val selected = sim.id == selectedSimId
                        Card(
                            modifier = Modifier
                                .clickable { selectedSimId = sim.id }
                                .then(if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)) else Modifier),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text(
                                sim.phoneNumber ?: stringResource(Res.string.port_num, sim.portNumber.toString()),
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showTemplates = true }) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = stringResource(Res.string.templates_tab)
                    )
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.length <= 160) text = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(Res.string.message_placeholder)) },
                    maxLines = 2
                )
                TextButton(
                    onClick = {
                        val trimmed = text.trim()
                        if (trimmed.isBlank() || sending) return@TextButton
                        val localId = nextLocalId++
                        pendingMessages = pendingMessages + PendingMessage(
                            localId = localId,
                            text = trimmed,
                            simCardId = selectedSimId,
                        )
                        text = ""
                        sending = true
                        scope.launch {
                            when (val r = viewModel.api.sendMessage(phone, trimmed, selectedSimId)) {
                                is ApiResult.Success -> {
                                    when (val mr = viewModel.api.getDialogMessages(phone)) {
                                        is ApiResult.Success -> {
                                            val prevIds = knownMessageIds
                                            val newMessages = mr.value.reversed()
                                            messages = newMessages
                                            knownMessageIds = newMessages.map { it.id }.toSet()
                                            if (pendingMessages.isNotEmpty()) {
                                                val serverOut = newMessages.filter { it.direction == "out" }
                                                pendingMessages = pendingMessages.filter { pending ->
                                                    serverOut.none { m ->
                                                        m.id !in prevIds &&
                                                            m.externalPhone == phone &&
                                                            m.text == pending.text &&
                                                            m.simCardId == pending.simCardId
                                                    }
                                                }
                                            }
                                        }
                                        else -> { }
                                    }
                                }
                                else -> {
                                    pendingMessages = pendingMessages.filterNot { it.localId == localId }
                                    text = trimmed
                                }
                            }
                            sending = false
                        }
                    },
                    enabled = text.isNotBlank() && !sending
                ) { Text(stringResource(Res.string.send)) }
            }
        }
    }

    if (showTemplates) {
        AlertDialog(
            onDismissRequest = { showTemplates = false },
            title = { Text(stringResource(Res.string.choose_template)) },
            text = {
                if (templates.isEmpty()) {
                    Text(stringResource(Res.string.no_available_templates))
                } else {
                    LazyColumn {
                        items(templates) { tmpl ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        text = tmpl.content
                                        showTemplates = false
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(tmpl.name, style = MaterialTheme.typography.titleMedium)
                                    Text(tmpl.content, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTemplates = false }) {
                    Text(stringResource(Res.string.close))
                }
            }
        )
    }
}
}

