package com.vasmarfas.smsnode

import com.vasmarfas.smsnode.data.api.SmsNodeApi
import com.vasmarfas.smsnode.data.api.createSmsNodeHttpClient
import com.vasmarfas.smsnode.data.polling.PollingWorker
import com.vasmarfas.smsnode.data.settings.ServerUrlStorage
import com.vasmarfas.smsnode.data.settings.TokenStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun performBackgroundFetch(completion: (Boolean) -> Unit) {
    CoroutineScope(Dispatchers.Default).launch {
        var success = false
        val httpClient = createSmsNodeHttpClient()
        try {
            val baseUrl = ServerUrlStorage.getBaseUrl()
            val token = TokenStorage.getToken()
            if (!baseUrl.isNullOrBlank() && !token.isNullOrBlank()) {
                val api = SmsNodeApi(httpClient, "http://localhost", null)
                PollingWorker.doPollingTick(api, baseUrl, token)
                success = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            httpClient.close()
            completion(success)
        }
    }
}
