package com.vasmarfas.smsnode

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object NotificationRouteHolder {
    private val _pendingChatPhone = MutableStateFlow<String?>(null)
    val pendingChatPhone: StateFlow<String?> = _pendingChatPhone

    fun requestOpenChat(phone: String) {
        _pendingChatPhone.value = phone
    }

    fun consumePendingChatPhone(): String? {
        val value = _pendingChatPhone.value
        if (value != null) {
            _pendingChatPhone.value = null
        }
        return value
    }
}

