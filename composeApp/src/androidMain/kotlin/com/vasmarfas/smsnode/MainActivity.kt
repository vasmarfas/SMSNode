package com.vasmarfas.smsnode

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.vasmarfas.smsnode.data.settings.SmsNodeContextHolder

class MainActivity : ComponentActivity() {

    private val requestNotificationsPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        Log.d("SmsNodeNotify", "POST_NOTIFICATIONS permission result: granted=$granted")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        SmsNodeContextHolder.appContext = applicationContext
        handleOpenChatIntent()

        requestPostNotificationsIfNeeded()

        setContent {
            App()
        }
    }

    private fun requestPostNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d("SmsNodeNotify", "Requesting POST_NOTIFICATIONS permission")
                requestNotificationsPermission.launch(permission)
            } else {
                Log.d("SmsNodeNotify", "POST_NOTIFICATIONS already granted")
            }
        }
    }

    private fun handleOpenChatIntent() {
        val phone = intent?.getStringExtra(EXTRA_OPEN_CHAT_PHONE) ?: return
        NotificationRouteHolder.requestOpenChat(phone)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOpenChatIntent()
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}