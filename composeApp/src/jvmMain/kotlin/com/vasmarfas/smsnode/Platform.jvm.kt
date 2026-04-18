package com.vasmarfas.smsnode

import com.vasmarfas.smsnode.data.models.MessageResponse
import com.vasmarfas.smsnode.data.api.SmsNodeApi
import com.vasmarfas.smsnode.data.api.createSmsNodeHttpClient
import com.vasmarfas.smsnode.data.polling.PollingWorker
import com.vasmarfas.smsnode.data.settings.ServerUrlStorage
import com.vasmarfas.smsnode.data.settings.TokenStorage
import kotlinx.coroutines.*
import java.awt.Color
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.image.BufferedImage

class JVMPlatform: Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
    override val isRegistrationEnabled: Boolean = !System.getProperty("os.name").contains("Mac", ignoreCase = true)
}

actual fun getPlatform(): Platform = JVMPlatform()

actual fun getTelegramInitData(): String? = null

private var globalTrayIcon: TrayIcon? = null

actual fun notifyNewMessages(messages: List<MessageResponse>) {
    if (!SystemTray.isSupported()) return
    if (messages.isEmpty()) return

    val latest = messages.maxByOrNull { it.id } ?: return

    val tray = SystemTray.getSystemTray()
    
    if (globalTrayIcon == null) {
        val size = 16
        val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        g.color = Color.RED
        g.fillOval(0, 0, size, size)
        g.color = Color.WHITE
        g.font = g.font.deriveFont(10f)
        g.drawString("S", 5, 12)
        g.dispose()

        globalTrayIcon = TrayIcon(image, "SMS Node").apply {
            isImageAutoSize = true
        }
        
        try {
            tray.add(globalTrayIcon)
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }
    }

    globalTrayIcon?.let { icon ->
        icon.actionListeners.forEach { icon.removeActionListener(it) }
        icon.addActionListener {
            NotificationRouteHolder.requestOpenChat(latest.externalPhone)
        }

        icon.displayMessage(
            "Новое сообщение от ${latest.externalPhone}",
            latest.text.take(80),
            TrayIcon.MessageType.INFO
        )
    }
}

private var pollingJob: Job? = null
private val pollingScope = CoroutineScope(Dispatchers.Default)

actual fun startBackgroundPolling() {
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
