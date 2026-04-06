package com.vasmarfas.smsnode.data.settings

object TokenStorage {
    fun getToken(): String? = getStoredToken()
    fun setToken(token: String) = setStoredToken(token)
    fun clearToken() = clearStoredToken()
}

expect fun getStoredToken(): String?
expect fun setStoredToken(token: String)
expect fun clearStoredToken()
