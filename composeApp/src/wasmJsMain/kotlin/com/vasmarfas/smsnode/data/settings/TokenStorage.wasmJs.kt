package com.vasmarfas.smsnode.data.settings

import kotlinx.browser.localStorage

private const val KEY_ACCESS_TOKEN = "access_token"

actual fun getStoredToken(): String? =
    try { localStorage.getItem(KEY_ACCESS_TOKEN) } catch (_: Exception) { null }

actual fun setStoredToken(token: String) {
    try { localStorage.setItem(KEY_ACCESS_TOKEN, token) } catch (_: Exception) { }
}

actual fun clearStoredToken() {
    try { localStorage.removeItem(KEY_ACCESS_TOKEN) } catch (_: Exception) { }
}
