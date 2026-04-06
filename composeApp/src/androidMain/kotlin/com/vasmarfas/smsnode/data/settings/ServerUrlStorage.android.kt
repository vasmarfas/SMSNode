package com.vasmarfas.smsnode.data.settings

import android.content.Context

private const val PREFS_NAME = "smsnode_prefs"
private const val KEY_BASE_URL = "server_base_url"

internal object SmsNodeContextHolder {
    var appContext: Context? = null
}

private fun getPrefs(): android.content.SharedPreferences? {
    val context = SmsNodeContextHolder.appContext ?: return null
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}

actual fun getStoredBaseUrl(): String? {
    return getPrefs()?.getString(KEY_BASE_URL, null)
}

actual fun setStoredBaseUrl(url: String) {
    getPrefs()?.edit()?.putString(KEY_BASE_URL, url)?.apply()
}
