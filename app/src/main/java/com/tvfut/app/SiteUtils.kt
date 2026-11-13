package com.tvfut.app

object SiteUtils {
    private val secondLevel = setOf(
        "co.uk", "com.ar", "com.br", "com.co", "com.mx", "com.pe", "com.ve",
        "com.ec", "com.uy", "com.py", "com.bo", "com.cl", "com.do", "com.gt",
        "com.au", "co.nz", "co.jp", "co.kr", "com.tr", "com.cn", "com.sg",
        "com.hk", "com.tw", "co.in", "com.my", "com.ph", "com.pk", "co.za"
    )

    fun siteKey(host: String?): String {
        if (host.isNullOrBlank()) return ""
        val h = host.lowercase().trimEnd('.')
        if (h.matches(Regex("\\d+(\\.\\d+){3}"))) return h
        val parts = h.split('.')
        if (parts.size <= 2) return h
        val lastTwo = parts.takeLast(2).joinToString(".")
        return if (lastTwo in secondLevel && parts.size >= 3) {
            parts.takeLast(3).joinToString(".")
        } else {
            lastTwo
        }
    }

    fun sameSite(a: String?, b: String?): Boolean {
        val ka = siteKey(a)
        return ka.isNotEmpty() && ka == siteKey(b)
    }
}
