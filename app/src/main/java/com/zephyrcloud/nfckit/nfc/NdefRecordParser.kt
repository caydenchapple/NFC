package com.zephyrcloud.nfckit.nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import java.nio.charset.Charset

/** Turns Android's raw [NdefRecord]s into the friendlier [ParsedNdefRecord] shapes the UI wants. */
object NdefRecordParser {

    fun parseMessage(message: NdefMessage): List<ParsedNdefRecord> =
        message.records.map { parseRecord(it) }

    fun parseRecord(record: NdefRecord): ParsedNdefRecord {
        val type = String(record.type, Charsets.US_ASCII)
        return when (record.tnf) {
            NdefRecord.TNF_WELL_KNOWN -> when {
                type == String(NdefRecord.RTD_TEXT, Charsets.US_ASCII) -> parseText(record)
                type == String(NdefRecord.RTD_URI, Charsets.US_ASCII) -> parseUri(record)
                else -> unknown(record, type)
            }
            NdefRecord.TNF_ABSOLUTE_URI -> ParsedNdefRecord.Uri(
                uri = String(record.payload, Charsets.UTF_8),
                rawPayload = record.payload,
            )
            NdefRecord.TNF_MIME_MEDIA -> parseMime(record, type)
            NdefRecord.TNF_EXTERNAL_TYPE -> parseExternal(record, type)
            else -> unknown(record, type)
        }
    }

    private fun parseText(record: NdefRecord): ParsedNdefRecord.Text {
        val payload = record.payload
        val statusByte = payload[0].toInt()
        val isUtf16 = (statusByte and 0x80) != 0
        val languageCodeLength = statusByte and 0x3F
        val charset = if (isUtf16) Charsets.UTF_16 else Charsets.UTF_8
        val locale = String(payload, 1, languageCodeLength, Charsets.US_ASCII)
        val text = String(payload, 1 + languageCodeLength, payload.size - 1 - languageCodeLength, charset)
        return ParsedNdefRecord.Text(text, locale, payload)
    }

    private fun parseUri(record: NdefRecord): ParsedNdefRecord.Uri {
        val payload = record.payload
        val prefix = UriRecords.PREFIXES.getOrElse(payload[0].toInt() and 0xFF) { "" }
        val rest = String(payload, 1, payload.size - 1, Charsets.UTF_8)
        return ParsedNdefRecord.Uri(prefix + rest, payload)
    }

    private fun parseMime(record: NdefRecord, mimeType: String): ParsedNdefRecord {
        return when {
            mimeType.equals("text/vcard", ignoreCase = true) || mimeType.equals("text/x-vcard", ignoreCase = true) ->
                parseVCard(record.payload)
            mimeType.equals("application/vnd.wfa.wsc", ignoreCase = true) ->
                parseWifi(record.payload)
            else -> ParsedNdefRecord.Mime(mimeType, record.payload)
        }
    }

    private fun parseVCard(payload: ByteArray): ParsedNdefRecord.VCard {
        val text = String(payload, Charsets.UTF_8)
        fun field(key: String): String? =
            Regex("(?im)^$key(?:;[^:]*)?:(.*)$").find(text)?.groupValues?.get(1)?.trim()

        val name = field("FN") ?: field("N")?.replace(";", " ")?.trim()
        return ParsedNdefRecord.VCard(
            name = name,
            phone = field("TEL"),
            email = field("EMAIL"),
            org = field("ORG"),
            rawPayload = payload,
        )
    }

    private fun parseWifi(payload: ByteArray): ParsedNdefRecord.WifiConfig {
        // Wi-Fi Simple Config "credential" TLV stream: 2-byte big-endian ID + 2-byte length + value.
        var ssid: String? = null
        var authType: String? = null
        var hasPassword = false
        var i = 0
        while (i + 4 <= payload.size) {
            val id = ((payload[i].toInt() and 0xFF) shl 8) or (payload[i + 1].toInt() and 0xFF)
            val len = ((payload[i + 2].toInt() and 0xFF) shl 8) or (payload[i + 3].toInt() and 0xFF)
            val start = i + 4
            if (start + len > payload.size) break
            when (id) {
                0x1045 -> ssid = String(payload, start, len, Charsets.UTF_8) // SSID
                0x1003 -> authType = decodeAuthType(payload, start, len) // Auth Type
                0x1027 -> hasPassword = len > 0 // Network Key
            }
            i = start + len
        }
        return ParsedNdefRecord.WifiConfig(ssid, authType, hasPassword, payload)
    }

    private fun decodeAuthType(payload: ByteArray, start: Int, len: Int): String {
        if (len < 2) return "Unknown"
        val value = ((payload[start].toInt() and 0xFF) shl 8) or (payload[start + 1].toInt() and 0xFF)
        return when (value) {
            0x0001 -> "Open"
            0x0002 -> "WPA-Personal"
            0x0004 -> "Shared"
            0x0008 -> "WPA-Enterprise"
            0x0010 -> "WPA2-Enterprise"
            0x0020 -> "WPA2-Personal"
            0x0022 -> "WPA/WPA2-Personal"
            else -> "Unknown"
        }
    }

    private fun parseExternal(record: NdefRecord, type: String): ParsedNdefRecord {
        return if (type.equals("android.com:pkg", ignoreCase = true)) {
            ParsedNdefRecord.AppLaunch(String(record.payload, Charsets.UTF_8), record.payload)
        } else {
            val domain = type.substringBefore(':', missingDelimiterValue = type)
            ParsedNdefRecord.External(domain, type, record.payload)
        }
    }

    private fun unknown(record: NdefRecord, type: String) =
        ParsedNdefRecord.Unknown(record.tnf, type, record.payload)
}

internal object UriRecords {
    // Index == the NdefRecord.RTD_URI payload's identifier-code byte (NFC Forum URI RTD spec).
    val PREFIXES = listOf(
        "", "http://www.", "https://www.", "http://", "https://",
        "tel:", "mailto:", "ftp://anonymous:anonymous@", "ftp://ftp.", "ftps://",
        "sftp://", "smb://", "nfs://", "ftp://", "dav://",
        "news:", "telnet://", "imap:", "rtsp://", "urn:",
        "pop:", "sip:", "sips:", "tftp:", "btspp://",
        "btl2cap://", "btgoep://", "tcpobex://", "irdaobex://", "file://",
        "urn:epc:id:", "urn:epc:tag:", "urn:epc:pat:", "urn:epc:raw:", "urn:epc:",
        "urn:nfc:",
    )
}
