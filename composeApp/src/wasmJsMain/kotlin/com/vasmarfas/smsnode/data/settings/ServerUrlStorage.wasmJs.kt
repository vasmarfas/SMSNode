package com.vasmarfas.smsnode.data.settings

import kotlinx.browser.localStorage

private const val KEY_BASE_URL = "server_base_url"

actual fun getStoredBaseUrl(): String? {
    return try {
        localStorage.getItem(KEY_BASE_URL)
    } catch (_: Exception) {
        null
    }
}

actual fun setStoredBaseUrl(url: String) {
    try {
        localStorage.setItem(KEY_BASE_URL, url)
    } catch (_: Exception) { }
}
