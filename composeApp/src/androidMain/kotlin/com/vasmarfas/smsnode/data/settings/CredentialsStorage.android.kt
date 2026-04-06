package com.vasmarfas.smsnode.data.settings

import android.content.Context

private const val PREFS_NAME = "smsnode_prefs"
private const val KEY_USERNAME = "credentials_username"
private const val KEY_PASSWORD = "credentials_password"

private fun getPrefs(): android.content.SharedPreferences? {
    val context = SmsNodeContextHolder.appContext ?: return null
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}

actual fun getStoredUsernameImpl(): String? =
    getPrefs()?.getString(KEY_USERNAME, null)

actual fun setStoredUsernameImpl(username: String) {
    getPrefs()?.edit()?.putString(KEY_USERNAME, username)?.apply()
}

actual fun clearStoredUsernameImpl() {
    getPrefs()?.edit()?.remove(KEY_USERNAME)?.apply()
}

actual fun getStoredPasswordImpl(): String? =
    getPrefs()?.getString(KEY_PASSWORD, null)

actual fun setStoredPasswordImpl(password: String) {
    getPrefs()?.edit()?.putString(KEY_PASSWORD, password)?.apply()
}

actual fun clearStoredPasswordImpl() {
    getPrefs()?.edit()?.remove(KEY_PASSWORD)?.apply()
}
