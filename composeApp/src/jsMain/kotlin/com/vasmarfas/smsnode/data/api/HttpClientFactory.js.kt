package com.vasmarfas.smsnode.data.api

import io.ktor.client.*
import io.ktor.client.engine.js.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*

actual fun createSmsNodeHttpClient(): HttpClient = HttpClient(Js) {
    install(ContentNegotiation) {
        json(json)
    }
}
