package com.tvfut.app

import android.content.Context
import android.net.Uri

class AdBlocker(context: Context) {

    private val appContext = context.applicationContext
    private val prefs =
        appContext.getSharedPreferences("tvfut_prefs", Context.MODE_PRIVATE)

    private val hosts: Set<String> by lazy { loadHosts() }
    private val script: String by lazy { loadScript() }

    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_ENABLED, value).apply()
        }

    fun isBlocked(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val host = try {
            Uri.parse(url).host
        } catch (_: Exception) {
            null
        }
        return isBlocked(host, null)
    }

    fun isBlocked(host: String?, path: String?): Boolean {
        if (!enabled || host.isNullOrBlank()) return false

        var target = host.lowercase()
        if (target.startsWith("www.")) {
            target = target.substring(4)
        }
        if (hosts.contains(target)) return true

        var dot = target.indexOf('.')
        while (dot >= 0) {
            if (hosts.contains(target.substring(dot + 1))) return true
            dot = target.indexOf('.', dot + 1)
        }
        return false
    }

    fun cosmeticScript(): String = script

    private fun loadHosts(): Set<String> {
        val result = HashSet<String>()
        try {
            appContext.assets.open("ad_hosts.txt").bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    val value = line.trim().lowercase()
                    if (value.isNotEmpty() && !value.startsWith("#")) {
                        result.add(value)
                    }
                }
            }
        } catch (_: Exception) {
        }
        return result
    }

    private fun loadScript(): String = try {
        appContext.assets.open("cosmetic.js").bufferedReader().use { it.readText() }
    } catch (_: Exception) {
        ""
    }

    companion object {
        private const val KEY_ENABLED = "adblock_enabled"
    }
}
