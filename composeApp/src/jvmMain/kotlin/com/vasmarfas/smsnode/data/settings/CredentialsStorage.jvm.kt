package com.vasmarfas.smsnode.data.settings

import java.util.prefs.Preferences

private val prefs: Preferences
    get() = Preferences.userRoot().node("com.vasmarfas.smsnode")

private const val KEY_USERNAME = "credentials_username"
private const val KEY_PASSWORD = "credentials_password"

actual fun getStoredUsernameImpl(): String? =
    try { prefs.get(KEY_USERNAME, null) } catch (_: Exception) { null }

actual fun setStoredUsernameImpl(username: String) {
    try { prefs.put(KEY_USERNAME, username) } catch (_: Exception) { }
}

actual fun clearStoredUsernameImpl() {
    try { prefs.remove(KEY_USERNAME) } catch (_: Exception) { }
}

actual fun getStoredPasswordImpl(): String? =
    try { prefs.get(KEY_PASSWORD, null) } catch (_: Exception) { null }

actual fun setStoredPasswordImpl(password: String) {
    try { prefs.put(KEY_PASSWORD, password) } catch (_: Exception) { }
}

actual fun clearStoredPasswordImpl() {
    try { prefs.remove(KEY_PASSWORD) } catch (_: Exception) { }
}
