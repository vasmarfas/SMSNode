package com.vasmarfas.smsnode.data.settings

import platform.Foundation.NSUserDefaults

private const val KEY_LAST_SINCE = "recent_messages_last_since"

actual fun getStoredLastSince(): String? =
    NSUserDefaults.standardUserDefaults.stringForKey(KEY_LAST_SINCE)

actual fun setStoredLastSince(value: String) {
    NSUserDefaults.standardUserDefaults.setObject(value, KEY_LAST_SINCE)
    NSUserDefaults.standardUserDefaults.synchronize()
}

actual fun clearStoredLastSince() {
    NSUserDefaults.standardUserDefaults.removeObjectForKey(KEY_LAST_SINCE)
    NSUserDefaults.standardUserDefaults.synchronize()
}

