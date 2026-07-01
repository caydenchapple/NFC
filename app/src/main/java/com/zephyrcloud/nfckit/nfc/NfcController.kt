package com.zephyrcloud.nfckit.nfc

import android.content.Context
import android.nfc.NdefMessage
import android.nfc.Tag
import com.zephyrcloud.nfckit.tasks.StepResult
import com.zephyrcloud.nfckit.tasks.TaskChainNdef
import com.zephyrcloud.nfckit.tasks.TaskExecutor
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/** What should happen the next time a tag is tapped. Screens set this before asking the user to scan. */
sealed class ScanMode {
    data object Idle : ScanMode()
    data object Read : ScanMode()
    data class Write(val message: NdefMessage) : ScanMode()
    data object CloneCapture : ScanMode()
    data class CloneWrite(val capture: TagCloner.Capture) : ScanMode()
    data object Format : ScanMode()
    data object Erase : ScanMode()
    data object Lock : ScanMode()
}

sealed class NfcEvent {
    data class TagRead(val info: TagInfo, val attachedTasks: com.zephyrcloud.nfckit.tasks.TaskChain?) : NfcEvent()
    data class WriteResult(val result: NfcOpResult) : NfcEvent()
    data class CloneCaptured(val capture: TagCloner.Capture) : NfcEvent()
    data class CloneWriteResult(val result: NfcOpResult) : NfcEvent()
    data class FormatResult(val result: NfcOpResult) : NfcEvent()
    data class EraseResult(val result: NfcOpResult) : NfcEvent()
    data class LockResult(val result: NfcOpResult) : NfcEvent()
    data class TasksRan(val results: List<StepResult>) : NfcEvent()
    data class Error(val message: String) : NfcEvent()
}

/**
 * Owned by [com.zephyrcloud.nfckit.NfcKitApp] for the process lifetime. MainActivity forwards
 * every discovered [Tag] here; this decides what to do with it based on [mode] and reports the
 * outcome through [events]. Keeping this outside any Android component means ViewModels can
 * collect it without holding an Activity reference.
 */
class NfcController(
    private val resolveProfileChain: suspend (Long) -> com.zephyrcloud.nfckit.tasks.TaskChain? = { null },
) {
    private val _mode = MutableStateFlow<ScanMode>(ScanMode.Idle)
    val mode = _mode.asStateFlow()

    private val _events = MutableSharedFlow<NfcEvent>(replay = 0, extraBufferCapacity = 4)
    val events = _events.asSharedFlow()

    fun setMode(mode: ScanMode) {
        _mode.value = mode
    }

    fun resetToRead() {
        _mode.value = ScanMode.Read
    }

    suspend fun onTagDiscovered(tag: Tag, context: Context, autoRunAttachedTasks: Boolean = true) {
        when (val current = _mode.value) {
            is ScanMode.Write -> {
                _events.emit(NfcEvent.WriteResult(NfcWriter.write(tag, current.message)))
            }
            is ScanMode.CloneCapture -> {
                when (val result = TagCloner.capture(tag)) {
                    is TagCloner.CaptureResult.Success -> _events.emit(NfcEvent.CloneCaptured(result.capture))
                    is TagCloner.CaptureResult.Error -> _events.emit(NfcEvent.Error(result.message))
                }
            }
            is ScanMode.CloneWrite -> {
                _events.emit(NfcEvent.CloneWriteResult(TagCloner.cloneTo(tag, current.capture)))
            }
            is ScanMode.Format -> {
                _events.emit(NfcEvent.FormatResult(NfcWriter.format(tag)))
            }
            is ScanMode.Erase -> {
                _events.emit(NfcEvent.EraseResult(NfcWriter.erase(tag)))
            }
            is ScanMode.Lock -> {
                _events.emit(NfcEvent.LockResult(NfcWriter.lock(tag)))
            }
            ScanMode.Idle, ScanMode.Read -> {
                val info = NfcTagReader.read(tag)
                val rawMessage = info.rawNdefBytes?.let { runCatching { NdefMessage(it) }.getOrNull() }
                val attachedChain = rawMessage?.let { TaskChainNdef.findInMessage(it) }
                _events.emit(NfcEvent.TagRead(info, attachedChain))
                if (autoRunAttachedTasks && attachedChain != null) {
                    val results = TaskExecutor(context, resolveProfileChain).run(attachedChain)
                    _events.emit(NfcEvent.TasksRan(results))
                }
            }
        }
    }
}
