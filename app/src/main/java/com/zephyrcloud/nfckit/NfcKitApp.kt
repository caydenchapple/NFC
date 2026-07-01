package com.zephyrcloud.nfckit

import android.app.Application
import com.zephyrcloud.nfckit.data.db.AppDatabase
import com.zephyrcloud.nfckit.data.repo.HistoryRepository
import com.zephyrcloud.nfckit.data.repo.ProfileRepository
import com.zephyrcloud.nfckit.nfc.NfcController
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class NfcKitApp : Application() {

    lateinit var database: AppDatabase
        private set
    lateinit var profileRepository: ProfileRepository
        private set
    lateinit var historyRepository: HistoryRepository
        private set
    lateinit var nfcController: NfcController
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        profileRepository = ProfileRepository(database.profileDao())
        historyRepository = HistoryRepository(database.historyDao())
        nfcController = NfcController(resolveProfileChain = { profileId ->
            val profile = database.profileDao().getById(profileId) ?: return@NfcController null
            val chainJson = profile.taskChainJson ?: return@NfcController null
            runCatching { Json { ignoreUnknownKeys = true }.decodeFromString<com.zephyrcloud.nfckit.tasks.TaskChain>(chainJson) }.getOrNull()
        })
    }
}
