package com.vitrum.tv.core

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** Shared HTTP client: the only network calls are GitHub and Open-Meteo. */
object Http {
    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .build()
    }

    const val USER_AGENT = "Vitrum-TV-Launcher"
}
