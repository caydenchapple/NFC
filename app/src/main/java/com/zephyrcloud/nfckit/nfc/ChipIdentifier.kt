package com.zephyrcloud.nfckit.nfc

import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.nfc.tech.NfcA

/**
 * Resolves manufacturer + exact chip model.
 *
 * The UID's first byte is the NFC Forum-assigned manufacturer code (ISO/IEC 7816-6 AID /
 * ISO 14443 vendor list). Anything beyond "which vendor" (e.g. NTAG213 vs NTAG215 vs NTAG216)
 * requires talking to the chip: for NXP's Ultralight/NTAG family that's the `GET_VERSION`
 * (0x60) command sent over raw NfcA, per NXP application note AN10752 / AN11004.
 */
object ChipIdentifier {

    private val MANUFACTURER_CODES = mapOf(
        0x01 to "Motorola",
        0x02 to "STMicroelectronics",
        0x03 to "Hitachi",
        0x04 to "NXP Semiconductors",
        0x05 to "Infineon Technologies",
        0x06 to "Cylink",
        0x07 to "Texas Instruments",
        0x08 to "Fujitsu",
        0x09 to "Matsushita/Panasonic",
        0x0A to "NEC",
        0x0B to "Oki Electric",
        0x0C to "Toshiba",
        0x0D to "Mitsubishi",
        0x0E to "Samsung",
        0x0F to "Hyundai",
        0x10 to "LG Semiconductors",
        0x16 to "EM Microelectronic-Marin",
        0x1D to "Renesas",
    )

    // NXP GET_VERSION storage-size byte -> chip model (index 6 of the 8-byte response).
    private val NXP_STORAGE_SIZE_CODES = mapOf(
        0x0B to "NTAG213",
        0x0E to "NTAG215",
        0x0F to "NTAG216",
        0x03 to "MIFARE Ultralight",
        0x06 to "MIFARE Ultralight EV1 (48 bytes)",
        0x0A to "MIFARE Ultralight EV1 (128 bytes)",
    )

    fun manufacturerOf(uid: ByteArray): String {
        if (uid.isEmpty()) return "Unknown"
        val code = uid[0].toInt() and 0xFF
        return MANUFACTURER_CODES[code] ?: "Unknown (0x%02X)".format(code)
    }

    /**
     * Best-effort exact model name. Falls back to a generic tech-based guess when the
     * chip doesn't answer GET_VERSION (many non-NXP tags, or a locked/damaged tag).
     */
    fun identify(tag: Tag): String {
        MifareClassic.get(tag)?.let { mifare ->
            return when (mifare.size) {
                MifareClassic.SIZE_1K -> "MIFARE Classic 1K"
                MifareClassic.SIZE_4K -> "MIFARE Classic 4K"
                MifareClassic.SIZE_MINI -> "MIFARE Classic Mini"
                else -> "MIFARE Classic"
            }
        }

        val nfcA = NfcA.get(tag)
        if (nfcA != null) {
            identifyViaGetVersion(nfcA)?.let { return it }
        }

        MifareUltralight.get(tag)?.let { ultralight ->
            return when (ultralight.type) {
                MifareUltralight.TYPE_ULTRALIGHT -> "MIFARE Ultralight"
                MifareUltralight.TYPE_ULTRALIGHT_C -> "MIFARE Ultralight C"
                else -> "MIFARE Ultralight (unknown subtype)"
            }
        }

        return tag.techList.lastOrNull()?.substringAfterLast('.') ?: "Unknown tag"
    }

    private fun identifyViaGetVersion(nfcA: NfcA): String? {
        return try {
            val wasConnected = try {
                nfcA.connect()
                true
            } catch (e: Exception) {
                false
            }
            if (!wasConnected) return null
            try {
                val response = nfcA.transceive(byteArrayOf(0x60))
                if (response.size >= 8) {
                    val storageCode = response[6].toInt() and 0xFF
                    NXP_STORAGE_SIZE_CODES[storageCode]
                } else {
                    null
                }
            } finally {
                try { nfcA.close() } catch (_: Exception) {}
            }
        } catch (_: Exception) {
            null
        }
    }
}
