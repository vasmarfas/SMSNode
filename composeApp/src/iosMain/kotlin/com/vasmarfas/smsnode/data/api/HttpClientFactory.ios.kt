package com.vasmarfas.smsnode.data.api

import io.ktor.client.*
import io.ktor.client.engine.darwin.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*

actual fun createSmsNodeHttpClient(): HttpClient = HttpClient(Darwin) {
    install(ContentNegotiation) {
        json(json)
    }
}
