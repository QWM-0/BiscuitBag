package com.biscuitbag.ui.viewmodel

import com.biscuitbag.data.repository.BiscuitBagRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ChapterEditViewModel(
    private val repository: BiscuitBagRepository,
    private val bookId: Long,
    private val chapterId: Long? = null
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _paragraphCount = MutableStateFlow("")
    val paragraphCount: StateFlow<String> = _paragraphCount.asStateFlow()

    private val _usePageEstimate = MutableStateFlow(true)
    val usePageEstimate: StateFlow<Boolean> = _usePageEstimate.asStateFlow()

    private val _pagesForChapter = MutableStateFlow("")
    val pagesForChapter: StateFlow<String> = _pagesForChapter.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    private val _isEdit = MutableStateFlow(false)
    val isEdit: StateFlow<Boolean> = _isEdit.asStateFlow()

    init {
        chapterId?.let { id ->
            repository.getChapterById(id)?.let { ch ->
                _isEdit.value = true
                _title.value = ch.title
                _paragraphCount.value = ch.paragraphCount.toString()
                _usePageEstimate.value = false
            }
        }
    }

    fun updateTitle(value: String) { _title.value = value }
    fun updateParagraphCount(value: String) { _paragraphCount.value = value }
    fun updatePagesForChapter(value: String) { _pagesForChapter.value = value }
    fun setUsePageEstimate(value: Boolean) { _usePageEstimate.value = value }

    /** 按页数估算段落数：假设每页约 3-5 个段落 */
    fun estimatedParagraphs(): Int {
        val pages = _pagesForChapter.value.toIntOrNull() ?: 0
        return pages * 4 // 每页平均 4 段
    }

    fun save() {
        val count = if (_usePageEstimate.value) {
            estimatedParagraphs()
        } else {
            _paragraphCount.value.toIntOrNull() ?: 0
        }
        if (count <= 0) return

        if (_isEdit.value && chapterId != null) {
            repository.updateChapter(chapterId, _title.value, count)
        } else {
            val chapters = repository.getChaptersByBookId(bookId)
            val nextNumber = (chapters.maxOfOrNull { it.chapterNumber } ?: 0) + 1
            val finalTitle = _title.value.ifBlank { "第${nextNumber}章" }
            repository.insertChapter(bookId, nextNumber, finalTitle, count)
        }
        _saved.value = true
    }
}
