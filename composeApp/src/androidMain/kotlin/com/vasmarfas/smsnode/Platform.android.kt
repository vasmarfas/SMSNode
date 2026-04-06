package com.vasmarfas.smsnode

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.vasmarfas.smsnode.data.models.MessageResponse
import com.vasmarfas.smsnode.data.settings.SmsNodeContextHolder

private const val SMSNODE_CHANNEL_ID = "smsnode_messages"
private const val SMSNODE_CHANNEL_NAME = "Новые сообщения"
private const val SMSNODE_LOG_TAG = "SmsNodeNotify"
internal const val EXTRA_OPEN_CHAT_PHONE = "open_chat_phone"

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual fun getTelegramInitData(): String? = null

actual fun notifyNewMessages(messages: List<MessageResponse>) {
    val context = SmsNodeContextHolder.appContext ?: run {
        Log.w(SMSNODE_LOG_TAG, "notifyNewMessages: appContext is null")
        return
    }
    if (messages.isEmpty()) {
        Log.d(SMSNODE_LOG_TAG, "notifyNewMessages: empty messages list")
        return
    }

    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: run {
                Log.w(SMSNODE_LOG_TAG, "notifyNewMessages: NotificationManager is null")
                return
            }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            SMSNODE_CHANNEL_ID,
            SMSNODE_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)
    }

    val latest = messages.maxByOrNull { it.id } ?: run {
        Log.w(SMSNODE_LOG_TAG, "notifyNewMessages: cannot find latest message")
        return
    }
    val title = "Новое сообщение от ${latest.externalPhone}"
    val text = latest.text.take(80)

    Log.d(
        SMSNODE_LOG_TAG,
        "notifyNewMessages: showing notification for phone=${latest.externalPhone}, text='$text'"
    )

    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra(EXTRA_OPEN_CHAT_PHONE, latest.externalPhone)
    }

    val pendingIntent = PendingIntent.getActivity(
        context,
        latest.id,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
    )

    val notification = NotificationCompat.Builder(context, SMSNODE_CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_dialog_email)
        .setContentTitle(title)
        .setContentText(text)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .build()

    notificationManager.notify(latest.id, notification)
}

actual fun startBackgroundPolling() {
    val context = SmsNodeContextHolder.appContext ?: return
    val intent = Intent(context, SmsNodePollingService::class.java).apply {
        action = SmsNodePollingService.ACTION_START
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}

actual fun stopBackgroundPolling() {
    val context = SmsNodeContextHolder.appContext ?: return
    val intent = Intent(context, SmsNodePollingService::class.java).apply {
        action = SmsNodePollingService.ACTION_STOP
    }
    context.startService(intent)
}
