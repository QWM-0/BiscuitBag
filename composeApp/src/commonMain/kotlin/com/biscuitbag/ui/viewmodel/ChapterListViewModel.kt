package com.biscuitbag.ui.viewmodel

import com.biscuitbag.data.repository.BiscuitBagRepository
import com.biscuitbag.data.repository.BookEntity
import com.biscuitbag.data.repository.ChapterEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChapterListViewModel(
    private val repository: BiscuitBagRepository,
    private val bookId: Long
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _book = MutableStateFlow<BookEntity?>(null)
    val book: StateFlow<BookEntity?> = _book.asStateFlow()

    private val _chapters = MutableStateFlow<List<ChapterWithProgress>>(emptyList())
    val chapters: StateFlow<List<ChapterWithProgress>> = _chapters.asStateFlow()

    fun load() {
        _book.value = repository.getBookById(bookId)
        loadChapters()
    }

    private fun loadChapters() {
        val chapters = repository.getChaptersByBookId(bookId)
        _chapters.value = chapters.map { ch ->
            val readCount = repository.getReadCountByChapterId(ch.id)
            ChapterWithProgress(ch, readCount.toInt(), ch.paragraphCount)
        }
    }

    fun deleteChapter(id: Long) {
        repository.deleteChapter(id)
        load()
    }
}

data class ChapterWithProgress(
    val chapter: ChapterEntity,
    val readCount: Int,
    val totalCount: Int
) {
    val progress: Float get() = if (totalCount > 0) readCount.toFloat() / totalCount else 0f
}
