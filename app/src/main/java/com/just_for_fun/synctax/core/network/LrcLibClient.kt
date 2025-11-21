package com.just_for_fun.synctax.core.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton object providing access to LRCLIB API
 * Handles Retrofit configuration and API instance creation
 */
object LrcLibClient {
    
    private const val BASE_URL = "https://lrclib.net/"
    private const val USER_AGENT = "SyncTax/1.1.0 (Android Music Player)"
    
    /**
     * OkHttp client with logging and custom headers
     */
    private val okHttpClient: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("User-Agent", USER_AGENT)
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(loggingInterceptor)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
    }
    
    /**
     * Retrofit instance configured for LRCLIB API
     */
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    /**
     * LRCLIB API service instance
     */
    val api: LrcLibApi by lazy {
        retrofit.create(LrcLibApi::class.java)
    }
}
