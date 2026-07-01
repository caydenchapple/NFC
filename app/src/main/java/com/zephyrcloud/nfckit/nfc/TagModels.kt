package com.zephyrcloud.nfckit.nfc

/**
 * Everything the Read tab needs to render for a scanned tag.
 */
data class TagInfo(
    val uidHex: String,
    val techList: List<String>,
    val manufacturer: String,
    val tagTypeName: String,
    val atqa: String? = null,
    val sak: String? = null,
    val isNdef: Boolean,
    val isNdefFormatable: Boolean,
    val isWritable: Boolean,
    val canMakeReadOnly: Boolean,
    val maxSizeBytes: Int? = null,
    val usedSizeBytes: Int? = null,
    val records: List<ParsedNdefRecord> = emptyList(),
    val rawNdefBytes: ByteArray? = null,
)

/** A human-decoded view of an NdefRecord, independent of the android.nfc classes. */
sealed class ParsedNdefRecord {
    abstract val rawPayload: ByteArray

    data class Text(val text: String, val locale: String, override val rawPayload: ByteArray) : ParsedNdefRecord()

    data class Uri(val uri: String, override val rawPayload: ByteArray) : ParsedNdefRecord()

    data class VCard(
        val name: String?,
        val phone: String?,
        val email: String?,
        val org: String?,
        override val rawPayload: ByteArray,
    ) : ParsedNdefRecord()

    data class WifiConfig(
        val ssid: String?,
        val authType: String?,
        val hasPassword: Boolean,
        override val rawPayload: ByteArray,
    ) : ParsedNdefRecord()

    data class AppLaunch(val packageName: String, override val rawPayload: ByteArray) : ParsedNdefRecord()

    data class Mime(val mimeType: String, override val rawPayload: ByteArray) : ParsedNdefRecord()

    data class External(val domain: String, val type: String, override val rawPayload: ByteArray) : ParsedNdefRecord()

    data class Unknown(val tnf: Short, val type: String, override val rawPayload: ByteArray) : ParsedNdefRecord()
}
