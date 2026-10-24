package com.example.data.api

import android.content.Context
import android.content.SharedPreferences
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class ApiClient private constructor(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("leaf_settings", Context.MODE_PRIVATE)

    val cookieJar = SessionCookieJar(context)

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    val okHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private var currentRetrofit: Retrofit? = null
    private var currentService: LeafApiService? = null
    private var currentBaseUrl: String = ""

    companion object {
        const val DEFAULT_BASE_URL = "http://10.0.2.2:5000/"
        private const val KEY_BASE_URL = "server_base_url"

        @Volatile
        private var INSTANCE: ApiClient? = null

        fun getInstance(context: Context): ApiClient {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ApiClient(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    fun getBaseUrl(): String {
        val stored = prefs.getString(KEY_BASE_URL, null)
        val url = if (stored.isNullOrBlank()) DEFAULT_BASE_URL else stored
        return if (url.endsWith("/")) url else "$url/"
    }

    fun setBaseUrl(newUrl: String) {
        var formatted = newUrl.trim()
        if (!formatted.startsWith("http://") && !formatted.startsWith("https://")) {
            formatted = "http://$formatted"
        }
        if (!formatted.endsWith("/")) {
            formatted = "$formatted/"
        }
        prefs.edit().putString(KEY_BASE_URL, formatted).apply()
        // Invalidate retrofit instance so it is recreated with new base URL
        synchronized(this) {
            currentRetrofit = null
            currentService = null
        }
    }

    @Synchronized
    fun getService(): LeafApiService {
        val baseUrl = getBaseUrl()
        if (currentService == null || currentBaseUrl != baseUrl) {
            currentBaseUrl = baseUrl
            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
            currentRetrofit = retrofit
            currentService = retrofit.create(LeafApiService::class.java)
        }
        return currentService!!
    }

    /**
     * Resolves a stream URL returned from /api/stream/{track_id}.
     * If it is relative (e.g. /api/yt-audio/... or /static/uploads/...), prepends base URL.
     */
    fun resolveMediaUrl(rawUrl: String): String {
        if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
            return rawUrl
        }
        val base = getBaseUrl().removeSuffix("/")
        val path = if (rawUrl.startsWith("/")) rawUrl else "/$rawUrl"
        return "$base$path"
    }
}
