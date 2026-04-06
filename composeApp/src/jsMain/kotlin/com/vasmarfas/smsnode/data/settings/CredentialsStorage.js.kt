package com.vasmarfas.smsnode.data.settings

import kotlinx.browser.localStorage

private const val KEY_USERNAME = "credentials_username"
private const val KEY_PASSWORD = "credentials_password"

actual fun getStoredUsernameImpl(): String? =
    try { localStorage.getItem(KEY_USERNAME) } catch (_: Exception) { null }

actual fun setStoredUsernameImpl(username: String) {
    try { localStorage.setItem(KEY_USERNAME, username) } catch (_: Exception) { }
}

actual fun clearStoredUsernameImpl() {
    try { localStorage.removeItem(KEY_USERNAME) } catch (_: Exception) { }
}

actual fun getStoredPasswordImpl(): String? =
    try { localStorage.getItem(KEY_PASSWORD) } catch (_: Exception) { null }

actual fun setStoredPasswordImpl(password: String) {
    try { localStorage.setItem(KEY_PASSWORD, password) } catch (_: Exception) { }
}

actual fun clearStoredPasswordImpl() {
    try { localStorage.removeItem(KEY_PASSWORD) } catch (_: Exception) { }
}
