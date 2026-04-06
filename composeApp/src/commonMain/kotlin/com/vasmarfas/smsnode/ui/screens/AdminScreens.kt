package com.vasmarfas.smsnode.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vasmarfas.smsnode.data.api.ApiResult
import com.vasmarfas.smsnode.data.models.DiscoveredChannel
import com.vasmarfas.smsnode.data.models.DiscoveredSimAddRequest
import com.vasmarfas.smsnode.data.models.GatewayCreateRequest
import com.vasmarfas.smsnode.data.models.GatewayResponse
import com.vasmarfas.smsnode.data.models.GatewayUpdateRequest
import com.vasmarfas.smsnode.data.models.SimCardCreateRequest
import com.vasmarfas.smsnode.data.models.SimCardResponse
import com.vasmarfas.smsnode.data.models.MessageResponse
import com.vasmarfas.smsnode.data.models.PendingRegistrationResponse
import com.vasmarfas.smsnode.data.models.UserCreateRequest
import com.vasmarfas.smsnode.data.models.UserResponse
import com.vasmarfas.smsnode.data.models.UserUpdateRequest
import com.vasmarfas.smsnode.data.models.AdminMessageResponse
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import com.vasmarfas.smsnode.ui.viewmodel.AppViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import smsnode.composeapp.generated.resources.*

@Composable
fun AdminMenuScreen(
    onNavigateTo: (String) -> Unit,
    onBack: () -> Unit,
) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                    Text(stringResource(Res.string.admin_tab), style = MaterialTheme.typography.titleLarge)
                }
            }
            listOf(
                stringResource(Res.string.admin_gateways) to "admin/gateways",
                stringResource(Res.string.admin_users) to "admin/users",
                stringResource(Res.string.admin_pending) to "admin/pending",
                stringResource(Res.string.admin_regmode) to "admin/regmode",
                stringResource(Res.string.admin_messages) to "admin/messages",
            ).forEach { (label, route) ->
                Card(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onNavigateTo(route) }
                ) {
                    Text(label, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

private val GATEWAY_TYPES = listOf("goip_udp", "goip_http", "skyline")
private val USER_ROLES = listOf("user", "admin")
private val REG_MODES = listOf(
    "open" to "Открытая (свободная)", 
    "closed" to "Закрытая", 
    "semi_open" to "По заявкам"
)

@Composable
fun AdminGatewaysScreen(viewModel: AppViewModel, onBack: () -> Unit, onOpenGatewayDetail: (Int) -> Unit = {}) {
    var list by remember { mutableStateOf<List<GatewayResponse>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableStateOf(0) }
    var showCreate by remember { mutableStateOf(false) }
    var editGateway by remember { mutableStateOf<GatewayResponse?>(null) }
    var deleteGateway by remember { mutableStateOf<GatewayResponse?>(null) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    var testResult by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(refresh) {
        viewModel.applyStoredToken()
        when (val r = viewModel.api.getGateways()) {
            is ApiResult.Success -> { list = r.value; error = null }
            is ApiResult.Forbidden -> error = r.message
            else -> error = "Ошибка загрузки"
        }
        loading = false
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                    Text(stringResource(Res.string.admin_gateways_title), style = MaterialTheme.typography.titleLarge)
                }
                Button(onClick = { showCreate = true }) { Text(stringResource(Res.string.add)) }
            }
            testResult?.let { Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(4.dp)) }
            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
                LazyColumn(Modifier.fillMaxSize()) {
                    items(list) { g ->
                        Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Column(Modifier.padding(12.dp)) {
                                Text(g.name, style = MaterialTheme.typography.titleMedium)
                                Text("${g.type} @ ${g.host}:${g.port}", style = MaterialTheme.typography.bodySmall)
                                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TextButton(onClick = { onOpenGatewayDetail(g.id) }) { Text(stringResource(Res.string.sims_tab)) }
                                    TextButton(onClick = {
                                        scope.launch {
                                            testResult = null
                                            when (val r = viewModel.api.testGateway(g.id)) {
                                                is ApiResult.Success -> testResult = if (r.value.online) "Онлайн: ${r.value.detail}" else "Офлайн: ${r.value.detail}"
                                                else -> testResult = "Ошибка"
                                            }
                                        }
                                    }) { Text(stringResource(Res.string.test)) }
                                    IconButton(onClick = { editGateway = g }) { Icon(Icons.Default.Edit, contentDescription = stringResource(Res.string.edit)) }
                                    IconButton(onClick = { deleteGateway = g }) { Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.delete), tint = MaterialTheme.colorScheme.error) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreate) GatewayCreateDialog(
        onDismiss = { showCreate = false },
        onCreate = { name, type, host, port, username, password ->
            scope.launch {
                when (viewModel.api.createGateway(GatewayCreateRequest(name, type, host, port, username, password))) {
                    is ApiResult.Success -> { showCreate = false; refresh++ }
                    else -> { }
                }
            }
        }
    )
    editGateway?.let { g ->
        GatewayEditDialog(
            gateway = g,
            onDismiss = { editGateway = null },
            onSave = { name, host, port, username, password, isActive ->
                scope.launch {
                    when (viewModel.api.updateGateway(g.id, GatewayUpdateRequest(name, host, port, username, password, isActive))) {
                        is ApiResult.Success -> { editGateway = null; refresh++ }
                        else -> { }
                    }
                }
            }
        )
    }
    deleteGateway?.let { g ->
        AlertDialog(
            onDismissRequest = { 
                deleteGateway = null
                deleteError = null
            },
            title = { Text(stringResource(Res.string.delete_gateway)) },
            text = { 
                Column {
                    Text("${g.name} (${g.host}:${g.port})")
                    if (deleteError != null) {
                        Text(
                            text = deleteError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val force = deleteError != null
                        when (val r = viewModel.api.deleteGateway(g.id, force = force)) {
                            is ApiResult.Success -> { 
                                deleteGateway = null
                                deleteError = null
                                refresh++ 
                            }
                            is ApiResult.Conflict -> {
                                deleteError = r.message
                            }
                            else -> {
                                deleteError = "Ошибка при удалении"
                            }
                        }
                    }
                }) { 
                    Text(stringResource(Res.string.delete), color = MaterialTheme.colorScheme.error) 
                }
            },
            dismissButton = { 
                TextButton(onClick = { 
                    deleteGateway = null
                    deleteError = null
                }) { Text(stringResource(Res.string.cancel)) } 
            }
        )
    }
}

@Composable
private fun GatewayCreateDialog(onDismiss: () -> Unit, onCreate: (String, String, String, Int, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("goip_udp") }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("9991") }
    var username by remember { mutableStateOf("admin") }
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.new_gateway)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(Res.string.name)) }, modifier = Modifier.fillMaxWidth())
                Text(stringResource(Res.string.type), style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    GATEWAY_TYPES.forEach { t ->
                        OutlinedButton(onClick = { type = t }) { Text(t, style = MaterialTheme.typography.labelSmall) }
                    }
                }
                OutlinedTextField(value = host, onValueChange = { host = it }, label = { Text(stringResource(Res.string.host)) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = port, onValueChange = { port = it }, label = { Text(stringResource(Res.string.port)) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text(stringResource(Res.string.username)) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text(stringResource(Res.string.password)) }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { Button(onClick = { onCreate(name, type, host, port.toIntOrNull() ?: 9991, username, password) }) { Text(stringResource(Res.string.create)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } }
    )
}

@Composable
private fun GatewayEditDialog(gateway: GatewayResponse, onDismiss: () -> Unit, onSave: (String?, String?, Int?, String?, String?, Boolean?) -> Unit) {
    var name by remember { mutableStateOf(gateway.name) }
    var host by remember { mutableStateOf(gateway.host) }
    var port by remember { mutableStateOf(gateway.port.toString()) }
    var username by remember { mutableStateOf(gateway.username) }
    var password by remember { mutableStateOf("") }
    var isActive by remember { mutableStateOf(gateway.isActive) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.edit_gateway)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(Res.string.name)) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = host, onValueChange = { host = it }, label = { Text(stringResource(Res.string.host)) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = port, onValueChange = { port = it }, label = { Text(stringResource(Res.string.port)) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text(stringResource(Res.string.username)) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text(stringResource(Res.string.new_password_hint)) }, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(Res.string.is_active), modifier = Modifier.padding(end = 8.dp))
                    Checkbox(checked = isActive, onCheckedChange = { isActive = it })
                }
            }
        },
        confirmButton = { Button(onClick = { onSave(name, host, port.toIntOrNull(), username, password.ifBlank { null }, isActive) }) { Text(stringResource(Res.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } }
    )
}

@Composable
fun AdminGatewayDetailScreen(gatewayId: Int, viewModel: AppViewModel, onBack: () -> Unit) {
    var gateway by remember { mutableStateOf<GatewayResponse?>(null) }
    var sims by remember { mutableStateOf<List<SimCardResponse>>(emptyList()) }
    var discovered by remember { mutableStateOf<List<DiscoveredChannel>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableStateOf(0) }
    var showAddSim by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var editGateway by remember { mutableStateOf<GatewayResponse?>(null) }
    var deleteGateway by remember { mutableStateOf<GatewayResponse?>(null) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(gatewayId, refresh) {
        viewModel.applyStoredToken()
        loading = true
        when (val r = viewModel.api.getGateway(gatewayId)) {
            is ApiResult.Success -> gateway = r.value
            is ApiResult.Forbidden -> error = r.message
            else -> error = "Шлюз не найден"
        }
        when (val r = viewModel.api.getGatewaySims(gatewayId)) {
            is ApiResult.Success -> sims = r.value
            else -> { }
        }
        when (val r = viewModel.api.getDiscoveredSims(10)) {
            is ApiResult.Success -> discovered = r.value.filter { it.gatewayId == gatewayId }
            else -> { }
        }
        loading = false
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                    Text(gateway?.name ?: "Gateway #$gatewayId", style = MaterialTheme.typography.titleLarge)
                }
            }
            gateway?.let { g ->
                Text("${g.type} @ ${g.host}:${g.port}", style = MaterialTheme.typography.bodyMedium)
                testResult?.let { Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(4.dp)) }
                Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = {
                        scope.launch {
                            testResult = null
                            when (val r = viewModel.api.testGateway(g.id)) {
                                is ApiResult.Success -> testResult = if (r.value.online) "Онлайн: ${r.value.detail}" else "Офлайн: ${r.value.detail}"
                                else -> testResult = "Ошибка"
                            }
                        }
                    }) { Text(stringResource(Res.string.test)) }
                    IconButton(onClick = { editGateway = g }) { Icon(Icons.Default.Edit, contentDescription = stringResource(Res.string.edit)) }
                    IconButton(onClick = { deleteGateway = g }) { Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.delete), tint = MaterialTheme.colorScheme.error) }
                }
            }
            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Text(stringResource(Res.string.gateway_sims), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                Button(onClick = { showAddSim = true }, modifier = Modifier.padding(vertical = 4.dp)) { Text(stringResource(Res.string.add_sim)) }
                LazyColumn(Modifier.weight(1f)) {
                    items(sims) { sim ->
                        Card(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                val simTitle = if (!sim.label.isNullOrBlank()) "${sim.label} (${sim.phoneNumber ?: "—"})" else sim.phoneNumber ?: "—"
                                Text(stringResource(Res.string.port_num, sim.portNumber.toString()) + " — $simTitle (${sim.status})", style = MaterialTheme.typography.bodyMedium)
                                if (sim.assignedUserId != null) Text("→ user ${sim.assignedUserId}", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    item {
                        Text(stringResource(Res.string.discovered_channels), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
                    }
                    items(discovered) { ch ->
                        Card(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(stringResource(Res.string.port_num, ch.portNumber.toString()) + " — ${ch.phoneNumber ?: "—"}", style = MaterialTheme.typography.bodyMedium)
                                    ch.gsmStatus?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
                                }
                                if (ch.canAdd) {
                                    TextButton(onClick = {
                                        scope.launch {
                                            when (viewModel.api.addDiscoveredSim(DiscoveredSimAddRequest(gatewayId, ch.portNumber, ch.phoneNumber))) {
                                                is ApiResult.Success -> refresh++
                                                else -> { }
                                            }
                                        }
                                    }) { Text(stringResource(Res.string.add)) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddSim) AddSimDialog(
        onDismiss = { showAddSim = false },
        onAdd = { port, phone ->
            scope.launch {
                when (viewModel.api.addGatewaySim(gatewayId, port, phone.ifBlank { null })) {
                    is ApiResult.Success -> { showAddSim = false; refresh++ }
                    else -> { }
                }
            }
        }
    )
    editGateway?.let { g ->
        GatewayEditDialog(
            gateway = g,
            onDismiss = { editGateway = null },
            onSave = { name, host, port, username, password, isActive ->
                scope.launch {
                    when (viewModel.api.updateGateway(g.id, GatewayUpdateRequest(name, host, port, username, password, isActive))) {
                        is ApiResult.Success -> { editGateway = null; refresh++ }
                        else -> { }
                    }
                }
            }
        )
    }
    deleteGateway?.let { g ->
        AlertDialog(
            onDismissRequest = { 
                deleteGateway = null
                deleteError = null
            },
            title = { Text(stringResource(Res.string.delete_gateway)) },
            text = { 
                Column {
                    Text("${g.name} (${g.host}:${g.port})")
                    if (deleteError != null) {
                        Text(
                            text = deleteError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val force = deleteError != null
                        when (val r = viewModel.api.deleteGateway(g.id, force = force)) {
                            is ApiResult.Success -> { 
                                deleteGateway = null
                                deleteError = null
                                onBack() 
                            }
                            is ApiResult.Conflict -> {
                                deleteError = r.message
                            }
                            else -> {
                                deleteError = "Ошибка при удалении"
                            }
                        }
                    }
                }) { 
                    Text(stringResource(Res.string.delete), color = MaterialTheme.colorScheme.error) 
                }
            },
            dismissButton = { 
                TextButton(onClick = { 
                    deleteGateway = null
                    deleteError = null
                }) { Text(stringResource(Res.string.cancel)) } 
            }
        )
    }
}

@Composable
private fun AddSimDialog(onDismiss: () -> Unit, onAdd: (Int, String) -> Unit) {
    var port by remember { mutableStateOf("1") }
    var phone by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.add_sim)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = port, onValueChange = { port = it }, label = { Text(stringResource(Res.string.port_number_hint)) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text(stringResource(Res.string.phone_optional_hint)) }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { Button(onClick = { onAdd(port.toIntOrNull() ?: 1, phone) }) { Text(stringResource(Res.string.add)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } }
    )
}

@Composable
fun AdminUserDetailScreen(userId: Int, viewModel: AppViewModel, onBack: () -> Unit) {
    var user by remember { mutableStateOf<UserResponse?>(null) }
    var userSims by remember { mutableStateOf<List<SimCardResponse>>(emptyList()) }
    var allSimsByGateway by remember { mutableStateOf<List<Pair<GatewayResponse, SimCardResponse>>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableStateOf(0) }
    var showAssignSim by remember { mutableStateOf(false) }
    var editUser by remember { mutableStateOf<UserResponse?>(null) }
    var deactivateUser by remember { mutableStateOf<UserResponse?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(userId, refresh) {
        viewModel.applyStoredToken()
        loading = true
        when (val r = viewModel.api.getUser(userId)) {
            is ApiResult.Success -> user = r.value
            is ApiResult.Forbidden -> error = r.message
            else -> error = "Пользователь не найден"
        }
        when (val r = viewModel.api.getUserSims(userId)) {
            is ApiResult.Success -> userSims = r.value
            else -> { }
        }
        when (val r = viewModel.api.getGateways()) {
            is ApiResult.Success -> {
                val gateways = r.value
                val acc = mutableListOf<Pair<GatewayResponse, SimCardResponse>>()
                gateways.forEach { gw ->
                    when (val sr = viewModel.api.getGatewaySims(gw.id)) {
                        is ApiResult.Success -> sr.value.forEach { sim -> acc.add(gw to sim) }
                        else -> { }
                    }
                }
                allSimsByGateway = acc
            }
            else -> { }
        }
        loading = false
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                    Text(user?.username ?: "User #$userId", style = MaterialTheme.typography.titleLarge)
                }
            }
            user?.let { u ->
                Text("${u.role}, active=${u.isActive}", style = MaterialTheme.typography.bodyMedium)
                Column(Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(onClick = { editUser = u }) { Text(stringResource(Res.string.edit)) }
                    if (u.isActive) TextButton(onClick = { deactivateUser = u }) { Text(stringResource(Res.string.deactivate)) }
                }
            }
            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Text(stringResource(Res.string.assigned_numbers), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                Button(onClick = { showAssignSim = true }, modifier = Modifier.padding(vertical = 4.dp)) { Text(stringResource(Res.string.assign_sim)) }
                LazyColumn(Modifier.weight(1f)) {
                    items(userSims) { sim ->
                        Card(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                val simTitle = if (!sim.label.isNullOrBlank()) "${sim.label} (${sim.phoneNumber ?: "—"})" else sim.phoneNumber ?: "—"
                                Text(stringResource(Res.string.port_num, sim.portNumber.toString()) + " — $simTitle (${sim.status})", style = MaterialTheme.typography.bodyMedium)
                                TextButton(onClick = {
                                    scope.launch {
                                        when (viewModel.api.revokeSimFromUser(userId, sim.id)) {
                                            is ApiResult.Success -> refresh++
                                            else -> { }
                                        }
                                    }
                                }) { Text(stringResource(Res.string.revoke), color = MaterialTheme.colorScheme.error) }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAssignSim) {
        val availableSims = allSimsByGateway.filter { (_, sim) -> sim.assignedUserId == null }
        AssignSimDialog(
            simsWithGateway = availableSims,
            onDismiss = { showAssignSim = false },
            onAssign = { simId ->
                scope.launch {
                    when (viewModel.api.assignSimToUser(userId, simId)) {
                        is ApiResult.Success -> { showAssignSim = false; refresh++ }
                        else -> { }
                    }
                }
            }
        )
    }
    editUser?.let { u ->
        UserEditDialog(
            user = u,
            onDismiss = { editUser = null },
            onSave = { password, role, tgId, isActive ->
                scope.launch {
                    when (viewModel.api.updateUser(u.id, UserUpdateRequest(password, role, tgId, isActive))) {
                        is ApiResult.Success -> { editUser = null; refresh++ }
                        else -> { }
                    }
                }
            }
        )
    }
    deactivateUser?.let { u ->
        AlertDialog(
            onDismissRequest = { deactivateUser = null },
            title = { Text(stringResource(Res.string.deactivate_user_title)) },
            text = { Text(stringResource(Res.string.deactivate_user_text, u.username)) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        when (viewModel.api.deactivateUser(u.id)) {
                            is ApiResult.Success -> { deactivateUser = null; refresh++; onBack() }
                            else -> { }
                        }
                    }
                }) { Text(stringResource(Res.string.deactivate), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deactivateUser = null }) { Text(stringResource(Res.string.cancel)) } }
        )
    }
}

@Composable
private fun AssignSimDialog(
    simsWithGateway: List<Pair<GatewayResponse, SimCardResponse>>,
    onDismiss: () -> Unit,
    onAssign: (Int) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.assign_sim)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (simsWithGateway.isEmpty()) {
                    Text(stringResource(Res.string.no_free_sims), style = MaterialTheme.typography.bodyMedium)
                } else {
                    simsWithGateway.forEach { (gw, sim) ->
                        Row(Modifier.fillMaxWidth().clickable { onAssign(sim.id) }, horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("${gw.name}: порт ${sim.portNumber} — ${sim.phoneNumber ?: "—"}", style = MaterialTheme.typography.bodyMedium)
                            TextButton(onClick = { onAssign(sim.id) }) { Text(stringResource(Res.string.assign)) }
                        }
                    }
                }
            }
        },
        confirmButton = { },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.close)) } }
    )
}

@Composable
fun AdminUsersScreen(viewModel: AppViewModel, onBack: () -> Unit, onOpenUserDetail: (Int) -> Unit = {}) {
    var list by remember { mutableStateOf<List<UserResponse>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableStateOf(0) }
    var showCreate by remember { mutableStateOf(false) }
    var editUser by remember { mutableStateOf<UserResponse?>(null) }
    var deactivateUser by remember { mutableStateOf<UserResponse?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(refresh) {
        viewModel.applyStoredToken()
        when (val r = viewModel.api.getUsers()) {
            is ApiResult.Success -> { list = r.value; error = null }
            is ApiResult.Forbidden -> error = r.message
            else -> error = "Ошибка загрузки"
        }
        loading = false
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                    Text(stringResource(Res.string.admin_users_title), style = MaterialTheme.typography.titleLarge)
                }
                Button(onClick = { showCreate = true }) { Text(stringResource(Res.string.add)) }
            }
            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
                LazyColumn(Modifier.fillMaxSize()) {
                    items(list) { u ->
                        Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text("${u.username} (${u.role})", style = MaterialTheme.typography.titleMedium)
                                    val tgIdText = if (u.telegramId != null) "tg_id=${u.telegramId}" else ""
                                    Text("id=${u.id} active=${u.isActive} $tgIdText", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                                    IconButton(onClick = { onOpenUserDetail(u.id) }) { Icon(Icons.Default.Info, contentDescription = stringResource(Res.string.sims_tab), tint = MaterialTheme.colorScheme.primary) }
                                    IconButton(onClick = { editUser = u }) { Icon(Icons.Default.Edit, contentDescription = stringResource(Res.string.edit)) }
                                    if (u.isActive) IconButton(onClick = { deactivateUser = u }) { Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.deactivate), tint = MaterialTheme.colorScheme.error) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreate) UserCreateDialog(
        onDismiss = { showCreate = false },
        onCreate = { username, password, role ->
            scope.launch {
                when (viewModel.api.createUser(UserCreateRequest(username, password, role))) {
                    is ApiResult.Success -> { showCreate = false; refresh++ }
                    else -> { }
                }
            }
        }
    )
    editUser?.let { u ->
        UserEditDialog(
            user = u,
            onDismiss = { editUser = null },
            onSave = { password, role, tgId, isActive ->
                scope.launch {
                    when (viewModel.api.updateUser(u.id, UserUpdateRequest(password, role, tgId, isActive))) {
                        is ApiResult.Success -> { editUser = null; refresh++ }
                        else -> { }
                    }
                }
            }
        )
    }
    deactivateUser?.let { u ->
        AlertDialog(
            onDismissRequest = { deactivateUser = null },
            title = { Text(stringResource(Res.string.deactivate_user_title)) },
            text = { Text(stringResource(Res.string.deactivate_user_text, u.username)) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        when (viewModel.api.deactivateUser(u.id)) {
                            is ApiResult.Success -> { deactivateUser = null; refresh++ }
                            else -> { }
                        }
                    }
                }) { Text(stringResource(Res.string.deactivate), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deactivateUser = null }) { Text(stringResource(Res.string.cancel)) } }
        )
    }
}

@Composable
private fun UserCreateDialog(onDismiss: () -> Unit, onCreate: (String, String, String) -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("user") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.new_user)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text(stringResource(Res.string.username)) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text(stringResource(Res.string.password)) }, modifier = Modifier.fillMaxWidth())
                Text(stringResource(Res.string.role), style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    USER_ROLES.forEach { r ->
                        OutlinedButton(onClick = { role = r }) { Text(r) }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onCreate(username, password, role) }) { Text(stringResource(Res.string.create)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } }
    )
}

@Composable
private fun UserEditDialog(user: UserResponse, onDismiss: () -> Unit, onSave: (String?, String?, Long?, Boolean?) -> Unit) {
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(user.role) }
    var isActive by remember { mutableStateOf(user.isActive) }
    var tgIdStr by remember { mutableStateOf(user.telegramId?.toString() ?: "") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.edit_user)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text(stringResource(Res.string.new_password_hint)) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = tgIdStr, onValueChange = { tgIdStr = it }, label = { Text(stringResource(Res.string.tg_id_hint)) }, modifier = Modifier.fillMaxWidth())
                Text(stringResource(Res.string.role), style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    USER_ROLES.forEach { r ->
                        OutlinedButton(onClick = { role = r }) { Text(r) }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(Res.string.is_active), modifier = Modifier.padding(end = 8.dp))
                    Checkbox(checked = isActive, onCheckedChange = { isActive = it })
                }
            }
        },
        confirmButton = { Button(onClick = { 
            val tgId = tgIdStr.toLongOrNull() ?: if (tgIdStr.isBlank()) 0L else user.telegramId
            onSave(password.ifBlank { null }, role, tgId, isActive) 
        }) { Text(stringResource(Res.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } }
    )
}

@Composable
fun AdminPendingScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    var list by remember { mutableStateOf<List<PendingRegistrationResponse>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(refresh) {
        viewModel.applyStoredToken()
        when (val r = viewModel.api.getPendingRegistrations()) {
            is ApiResult.Success -> { list = r.value; error = null }
            is ApiResult.Forbidden -> error = r.message
            else -> error = "Ошибка загрузки"
        }
        loading = false
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                    Text(stringResource(Res.string.admin_pending_title), style = MaterialTheme.typography.titleLarge)
                }
            }
            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
                if (list.isEmpty()) Text(stringResource(Res.string.no_pending_requests), modifier = Modifier.padding(8.dp))
                LazyColumn(Modifier.fillMaxSize()) {
                    items(list) { p ->
                        Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("${p.username} (${p.source})", style = MaterialTheme.typography.titleMedium)
                                    Text("id=${p.id} ${p.createdAt}", style = MaterialTheme.typography.bodySmall)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = {
                                        scope.launch {
                                            when (viewModel.api.approvePendingRegistration(p.id)) {
                                                is ApiResult.Success -> refresh++
                                                else -> { }
                                            }
                                        }
                                    }) { Text(stringResource(Res.string.approve)) }
                                    OutlinedButton(onClick = {
                                        scope.launch {
                                            when (viewModel.api.rejectPendingRegistration(p.id)) {
                                                is ApiResult.Success -> refresh++
                                                else -> { }
                                            }
                                        }
                                    }) { Text(stringResource(Res.string.reject)) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminRegModeScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    var mode by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(refresh) {
        viewModel.applyStoredToken()
        when (val r = viewModel.api.getRegistrationMode()) {
            is ApiResult.Success -> { mode = r.value.mode; error = null }
            is ApiResult.Forbidden -> error = r.message
            else -> error = "Ошибка загрузки"
        }
        loading = false
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                    Text(stringResource(Res.string.admin_regmode_title), style = MaterialTheme.typography.titleLarge)
                }
            }
            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
                mode?.let { 
                    val humanMode = REG_MODES.find { m -> m.first == it }?.second ?: it
                    Text(stringResource(Res.string.current_mode, humanMode), modifier = Modifier.padding(8.dp)) 
                }
                Column(Modifier.padding(8.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    REG_MODES.forEach { (mId, mDesc) ->
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                scope.launch {
                                    when (viewModel.api.setRegistrationMode(mId)) {
                                        is ApiResult.Success -> refresh++
                                        else -> { }
                                    }
                                }
                            }
                        ) { Text(mDesc) }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminMessagesScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    var list by remember { mutableStateOf<List<AdminMessageResponse>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        viewModel.applyStoredToken()
        when (val r = viewModel.api.getAdminMessages(limit = 100)) {
            is ApiResult.Success -> { list = r.value; error = null }
            is ApiResult.Forbidden -> error = r.message
            else -> error = "Ошибка загрузки"
        }
        loading = false
    }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                    Text(stringResource(Res.string.admin_messages_title), style = MaterialTheme.typography.titleLarge)
                }
            }
            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
                LazyColumn(Modifier.fillMaxSize()) {
                    items(list) { m ->
                        Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Column(Modifier.padding(12.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(
                                        text = if (m.direction == "in") "Входящее: ${m.externalPhone}" else "Исходящее: ${m.externalPhone}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (m.direction == "in") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                    )
                                    if (m.createdAt != null) {
                                        Text(m.createdAt.take(19).replace("T", " "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text("User: ${m.username ?: "—"} | SIM: ${m.simCardLabel ?: "—"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(8.dp))
                                Text(m.text, style = MaterialTheme.typography.bodyMedium, maxLines = 3)
                            }
                        }
                    }
                }
            }
        }
    }
}
