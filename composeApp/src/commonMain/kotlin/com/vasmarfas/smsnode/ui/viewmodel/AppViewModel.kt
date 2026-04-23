package com.vasmarfas.smsnode.ui.viewmodel

import com.vasmarfas.smsnode.data.api.ApiResult
import com.vasmarfas.smsnode.data.api.RegisterResult
import com.vasmarfas.smsnode.data.api.SmsNodeApi
import com.vasmarfas.smsnode.data.api.createSmsNodeHttpClient
import com.vasmarfas.smsnode.data.models.ContactResponse
import com.vasmarfas.smsnode.data.models.DialogSummary
import com.vasmarfas.smsnode.data.models.MessageResponse
import com.vasmarfas.smsnode.data.models.TemplateResponse
import com.vasmarfas.smsnode.data.settings.CredentialsStorage
import com.vasmarfas.smsnode.data.settings.RecentMessagesStorage
import com.vasmarfas.smsnode.data.settings.ServerUrlStorage
import com.vasmarfas.smsnode.data.session.SessionManager
import com.vasmarfas.smsnode.data.settings.TokenStorage
import com.vasmarfas.smsnode.data.events.SmsNodeEvents
import com.vasmarfas.smsnode.startBackgroundPolling
import com.vasmarfas.smsnode.stopBackgroundPolling
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppViewModel(
    private val sessionManager: SessionManager,
    baseUrl: String = "http://localhost:8007"
) {
    private val scope = CoroutineScope(Dispatchers.Default)

    private fun normalizeBaseUrl(url: String): String {
        var normalized = url.trim().trimEnd('/')
        if (normalized.isBlank()) return normalized

        val firstSchemeIdx = normalized.indexOf("://")
        if (firstSchemeIdx >= 0) {
            val afterScheme = firstSchemeIdx + 3
            val secondSchemeIdx = normalized.indexOf("://", afterScheme)
            normalized = if (secondSchemeIdx > 0) {
                normalized.substring(0, secondSchemeIdx)
            } else {
                val slashAfterHost = normalized.indexOf('/', afterScheme)
                if (slashAfterHost > 0) normalized.substring(0, slashAfterHost) else normalized
            }
        }

        return normalized
    }

    private val initialBaseUrl = normalizeBaseUrl(baseUrl)
    val api: SmsNodeApi = SmsNodeApi(createSmsNodeHttpClient(), initialBaseUrl)

    private val _baseUrl = MutableStateFlow(initialBaseUrl)
    val baseUrl: StateFlow<String> = _baseUrl.asStateFlow()

    private val _loginResult = MutableStateFlow<LoginResult?>(null)
    val loginResult: StateFlow<LoginResult?> = _loginResult.asStateFlow()

    private val _registerResult = MutableStateFlow<RegisterUiResult?>(null)
    val registerResult: StateFlow<RegisterUiResult?> = _registerResult.asStateFlow()

    private val _isSessionRestoring = MutableStateFlow(true)
    val isSessionRestoring: StateFlow<Boolean> = _isSessionRestoring.asStateFlow()

    private val _dialogs = MutableStateFlow<List<DialogSummary>>(emptyList())
    val dialogs: StateFlow<List<DialogSummary>> = _dialogs.asStateFlow()

    private val _dialogsLoading = MutableStateFlow(false)
    val dialogsLoading: StateFlow<Boolean> = _dialogsLoading.asStateFlow()

    private val _dialogsError = MutableStateFlow<String?>(null)
    val dialogsError: StateFlow<String?> = _dialogsError.asStateFlow()

    private val _contacts = MutableStateFlow<List<ContactResponse>>(emptyList())
    val contacts: StateFlow<List<ContactResponse>> = _contacts.asStateFlow()

    private val _unreadCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val unreadCounts: StateFlow<Map<String, Int>> = _unreadCounts.asStateFlow()

    private val _templates = MutableStateFlow<List<TemplateResponse>>(emptyList())
    val templates: StateFlow<List<TemplateResponse>> = _templates.asStateFlow()
    private val _templatesLoading = MutableStateFlow(false)
    val templatesLoading: StateFlow<Boolean> = _templatesLoading.asStateFlow()

    private val _templatesError = MutableStateFlow<String?>(null)
    val templatesError: StateFlow<String?> = _templatesError.asStateFlow()

    init {
        scope.launch {
            SmsNodeEvents.newMessages.collect { msgs ->
                applyRecentMessagesToState(msgs)
            }
        }
        scope.launch {
            SmsNodeEvents.unauthorized.collect {
                logout()
            }
        }
    }

    fun clearLoginResult() { _loginResult.value = null }
    fun clearRegisterResult() { _registerResult.value = null }

    private fun startMessagesPolling() {
        startBackgroundPolling()
    }

    private fun stopMessagesPolling() {
        stopBackgroundPolling()
    }

    fun setBaseUrl(url: String) {
        var normalized = normalizeBaseUrl(url)
        if (normalized.isBlank()) return

        _baseUrl.value = normalized
        api.setBaseUrl(normalized)
    }

    fun loadDialogsAndContacts() {
        if (_dialogsLoading.value) return

        scope.launch {
            _dialogsLoading.value = true
            _dialogsError.value = null

            api.setBaseUrl(_baseUrl.value)

            when (val r = api.getDialogs()) {
                is ApiResult.Success -> _dialogs.value = r.value
                is ApiResult.NetworkError -> _dialogsError.value = "NETWORK_ERROR"
                is ApiResult.Forbidden -> _dialogsError.value = r.message
                is ApiResult.Error -> _dialogsError.value = "Ошибка загрузки: ${r.message}"
                else -> _dialogsError.value = "Неизвестная ошибка загрузки"
            }

            if (_dialogsError.value == null) {
                when (val c = api.getContacts()) {
                    is ApiResult.Success -> _contacts.value = c.value
                    else -> { }
                }
            }

            _dialogsLoading.value = false
        }
    }

    fun loadTemplates() {
        if (_templatesLoading.value) return
        scope.launch {
            _templatesLoading.value = true
            _templatesError.value = null
            when (val r = api.getTemplates()) {
                is ApiResult.Success -> _templates.value = r.value
                is ApiResult.NetworkError -> _templatesError.value = "NETWORK_ERROR"
                else -> { }
            }
            _templatesLoading.value = false
        }
    }

    suspend fun createTemplate(name: String, content: String, isGlobal: Boolean = false) {
        val r = api.createTemplate(name, content, isGlobal)
        if (r is ApiResult.Success) {
            val current = _templates.value.toMutableList()
            current.add(r.value)
            _templates.value = current.sortedBy { it.name }
        }
    }

    suspend fun updateTemplate(id: Int, name: String?, content: String?, isGlobal: Boolean? = null) {
        val r = api.updateTemplate(id, name, content, isGlobal)
        if (r is ApiResult.Success) {
            val current = _templates.value.toMutableList()
            val index = current.indexOfFirst { it.id == id }
            if (index != -1) {
                current[index] = r.value
                _templates.value = current.sortedBy { it.name }
            }
        }
    }

    suspend fun deleteTemplate(id: Int) {
        val r = api.deleteTemplate(id)
        if (r is ApiResult.Success) {
            val current = _templates.value.toMutableList()
            current.removeAll { it.id == id }
            _templates.value = current
        }
    }

    fun markDialogRead(phone: String) {
        val current = _unreadCounts.value
        if (!current.containsKey(phone)) return

        val mutable = current.toMutableMap()
        mutable.remove(phone)
        _unreadCounts.value = mutable
    }

    private fun applyRecentMessagesToState(msgs: List<MessageResponse>) {
        if (msgs.isEmpty()) return

        val incomingMsgs = msgs.filter { it.direction == "in" }
        val incomingByPhone = incomingMsgs.groupBy { it.externalPhone }
        val currentUnread = _unreadCounts.value.toMutableMap()
        for ((phone, list) in incomingByPhone) {
            val existing = currentUnread[phone] ?: 0
            currentUnread[phone] = existing + list.size
        }
        _unreadCounts.value = currentUnread

        val currentDialogs = _dialogs.value.toMutableList()
        val allByPhone = msgs.groupBy { it.externalPhone }
        val latestByPhone: Map<String, MessageResponse> = allByPhone.mapValues { (_, list) ->
            list.maxByOrNull { it.createdAt ?: "" } ?: list.maxByOrNull { it.id }!!
        }

        for ((phone, msg) in latestByPhone) {
            val idx = currentDialogs.indexOfFirst { it.externalPhone == phone }
            val updated = DialogSummary(
                externalPhone = phone,
                lastText = msg.text,
                lastAt = msg.createdAt,
                lastDirection = msg.direction
            )
            if (idx >= 0) {
                currentDialogs[idx] = updated
            } else {
                currentDialogs.add(updated)
            }
        }

        _dialogs.value = currentDialogs.sortedByDescending { it.lastAt ?: "" }
    }

    fun saveBaseUrl() {
        ServerUrlStorage.setBaseUrl(_baseUrl.value)
    }

    fun applyStoredToken() {
        val stored = sessionManager.token.value ?: TokenStorage.getToken()
        if (stored != null) {
            sessionManager.setSession(stored, sessionManager.user.value)
            api.setToken(stored)
        }
    }

    fun login(username: String, password: String) {
        _loginResult.value = null
        scope.launch {
            api.setBaseUrl(_baseUrl.value)
            when (val r = api.login(username, password)) {
                is ApiResult.Success -> {
                    val token = r.value.accessToken
                    api.setToken(token)
                    when (val me = api.me()) {
                        is ApiResult.Success -> {
                            sessionManager.setSession(token, me.value)
                            saveBaseUrl()
                            CredentialsStorage.setStoredCredentials(username, password)
                            TokenStorage.setToken(token)
                            _loginResult.value = LoginResult.Success
                            startMessagesPolling()
                        }
                        is ApiResult.Unauthorized -> {
                            sessionManager.clear()
                            _loginResult.value = LoginResult.Error("Сессия недействительна")
                        }
                        is ApiResult.NetworkError -> _loginResult.value = LoginResult.Error("NETWORK_ERROR")
                        else -> _loginResult.value = LoginResult.Error("Ошибка загрузки профиля: ${msgOf(me)}")
                    }
                }
                is ApiResult.Unauthorized -> _loginResult.value = LoginResult.Error("Неверный логин или пароль")
                is ApiResult.Forbidden -> _loginResult.value = LoginResult.Error(r.message)
                is ApiResult.NetworkError -> _loginResult.value = LoginResult.Error("NETWORK_ERROR")
                is ApiResult.Error -> _loginResult.value = LoginResult.Error("Ошибка сервера: ${r.message}")
                else -> _loginResult.value = LoginResult.Error("Неизвестная ошибка входа")
            }
        }
    }

    fun register(username: String, password: String) {
        if (username.length < 3 || password.length < 6) {
             _registerResult.value = RegisterUiResult.Error("Логин от 3 символов, пароль от 6")
             return
        }

        _registerResult.value = null
        scope.launch {
            api.setBaseUrl(_baseUrl.value)
            when (val r = api.register(username, password)) {
                is ApiResult.Success -> when (val res = r.value) {
                    is RegisterResult.Created -> {
                        when (val loginR = api.login(username, password)) {
                            is ApiResult.Success -> {
                                api.setToken(loginR.value.accessToken)
                                when (val me = api.me()) {
                                    is ApiResult.Success -> {
                                        sessionManager.setSession(loginR.value.accessToken, me.value)
                                        saveBaseUrl()
                                        CredentialsStorage.setStoredCredentials(username, password)
                                        TokenStorage.setToken(loginR.value.accessToken)
                                        _registerResult.value = RegisterUiResult.Created
                                        startMessagesPolling()
                                    }
                                    else -> _registerResult.value = RegisterUiResult.Error("Не удалось загрузить профиль")
                                }
                            }
                            else -> _registerResult.value = RegisterUiResult.Error("Ошибка авто-входа: ${msgOf(loginR)}")
                        }
                    }
                    is RegisterResult.Pending -> _registerResult.value = RegisterUiResult.Pending(res.message)
                }
                is ApiResult.Forbidden -> _registerResult.value = RegisterUiResult.Error(r.message)
                is ApiResult.Conflict -> _registerResult.value = RegisterUiResult.Error(r.message)
                is ApiResult.NetworkError -> _registerResult.value = RegisterUiResult.Error("NETWORK_ERROR")
                is ApiResult.Error -> _registerResult.value = RegisterUiResult.Error("Ошибка сервера: ${r.message}")
                else -> _registerResult.value = RegisterUiResult.Error("Неизвестная ошибка регистрации")
            }
        }
    }

    fun logout() {
        api.setToken(null)
        TokenStorage.clearToken()
        stopMessagesPolling()
        sessionManager.clear()
        _loginResult.value = null
        _registerResult.value = null
        RecentMessagesStorage.clearLastSince()
        _dialogs.value = emptyList()
        _dialogsError.value = null
        _dialogsLoading.value = false
        _contacts.value = emptyList()
        _unreadCounts.value = emptyMap()
        CredentialsStorage.clearStoredCredentials()
    }

    fun tryTelegramLogin(initData: String) {
        scope.launch {
            api.setBaseUrl(_baseUrl.value)
            when (val r = api.loginByTelegram(initData)) {
                is ApiResult.Success -> {
                    val token = r.value.accessToken
                    api.setToken(token)
                    when (val me = api.me()) {
                        is ApiResult.Success -> {
                            sessionManager.setSession(token, me.value)
                            saveBaseUrl()
                            TokenStorage.setToken(token)
                            startMessagesPolling()
                        }
                        else -> { }
                    }
                }
                else -> { }
            }
        }
    }

    fun tryRestoreSessionFromCredentials() {
        scope.launch {
            try {
                var restored = false

                val currentToken = sessionManager.token.value
                if (currentToken != null) {
                    api.setBaseUrl(_baseUrl.value)
                    api.setToken(currentToken)
                    when (val me = api.me()) {
                        is ApiResult.Success -> {
                            sessionManager.setSession(currentToken, me.value)
                            startMessagesPolling()
                            restored = true
                        }
                        else -> { }
                    }
                }

                if (restored) return@launch

                val cred = CredentialsStorage.getStoredCredentials()
                if (cred != null) {
                    api.setBaseUrl(_baseUrl.value)
                    when (val r = api.login(cred.first, cred.second)) {
                        is ApiResult.Success -> {
                            val token = r.value.accessToken
                            api.setToken(token)
                            when (val me = api.me()) {
                                is ApiResult.Success -> {
                                    sessionManager.setSession(token, me.value)
                                    saveBaseUrl()
                                    TokenStorage.setToken(token)
                                    startMessagesPolling()
                                }
                                else -> { }
                            }
                        }
                        else -> { }
                    }
                }
            } finally {
                _isSessionRestoring.value = false
            }
        }
    }

    fun refreshMe(onUnauthorized: () -> Unit) {
        scope.launch {
            when (val r = api.me()) {
                is ApiResult.Success -> sessionManager.updateUser(r.value)
                is ApiResult.Unauthorized -> {
                    sessionManager.clear()
                    onUnauthorized()
                }
                else -> { }
            }
        }
    }

    private fun msgOf(r: ApiResult<*>): String = when (r) {
        is ApiResult.NetworkError -> "Нет соединения: ${r.message}"
        is ApiResult.Forbidden -> r.message
        is ApiResult.NotFound -> r.message
        is ApiResult.Conflict -> r.message
        is ApiResult.Error -> r.message
        else -> "Ошибка"
    }
}

sealed class LoginResult {
    data object Success : LoginResult()
    data class Error(val message: String) : LoginResult()
}

sealed class RegisterUiResult {
    object Created : RegisterUiResult()
    data class Pending(val message: String) : RegisterUiResult()
    data class Error(val message: String) : RegisterUiResult()
}
