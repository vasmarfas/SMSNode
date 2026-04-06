package com.vasmarfas.smsnode.data.session

import com.vasmarfas.smsnode.data.models.UserResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionManager {
    private val _token = MutableStateFlow<String?>(null)
    val token: StateFlow<String?> = _token.asStateFlow()

    private val _user = MutableStateFlow<UserResponse?>(null)
    val user: StateFlow<UserResponse?> = _user.asStateFlow()

    val isLoggedIn: Boolean get() = _token.value != null
    val isAdmin: Boolean get() = _user.value?.role == "admin"

    fun setSession(newToken: String?, newUser: UserResponse?) {
        _token.value = newToken
        _user.value = newUser
    }

    fun clear() {
        _token.value = null
        _user.value = null
    }

    fun updateUser(u: UserResponse) {
        _user.value = u
    }
}
