package com.biscuitbag.import

import kotlinx.serialization.Serializable

/**
 * 微信读书书架 API 响应模型。
 * API: GET https://weread.qq.com/web/shelf/sync
 * 需要 Cookie 头：wr_skey=xxx
 */
@Serializable
data class WRShelfResponse(
    val books: List<WRBook> = emptyList(),
    val success: Boolean = false
)

@Serializable
data class WRBook(
    val bookId: String = "",
    val title: String = "",
    val author: String = "",
    val cover: String = "",           // 封面 URL
    val readingProgress: Double = 0.0, // 阅读进度百分比（0~1）
    val readTime: Long = 0,           // 累计阅读时间（秒）
    val chapterCount: Int = 0,        // 总章节数
    val pageCount: Int = 0,           // 总页数
    val isbn: String = "",
    val publisher: String = ""
)
