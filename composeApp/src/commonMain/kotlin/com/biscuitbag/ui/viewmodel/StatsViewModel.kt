package com.biscuitbag.ui.viewmodel

import com.biscuitbag.data.repository.BiscuitBagRepository
import com.biscuitbag.data.repository.ReadingRecordEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class StatsViewModel(private val repository: BiscuitBagRepository) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _records = MutableStateFlow<List<ReadingRecordEntity>>(emptyList())
    val records: StateFlow<List<ReadingRecordEntity>> = _records.asStateFlow()

    private val _totalDays = MutableStateFlow(0L)
    val totalDays: StateFlow<Long> = _totalDays.asStateFlow()

    private val _totalBreadcrumbs = MutableStateFlow(0L)
    val totalBreadcrumbs: StateFlow<Long> = _totalBreadcrumbs.asStateFlow()

    fun load() {
        _records.value = repository.getAllReadingRecords()
        _totalDays.value = repository.getTotalReadingDays()
        _totalBreadcrumbs.value = repository.getTotalCompletedBreadcrumbs()
    }
}
