package com.biscuitbag.import

import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

/**
 * 微信读书 API 读取器。
 * 使用 Cookie（wr_skey）访问微信读书公开 API，获取用户书架数据。
 *
 * 由于 KMP 跨平台网络请求的限制，该类放在 androidMain 中，
 * 使用 java.net.HttpURLConnection 进行网络请求。
 */
class WeChatReadReader {

    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val SHELF_API = "https://weread.qq.com/web/shelf/sync"
        private const val TIMEOUT_MS = 15_000
    }

    /**
     * 用给定的 Cookie 获取微信读书书架列表。
     * @param cookie Cookie 字符串，如 "wr_skey=abc123; wr_pf=xxx"
     * @return 解析后的书架响应，失败时返回空列表的 WRShelfResponse
     */
    fun fetchShelf(cookie: String): WRShelfResponse {
        return try {
            val url = URL(SHELF_API)
            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                setRequestProperty("Cookie", cookie)
                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
            }

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                // 读取错误流获取详情
                val errorBody = try {
                    connection.errorStream?.bufferedReader()?.readText() ?: ""
                } catch (_: Exception) {
                    ""
                }
                android.util.Log.w("WeChatRead", "API 返回 $responseCode: $errorBody")
                return WRShelfResponse(success = false)
            }

            val body = connection.inputStream.bufferedReader().readText()
            json.decodeFromString<WRShelfResponse>(body)
        } catch (e: Exception) {
            android.util.Log.e("WeChatRead", "请求书架失败", e)
            WRShelfResponse(success = false)
        }
    }
}
