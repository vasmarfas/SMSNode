package com.vasmarfas.smsnode.data.settings

import java.util.prefs.Preferences

private const val KEY_LAST_SINCE = "recent_messages_last_since"

private val prefs: Preferences
    get() = Preferences.userRoot().node("com.vasmarfas.smsnode")

actual fun getStoredLastSince(): String? =
    try { prefs.get(KEY_LAST_SINCE, null) } catch (_: Exception) { null }

actual fun setStoredLastSince(value: String) {
    try { prefs.put(KEY_LAST_SINCE, value) } catch (_: Exception) { }
}

actual fun clearStoredLastSince() {
    try { prefs.remove(KEY_LAST_SINCE) } catch (_: Exception) { }
}

