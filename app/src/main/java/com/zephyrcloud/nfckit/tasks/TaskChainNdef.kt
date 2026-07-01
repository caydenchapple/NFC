package com.zephyrcloud.nfckit.tasks

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * A [TaskChain] can ride along on a tag as an extra MIME record so that scanning the tag
 * with NFC Kit both shows its normal NDEF content *and* offers to run the attached
 * automation -- this is the "tag triggers a task" flow from the Tasks tab.
 */
object TaskChainNdef {
    const val MIME_TYPE = "application/vnd.nfckit.tasks+json"

    private val json = Json { ignoreUnknownKeys = true }

    fun toRecord(chain: TaskChain): NdefRecord =
        NdefRecord(
            NdefRecord.TNF_MIME_MEDIA,
            MIME_TYPE.toByteArray(Charsets.US_ASCII),
            ByteArray(0),
            json.encodeToString(chain).toByteArray(Charsets.UTF_8),
        )

    fun findInMessage(message: NdefMessage): TaskChain? {
        val record = message.records.firstOrNull {
            it.tnf == NdefRecord.TNF_MIME_MEDIA && String(it.type, Charsets.US_ASCII) == MIME_TYPE
        } ?: return null
        return try {
            json.decodeFromString<TaskChain>(String(record.payload, Charsets.UTF_8))
        } catch (_: Exception) {
            null
        }
    }
}
