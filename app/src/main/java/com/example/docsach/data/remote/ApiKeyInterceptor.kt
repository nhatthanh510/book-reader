package com.example.docsach.data.remote

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Appends the API key to every Google Books call, plus `country` — without it Google intermittently
 * answers 403 "unsupported country" depending on how it geolocates the caller.
 */
class ApiKeyInterceptor(
    private val apiKey: String,
    private val country: String = DEFAULT_COUNTRY,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val urlBuilder = request.url.newBuilder()
            .addQueryParameter("country", country)
        if (apiKey.isNotBlank()) {
            urlBuilder.addQueryParameter("key", apiKey)
        }
        return chain.proceed(
            request.newBuilder()
                .url(urlBuilder.build())
                .build()
        )
    }

    private companion object {
        const val DEFAULT_COUNTRY = "VN"
    }
}
