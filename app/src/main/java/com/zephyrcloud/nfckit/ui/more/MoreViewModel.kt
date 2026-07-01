package com.zephyrcloud.nfckit.ui.more

import android.app.Application
import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zephyrcloud.nfckit.NfcKitApp
import com.zephyrcloud.nfckit.data.ImportExport
import com.zephyrcloud.nfckit.data.db.HistoryAction
import com.zephyrcloud.nfckit.hce.HceEmulationStore
import com.zephyrcloud.nfckit.hce.NfcKitHceService
import com.zephyrcloud.nfckit.nfc.NfcEvent
import com.zephyrcloud.nfckit.nfc.NfcOpResult
import com.zephyrcloud.nfckit.nfc.ScanMode
import com.zephyrcloud.nfckit.nfc.TagCloner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class MaintenanceOp { NONE, CLONE_CAPTURE, CLONE_WRITE, FORMAT, ERASE, LOCK }

data class MoreUiState(
    val activeOp: MaintenanceOp = MaintenanceOp.NONE,
    val statusMessage: String? = null,
    val cloneReady: Boolean = false,
    val hceEnabled: Boolean = false,
    val hceHasPayload: Boolean = false,
)

class MoreViewModel(application: Application) : AndroidViewModel(application) {
    private val app get() = getApplication<NfcKitApp>()
    private var capturedClone: TagCloner.Capture? = null

    private val _uiState = MutableStateFlow(
        MoreUiState(
            hceEnabled = HceEmulationStore.isEnabled(application),
            hceHasPayload = HceEmulationStore.getEmulatedPayload(application) != null,
        )
    )
    val uiState: StateFlow<MoreUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            app.nfcController.events.collect { event ->
                when (event) {
                    is NfcEvent.CloneCaptured -> {
                        capturedClone = event.capture
                        _uiState.value = _uiState.value.copy(
                            activeOp = MaintenanceOp.NONE,
                            cloneReady = true,
                            statusMessage = "Source captured (${event.capture.sourceUidHex}). Now scan the target tag.",
                        )
                    }
                    is NfcEvent.CloneWriteResult -> finishOp(event.result, "Tag cloned successfully.", HistoryAction.CLONE)
                    is NfcEvent.FormatResult -> finishOp(event.result, "Tag formatted.", HistoryAction.FORMAT)
                    is NfcEvent.EraseResult -> finishOp(event.result, "Tag erased.", HistoryAction.ERASE)
                    is NfcEvent.LockResult -> finishOp(event.result, "Tag locked read-only.", HistoryAction.LOCK)
                    is NfcEvent.TagRead -> {
                        // Feeds the "emulate last scanned tag" HCE shortcut.
                        event.info.rawNdefBytes?.let {
                            HceEmulationStore.setEmulatedMessage(app, android.nfc.NdefMessage(it))
                            _uiState.value = _uiState.value.copy(hceHasPayload = true)
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun finishOp(result: NfcOpResult, successMessage: String, action: HistoryAction) {
        val message = when (result) {
            is NfcOpResult.Success -> successMessage
            is NfcOpResult.Error -> "Failed: ${result.message}"
        }
        _uiState.value = _uiState.value.copy(activeOp = MaintenanceOp.NONE, statusMessage = message, cloneReady = false)
        if (result is NfcOpResult.Success) {
            viewModelScope.launch {
                app.historyRepository.record(action = action, summary = successMessage, epochMillis = System.currentTimeMillis())
            }
        }
        app.nfcController.resetToRead()
    }

    fun startCloneCapture() {
        capturedClone = null
        _uiState.value = _uiState.value.copy(activeOp = MaintenanceOp.CLONE_CAPTURE, statusMessage = "Scan the source tag…", cloneReady = false)
        app.nfcController.setMode(ScanMode.CloneCapture)
    }

    fun startCloneWrite() {
        val capture = capturedClone ?: return
        _uiState.value = _uiState.value.copy(activeOp = MaintenanceOp.CLONE_WRITE, statusMessage = "Scan the target tag…")
        app.nfcController.setMode(ScanMode.CloneWrite(capture))
    }

    fun startFormat() {
        _uiState.value = _uiState.value.copy(activeOp = MaintenanceOp.FORMAT, statusMessage = "Scan a tag to format…")
        app.nfcController.setMode(ScanMode.Format)
    }

    fun startErase() {
        _uiState.value = _uiState.value.copy(activeOp = MaintenanceOp.ERASE, statusMessage = "Scan a tag to erase…")
        app.nfcController.setMode(ScanMode.Erase)
    }

    fun startLock() {
        _uiState.value = _uiState.value.copy(activeOp = MaintenanceOp.LOCK, statusMessage = "Scan a tag to lock (this is permanent!)…")
        app.nfcController.setMode(ScanMode.Lock)
    }

    fun cancelOp() {
        _uiState.value = _uiState.value.copy(activeOp = MaintenanceOp.NONE, statusMessage = null, cloneReady = false)
        app.nfcController.resetToRead()
    }

    fun setHceEnabled(enabled: Boolean) {
        HceEmulationStore.setEnabled(app, enabled)
        app.packageManager.setComponentEnabledSetting(
            ComponentName(app, NfcKitHceService::class.java),
            if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP,
        )
        _uiState.value = _uiState.value.copy(hceEnabled = enabled)
    }

    fun exportProfiles(onReady: (String) -> Unit) {
        viewModelScope.launch {
            val current = app.database.profileDao().observeAll().first()
            onReady(ImportExport.export(current, System.currentTimeMillis()))
        }
    }

    fun importProfiles(json: String) {
        viewModelScope.launch {
            val profiles = ImportExport.import(json)
            app.database.profileDao().upsertAll(profiles)
            _uiState.value = _uiState.value.copy(statusMessage = "Imported ${profiles.size} profile(s).")
        }
    }
}
