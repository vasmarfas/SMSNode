package com.vasmarfas.smsnode

import com.vasmarfas.smsnode.data.models.MessageResponse
import com.vasmarfas.smsnode.data.api.SmsNodeApi
import com.vasmarfas.smsnode.data.api.createSmsNodeHttpClient
import com.vasmarfas.smsnode.data.polling.PollingWorker
import com.vasmarfas.smsnode.data.settings.ServerUrlStorage
import com.vasmarfas.smsnode.data.settings.TokenStorage
import kotlinx.coroutines.*

class WasmPlatform: Platform {
    override val name: String = "Web with Kotlin/Wasm"
}

actual fun getPlatform(): Platform = WasmPlatform()

actual fun getTelegramInitData(): String? = null

@JsFun("""
function requestNotificationPermission() {
    if ("Notification" in window && Notification.permission !== "denied") {
        Notification.requestPermission();
    }
}
""")
private external fun requestNotificationPermission()

@JsFun("""
function showNotification(title, body) {
    if ("Notification" in window && Notification.permission === "granted") {
        new Notification(title, { body: body });
    }
}
""")
private external fun showNotification(title: String, body: String)

actual fun notifyNewMessages(messages: List<MessageResponse>) {
    val latest = messages.maxByOrNull { it.id } ?: return
    showNotification("Новое сообщение", "${latest.externalPhone}: ${latest.text.take(80)}")
}

private var pollingJob: Job? = null
private val pollingScope = CoroutineScope(Dispatchers.Default)

actual fun startBackgroundPolling() {
    requestNotificationPermission()
    
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
