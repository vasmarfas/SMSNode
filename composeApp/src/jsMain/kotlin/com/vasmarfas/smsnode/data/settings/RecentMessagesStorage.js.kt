package com.vasmarfas.smsnode.data.settings

import kotlinx.browser.localStorage

private const val KEY_LAST_SINCE = "recent_messages_last_since"

actual fun getStoredLastSince(): String? =
    try { localStorage.getItem(KEY_LAST_SINCE) } catch (_: Exception) { null }

actual fun setStoredLastSince(value: String) {
    try { localStorage.setItem(KEY_LAST_SINCE, value) } catch (_: Exception) { }
}

actual fun clearStoredLastSince() {
    try { localStorage.removeItem(KEY_LAST_SINCE) } catch (_: Exception) { }
}

