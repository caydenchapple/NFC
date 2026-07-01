package com.zephyrcloud.nfckit.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * A saved combination of NDEF records (+ optionally a task chain to run on scan).
 * [recordsJson] is a JSON-encoded `List<RecordSpec>`; [taskChainJson] a JSON-encoded
 * `TaskChain`, both via kotlinx.serialization -- kept as opaque strings here so this
 * table doesn't need to know about the nfc/tasks packages.
 */
@Serializable
@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAtEpochMillis: Long,
    val recordsJson: String,
    val taskChainJson: String? = null,
)
