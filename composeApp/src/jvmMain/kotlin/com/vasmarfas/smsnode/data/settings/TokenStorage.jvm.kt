package com.vasmarfas.smsnode.data.settings

import java.util.prefs.Preferences

private const val KEY_ACCESS_TOKEN = "access_token"

private val prefs: Preferences
    get() = Preferences.userRoot().node("com.vasmarfas.smsnode")

actual fun getStoredToken(): String? =
    try { prefs.get(KEY_ACCESS_TOKEN, null) } catch (_: Exception) { null }

actual fun setStoredToken(token: String) {
    try { prefs.put(KEY_ACCESS_TOKEN, token) } catch (_: Exception) { }
}

actual fun clearStoredToken() {
    try { prefs.remove(KEY_ACCESS_TOKEN) } catch (_: Exception) { }
}
