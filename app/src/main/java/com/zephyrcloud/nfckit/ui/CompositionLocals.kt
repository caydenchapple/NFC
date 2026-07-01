package com.zephyrcloud.nfckit.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.zephyrcloud.nfckit.data.repo.HistoryRepository
import com.zephyrcloud.nfckit.data.repo.ProfileRepository
import com.zephyrcloud.nfckit.nfc.NfcController

val LocalNfcController = staticCompositionLocalOf<NfcController> {
    error("LocalNfcController not provided")
}

val LocalProfileRepository = staticCompositionLocalOf<ProfileRepository> {
    error("LocalProfileRepository not provided")
}

val LocalHistoryRepository = staticCompositionLocalOf<HistoryRepository> {
    error("LocalHistoryRepository not provided")
}
