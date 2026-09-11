package com.example.docsach.di

import android.content.Context
import com.example.docsach.BuildConfig
import com.example.docsach.data.BookRepository
import com.example.docsach.data.local.AppDatabase
import com.example.docsach.data.remote.ApiKeyInterceptor
import com.example.docsach.data.remote.GoogleBooksApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Hand-rolled dependency graph. A DI framework would mean a second annotation processor on top of
 * Room's, which buys nothing at this size.
 */
class AppContainer(context: Context) {

    private val apiKey: String = BuildConfig.GOOGLE_BOOKS_API_KEY

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(ApiKeyInterceptor(apiKey))
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = if (BuildConfig.DEBUG) {
                        HttpLoggingInterceptor.Level.BASIC
                    } else {
                        HttpLoggingInterceptor.Level.NONE
                    }
                }
            )
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    private val api: GoogleBooksApi by lazy {
        Retrofit.Builder()
            .baseUrl(GoogleBooksApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GoogleBooksApi::class.java)
    }

    private val database: AppDatabase by lazy { AppDatabase.build(context.applicationContext) }

    val bookRepository: BookRepository by lazy {
        BookRepository(
            api = api,
            savedBookDao = database.savedBookDao(),
            progressDao = database.readingProgressDao(),
            apiKey = apiKey,
        )
    }
}
