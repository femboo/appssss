package com.example.data.api

import android.content.Context
import android.content.SharedPreferences
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class SessionCookieJar(context: Context) : CookieJar {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("leaf_cookie_prefs", Context.MODE_PRIVATE)
    private val cookieStore = mutableMapOf<String, MutableList<Cookie>>()

    init {
        loadPersistedCookies()
    }

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val host = url.host
        val currentCookies = cookieStore.getOrPut(host) { mutableListOf() }
        
        for (cookie in cookies) {
            // Replace existing cookie with same name
            currentCookies.removeAll { it.name == cookie.name }
            currentCookies.add(cookie)
        }
        persistCookies()
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val host = url.host
        val matchingCookies = mutableListOf<Cookie>()
        
        // Exact match or domain match
        cookieStore.forEach { (storedHost, cookies) ->
            if (host == storedHost || host.endsWith(".$storedHost") || storedHost.endsWith(".$host")) {
                matchingCookies.addAll(cookies.filter { it.matches(url) })
            }
        }
        return matchingCookies
    }

    @Synchronized
    fun clearCookies() {
        cookieStore.clear()
        prefs.edit().clear().apply()
    }

    private fun persistCookies() {
        val editor = prefs.edit()
        editor.clear()
        cookieStore.forEach { (host, cookies) ->
            val serialized = cookies.joinToString("|||") { cookie ->
                "${cookie.name}===${cookie.value}===${cookie.domain}===${cookie.path}===${cookie.secure}===${cookie.httpOnly}===${cookie.expiresAt}"
            }
            editor.putString(host, serialized)
        }
        editor.apply()
    }

    private fun loadPersistedCookies() {
        val all = prefs.all
        for ((host, value) in all) {
            if (value is String) {
                val list = mutableListOf<Cookie>()
                val items = value.split("|||")
                for (item in items) {
                    val parts = item.split("===")
                    if (parts.size >= 7) {
                        try {
                            val builder = Cookie.Builder()
                                .name(parts[0])
                                .value(parts[1])
                                .domain(parts[2].removePrefix("."))
                                .path(parts[3])
                                .expiresAt(parts[6].toLongOrNull() ?: Long.MAX_VALUE)
                            if (parts[4].toBoolean()) builder.secure()
                            if (parts[5].toBoolean()) builder.httpOnly()
                            list.add(builder.build())
                        } catch (_: Exception) {}
                    }
                }
                if (list.isNotEmpty()) {
                    cookieStore[host] = list
                }
            }
        }
    }
}
