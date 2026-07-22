package com.biscuitbag

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.biscuitbag.data.repository.BiscuitBagRepository
import com.biscuitbag.database.BiscuitBagDatabase
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BiscuitBagCRUDTest {

    private lateinit var database: BiscuitBagDatabase
    private lateinit var repository: BiscuitBagRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val driver = AndroidSqliteDriver(BiscuitBagDatabase.Schema, context, null) // 内存数据库
        database = BiscuitBagDatabase(driver)
        repository = BiscuitBagRepository(database)
    }

    @After
    fun tearDown() {
        // 内存数据库会在每个测试后自动丢弃，无需额外清理
    }

    // ==================== Book CRUD ====================

    @Test
    fun testInsertAndQueryBook() {
        val id = repository.insertBook(
            title = "《测试书籍》",
            author = "测试作者",
            totalPages = 300,
            type = 0,
            coverPath = "/path/cover.jpg",
            thickMode = true
        )
        assertTrue(id > 0)

        val book = repository.getBookById(id)
        assertNotNull(book)
        assertEquals("《测试书籍》", book!!.title)
        assertEquals("测试作者", book.author)
        assertEquals(300, book.totalPages)
        assertEquals(0, book.type)
        assertEquals("/path/cover.jpg", book.coverPath)
        assertTrue(book.thickMode)
    }

    @Test
    fun testUpdateBook() {
        val id = repository.insertBook(
            title = "《原始标题》",
            author = "原始作者",
            totalPages = 100,
            type = 0,
            thickMode = true
        )

        repository.updateBook(
            id = id,
            title = "《更新标题》",
            author = "更新作者",
            totalPages = 200,
            type = 0,
            coverPath = "/new/cover.jpg",
            thickMode = false
        )

        val book = repository.getBookById(id)
        assertNotNull(book)
        assertEquals("《更新标题》", book!!.title)
        assertEquals("更新作者", book.author)
        assertEquals(200, book.totalPages)
        assertEquals("/new/cover.jpg", book.coverPath)
        assertFalse(book.thickMode)
    }

    @Test
    fun testDeleteBook() {
        val id = repository.insertBook(
            title = "《待删除》",
            author = "作者",
            totalPages = 50,
            type = 0,
            thickMode = true
        )
        assertNotNull(repository.getBookById(id))

        repository.deleteBook(id)
        assertNull(repository.getBookById(id))
    }

    @Test
    fun testGetAllBooksOrder() = runTest {
        val id1 = repository.insertBook(title = "《第一本》", author = "A", totalPages = 100, thickMode = true)
        Thread.sleep(10) // 确保时间戳不同
        val id2 = repository.insertBook(title = "《第二本》", author = "B", totalPages = 200, thickMode = true)

        val books = repository.getAllBooks().firstOrNull() ?: emptyList()
        assertTrue(books.size >= 2)

        // 按 createdAt DESC 排序，最新创建的在前面
        val firstBook = books.first()
        assertEquals(id2, firstBook.id)
    }

    // ==================== Chapter CRUD ====================

    @Test
    fun testInsertAndQueryChapter() {
        val bookId = repository.insertBook(title = "《书》", author = "A", totalPages = 100, thickMode = true)

        val chapterId = repository.insertChapter(
            bookId = bookId,
            chapterNumber = 1,
            title = "第一章",
            paragraphCount = 10
        )
        assertTrue(chapterId > 0)

        val chapter = repository.getChapterById(chapterId)
        assertNotNull(chapter)
        assertEquals("第一章", chapter!!.title)
        assertEquals(1, chapter.chapterNumber)
        assertEquals(10, chapter.paragraphCount)
        assertEquals(bookId, chapter.bookId)
    }

    @Test
    fun testInsertChapterCreatesBreadcrumbs() {
        val bookId = repository.insertBook(title = "《书》", author = "A", totalPages = 100, thickMode = true)

        val chapterId = repository.insertChapter(
            bookId = bookId,
            chapterNumber = 1,
            title = "第一章",
            paragraphCount = 5
        )

        val breadcrumbs = repository.getBreadcrumbsByChapterId(chapterId)
        assertEquals(5, breadcrumbs.size)
        // 验证 paragraphIndex 从 0 开始
        for (i in 0 until 5) {
            assertEquals(i, breadcrumbs[i].paragraphIndex)
            assertFalse(breadcrumbs[i].isRead)
        }
    }

    @Test
    fun testDeleteChapterCascadeDeleteBreadcrumbs() {
        val bookId = repository.insertBook(title = "《书》", author = "A", totalPages = 100, thickMode = true)
        val chapterId = repository.insertChapter(bookId = bookId, chapterNumber = 1, title = "C1", paragraphCount = 3)

        // 确认饼干屑存在
        assertEquals(3, repository.getBreadcrumbsByChapterId(chapterId).size)

        // 删除章节
        repository.deleteChapter(chapterId)

        // 章节应不存在
        assertNull(repository.getChapterById(chapterId))
        // 饼干屑应级联删除
        assertEquals(0, repository.getBreadcrumbsByChapterId(chapterId).size)
    }

    @Test
    fun testGetChaptersByBookId() {
        val bookId = repository.insertBook(title = "《书》", author = "A", totalPages = 100, thickMode = true)

        repository.insertChapter(bookId = bookId, chapterNumber = 1, title = "第一章", paragraphCount = 5)
        repository.insertChapter(bookId = bookId, chapterNumber = 2, title = "第二章", paragraphCount = 8)
        repository.insertChapter(bookId = bookId, chapterNumber = 3, title = "第三章", paragraphCount = 3)

        val chapters = repository.getChaptersByBookId(bookId)
        assertEquals(3, chapters.size)
        // 按 chapterNumber 排序
        assertEquals(1, chapters[0].chapterNumber)
        assertEquals(2, chapters[1].chapterNumber)
        assertEquals(3, chapters[2].chapterNumber)
    }

    // ==================== Breadcrumb ====================

    @Test
    fun testToggleBreadcrumbReadStatus() {
        val bookId = repository.insertBook(title = "《书》", author = "A", totalPages = 100, thickMode = true)
        val chapterId = repository.insertChapter(bookId = bookId, chapterNumber = 1, title = "C1", paragraphCount = 3)

        val breadcrumbs = repository.getBreadcrumbsByChapterId(chapterId)
        assertEquals(3, breadcrumbs.size)
        assertFalse(breadcrumbs[0].isRead)

        // 标记为已读
        repository.toggleBreadcrumb(breadcrumbs[0].id, isRead = true)
        val updated = repository.getBreadcrumbsByChapterId(chapterId)
        assertTrue(updated[0].isRead)
        assertNotNull(updated[0].readAt)

        // 取消已读
        repository.toggleBreadcrumb(breadcrumbs[0].id, isRead = false)
        val reverted = repository.getBreadcrumbsByChapterId(chapterId)
        assertFalse(reverted[0].isRead)
    }

    @Test
    fun testCountReadBreadcrumbs() {
        val bookId = repository.insertBook(title = "《书》", author = "A", totalPages = 100, thickMode = true)
        val chapterId = repository.insertChapter(bookId = bookId, chapterNumber = 1, title = "C1", paragraphCount = 5)

        val breadcrumbs = repository.getBreadcrumbsByChapterId(chapterId)

        // 初始全部未读
        assertEquals(0, repository.getReadCountByChapterId(chapterId))

        // 标记前 3 个为已读
        repository.toggleBreadcrumb(breadcrumbs[0].id, isRead = true)
        repository.toggleBreadcrumb(breadcrumbs[1].id, isRead = true)
        repository.toggleBreadcrumb(breadcrumbs[2].id, isRead = true)

        assertEquals(3, repository.getReadCountByChapterId(chapterId))
    }

    @Test
    fun testCountReadBreadcrumbsByBookId() {
        val bookId = repository.insertBook(title = "《书》", author = "A", totalPages = 100, thickMode = true)
        val ch1 = repository.insertChapter(bookId = bookId, chapterNumber = 1, title = "C1", paragraphCount = 3)
        val ch2 = repository.insertChapter(bookId = bookId, chapterNumber = 2, title = "C2", paragraphCount = 3)

        val bc1 = repository.getBreadcrumbsByChapterId(ch1)
        val bc2 = repository.getBreadcrumbsByChapterId(ch2)

        // 标记 ch1 全部已读，ch2 第 1 个已读
        repository.toggleBreadcrumb(bc1[0].id, isRead = true)
        repository.toggleBreadcrumb(bc1[1].id, isRead = true)
        repository.toggleBreadcrumb(bc1[2].id, isRead = true)
        repository.toggleBreadcrumb(bc2[0].id, isRead = true)

        assertEquals(4, repository.getReadCountByBookId(bookId))
        assertEquals(6, repository.getTotalBreadcrumbCountByBookId(bookId))
    }
}
