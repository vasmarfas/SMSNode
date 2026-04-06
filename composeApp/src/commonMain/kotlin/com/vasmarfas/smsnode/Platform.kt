package com.vasmarfas.smsnode

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

expect fun getTelegramInitData(): String?

expect fun notifyNewMessages(messages: List<com.vasmarfas.smsnode.data.models.MessageResponse>)

expect fun startBackgroundPolling()

expect fun stopBackgroundPolling()
