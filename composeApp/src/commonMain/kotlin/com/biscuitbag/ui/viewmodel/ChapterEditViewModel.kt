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

    val bookType: Int  // 0 = 纸质书, 1 = 电子书
    private val bookTotal: Int  // 总页数(纸质书) 或 总段落数(电子书)

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _paragraphCount = MutableStateFlow("")
    val paragraphCount: StateFlow<String> = _paragraphCount.asStateFlow()

    private val _useAutoEstimate = MutableStateFlow(true)
    val useAutoEstimate: StateFlow<Boolean> = _useAutoEstimate.asStateFlow()

    private val _pagesForChapter = MutableStateFlow("")
    val pagesForChapter: StateFlow<String> = _pagesForChapter.asStateFlow()

    /** 电子书百分比输入 */
    private val _percentForChapter = MutableStateFlow("")
    val percentForChapter: StateFlow<String> = _percentForChapter.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    private val _isEdit = MutableStateFlow(false)
    val isEdit: StateFlow<Boolean> = _isEdit.asStateFlow()

    init {
        val book = repository.getBookById(bookId)
        bookType = book?.type ?: 0
        bookTotal = book?.totalPages ?: 0

        chapterId?.let { id ->
            repository.getChapterById(id)?.let { ch ->
                _isEdit.value = true
                _title.value = ch.title
                _paragraphCount.value = ch.paragraphCount.toString()
                _useAutoEstimate.value = false
            }
        }
    }

    fun updateTitle(value: String) { _title.value = value }
    fun updateParagraphCount(value: String) { _paragraphCount.value = value }
    fun updatePagesForChapter(value: String) { _pagesForChapter.value = value }
    fun updatePercentForChapter(value: String) { _percentForChapter.value = value }
    fun setUseAutoEstimate(value: Boolean) { _useAutoEstimate.value = value }

    /** 自动估算段落数 */
    fun estimatedParagraphs(): Int {
        return if (bookType == 0) {
            // 纸质书：按页数 × 4
            val pages = _pagesForChapter.value.toIntOrNull() ?: 0
            pages * 4
        } else {
            // 电子书：按总段落数 × 百分比
            val pct = _percentForChapter.value.toIntOrNull() ?: 0
            if (pct in 1..100) (bookTotal * pct) / 100 else 0
        }
    }

    /** 返回自动估算的说明文字 */
    fun estimateHint(): String {
        val count = estimatedParagraphs()
        return if (bookType == 0) {
            "按每页约 4 段估算，预估 $count 个饼干屑"
        } else {
            "总段落数 $bookTotal × ${
                _percentForChapter.value.ifBlank { "0" }
            }% = $count 个饼干屑"
        }
    }

    fun save() {
        val count = if (_useAutoEstimate.value) {
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
