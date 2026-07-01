package com.zephyrcloud.nfckit.data.repo

import com.zephyrcloud.nfckit.data.db.HistoryAction
import com.zephyrcloud.nfckit.data.db.HistoryDao
import com.zephyrcloud.nfckit.data.db.HistoryEntity
import kotlinx.coroutines.flow.Flow

class HistoryRepository(private val dao: HistoryDao) {

    fun observeRecent(): Flow<List<HistoryEntity>> = dao.observeRecent()

    suspend fun record(
        action: HistoryAction,
        summary: String,
        tagUidHex: String? = null,
        tagTypeName: String? = null,
        epochMillis: Long,
    ) {
        dao.insert(
            HistoryEntity(
                timestampEpochMillis = epochMillis,
                action = action,
                tagUidHex = tagUidHex,
                tagTypeName = tagTypeName,
                summary = summary,
            )
        )
    }

    suspend fun clear() = dao.clear()
}
