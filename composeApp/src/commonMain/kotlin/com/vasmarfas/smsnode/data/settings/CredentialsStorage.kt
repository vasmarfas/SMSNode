package com.vasmarfas.smsnode.data.settings

object CredentialsStorage {
    fun getStoredCredentials(): Pair<String, String>? {
        val u = getStoredUsername() ?: return null
        val p = getStoredPassword() ?: return null
        return u to p
    }

    fun setStoredCredentials(username: String, password: String) {
        setStoredUsername(username)
        setStoredPassword(password)
    }

    fun clearStoredCredentials() {
        clearStoredUsername()
        clearStoredPassword()
    }

    private fun getStoredUsername(): String? = getStoredUsernameImpl()
    private fun setStoredUsername(username: String) = setStoredUsernameImpl(username)
    private fun clearStoredUsername() = clearStoredUsernameImpl()
    private fun getStoredPassword(): String? = getStoredPasswordImpl()
    private fun setStoredPassword(password: String) = setStoredPasswordImpl(password)
    private fun clearStoredPassword() = clearStoredPasswordImpl()
}

expect fun getStoredUsernameImpl(): String?
expect fun setStoredUsernameImpl(username: String)
expect fun clearStoredUsernameImpl()
expect fun getStoredPasswordImpl(): String?
expect fun setStoredPasswordImpl(password: String)
expect fun clearStoredPasswordImpl()
