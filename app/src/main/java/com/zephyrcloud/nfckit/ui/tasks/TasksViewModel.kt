package com.zephyrcloud.nfckit.ui.tasks

import android.app.Application
import android.nfc.NdefMessage
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zephyrcloud.nfckit.NfcKitApp
import com.zephyrcloud.nfckit.data.db.HistoryAction
import com.zephyrcloud.nfckit.nfc.NfcEvent
import com.zephyrcloud.nfckit.nfc.NfcOpResult
import com.zephyrcloud.nfckit.nfc.ScanMode
import com.zephyrcloud.nfckit.tasks.StepResult
import com.zephyrcloud.nfckit.tasks.TaskActionSpec
import com.zephyrcloud.nfckit.tasks.TaskChain
import com.zephyrcloud.nfckit.tasks.TaskChainNdef
import com.zephyrcloud.nfckit.tasks.TaskCondition
import com.zephyrcloud.nfckit.tasks.TaskExecutor
import com.zephyrcloud.nfckit.tasks.TaskStep
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

sealed class WriteChainStatus {
    data object Idle : WriteChainStatus()
    data object WaitingForTag : WriteChainStatus()
    data object Success : WriteChainStatus()
    data class Error(val message: String) : WriteChainStatus()
}

class TasksViewModel(application: Application) : AndroidViewModel(application) {
    private val app get() = getApplication<NfcKitApp>()
    private val json = Json { ignoreUnknownKeys = true }

    private val _steps = MutableStateFlow<List<TaskStep>>(emptyList())
    val steps: StateFlow<List<TaskStep>> = _steps.asStateFlow()

    private val _runResults = MutableStateFlow<List<StepResult>?>(null)
    val runResults: StateFlow<List<StepResult>?> = _runResults.asStateFlow()

    private val _writeStatus = MutableStateFlow<WriteChainStatus>(WriteChainStatus.Idle)
    val writeStatus: StateFlow<WriteChainStatus> = _writeStatus.asStateFlow()

    init {
        viewModelScope.launch {
            app.nfcController.events.collect { event ->
                if (event is NfcEvent.WriteResult) {
                    _writeStatus.value = when (val result = event.result) {
                        is NfcOpResult.Success -> WriteChainStatus.Success
                        is NfcOpResult.Error -> WriteChainStatus.Error(result.message)
                    }
                    app.nfcController.resetToRead()
                }
            }
        }
    }

    fun addStep(action: TaskActionSpec) {
        _steps.value = _steps.value + TaskStep(action)
    }

    fun updateStep(index: Int, action: TaskActionSpec) {
        _steps.value = _steps.value.toMutableList().also {
            it[index] = it[index].copy(action = action)
        }
    }

    fun updateCondition(index: Int, condition: TaskCondition) {
        _steps.value = _steps.value.toMutableList().also {
            it[index] = it[index].copy(condition = condition)
        }
    }

    fun removeStep(index: Int) {
        _steps.value = _steps.value.filterIndexed { i, _ -> i != index }
    }

    fun runNow() {
        viewModelScope.launch {
            val chain = TaskChain(_steps.value)
            val results = TaskExecutor(app, resolveProfileChain = ::resolveProfileChain).run(chain)
            _runResults.value = results
            app.historyRepository.record(
                action = HistoryAction.TASK_RUN,
                summary = "Ran ${chain.steps.size} task step(s)",
                epochMillis = System.currentTimeMillis(),
            )
        }
    }

    fun saveAsProfile(name: String) {
        if (name.isBlank() || _steps.value.isEmpty()) return
        viewModelScope.launch {
            app.profileRepository.save(
                name = name,
                records = emptyList(),
                taskChainJson = json.encodeToString(TaskChain(_steps.value)),
                epochMillis = System.currentTimeMillis(),
            )
        }
    }

    fun writeToTag() {
        if (_steps.value.isEmpty()) return
        val record = TaskChainNdef.toRecord(TaskChain(_steps.value))
        _writeStatus.value = WriteChainStatus.WaitingForTag
        app.nfcController.setMode(ScanMode.Write(NdefMessage(arrayOf(record))))
    }

    private suspend fun resolveProfileChain(profileId: Long): TaskChain? {
        val profile = app.database.profileDao().getById(profileId) ?: return null
        val chainJson = profile.taskChainJson ?: return null
        return runCatching { json.decodeFromString<TaskChain>(chainJson) }.getOrNull()
    }
}
