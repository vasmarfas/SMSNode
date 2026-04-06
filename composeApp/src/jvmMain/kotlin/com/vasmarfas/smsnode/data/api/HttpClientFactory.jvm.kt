package com.vasmarfas.smsnode.data.api

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*

actual fun createSmsNodeHttpClient(): HttpClient = HttpClient(OkHttp) {
    install(ContentNegotiation) {
        json(json)
    }
}
