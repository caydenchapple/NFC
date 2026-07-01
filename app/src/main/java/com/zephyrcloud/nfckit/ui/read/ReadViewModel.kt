package com.zephyrcloud.nfckit.ui.read

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zephyrcloud.nfckit.NfcKitApp
import com.zephyrcloud.nfckit.data.db.HistoryAction
import com.zephyrcloud.nfckit.nfc.NfcEvent
import com.zephyrcloud.nfckit.nfc.TagInfo
import com.zephyrcloud.nfckit.tasks.StepResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReadUiState(
    val lastTag: TagInfo? = null,
    val lastTaskRun: List<StepResult>? = null,
    val waiting: Boolean = true,
)

class ReadViewModel(application: Application) : AndroidViewModel(application) {
    private val app get() = getApplication<NfcKitApp>()

    private val _uiState = MutableStateFlow(ReadUiState())
    val uiState: StateFlow<ReadUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            app.nfcController.events.collect { event ->
                when (event) {
                    is NfcEvent.TagRead -> {
                        _uiState.value = ReadUiState(lastTag = event.info, waiting = false)
                        app.historyRepository.record(
                            action = HistoryAction.READ,
                            summary = "Read ${event.info.tagTypeName} (${event.info.records.size} records)",
                            tagUidHex = event.info.uidHex,
                            tagTypeName = event.info.tagTypeName,
                            epochMillis = System.currentTimeMillis(),
                        )
                    }
                    is NfcEvent.TasksRan -> {
                        _uiState.value = _uiState.value.copy(lastTaskRun = event.results)
                    }
                    else -> Unit
                }
            }
        }
    }
}
