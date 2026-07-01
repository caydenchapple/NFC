package com.zephyrcloud.nfckit.nfc

sealed class NfcOpResult {
    data object Success : NfcOpResult()
    data class Error(val message: String) : NfcOpResult()
}
