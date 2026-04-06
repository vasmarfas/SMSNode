package com.vasmarfas.smsnode.data.settings

import android.content.Context

private const val PREFS_NAME = "smsnode_prefs"
private const val KEY_ACCESS_TOKEN = "access_token"

private fun getPrefs(): android.content.SharedPreferences? {
    val context = SmsNodeContextHolder.appContext ?: return null
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}

actual fun getStoredToken(): String? =
    getPrefs()?.getString(KEY_ACCESS_TOKEN, null)

actual fun setStoredToken(token: String) {
    getPrefs()?.edit()?.putString(KEY_ACCESS_TOKEN, token)?.apply()
}

actual fun clearStoredToken() {
    getPrefs()?.edit()?.remove(KEY_ACCESS_TOKEN)?.apply()
}
