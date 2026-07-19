package com.biscuitbag.ui.viewmodel

import com.biscuitbag.data.repository.BiscuitBagRepository
import com.biscuitbag.data.repository.BreadcrumbEntity
import com.biscuitbag.data.repository.ChapterEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ReadingViewModel(
    private val repository: BiscuitBagRepository,
    private val chapterId: Long
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _chapter = MutableStateFlow<ChapterEntity?>(null)
    val chapter: StateFlow<ChapterEntity?> = _chapter.asStateFlow()

    private val _breadcrumbs = MutableStateFlow<List<BreadcrumbEntity>>(emptyList())
    val breadcrumbs: StateFlow<List<BreadcrumbEntity>> = _breadcrumbs.asStateFlow()

    private val _readCount = MutableStateFlow(0)
    val readCount: StateFlow<Int> = _readCount.asStateFlow()

    private val _totalCount = MutableStateFlow(0)
    val totalCount: StateFlow<Int> = _totalCount.asStateFlow()

    fun load() {
        _chapter.value = repository.getChapterById(chapterId)
        val crumbs = repository.getBreadcrumbsByChapterId(chapterId)
        _breadcrumbs.value = crumbs
        _readCount.value = crumbs.count { it.isRead }
        _totalCount.value = crumbs.size
    }

    fun toggleBreadcrumb(crumb: BreadcrumbEntity) {
        val newState = !crumb.isRead
        repository.toggleBreadcrumb(crumb.id, newState)
        load() // 重新加载以获取最新状态
    }
}
