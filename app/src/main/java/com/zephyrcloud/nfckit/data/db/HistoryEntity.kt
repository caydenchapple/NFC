package com.zephyrcloud.nfckit.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

enum class HistoryAction { READ, WRITE, CLONE, FORMAT, ERASE, LOCK, TASK_RUN, HCE_EMULATE }

@Serializable
@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampEpochMillis: Long,
    val action: HistoryAction,
    val tagUidHex: String? = null,
    val tagTypeName: String? = null,
    val summary: String,
)
