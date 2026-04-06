package com.vasmarfas.smsnode.data.settings

object RecentMessagesStorage {
    fun getLastSince(): String? = getStoredLastSince()
    fun setLastSince(value: String) = setStoredLastSince(value)
    fun clearLastSince() = clearStoredLastSince()
}

expect fun getStoredLastSince(): String?
expect fun setStoredLastSince(value: String)
expect fun clearStoredLastSince()

