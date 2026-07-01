package com.zephyrcloud.nfckit.nfc

import android.nfc.NdefMessage
import android.nfc.Tag
import android.nfc.tech.Ndef

/**
 * Copy/Clone flow: scan a source tag to capture its NDEF message, then scan a blank/target
 * tag to write the captured bytes onto it. The capture is plain data (a byte array) so it
 * can be held in a ViewModel between the two scans, or saved as a Profile for later reuse.
 */
object TagCloner {

    data class Capture(val sourceUidHex: String, val message: NdefMessage)

    sealed class CaptureResult {
        data class Success(val capture: Capture) : CaptureResult()
        data class Error(val message: String) : CaptureResult()
    }

    fun capture(tag: Tag): CaptureResult {
        val ndef = Ndef.get(tag)
            ?: return CaptureResult.Error("Source tag has no NDEF data to clone.")
        return try {
            ndef.connect()
            val message = ndef.cachedNdefMessage ?: ndef.ndefMessage
                ?: return CaptureResult.Error("Source tag is empty.")
            val uidHex = tag.id.joinToString("") { "%02X".format(it) }
            CaptureResult.Success(Capture(uidHex, message))
        } catch (e: Exception) {
            CaptureResult.Error(e.message ?: "Couldn't read source tag.")
        } finally {
            try { ndef.close() } catch (_: Exception) {}
        }
    }

    fun cloneTo(targetTag: Tag, capture: Capture): NfcOpResult = NfcWriter.write(targetTag, capture.message)
}
