package com.vasmarfas.smsnode.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vasmarfas.smsnode.data.models.TemplateResponse
import com.vasmarfas.smsnode.ui.viewmodel.AppViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import smsnode.composeapp.generated.resources.*

@Composable
fun TemplatesScreen(viewModel: AppViewModel, isAdmin: Boolean = false) {
    val templates by viewModel.templates.collectAsState()
    val isLoading by viewModel.templatesLoading.collectAsState()
    val scope = rememberCoroutineScope()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var templateToEdit by remember { mutableStateOf<TemplateResponse?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadTemplates()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(Res.string.templates_tab), style = MaterialTheme.typography.titleLarge)
                Button(onClick = { showAddDialog = true }) {
                    Text(stringResource(Res.string.create))
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (isLoading && templates.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (templates.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.no_templates),
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(templates) { tmpl ->
                            TemplateItem(
                                template = tmpl,
                                isAdmin = isAdmin,
                                onEdit = { templateToEdit = tmpl },
                                onDelete = {
                                    scope.launch { viewModel.deleteTemplate(tmpl.id) }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        TemplateDialog(
            isAdmin = isAdmin,
            initialTemplate = null,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, content, isGlobal ->
                showAddDialog = false
                scope.launch {
                    viewModel.createTemplate(name, content, isGlobal)
                }
            }
        )
    }

    templateToEdit?.let { tmpl ->
        TemplateDialog(
            isAdmin = isAdmin,
            initialTemplate = tmpl,
            onDismiss = { templateToEdit = null },
            onConfirm = { name, content, isGlobal ->
                templateToEdit = null
                scope.launch {
                    viewModel.updateTemplate(tmpl.id, name, content, isGlobal)
                }
            }
        )
    }
}

@Composable
private fun TemplateItem(template: TemplateResponse, isAdmin: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = template.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (template.isGlobal) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = stringResource(Res.string.global_template),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = template.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (!template.isGlobal || isAdmin) {
                Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = stringResource(Res.string.edit),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(Res.string.delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TemplateDialog(
    isAdmin: Boolean,
    initialTemplate: TemplateResponse?,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Boolean) -> Unit
) {
    var name by remember { mutableStateOf(initialTemplate?.name ?: "") }
    var content by remember { mutableStateOf(initialTemplate?.content ?: "") }
    var isGlobal by remember { mutableStateOf(initialTemplate?.isGlobal ?: false) }

    val isEdit = initialTemplate != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) stringResource(Res.string.edit_template) else stringResource(Res.string.new_template)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(Res.string.name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text(stringResource(Res.string.template_content)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                if (isAdmin) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(checked = isGlobal, onCheckedChange = { isGlobal = it })
                        Text(stringResource(Res.string.global_template))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, content, isGlobal) },
                enabled = name.isNotBlank() && content.isNotBlank()
            ) {
                Text(if (isEdit) stringResource(Res.string.save) else stringResource(Res.string.create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
        }
    )
}
