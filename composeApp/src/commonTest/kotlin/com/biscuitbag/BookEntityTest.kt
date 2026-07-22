package com.biscuitbag

import com.biscuitbag.data.repository.BookEntity
import com.biscuitbag.data.repository.ChapterEntity
import com.biscuitbag.data.repository.BreadcrumbEntity
import com.biscuitbag.data.repository.ReadingRecordEntity
import com.biscuitbag.ui.viewmodel.BookEditViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BookEntityTest {

    @Test
    fun testBookEntityProperties() {
        val book = BookEntity(
            id = 1,
            title = "《测试书籍》",
            author = "测试作者",
            totalPages = 300,
            type = 0,
            coverPath = "/path/cover.jpg",
            createdAt = 1700000000000L,
            thickMode = true
        )
        assertEquals(1, book.id)
        assertEquals("《测试书籍》", book.title)
        assertEquals("测试作者", book.author)
        assertEquals(300, book.totalPages)
        assertEquals(0, book.type)
        assertEquals("/path/cover.jpg", book.coverPath)
        assertEquals(1700000000000L, book.createdAt)
        assertTrue(book.thickMode)
    }

    @Test
    fun testChapterEntityProperties() {
        val chapter = ChapterEntity(
            id = 1,
            bookId = 10,
            chapterNumber = 3,
            title = "第三章",
            paragraphCount = 50,
            createdAt = 1700000000000L
        )
        assertEquals(1, chapter.id)
        assertEquals(10, chapter.bookId)
        assertEquals(3, chapter.chapterNumber)
        assertEquals("第三章", chapter.title)
        assertEquals(50, chapter.paragraphCount)
    }

    @Test
    fun testBreadcrumbEntityProperties() {
        val breadcrumb = BreadcrumbEntity(
            id = 1,
            chapterId = 5,
            paragraphIndex = 2,
            isRead = true,
            readAt = 1700000000000L
        )
        assertEquals(1, breadcrumb.id)
        assertEquals(5, breadcrumb.chapterId)
        assertEquals(2, breadcrumb.paragraphIndex)
        assertTrue(breadcrumb.isRead)
        assertEquals(1700000000000L, breadcrumb.readAt)
    }

    @Test
    fun testReadingRecordEntityProperties() {
        val record = ReadingRecordEntity(
            id = 1,
            date = "2024-01-15",
            breadcrumbsCompleted = 42
        )
        assertEquals(1, record.id)
        assertEquals("2024-01-15", record.date)
        assertEquals(42, record.breadcrumbsCompleted)
    }

    @Test
    fun testUnwrapTitleWithBookmarks() {
        assertEquals("三体", BookEditViewModel.unwrapTitle("《三体》"))
        assertEquals("百年孤独", BookEditViewModel.unwrapTitle("《百年孤独》"))
    }

    @Test
    fun testUnwrapTitleWithoutBookmarks() {
        assertEquals("普通标题", BookEditViewModel.unwrapTitle("普通标题"))
        assertEquals("", BookEditViewModel.unwrapTitle(""))
    }

    @Test
    fun testUnwrapTitlePartialBookmarks() {
        // 只有左书名号
        assertEquals("《只有左", BookEditViewModel.unwrapTitle("《只有左"))
        // 只有右书名号
        assertEquals("只有右》", BookEditViewModel.unwrapTitle("只有右》"))
    }

    @Test
    fun testWrapTitle() {
        assertEquals("《三体》", BookEditViewModel.wrapTitle("三体"))
        assertEquals("《百年孤独》", BookEditViewModel.wrapTitle("百年孤独"))
    }

    @Test
    fun testBreadcrumbIsReadDefault() {
        val breadcrumb = BreadcrumbEntity(
            id = 1,
            chapterId = 1,
            paragraphIndex = 0,
            isRead = false,
            readAt = null
        )
        assertFalse(breadcrumb.isRead)
        assertEquals(null, breadcrumb.readAt)
    }

    @Test
    fun testThickModeFalse() {
        val book = BookEntity(
            id = 2,
            title = "《非厚读书籍》",
            author = "作者",
            totalPages = 100,
            type = 0,
            coverPath = "",
            createdAt = 1700000000000L,
            thickMode = false
        )
        assertFalse(book.thickMode)
    }
}
