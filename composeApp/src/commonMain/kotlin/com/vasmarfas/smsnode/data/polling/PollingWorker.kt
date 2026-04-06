package com.vasmarfas.smsnode.data.polling

import com.vasmarfas.smsnode.data.api.ApiResult
import com.vasmarfas.smsnode.data.api.SmsNodeApi
import com.vasmarfas.smsnode.data.events.SmsNodeEvents
import com.vasmarfas.smsnode.data.settings.RecentMessagesStorage
import com.vasmarfas.smsnode.notifyNewMessages

object PollingWorker {
    suspend fun doPollingTick(api: SmsNodeApi, baseUrl: String, token: String) {
        val sinceForRequest = RecentMessagesStorage.getLastSince()
        val thisRequestTime = kotlin.time.Clock.System.now().toString()

        api.setBaseUrl(baseUrl)
        api.setToken(token)

        when (val r = api.getRecentMessages(sinceForRequest)) {
            is ApiResult.Success -> {
                val msgs = r.value.messages
                if (msgs.isNotEmpty()) {
                    val incomingMsgs = msgs.filter { it.direction == "in" }
                    if (incomingMsgs.isNotEmpty()) {
                        notifyNewMessages(incomingMsgs)
                    }
                    SmsNodeEvents.emitNewMessages(msgs)
                }
                RecentMessagesStorage.setLastSince(thisRequestTime)
            }
            is ApiResult.Unauthorized -> {
                println("PollingWorker: unauthorized")
            }
            is ApiResult.NetworkError -> {
                println("PollingWorker: network error=${r.message}")
            }
            else -> {
                println("PollingWorker: other result=$r")
            }
        }
    }
}
