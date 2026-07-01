package com.zephyrcloud.nfckit.ui.profiles

import android.app.Application
import android.nfc.NdefMessage
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zephyrcloud.nfckit.NfcKitApp
import com.zephyrcloud.nfckit.data.db.ProfileEntity
import com.zephyrcloud.nfckit.nfc.NfcEvent
import com.zephyrcloud.nfckit.nfc.NfcOpResult
import com.zephyrcloud.nfckit.nfc.ScanMode
import com.zephyrcloud.nfckit.tasks.TaskChain
import com.zephyrcloud.nfckit.tasks.TaskChainNdef
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

sealed class ProfileWriteStatus {
    data object Idle : ProfileWriteStatus()
    data class Waiting(val profileId: Long) : ProfileWriteStatus()
    data object Success : ProfileWriteStatus()
    data class Error(val message: String) : ProfileWriteStatus()
}

class ProfilesViewModel(application: Application) : AndroidViewModel(application) {
    private val app get() = getApplication<NfcKitApp>()
    private val json = Json { ignoreUnknownKeys = true }

    val profiles: StateFlow<List<ProfileEntity>> = app.profileRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _status = MutableStateFlow<ProfileWriteStatus>(ProfileWriteStatus.Idle)
    val status: StateFlow<ProfileWriteStatus> = _status.asStateFlow()

    init {
        viewModelScope.launch {
            app.nfcController.events.collect { event ->
                if (event is NfcEvent.WriteResult) {
                    _status.value = when (val result = event.result) {
                        is NfcOpResult.Success -> ProfileWriteStatus.Success
                        is NfcOpResult.Error -> ProfileWriteStatus.Error(result.message)
                    }
                    app.nfcController.resetToRead()
                }
            }
        }
    }

    fun writeToTag(profile: ProfileEntity) {
        val records = app.profileRepository.decodeRecords(profile).map { it.toNdefRecord() }
        val taskRecord = profile.taskChainJson?.let { chainJson ->
            runCatching { json.decodeFromString<TaskChain>(chainJson) }.getOrNull()?.let { TaskChainNdef.toRecord(it) }
        }
        val allRecords = (records + listOfNotNull(taskRecord))
        if (allRecords.isEmpty()) return
        _status.value = ProfileWriteStatus.Waiting(profile.id)
        app.nfcController.setMode(ScanMode.Write(NdefMessage(allRecords.toTypedArray())))
    }

    fun delete(profile: ProfileEntity) {
        viewModelScope.launch { app.profileRepository.delete(profile) }
    }
}
