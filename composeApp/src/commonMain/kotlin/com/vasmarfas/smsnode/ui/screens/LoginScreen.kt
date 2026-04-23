package com.vasmarfas.smsnode.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.vasmarfas.smsnode.getPlatform
import com.vasmarfas.smsnode.ui.components.NetworkErrorView
import com.vasmarfas.smsnode.ui.viewmodel.AppViewModel
import com.vasmarfas.smsnode.ui.viewmodel.LoginResult
import org.jetbrains.compose.resources.stringResource
import smsnode.composeapp.generated.resources.Res
import smsnode.composeapp.generated.resources.fill_review_demo_btn
import smsnode.composeapp.generated.resources.login_btn
import smsnode.composeapp.generated.resources.login_title
import smsnode.composeapp.generated.resources.password
import smsnode.composeapp.generated.resources.privacy_policy
import smsnode.composeapp.generated.resources.register_btn
import smsnode.composeapp.generated.resources.server_address
import smsnode.composeapp.generated.resources.server_address_hint
import smsnode.composeapp.generated.resources.terms_of_use
import smsnode.composeapp.generated.resources.username

private const val REVIEW_DEMO_URL = "https://demo.smsnode.vasmarfas.com"
private const val REVIEW_DEMO_USERNAME = "demo"
private const val REVIEW_DEMO_PASSWORD = "demo"


private const val PRIVACY_POLICY_URL = "https://vasmarfas.github.io/SMSNode/privacy.html"
private const val TERMS_OF_USE_URL = "https://vasmarfas.github.io/SMSNode/terms.html"


@Composable
fun LoginScreen(
    viewModel: AppViewModel,
    loginResult: LoginResult?,
    onClearLoginResult: () -> Unit,
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    if (loginResult != null && loading) {
        loading = false
    }

    if (loginResult == LoginResult.Success) {
        onLoginSuccess()
        return
    }

    val baseUrl by viewModel.baseUrl.collectAsState()

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        if (loginResult is LoginResult.Error && loginResult.message == "NETWORK_ERROR") {
            NetworkErrorView(
                onRetry = {
                    onClearLoginResult()
                    loading = true
                    viewModel.login(username, password)
                }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(Res.string.login_title),
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(Modifier.height(24.dp))

                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { viewModel.setBaseUrl(it) },
                    label = { Text(stringResource(Res.string.server_address)) },
                    placeholder = { Text(stringResource(Res.string.server_address_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(stringResource(Res.string.username)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(Res.string.password)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (username.isNotBlank() && password.isNotBlank() && !loading) {
                                onClearLoginResult()
                                loading = true
                                viewModel.login(username, password)
                            }
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (loginResult is LoginResult.Error && loginResult.message != "NETWORK_ERROR") {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = loginResult.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(Modifier.height(24.dp))

                TextButton(
                    onClick = {
                        viewModel.setBaseUrl(REVIEW_DEMO_URL)
                        username = REVIEW_DEMO_USERNAME
                        password = REVIEW_DEMO_PASSWORD
                    },
                    enabled = !loading
                ) {
                    Text(stringResource(Res.string.fill_review_demo_btn))
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        onClearLoginResult()
                        loading = true
                        viewModel.login(username, password)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = username.isNotBlank() && password.isNotBlank() && !loading
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(stringResource(Res.string.login_btn))
                    }
                }
                if (getPlatform().isRegistrationEnabled) {
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = onNavigateToRegister) {
                        Text(stringResource(Res.string.register_btn))
                    }
                }

                Spacer(Modifier.weight(1f))

                // Policy and Terms
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.padding(bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(Res.string.privacy_policy),
                            style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.Underline),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { getPlatform().openUrl(PRIVACY_POLICY_URL) }
                        )
                        Text(
                            text = " & ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        Text(
                            text = stringResource(Res.string.terms_of_use),
                            style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.Underline),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { getPlatform().openUrl(TERMS_OF_USE_URL) }
                        )
                    }
                }
            }
        }
    }
}
