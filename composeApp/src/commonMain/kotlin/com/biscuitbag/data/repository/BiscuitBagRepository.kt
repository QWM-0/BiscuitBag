package com.biscuitbag.data.repository

import com.biscuitbag.database.BiscuitBagDatabase
import app.cash.sqldelight.db.QueryResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock

class BiscuitBagRepository(private val database: BiscuitBagDatabase) {

    private val queries get() = database.biscuitBagQueries

    @Suppress("UNCHECKED_CAST")
    private fun <T> QueryResult<T>.getValue(): T = (this as QueryResult.Value<T>).value

    // ===== 书籍 =====

    fun getAllBooks(): Flow<List<BookEntity>> = flow {
        val books = queries.selectAllBooks().executeAsList().map { it.toEntity() }
        emit(books)
    }

    fun getBookById(id: Long): BookEntity? {
        return queries.selectBookById(id).executeAsOneOrNull()?.toEntity()
    }

    fun insertBook(title: String, author: String, totalPages: Int, type: Int = 0, coverPath: String = "") {
        queries.insertBook(
            title = title,
            author = author,
            totalPages = totalPages.toLong(),
            type = type.toLong(),
            coverPath = coverPath,
            createdAt = Clock.System.now().toEpochMilliseconds()
        )
    }

    fun updateBook(id: Long, title: String, author: String, totalPages: Int, type: Int = 0, coverPath: String = "") {
        queries.updateBook(
            title = title,
            author = author,
            totalPages = totalPages.toLong(),
            type = type.toLong(),
            coverPath = coverPath,
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

    fun insertChapter(bookId: Long, chapterNumber: Int, title: String, paragraphCount: Int): Long {
        val id = queries.insertChapter(
            bookId = bookId,
            chapterNumber = chapterNumber.toLong(),
            title = title,
            paragraphCount = paragraphCount.toLong(),
            createdAt = Clock.System.now().toEpochMilliseconds()
        ).getValue()

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
    val createdAt: Long
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
    createdAt = createdAt
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
