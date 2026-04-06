package com.vasmarfas.smsnode

import com.vasmarfas.smsnode.data.models.MessageResponse
import com.vasmarfas.smsnode.data.api.SmsNodeApi
import com.vasmarfas.smsnode.data.api.createSmsNodeHttpClient
import com.vasmarfas.smsnode.data.polling.PollingWorker
import com.vasmarfas.smsnode.data.settings.ServerUrlStorage
import com.vasmarfas.smsnode.data.settings.TokenStorage
import kotlinx.coroutines.*
import kotlin.js.js

class JsPlatform: Platform {
    override val name: String = "Web with Kotlin/JS"
}

actual fun getPlatform(): Platform = JsPlatform()

actual fun getTelegramInitData(): String? {
    val w = js("typeof window !== 'undefined' ? window : null").unsafeCast<dynamic>()
    if (w == null) return null
    val tg = w.Telegram
    if (tg == null) return null
    val wa = tg.WebApp
    if (wa == null) return null
    val id = wa.initData
    return id?.unsafeCast<String>()
}

actual fun notifyNewMessages(messages: List<MessageResponse>) {
    val latest = messages.maxByOrNull { it.id } ?: return
    val title = "Новое сообщение"
    val bodyText = "${latest.externalPhone}: ${latest.text.take(80)}"
    
    js("""
        if (typeof window !== 'undefined' && 'Notification' in window && Notification.permission === 'granted') {
            new Notification(title, { body: bodyText });
        }
    """)
}

private var pollingJob: Job? = null
private val pollingScope = CoroutineScope(Dispatchers.Default)

actual fun startBackgroundPolling() {
    js("""
        if (typeof window !== 'undefined' && 'Notification' in window && Notification.permission !== 'denied') {
            Notification.requestPermission();
        }
    """)

    if (pollingJob?.isActive == true) return
    pollingJob = pollingScope.launch {
        val httpClient = createSmsNodeHttpClient()
        val api = SmsNodeApi(httpClient, "http://localhost", null)
        try {
            while (isActive) {
                 val baseUrl = ServerUrlStorage.getBaseUrl()
                 val token = TokenStorage.getToken()
                 if (!baseUrl.isNullOrBlank() && !token.isNullOrBlank()) {
                     try {
                         PollingWorker.doPollingTick(api, baseUrl, token)
                     } catch (e: Exception) { e.printStackTrace() }
                 }
                 delay(10_000)
            }
        } finally {
            httpClient.close()
        }
    }
}

actual fun stopBackgroundPolling() {
    pollingJob?.cancel()
    pollingJob = null
}
