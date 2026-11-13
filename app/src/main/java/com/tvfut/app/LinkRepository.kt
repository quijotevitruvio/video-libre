package com.tvfut.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class LinkItem(
    val id: String,
    val title: String,
    val url: String
)

class LinkRepository(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences("tvfut_links", Context.MODE_PRIVATE)

    fun load(): MutableList<LinkItem> {
        val raw = prefs.getString(KEY, null) ?: return seed()
        return try {
            val array = JSONArray(raw)
            val result = mutableListOf<LinkItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val url = obj.optString("url").trim()
                if (url.isNotEmpty()) {
                    result.add(
                        LinkItem(
                            id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
                            title = obj.optString("title").trim().ifBlank { url },
                            url = url
                        )
                    )
                }
            }
            result
        } catch (_: Exception) {
            mutableListOf()
        }
    }

    fun save(items: List<LinkItem>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("title", item.title)
                    .put("url", item.url)
            )
        }
        prefs.edit().putString(KEY, array.toString()).apply()
    }

    private fun seed(): MutableList<LinkItem> {
        val demos = mutableListOf(
            LinkItem(
                UUID.randomUUID().toString(),
                "Big Buck Bunny (demo MP4)",
                "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
            ),
            LinkItem(
                UUID.randomUUID().toString(),
                "HLS de prueba (demo M3U8)",
                "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
            )
        )
        save(demos)
        return demos
    }

    companion object {
        private const val KEY = "links_json"
    }
}
