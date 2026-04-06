package com.vasmarfas.smsnode.data.settings

object ServerUrlStorage {
    fun getBaseUrl(): String? = getStoredBaseUrl()
    fun setBaseUrl(url: String) = setStoredBaseUrl(url)
}

expect fun getStoredBaseUrl(): String?
expect fun setStoredBaseUrl(url: String)
