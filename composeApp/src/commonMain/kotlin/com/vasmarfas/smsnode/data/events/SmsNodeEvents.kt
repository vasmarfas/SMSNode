package com.vasmarfas.smsnode.data.events

import com.vasmarfas.smsnode.data.models.MessageResponse
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object SmsNodeEvents {
    private val _newMessages = MutableSharedFlow<List<MessageResponse>>()
    val newMessages: SharedFlow<List<MessageResponse>> = _newMessages.asSharedFlow()

    private val _unauthorized = MutableSharedFlow<Unit>()
    val unauthorized: SharedFlow<Unit> = _unauthorized.asSharedFlow()

    suspend fun emitNewMessages(messages: List<MessageResponse>) {
        _newMessages.emit(messages)
    }

    suspend fun emitUnauthorized() {
        _unauthorized.emit(Unit)
    }
}
