package com.zephyrcloud.nfckit.data.repo

import com.zephyrcloud.nfckit.data.db.ProfileDao
import com.zephyrcloud.nfckit.data.db.ProfileEntity
import com.zephyrcloud.nfckit.data.model.RecordSpec
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ProfileRepository(private val dao: ProfileDao) {

    private val json = Json { ignoreUnknownKeys = true }

    fun observeAll(): Flow<List<ProfileEntity>> = dao.observeAll()

    suspend fun save(name: String, records: List<RecordSpec>, taskChainJson: String? = null, epochMillis: Long): Long {
        val entity = ProfileEntity(
            name = name,
            createdAtEpochMillis = epochMillis,
            recordsJson = json.encodeToString(records),
            taskChainJson = taskChainJson,
        )
        return dao.upsert(entity)
    }

    suspend fun delete(profile: ProfileEntity) = dao.delete(profile)

    fun decodeRecords(profile: ProfileEntity): List<RecordSpec> =
        json.decodeFromString(profile.recordsJson)
}
