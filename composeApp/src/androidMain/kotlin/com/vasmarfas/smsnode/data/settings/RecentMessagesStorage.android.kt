package com.vasmarfas.smsnode.data.settings

import android.content.Context

private const val PREFS_NAME = "smsnode_prefs"
private const val KEY_LAST_SINCE = "recent_messages_last_since"

private fun getPrefs(): android.content.SharedPreferences? {
    val context = SmsNodeContextHolder.appContext ?: return null
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}

actual fun getStoredLastSince(): String? =
    getPrefs()?.getString(KEY_LAST_SINCE, null)

actual fun setStoredLastSince(value: String) {
    getPrefs()?.edit()?.putString(KEY_LAST_SINCE, value)?.apply()
}

actual fun clearStoredLastSince() {
    getPrefs()?.edit()?.remove(KEY_LAST_SINCE)?.apply()
}

