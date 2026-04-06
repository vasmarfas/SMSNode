package com.vasmarfas.smsnode.data.settings

import platform.Foundation.NSUserDefaults

private const val KEY_ACCESS_TOKEN = "access_token"

actual fun getStoredToken(): String? =
    NSUserDefaults.standardUserDefaults.stringForKey(KEY_ACCESS_TOKEN)

actual fun setStoredToken(token: String) {
    NSUserDefaults.standardUserDefaults.setObject(token, KEY_ACCESS_TOKEN)
    NSUserDefaults.standardUserDefaults.synchronize()
}

actual fun clearStoredToken() {
    NSUserDefaults.standardUserDefaults.removeObjectForKey(KEY_ACCESS_TOKEN)
    NSUserDefaults.standardUserDefaults.synchronize()
}
