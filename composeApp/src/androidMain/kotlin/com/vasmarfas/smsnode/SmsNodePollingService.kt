package com.vasmarfas.smsnode

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.vasmarfas.smsnode.data.api.SmsNodeApi
import com.vasmarfas.smsnode.data.api.createSmsNodeHttpClient
import com.vasmarfas.smsnode.data.polling.PollingWorker
import com.vasmarfas.smsnode.data.settings.ServerUrlStorage
import com.vasmarfas.smsnode.data.settings.TokenStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SmsNodePollingService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var job: Job? = null

    private val httpClient = createSmsNodeHttpClient()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopPolling()
            stopForeground(true)
            stopSelf()
            return START_NOT_STICKY
        }

        startForegroundService()
        startPolling()

        return START_STICKY
    }

    private fun startForegroundService() {
        val channelId = "smsnode_service"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Фоновая работа",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("SMS Node")
            .setContentText("Поиск новых сообщений...")
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1, notification)
    }

    private fun startPolling() {
        if (job?.isActive == true) return
        job = scope.launch {
            val api = SmsNodeApi(httpClient, "http://localhost", null)

            while (isActive) {
                val baseUrl = ServerUrlStorage.getBaseUrl()
                val token = TokenStorage.getToken()

                if (!baseUrl.isNullOrBlank() && !token.isNullOrBlank()) {
                    try {
                        PollingWorker.doPollingTick(api, baseUrl, token)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    println("SmsNodePollingService: baseUrl or token missing, skipping tick")
                }

                delay(10_000)
            }
        }
    }

    private fun stopPolling() {
        job?.cancel()
        job = null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPolling()
        scope.cancel()
        httpClient.close()
    }

    companion object {
        const val ACTION_START = "START"
        const val ACTION_STOP = "STOP"
    }
}
