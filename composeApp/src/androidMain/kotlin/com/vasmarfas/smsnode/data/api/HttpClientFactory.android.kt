package com.vasmarfas.smsnode.data.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json

actual fun createSmsNodeHttpClient(): HttpClient = HttpClient(Android) {
    install(ContentNegotiation) {
        json(json)
    }
}
