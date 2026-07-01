package com.zephyrcloud.nfckit.data

import com.zephyrcloud.nfckit.data.db.ProfileEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Bundle format written to/read from a `.json` file via the More tab's export/import. */
@Serializable
data class ProfileBackup(
    val formatVersion: Int = 1,
    val exportedAtEpochMillis: Long,
    val profiles: List<ProfileEntity>,
)

object ImportExport {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun export(profiles: List<ProfileEntity>, epochMillis: Long): String =
        json.encodeToString(ProfileBackup(exportedAtEpochMillis = epochMillis, profiles = profiles))

    fun import(payload: String): List<ProfileEntity> =
        json.decodeFromString<ProfileBackup>(payload).profiles.map { it.copy(id = 0) }
}
