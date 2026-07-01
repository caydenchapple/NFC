package com.zephyrcloud.nfckit.nfc

import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable

/** Reads a scanned [Tag] into a [TagInfo] snapshot for the Read tab / clone / history. */
object NfcTagReader {

    fun read(tag: Tag): TagInfo {
        val uidHex = tag.id.joinToString("") { "%02X".format(it) }
        val techList = tag.techList.map { it.substringAfterLast('.') }
        val manufacturer = ChipIdentifier.manufacturerOf(tag.id)
        val tagTypeName = ChipIdentifier.identify(tag)

        val ndef = Ndef.get(tag)
        val ndefFormatable = NdefFormatable.get(tag)

        if (ndef == null) {
            return TagInfo(
                uidHex = uidHex,
                techList = techList,
                manufacturer = manufacturer,
                tagTypeName = tagTypeName,
                isNdef = false,
                isNdefFormatable = ndefFormatable != null,
                isWritable = false,
                canMakeReadOnly = false,
            )
        }

        return try {
            ndef.connect()
            val message = ndef.cachedNdefMessage ?: ndef.ndefMessage
            val records = message?.let { NdefRecordParser.parseMessage(it) } ?: emptyList()
            TagInfo(
                uidHex = uidHex,
                techList = techList,
                manufacturer = manufacturer,
                tagTypeName = tagTypeName,
                isNdef = true,
                isNdefFormatable = false,
                isWritable = ndef.isWritable,
                canMakeReadOnly = true,
                maxSizeBytes = ndef.maxSize,
                usedSizeBytes = message?.toByteArray()?.size,
                records = records,
                rawNdefBytes = message?.toByteArray(),
            )
        } finally {
            try { ndef.close() } catch (_: Exception) {}
        }
    }
}
