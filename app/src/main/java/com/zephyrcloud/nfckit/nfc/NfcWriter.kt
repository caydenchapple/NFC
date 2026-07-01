package com.zephyrcloud.nfckit.nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable

/** Write / erase / format / lock operations for the Write and More tabs. */
object NfcWriter {

    private val EMPTY_MESSAGE = NdefMessage(
        arrayOf(NdefRecord(NdefRecord.TNF_EMPTY, ByteArray(0), ByteArray(0), ByteArray(0)))
    )

    fun write(tag: Tag, message: NdefMessage): NfcOpResult {
        val ndef = Ndef.get(tag)
        if (ndef != null) {
            return runOn(ndef, connect = true) {
                if (!ndef.isWritable) return@runOn NfcOpResult.Error("This tag is read-only.")
                val size = message.toByteArray().size
                if (size > ndef.maxSize) {
                    return@runOn NfcOpResult.Error("Message is $size bytes but the tag only holds ${ndef.maxSize} bytes.")
                }
                ndef.writeNdefMessage(message)
                NfcOpResult.Success
            }
        }

        val formatable = NdefFormatable.get(tag)
            ?: return NfcOpResult.Error("This tag doesn't support NDEF and can't be formatted.")
        return runOn(formatable, connect = true) {
            formatable.format(message)
            NfcOpResult.Success
        }
    }

    fun erase(tag: Tag): NfcOpResult = write(tag, EMPTY_MESSAGE)

    fun format(tag: Tag): NfcOpResult {
        val formatable = NdefFormatable.get(tag)
        if (formatable != null) {
            return runOn(formatable, connect = true) {
                formatable.format(EMPTY_MESSAGE)
                NfcOpResult.Success
            }
        }
        // Already NDEF-formatted: "format" just means wipe the content.
        return erase(tag)
    }

    /** Irreversible: makes the tag permanently read-only. */
    fun lock(tag: Tag): NfcOpResult {
        val ndef = Ndef.get(tag) ?: return NfcOpResult.Error("Tag must be NDEF-formatted before it can be locked.")
        return runOn(ndef, connect = true) {
            if (!ndef.canMakeReadOnly()) return@runOn NfcOpResult.Error("This tag doesn't support locking.")
            if (ndef.makeReadOnly()) NfcOpResult.Success else NfcOpResult.Error("Lock failed.")
        }
    }

    private inline fun <T : android.nfc.tech.TagTechnology> runOn(
        tech: T,
        connect: Boolean,
        block: () -> NfcOpResult,
    ): NfcOpResult {
        return try {
            if (connect) tech.connect()
            block()
        } catch (e: Exception) {
            NfcOpResult.Error(e.message ?: "Unknown NFC error")
        } finally {
            try { tech.close() } catch (_: Exception) {}
        }
    }
}
