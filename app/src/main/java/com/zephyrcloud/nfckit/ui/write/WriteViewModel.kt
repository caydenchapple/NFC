package com.zephyrcloud.nfckit.ui.write

import android.app.Application
import android.nfc.NdefMessage
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zephyrcloud.nfckit.NfcKitApp
import com.zephyrcloud.nfckit.data.db.HistoryAction
import com.zephyrcloud.nfckit.data.model.RecordSpec
import com.zephyrcloud.nfckit.nfc.NfcEvent
import com.zephyrcloud.nfckit.nfc.NfcOpResult
import com.zephyrcloud.nfckit.nfc.ScanMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class WriteStatus {
    data object Idle : WriteStatus()
    data object WaitingForTag : WriteStatus()
    data object Success : WriteStatus()
    data class Error(val message: String) : WriteStatus()
}

class WriteViewModel(application: Application) : AndroidViewModel(application) {
    private val app get() = getApplication<NfcKitApp>()

    private val _records = MutableStateFlow<List<RecordSpec>>(emptyList())
    val records: StateFlow<List<RecordSpec>> = _records.asStateFlow()

    private val _status = MutableStateFlow<WriteStatus>(WriteStatus.Idle)
    val status: StateFlow<WriteStatus> = _status.asStateFlow()

    init {
        viewModelScope.launch {
            app.nfcController.events.collect { event ->
                if (event is NfcEvent.WriteResult) {
                    _status.value = when (val result = event.result) {
                        is NfcOpResult.Success -> {
                            app.historyRepository.record(
                                action = HistoryAction.WRITE,
                                summary = "Wrote ${_records.value.size} record(s)",
                                epochMillis = System.currentTimeMillis(),
                            )
                            WriteStatus.Success
                        }
                        is NfcOpResult.Error -> WriteStatus.Error(result.message)
                    }
                    app.nfcController.resetToRead()
                }
            }
        }
    }

    fun addRecord(spec: RecordSpec) {
        _records.value = _records.value + spec
    }

    fun removeRecord(index: Int) {
        _records.value = _records.value.filterIndexed { i, _ -> i != index }
    }

    fun clear() {
        _records.value = emptyList()
        _status.value = WriteStatus.Idle
    }

    fun startWriting() {
        if (_records.value.isEmpty()) return
        val message = NdefMessage(_records.value.map { it.toNdefRecord() }.toTypedArray())
        _status.value = WriteStatus.WaitingForTag
        app.nfcController.setMode(ScanMode.Write(message))
    }

    fun saveAsProfile(name: String) {
        if (_records.value.isEmpty() || name.isBlank()) return
        viewModelScope.launch {
            app.profileRepository.save(
                name = name,
                records = _records.value,
                epochMillis = System.currentTimeMillis(),
            )
        }
    }
}
