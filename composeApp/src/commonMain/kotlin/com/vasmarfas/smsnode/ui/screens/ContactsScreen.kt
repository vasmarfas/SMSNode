package com.vasmarfas.smsnode.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vasmarfas.smsnode.data.api.ApiResult
import com.vasmarfas.smsnode.data.models.ContactGroupResponse
import com.vasmarfas.smsnode.data.models.ContactResponse
import com.vasmarfas.smsnode.data.models.SimCardResponse
import com.vasmarfas.smsnode.ui.viewmodel.AppViewModel
import org.jetbrains.compose.resources.stringResource
import smsnode.composeapp.generated.resources.*

import com.vasmarfas.smsnode.ui.components.NetworkErrorView

@Composable
fun ContactsScreen(
    viewModel: AppViewModel,
    onOpenChat: (String) -> Unit = {},
) {
    var contacts by remember { mutableStateOf<List<ContactResponse>>(emptyList()) }
    var groups by remember { mutableStateOf<List<ContactGroupResponse>>(emptyList()) }
    var mySims by remember { mutableStateOf<List<SimCardResponse>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableStateOf(0) }

    var showAddContactDialog by remember { mutableStateOf(false) }
    var addName by remember { mutableStateOf("") }
    var addPhone by remember { mutableStateOf("") }
    var addingContact by remember { mutableStateOf(false) }
    var editContact by remember { mutableStateOf<ContactResponse?>(null) }
    var contactToDelete by remember { mutableStateOf<ContactResponse?>(null) }

    var showAddGroupDialog by remember { mutableStateOf(false) }
    var addGroupName by remember { mutableStateOf("") }
    var addingGroup by remember { mutableStateOf(false) }
    var groupToEdit by remember { mutableStateOf<ContactGroupResponse?>(null) }
    var groupToDelete by remember { mutableStateOf<ContactGroupResponse?>(null) }
    var groupToManageMembers by remember { mutableStateOf<ContactGroupResponse?>(null) }
    var groupToSend by remember { mutableStateOf<ContactGroupResponse?>(null) }

    var refresh by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    suspend fun loadContacts() {
        viewModel.applyStoredToken()
        when (val r = viewModel.api.getContacts()) {
            is ApiResult.Success -> { contacts = r.value; error = null }
            is ApiResult.NetworkError -> error = "NETWORK_ERROR"
            else -> error = "Ошибка загрузки"
        }
    }

    suspend fun loadGroups() {
        viewModel.applyStoredToken()
        when (val r = viewModel.api.getContactGroups()) {
            is ApiResult.Success -> { groups = r.value; error = null }
            is ApiResult.NetworkError -> error = "NETWORK_ERROR"
            else -> error = "Ошибка загрузки"
        }
    }

    suspend fun loadSims() {
        when (val r = viewModel.api.getMySims()) {
            is ApiResult.Success -> mySims = r.value
            else -> {}
        }
    }

    LaunchedEffect(Unit, refresh) {
        loadContacts()
        loadGroups()
        loadSims()
        loading = false
    }

    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.padding(24.dp))
        }
        return
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        if (error == "NETWORK_ERROR") {
            NetworkErrorView(onRetry = { refresh++ })
        } else {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(Res.string.contacts_tab), style = MaterialTheme.typography.titleLarge)
                    when (selectedTab) {
                        0 -> Button(onClick = { showAddContactDialog = true }) { Text(stringResource(Res.string.add)) }
                        else -> Button(onClick = { showAddGroupDialog = true }) { Text(stringResource(Res.string.create_group)) }
                    }
                }
                TabRow(selectedTabIndex = selectedTab, modifier = Modifier.fillMaxWidth()) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(stringResource(Res.string.contacts_tab)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(stringResource(Res.string.groups_tab)) }
                    )
                }
                if (error != null && error != "NETWORK_ERROR") {
                    Text(
                        error!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            when (selectedTab) {
                0 -> {
                    LazyColumn(Modifier.fillMaxSize().padding(8.dp)) {
                        items(contacts) { c ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { onOpenChat(c.phoneNumber) }
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f).padding(end = 8.dp)) {
                                        Text(
                                            c.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            c.phoneNumber,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                                        IconButton(onClick = { editContact = c }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Изменить")
                                        }
                                        IconButton(onClick = { contactToDelete = c }) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Удалить",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(Modifier.fillMaxSize().padding(8.dp)) {
                        items(groups) { g ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f).padding(end = 8.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Group,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                            )
                                            Text(
                                                g.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(start = 8.dp)
                                            )
                                        }
                                        Text(
                                            "Контактов: ${g.contactsCount}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                                        IconButton(onClick = { groupToSend = g }) {
                                            Icon(Icons.Default.Send, contentDescription = "Рассылка")
                                        }
                                        IconButton(onClick = { groupToManageMembers = g }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Состав группы")
                                        }
                                        IconButton(onClick = { groupToEdit = g }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Переименовать")
                                        }
                                        IconButton(onClick = { groupToDelete = g }) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Удалить",
                                                tint = MaterialTheme.colorScheme.error
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

    if (showAddContactDialog) {
        AlertDialog(
            onDismissRequest = { if (!addingContact) showAddContactDialog = false },
            title = { Text(stringResource(Res.string.new_contact)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = addName,
                        onValueChange = { addName = it },
                        label = { Text(stringResource(Res.string.name)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = addPhone,
                        onValueChange = { addPhone = it },
                        label = { Text(stringResource(Res.string.phone_number)) },
                        placeholder = { Text(stringResource(Res.string.phone_placeholder)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val name = addName.trim()
                        val phone = addPhone.trim()
                        if (name.isNotEmpty() && phone.isNotEmpty()) {
                            addingContact = true
                            scope.launch {
                                when (viewModel.api.createContact(name, phone)) {
                                    is ApiResult.Success -> {
                                        addName = ""; addPhone = ""
                                        loadContacts()
                                        showAddContactDialog = false
                                    }
                                    else -> { }
                                }
                                addingContact = false
                            }
                        }
                    }
                ) { Text(stringResource(Res.string.create)) }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddContactDialog = false }) { Text(stringResource(Res.string.cancel)) }
            }
        )
    }

    editContact?.let { c ->
        var editName by remember(c.id) { mutableStateOf(c.name) }
        var editPhone by remember(c.id) { mutableStateOf(c.phoneNumber) }
        AlertDialog(
            onDismissRequest = { editContact = null },
            title = { Text(stringResource(Res.string.edit_contact)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text(stringResource(Res.string.name)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = { Text(stringResource(Res.string.phone_number)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            when (viewModel.api.updateContact(c.id, editName.trim().ifBlank { null }, editPhone.trim().ifBlank { null })) {
                                is ApiResult.Success -> { editContact = null; refresh++ }
                                else -> { }
                            }
                        }
                    }
                ) { Text(stringResource(Res.string.save)) }
            },
            dismissButton = { TextButton(onClick = { editContact = null }) { Text(stringResource(Res.string.cancel)) } }
        )
    }

    contactToDelete?.let { c ->
        AlertDialog(
            onDismissRequest = { contactToDelete = null },
            title = { Text(stringResource(Res.string.delete_contact)) },
            text = { Text("${c.name} — ${c.phoneNumber}") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            when (viewModel.api.deleteContact(c.id)) {
                                is ApiResult.Success -> { contactToDelete = null; refresh++ }
                                else -> { }
                            }
                        }
                    }
                ) { Text(stringResource(Res.string.delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { contactToDelete = null }) { Text(stringResource(Res.string.cancel)) } }
        )
    }

    if (showAddGroupDialog) {
        AlertDialog(
            onDismissRequest = { if (!addingGroup) showAddGroupDialog = false },
            title = { Text(stringResource(Res.string.new_group)) },
            text = {
                OutlinedTextField(
                    value = addGroupName,
                    onValueChange = { addGroupName = it },
                    label = { Text(stringResource(Res.string.name)) },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val name = addGroupName.trim()
                        if (name.isNotEmpty()) {
                            addingGroup = true
                            scope.launch {
                                when (viewModel.api.createContactGroup(name)) {
                                    is ApiResult.Success -> {
                                        addGroupName = ""
                                        loadGroups()
                                        showAddGroupDialog = false
                                    }
                                    else -> { }
                                }
                                addingGroup = false
                            }
                        }
                    }
                ) { Text(stringResource(Res.string.create)) }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddGroupDialog = false }) { Text(stringResource(Res.string.cancel)) }
            }
        )
    }

    groupToEdit?.let { g ->
        var editName by remember(g.id) { mutableStateOf(g.name) }
        AlertDialog(
            onDismissRequest = { groupToEdit = null },
            title = { Text(stringResource(Res.string.rename_group)) },
            text = {
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text(stringResource(Res.string.name)) },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            when (viewModel.api.updateContactGroup(g.id, editName.trim().ifBlank { null })) {
                                is ApiResult.Success -> { groupToEdit = null; refresh++ }
                                else -> { }
                            }
                        }
                    }
                ) { Text(stringResource(Res.string.save)) }
            },
            dismissButton = { TextButton(onClick = { groupToEdit = null }) { Text(stringResource(Res.string.cancel)) } }
        )
    }

    groupToDelete?.let { g ->
        AlertDialog(
            onDismissRequest = { groupToDelete = null },
            title = { Text(stringResource(Res.string.delete_group)) },
            text = { Text("${g.name} (контактов: ${g.contactsCount})") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            when (viewModel.api.deleteContactGroup(g.id)) {
                                is ApiResult.Success -> { groupToDelete = null; refresh++ }
                                else -> { }
                            }
                        }
                    }
                ) { Text(stringResource(Res.string.delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { groupToDelete = null }) { Text(stringResource(Res.string.cancel)) } }
        )
    }

    groupToManageMembers?.let { g ->
        var selected by remember(g.id, contacts, groups) {
            mutableStateOf(g.contactIds.toSet())
        }
        AlertDialog(
            onDismissRequest = { groupToManageMembers = null },
            title = { Text(stringResource(Res.string.group_members_title, g.name)) },
            text = {
                Column(Modifier.fillMaxWidth()) {
                    if (contacts.isEmpty()) {
                        Text(stringResource(Res.string.group_members_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        LazyColumn(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                            items(contacts) { c ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selected = if (selected.contains(c.id)) selected - c.id else selected + c.id
                                        }
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = selected.contains(c.id),
                                        onCheckedChange = {
                                            selected = if (it) selected + c.id else selected - c.id
                                        }
                                    )
                                    Column(Modifier.padding(start = 8.dp)) {
                                        Text(c.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(
                                            c.phoneNumber,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            when (viewModel.api.setContactGroupMembers(g.id, selected.toList())) {
                                is ApiResult.Success -> { groupToManageMembers = null; refresh++ }
                                else -> { }
                            }
                        }
                    }
                ) { Text(stringResource(Res.string.save)) }
            },
            dismissButton = { TextButton(onClick = { groupToManageMembers = null }) { Text(stringResource(Res.string.cancel)) } }
        )
    }

    groupToSend?.let { g ->
        var text by remember { mutableStateOf("") }
        var sending by remember { mutableStateOf(false) }
        var sendError by remember { mutableStateOf<String?>(null) }
        var selectedSimId by remember { mutableStateOf<Int?>(mySims.firstOrNull()?.id) }
        var simDropdownExpanded by remember { mutableStateOf(false) }
        
        AlertDialog(
            onDismissRequest = { if (!sending) groupToSend = null },
            title = { Text(stringResource(Res.string.group_mass_send, g.name)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(Res.string.group_mass_send_text), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    if (mySims.isNotEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { simDropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val selectedSim = mySims.find { it.id == selectedSimId }
                                val label = selectedSim?.let { 
                                    if (!it.label.isNullOrBlank()) "${it.label} (${it.phoneNumber ?: stringResource(Res.string.port_num, it.portNumber.toString())})"
                                    else it.phoneNumber ?: stringResource(Res.string.port_num, it.portNumber.toString())
                                } ?: stringResource(Res.string.auto_random)
                                Text(label)
                            }
                            DropdownMenu(
                                expanded = simDropdownExpanded,
                                onDismissRequest = { simDropdownExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.8f)
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(Res.string.auto_random)) },
                                    onClick = {
                                        selectedSimId = null
                                        simDropdownExpanded = false
                                    }
                                )
                                mySims.forEach { sim ->
                                    val simLabel = if (!sim.label.isNullOrBlank()) "${sim.label} (${sim.phoneNumber ?: stringResource(Res.string.port_num, sim.portNumber.toString())})" else sim.phoneNumber ?: stringResource(Res.string.port_num, sim.portNumber.toString())
                                    DropdownMenuItem(
                                        text = { Text(simLabel) },
                                        onClick = {
                                            selectedSimId = sim.id
                                            simDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text(stringResource(Res.string.text_field)) },
                        modifier = Modifier.fillMaxWidth().height(120.dp)
                    )
                    if (sendError != null) {
                        Text(sendError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !sending && text.isNotBlank(),
                    onClick = {
                        sending = true
                        sendError = null
                        scope.launch {
                            when (val r = viewModel.api.sendGroupMessage(g.id, text.trim(), selectedSimId)) {
                                is ApiResult.Success -> {
                                    groupToSend = null
                                    text = ""
                                }
                                is ApiResult.NetworkError -> sendError = r.message
                                else -> sendError = "Error"
                            }
                            sending = false
                        }
                    }
                ) { Text(stringResource(Res.string.send)) }
            },
            dismissButton = { TextButton(onClick = { groupToSend = null }) { Text(stringResource(Res.string.cancel)) } }
        )
    }
}
}
