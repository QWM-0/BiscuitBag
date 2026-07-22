package com.biscuitbag.data.repository

import com.biscuitbag.database.BiscuitBagDatabase
import app.cash.sqldelight.db.QueryResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock

class BiscuitBagRepository(private val database: BiscuitBagDatabase) {

    private val queries get() = database.biscuitBagQueries

    /** 获取刚插入行的主键 ID */
    private fun lastInsertId(): Long = queries.lastInsertRowId().executeAsOneOrNull() ?: 0L

    // ===== 书籍 =====

    fun getAllBooks(): Flow<List<BookEntity>> = flow {
        val books = queries.selectAllBooks().executeAsList().map { it.toEntity() }
        emit(books)
    }

    fun getBookById(id: Long): BookEntity? {
        return queries.selectBookById(id).executeAsOneOrNull()?.toEntity()
    }

    fun insertBook(title: String, author: String, totalPages: Int, type: Int = 0, coverPath: String = "", thickMode: Boolean = true): Long {
        queries.insertBook(
            title = title,
            author = author,
            totalPages = totalPages.toLong(),
            type = type.toLong(),
            coverPath = coverPath,
            createdAt = Clock.System.now().toEpochMilliseconds(),
            thickMode = if (thickMode) 1L else 0L
        )
        return lastInsertId()
    }

    fun updateBook(id: Long, title: String, author: String, totalPages: Int, type: Int = 0, coverPath: String = "", thickMode: Boolean = true) {
        queries.updateBook(
            title = title,
            author = author,
            totalPages = totalPages.toLong(),
            type = type.toLong(),
            coverPath = coverPath,
            thickMode = if (thickMode) 1L else 0L,
            id = id
        )
    }

    fun deleteBook(id: Long) {
        queries.deleteBook(id)
    }

    // ===== 章节 =====

    fun getChaptersByBookId(bookId: Long): List<ChapterEntity> {
        return queries.selectChaptersByBookId(bookId).executeAsList().map { it.toEntity() }
    }

    fun getChapterById(id: Long): ChapterEntity? {
        return queries.selectChapterById(id).executeAsOneOrNull()?.toEntity()
    }

    /**
     * 获取或创建默认章节（非厚读模式使用）。
     * 如果该书已有章节则返回第一个，否则创建一个总段落数等于总页数的默认章节。
     */
    fun getOrCreateDefaultChapter(bookId: Long): ChapterEntity {
        val existing = queries.selectChaptersByBookId(bookId).executeAsList()
        if (existing.isNotEmpty()) return existing.first().toEntity()

        val book = queries.selectBookById(bookId).executeAsOne()
        queries.insertChapter(
            bookId = bookId,
            chapterNumber = 1L,
            title = "",
            paragraphCount = book.totalPages,
            createdAt = Clock.System.now().toEpochMilliseconds()
        )
        val id = lastInsertId()

        for (i in 0 until book.totalPages.toInt()) {
            queries.insertBreadcrumb(chapterId = id, paragraphIndex = i.toLong())
        }
        return queries.selectChapterById(id).executeAsOne().toEntity()
    }

    fun insertChapter(bookId: Long, chapterNumber: Int, title: String, paragraphCount: Int): Long {
        queries.insertChapter(
            bookId = bookId,
            chapterNumber = chapterNumber.toLong(),
            title = title,
            paragraphCount = paragraphCount.toLong(),
            createdAt = Clock.System.now().toEpochMilliseconds()
        )
        val id = lastInsertId()

        // 创建对应数量的饼干屑
        for (i in 0 until paragraphCount) {
            queries.insertBreadcrumb(
                chapterId = id,
                paragraphIndex = i.toLong()
            )
        }
        return id
    }

    fun updateChapter(id: Long, title: String, paragraphCount: Int) {
        val old = queries.selectChapterById(id).executeAsOne()
        queries.updateChapter(
            title = title,
            paragraphCount = paragraphCount.toLong(),
            id = id
        )
        // 如果段落数变了，重建饼干屑
        if (old.paragraphCount != paragraphCount.toLong()) {
            queries.deleteChapter(id) // 级联删除旧的饼干屑
            insertChapter(old.bookId, old.chapterNumber.toInt(), title, paragraphCount)
        }
    }

    fun deleteChapter(id: Long) {
        queries.deleteChapter(id)
    }

    // ===== 饼干屑 =====

    fun getBreadcrumbsByChapterId(chapterId: Long): List<BreadcrumbEntity> {
        return queries.selectBreadcrumbsByChapterId(chapterId).executeAsList().map { it.toEntity() }
    }

    fun toggleBreadcrumb(id: Long, isRead: Boolean) {
        val readAt = if (isRead) Clock.System.now().toEpochMilliseconds() else null
        queries.updateBreadcrumb(
            isRead = if (isRead) 1L else 0L,
            readAt = readAt,
            id = id
        )
        if (isRead) {
            recordTodayReading()
        }
    }

    fun getReadCountByChapterId(chapterId: Long): Long {
        return queries.countReadBreadcrumbs(chapterId).executeAsOne()
    }

    fun getReadCountByBookId(bookId: Long): Long {
        return queries.countReadBreadcrumbsByBookId(bookId).executeAsOne()
    }

    fun getTotalBreadcrumbCountByBookId(bookId: Long): Long {
        return queries.countTotalBreadcrumbsByBookId(bookId).executeAsOne()
    }

    // ===== 阅读记录 =====

    private fun recordTodayReading() {
        val today = kotlinx.datetime.Clock.System.now()
            .toString().substringBefore('T') // yyyy-MM-dd
        val existing = queries.selectReadingRecordByDate(today).executeAsOneOrNull()
        if (existing != null) {
            queries.updateReadingRecord(today)
        } else {
            queries.insertReadingRecord(today, 1)
        }
    }

    fun getAllReadingRecords(): List<ReadingRecordEntity> {
        return queries.selectAllReadingRecords().executeAsList().map { it.toEntity() }
    }

    fun getTotalReadingDays(): Long {
        return queries.countReadingDays().executeAsOne()
    }

    fun getTotalCompletedBreadcrumbs(): Long {
        return queries.totalCompletedBreadcrumbs().executeAsOne()
    }
}

// ===== 实体映射 =====

data class BookEntity(
    val id: Long,
    val title: String,
    val author: String,
    val totalPages: Int,
    val type: Int,
    val coverPath: String,
    val createdAt: Long,
    val thickMode: Boolean = true
)

data class ChapterEntity(
    val id: Long,
    val bookId: Long,
    val chapterNumber: Int,
    val title: String,
    val paragraphCount: Int,
    val createdAt: Long
)

data class BreadcrumbEntity(
    val id: Long,
    val chapterId: Long,
    val paragraphIndex: Int,
    val isRead: Boolean,
    val readAt: Long?
)

data class ReadingRecordEntity(
    val id: Long,
    val date: String,
    val breadcrumbsCompleted: Int
)

// ===== SQLDelight 结果映射 =====

private fun com.biscuitbag.Book.toEntity() = BookEntity(
    id = id,
    title = title,
    author = author,
    totalPages = totalPages.toInt(),
    type = type.toInt(),
    coverPath = coverPath,
    createdAt = createdAt,
    thickMode = thickMode != 0L
)

private fun com.biscuitbag.Chapter.toEntity() = ChapterEntity(
    id = id,
    bookId = bookId,
    chapterNumber = chapterNumber.toInt(),
    title = title,
    paragraphCount = paragraphCount.toInt(),
    createdAt = createdAt
)

private fun com.biscuitbag.Breadcrumb.toEntity() = BreadcrumbEntity(
    id = id,
    chapterId = chapterId,
    paragraphIndex = paragraphIndex.toInt(),
    isRead = isRead != 0L,
    readAt = readAt
)

private fun com.biscuitbag.ReadingRecord.toEntity() = ReadingRecordEntity(
    id = id,
    date = date,
    breadcrumbsCompleted = breadcrumbsCompleted.toInt()
)
