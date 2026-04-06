package com.vasmarfas.smsnode.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.vasmarfas.smsnode.ui.viewmodel.AppViewModel
import com.vasmarfas.smsnode.ui.viewmodel.RegisterUiResult

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import org.jetbrains.compose.resources.stringResource
import smsnode.composeapp.generated.resources.*

@Composable
fun RegisterScreen(
    viewModel: AppViewModel,
    registerResult: RegisterUiResult?,
    onClearRegisterResult: () -> Unit,
    onRegisterCreated: () -> Unit,
    onBack: () -> Unit,
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    when (registerResult) {
        is RegisterUiResult.Created -> onRegisterCreated()
        else -> { }
    }

    val baseUrl by viewModel.baseUrl.collectAsState()

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        Text(stringResource(Res.string.registration), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = baseUrl,
            onValueChange = { viewModel.setBaseUrl(it) },
            label = { Text(stringResource(Res.string.server_address)) },
            placeholder = { Text(stringResource(Res.string.server_address_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text(stringResource(Res.string.username_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(Res.string.password_hint)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )

        when (registerResult) {
            is RegisterUiResult.Pending -> {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = registerResult.message, 
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            is RegisterUiResult.Error -> {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = registerResult.message, 
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            else -> { }
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                onClearRegisterResult()
                viewModel.register(username, password)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = username.length in 3..50 && password.length >= 6
        ) {
            Text(stringResource(Res.string.register_btn))
        }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onBack) {
            Text(stringResource(Res.string.back))
        }
    }
}
}
