package com.biscuitbag.ui.viewmodel

import com.biscuitbag.data.repository.BiscuitBagRepository
import com.biscuitbag.data.repository.BookEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BookEditViewModel(
    private val repository: BiscuitBagRepository,
    private val bookId: Long? = null
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _author = MutableStateFlow("")
    val author: StateFlow<String> = _author.asStateFlow()

    private val _totalPages = MutableStateFlow("")
    val totalPages: StateFlow<String> = _totalPages.asStateFlow()

    /** 0 = 书本, 1 = 自定义 */
    private val _bookType = MutableStateFlow(0)
    val bookType: StateFlow<Int> = _bookType.asStateFlow()

    private val _coverPath = MutableStateFlow("")
    val coverPath: StateFlow<String> = _coverPath.asStateFlow()

    private val _isEdit = MutableStateFlow(false)
    val isEdit: StateFlow<Boolean> = _isEdit.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    init {
        bookId?.let { id ->
            repository.getBookById(id)?.let { book ->
                _isEdit.value = true
                _bookType.value = book.type
                _coverPath.value = book.coverPath
                _title.value = if (book.type == 0) unwrapTitle(book.title) else book.title
                _author.value = book.author
                _totalPages.value = book.totalPages.toString()
            }
        }
    }

    fun updateTitle(value: String) { _title.value = value }
    fun updateAuthor(value: String) { _author.value = value }
    fun updateTotalPages(value: String) { _totalPages.value = value }
    fun setBookType(value: Int) { _bookType.value = value }
    fun setCoverPath(value: String) { _coverPath.value = value }

    fun save() {
        val pages = _totalPages.value.toIntOrNull() ?: 0
        val rawTitle = _title.value.ifBlank { "未命名书籍" }
        val finalTitle = if (_bookType.value == 0) "《$rawTitle》" else rawTitle

        if (_isEdit.value && bookId != null) {
            repository.updateBook(bookId, finalTitle, _author.value, pages, _bookType.value, _coverPath.value)
        } else {
            repository.insertBook(finalTitle, _author.value, pages, _bookType.value, _coverPath.value)
        }
        _saved.value = true
    }

    companion object {
        /** 去掉《》 */
        fun unwrapTitle(title: String): String {
            return if (title.startsWith("《") && title.endsWith("》"))
                title.substring(1, title.length - 1)
            else title
        }
    }
}
