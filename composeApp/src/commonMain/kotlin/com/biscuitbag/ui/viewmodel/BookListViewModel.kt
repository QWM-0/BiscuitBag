package com.biscuitbag.ui.viewmodel

import com.biscuitbag.data.repository.BiscuitBagRepository
import com.biscuitbag.data.repository.BookEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BookListViewModel(private val repository: BiscuitBagRepository) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _books = MutableStateFlow<List<BookEntity>>(emptyList())
    val books: StateFlow<List<BookEntity>> = _books.asStateFlow()

    fun loadBooks() {
        scope.launch {
            repository.getAllBooks().collect {
                _books.value = it
            }
        }
    }

    fun deleteBook(id: Long) {
        repository.deleteBook(id)
        loadBooks()
    }
}
