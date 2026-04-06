package com.vasmarfas.smsnode.data.settings

import platform.Foundation.NSUserDefaults

private const val KEY_USERNAME = "credentials_username"
private const val KEY_PASSWORD = "credentials_password"

private val defaults: NSUserDefaults get() = NSUserDefaults.standardUserDefaults

actual fun getStoredUsernameImpl(): String? =
    defaults.stringForKey(KEY_USERNAME)

actual fun setStoredUsernameImpl(username: String) {
    defaults.setObject(username, KEY_USERNAME)
    defaults.synchronize()
}

actual fun clearStoredUsernameImpl() {
    defaults.removeObjectForKey(KEY_USERNAME)
    defaults.synchronize()
}

actual fun getStoredPasswordImpl(): String? =
    defaults.stringForKey(KEY_PASSWORD)

actual fun setStoredPasswordImpl(password: String) {
    defaults.setObject(password, KEY_PASSWORD)
    defaults.synchronize()
}

actual fun clearStoredPasswordImpl() {
    defaults.removeObjectForKey(KEY_PASSWORD)
    defaults.synchronize()
}
