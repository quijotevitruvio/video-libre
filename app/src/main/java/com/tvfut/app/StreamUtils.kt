package com.tvfut.app

import android.net.Uri

object StreamUtils {

    private val mediaExtensions = listOf(
        ".m3u8", ".mpd", ".mp4", ".m4v", ".webm", ".mkv", ".mov"
    )

    fun isMediaUrl(url: String): Boolean {
        if (!url.startsWith("http://") && !url.startsWith("https://")) return false

        val path = try {
            (Uri.parse(url).path ?: "").lowercase()
        } catch (_: Exception) {
            ""
        }
        if (mediaExtensions.any { path.endsWith(it) }) return true

        val lower = url.lowercase()
        return lower.contains(".m3u8?") || lower.contains(".mpd?")
    }

    fun looksLikeStream(url: String): Boolean = isMediaUrl(url)
}
