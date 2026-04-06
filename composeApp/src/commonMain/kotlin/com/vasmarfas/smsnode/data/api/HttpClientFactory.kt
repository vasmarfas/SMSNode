package com.vasmarfas.smsnode.data.api

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
}

expect fun createSmsNodeHttpClient(): HttpClient
