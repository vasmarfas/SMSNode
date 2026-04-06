package com.vasmarfas.smsnode.data.settings

import platform.Foundation.NSUserDefaults

private const val KEY_BASE_URL = "server_base_url"

actual fun getStoredBaseUrl(): String? {
    return NSUserDefaults.standardUserDefaults.stringForKey(KEY_BASE_URL)
}

actual fun setStoredBaseUrl(url: String) {
    NSUserDefaults.standardUserDefaults.setObject(url, KEY_BASE_URL)
    NSUserDefaults.standardUserDefaults.synchronize()
}
