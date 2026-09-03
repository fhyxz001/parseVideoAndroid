package com.videoparser.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class Author(val name: String)

data class VideoData(
    val videoUrl: String,
    val coverUrl: String,
    val title: String,
    val author: Author?
)

sealed class ParseResult {
    data class Success(val data: VideoData) : ParseResult()
    data class Error(val message: String) : ParseResult()
}

object VideoApi {

    /** 默认解析服务器地址 */
    const val DEFAULT_SERVER = "http://122.51.115.245:8888"

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun parse(url: String, baseUrl: String = DEFAULT_SERVER): ParseResult = withContext(Dispatchers.IO) {
        try {
            val base = baseUrl.trim().trimEnd('/').ifBlank { DEFAULT_SERVER }
            val encoded = URLEncoder.encode(url.trim(), "UTF-8")
            val request = Request.Builder()
                .url("$base/video/share/url/parse?url=$encoded")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                if (!response.isSuccessful || body.isNullOrBlank()) {
                    return@withContext ParseResult.Error("请求失败，请重试")
                }
                val json = JSONObject(body)
                if (json.optInt("code") != 200) {
                    return@withContext ParseResult.Error(json.optString("msg", "解析失败"))
                }
                val data = json.optJSONObject("data")
                    ?: return@withContext ParseResult.Error("解析失败")
                val authorObj = data.optJSONObject("author")
                val authorName = authorObj?.optString("name").orEmpty()
                ParseResult.Success(
                    VideoData(
                        videoUrl = data.optString("video_url"),
                        coverUrl = data.optString("cover_url"),
                        title = data.optString("title"),
                        author = if (authorName.isNotBlank()) Author(authorName) else null
                    )
                )
            }
        } catch (e: Exception) {
            ParseResult.Error("网络错误，请检查网络")
        }
    }
}
