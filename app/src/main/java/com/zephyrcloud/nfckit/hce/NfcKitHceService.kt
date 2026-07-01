package com.zephyrcloud.nfckit.hce

import android.nfc.cardemulation.HostApduService
import android.os.Bundle

/**
 * Emulates a read-only NFC Forum Type 4 Tag, so this phone can be tapped against another
 * NFC reader (or another phone's Read tab) as if it were a physical NDEF tag. Implements
 * just enough of ISO/IEC 7816-4 + the NFC Forum Type 4 Tag Operation spec to be read:
 * SELECT (AID / CC file / NDEF file) and READ BINARY. UPDATE BINARY is intentionally
 * unsupported -- this is an emulated *read* target, not a writable one.
 */
class NfcKitHceService : HostApduService() {

    companion object {
        private val NDEF_AID = hexToBytes("D2760000850101")
        private val CC_FILE_ID = hexToBytes("E103")
        private val NDEF_FILE_ID = hexToBytes("E104")

        private val SW_OK = hexToBytes("9000")
        private val SW_FILE_NOT_FOUND = hexToBytes("6A82")
        private val SW_WRONG_PARAMS = hexToBytes("6B00")
        private val SW_INS_NOT_SUPPORTED = hexToBytes("6D00")
        private val SW_CLA_NOT_SUPPORTED = hexToBytes("6E00")

        private const val CLA_STANDARD = 0x00.toByte()
        private const val INS_SELECT = 0xA4.toByte()
        private const val INS_READ_BINARY = 0xB0.toByte()

        private fun hexToBytes(hex: String): ByteArray =
            ByteArray(hex.length / 2) { i -> ((Character.digit(hex[i * 2], 16) shl 4) + Character.digit(hex[i * 2 + 1], 16)).toByte() }
    }

    private var selectedFile: SelectedFile = SelectedFile.NONE
    private lateinit var ndefMessageBytes: ByteArray
    private lateinit var ccFileBytes: ByteArray

    private enum class SelectedFile { NONE, CC, NDEF }

    override fun onCreate() {
        super.onCreate()
        ndefMessageBytes = HceEmulationStore.getEmulatedPayload(this) ?: ByteArray(0)
        ccFileBytes = buildCapabilityContainer(ndefMessageBytes.size)
    }

    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        val apdu = commandApdu ?: return SW_WRONG_PARAMS
        if (apdu.size < 4) return SW_WRONG_PARAMS

        val cla = apdu[0]
        val ins = apdu[1]
        val p1 = apdu[2]
        val p2 = apdu[3]

        if (cla != CLA_STANDARD) return SW_CLA_NOT_SUPPORTED

        return when (ins) {
            INS_SELECT -> handleSelect(apdu, p1, p2)
            INS_READ_BINARY -> handleReadBinary(p1, p2, apdu)
            else -> SW_INS_NOT_SUPPORTED
        }
    }

    private fun handleSelect(apdu: ByteArray, p1: Byte, p2: Byte): ByteArray {
        if (apdu.size < 5) return SW_WRONG_PARAMS
        val lc = apdu[4].toInt() and 0xFF
        if (apdu.size < 5 + lc) return SW_WRONG_PARAMS
        val data = apdu.copyOfRange(5, 5 + lc)

        // P1=04 P2=00: select by name (AID). P1=00 P2=0C: select by file ID.
        return when {
            p1 == 0x04.toByte() && data.contentEquals(NDEF_AID) -> {
                selectedFile = SelectedFile.NONE
                SW_OK
            }
            p1 == 0x00.toByte() && data.contentEquals(CC_FILE_ID) -> {
                selectedFile = SelectedFile.CC
                SW_OK
            }
            p1 == 0x00.toByte() && data.contentEquals(NDEF_FILE_ID) -> {
                selectedFile = SelectedFile.NDEF
                SW_OK
            }
            else -> SW_FILE_NOT_FOUND
        }
    }

    private fun handleReadBinary(p1: Byte, p2: Byte, apdu: ByteArray): ByteArray {
        if (apdu.size < 5) return SW_WRONG_PARAMS
        val offset = ((p1.toInt() and 0xFF) shl 8) or (p2.toInt() and 0xFF)
        val le = apdu[4].toInt() and 0xFF
        val length = if (le == 0) 256 else le

        val file = when (selectedFile) {
            SelectedFile.CC -> ccFileBytes
            // File = 2-byte big-endian NLEN prefix followed by the raw NDEF message.
            SelectedFile.NDEF -> byteArrayOf(
                (ndefMessageBytes.size shr 8).toByte(),
                (ndefMessageBytes.size and 0xFF).toByte(),
            ) + ndefMessageBytes
            SelectedFile.NONE -> return SW_FILE_NOT_FOUND
        }

        if (offset >= file.size) return SW_WRONG_PARAMS
        val end = minOf(offset + length, file.size)
        return file.copyOfRange(offset, end) + SW_OK
    }

    private fun buildCapabilityContainer(ndefSize: Int): ByteArray {
        val fileSize = ndefSize + 2 // + NLEN prefix
        return byteArrayOf(
            0x00, 0x0F,             // CCLEN = 15 bytes
            0x20,                   // Mapping version 2.0
            0x00, 0x3B,             // MLe: max R-APDU data size this service returns
            0x00, 0x34,             // MLc: max C-APDU data size this service accepts
            0x04, 0x06,             // NDEF File Control TLV: tag=04, len=6
            NDEF_FILE_ID[0], NDEF_FILE_ID[1],
            (fileSize shr 8).toByte(), (fileSize and 0xFF).toByte(),
            0x00,                   // Read access: unrestricted
            0x00,                   // Write access: unrestricted (service itself rejects UPDATE BINARY)
        )
    }

    override fun onDeactivated(reason: Int) {
        selectedFile = SelectedFile.NONE
    }
}
