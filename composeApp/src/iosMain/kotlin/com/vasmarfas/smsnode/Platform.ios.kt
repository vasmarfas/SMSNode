package com.vasmarfas.smsnode

import com.vasmarfas.smsnode.data.models.MessageResponse
import platform.Foundation.NSDate
import platform.Foundation.NSDateComponents
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitSecond
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNUserNotificationCenter
import platform.UIKit.UIDevice
import com.vasmarfas.smsnode.data.api.SmsNodeApi
import com.vasmarfas.smsnode.data.api.createSmsNodeHttpClient
import com.vasmarfas.smsnode.data.polling.PollingWorker
import com.vasmarfas.smsnode.data.settings.ServerUrlStorage
import com.vasmarfas.smsnode.data.settings.TokenStorage
import kotlinx.coroutines.*
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationOptionBadge

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

actual fun getTelegramInitData(): String? = null

actual fun notifyNewMessages(messages: List<MessageResponse>) {
    val latest = messages.maxByOrNull { it.id } ?: return

    val content = UNMutableNotificationContent().apply {
        setTitle("Новое сообщение")
        setBody("${latest.externalPhone}: ${latest.text.take(80)}")
        setSound(UNNotificationSound.defaultSound())
    }

    val trigger = null 

    val request = UNNotificationRequest.requestWithIdentifier(
        identifier = "smsnode_${latest.id}",
        content = content,
        trigger = trigger
    )

    UNUserNotificationCenter.currentNotificationCenter()
        .addNotificationRequest(request, withCompletionHandler = null)
}

private var pollingJob: Job? = null
private val pollingScope = CoroutineScope(Dispatchers.Default)

actual fun startBackgroundPolling() {
    UNUserNotificationCenter.currentNotificationCenter().requestAuthorizationWithOptions(
        UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
    ) { granted, error ->
        if (error != null) {
            println("Notification authorization error: $error")
        }
    }

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
