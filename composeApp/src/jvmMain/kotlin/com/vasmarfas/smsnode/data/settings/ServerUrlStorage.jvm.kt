package com.vasmarfas.smsnode.data.settings

import java.util.prefs.Preferences

private const val KEY_BASE_URL = "server_base_url"

actual fun getStoredBaseUrl(): String? {
    return try {
        Preferences.userRoot().node("com.vasmarfas.smsnode").get(KEY_BASE_URL, null)
    } catch (_: Exception) {
        null
    }
}

actual fun setStoredBaseUrl(url: String) {
    try {
        Preferences.userRoot().node("com.vasmarfas.smsnode").put(KEY_BASE_URL, url)
    } catch (_: Exception) { }
}
