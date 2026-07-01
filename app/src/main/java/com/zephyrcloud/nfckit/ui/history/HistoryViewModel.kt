package com.zephyrcloud.nfckit.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zephyrcloud.nfckit.NfcKitApp
import com.zephyrcloud.nfckit.data.db.HistoryEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val app get() = getApplication<NfcKitApp>()

    val entries: StateFlow<List<HistoryEntity>> = app.historyRepository.observeRecent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clear() {
        viewModelScope.launch { app.historyRepository.clear() }
    }
}
