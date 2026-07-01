package com.zephyrcloud.nfckit.nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import java.util.Locale

/** Builders for every "standardized record" the Write tab exposes. */
object NdefRecordFactory {

    fun plainText(text: String, locale: Locale = Locale.US): NdefRecord =
        NdefRecord.createTextRecord(locale.language, text)

    fun uri(uri: String): NdefRecord = NdefRecord.createUri(uri)

    fun url(url: String): NdefRecord {
        val normalized = if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
        return NdefRecord.createUri(normalized)
    }

    fun phoneCall(phoneNumber: String): NdefRecord = NdefRecord.createUri("tel:$phoneNumber")

    fun sms(phoneNumber: String, message: String? = null): NdefRecord {
        val body = if (message.isNullOrBlank()) "" else "?body=${encode(message)}"
        return NdefRecord.createUri("sms:$phoneNumber$body")
    }

    fun email(address: String, subject: String? = null, body: String? = null): NdefRecord {
        val params = buildList {
            if (!subject.isNullOrBlank()) add("subject=${encode(subject)}")
            if (!body.isNullOrBlank()) add("body=${encode(body)}")
        }
        val query = if (params.isEmpty()) "" else "?${params.joinToString("&")}"
        return NdefRecord.createUri("mailto:$address$query")
    }

    fun geoLocation(latitude: Double, longitude: Double, label: String? = null): NdefRecord {
        val q = if (label.isNullOrBlank()) "" else "?q=${encode(label)}"
        return NdefRecord.createUri("geo:$latitude,$longitude$q")
    }

    fun appLaunch(packageName: String): NdefRecord = NdefRecord.createApplicationRecord(packageName)

    fun socialProfile(network: SocialNetwork, username: String): NdefRecord =
        NdefRecord.createUri(network.profileUrl(username))

    fun videoLink(url: String): NdefRecord = url(url)

    fun wifiConfig(ssid: String, password: String?, auth: WifiAuthType): NdefRecord {
        fun tlv(id: Int, value: ByteArray): ByteArray {
            val header = byteArrayOf(
                (id shr 8).toByte(), (id and 0xFF).toByte(),
                (value.size shr 8).toByte(), (value.size and 0xFF).toByte(),
            )
            return header + value
        }

        val ssidBytes = ssid.toByteArray(Charsets.UTF_8)
        val authBytes = byteArrayOf(0x00, auth.wscValue.toByte())
        val encryptBytes = byteArrayOf(0x00, if (auth == WifiAuthType.OPEN) 0x01 else 0x08) // none vs AES
        val keyBytes = (password ?: "").toByteArray(Charsets.UTF_8)

        val networkIndex = tlv(0x1026, byteArrayOf(1))
        val ssidTlv = tlv(0x1045, ssidBytes)
        val authTlv = tlv(0x1003, authBytes)
        val encryptTlv = tlv(0x100F, encryptBytes)
        val keyTlv = tlv(0x1027, keyBytes)
        val macTlv = tlv(0x1020, byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()))

        val credentialValue = networkIndex + ssidTlv + authTlv + encryptTlv + keyTlv + macTlv
        val credential = tlv(0x100E, credentialValue)

        return NdefRecord(
            NdefRecord.TNF_MIME_MEDIA,
            "application/vnd.wfa.wsc".toByteArray(Charsets.US_ASCII),
            ByteArray(0),
            credential,
        )
    }

    fun vCard(
        name: String,
        phone: String? = null,
        email: String? = null,
        org: String? = null,
        url: String? = null,
    ): NdefRecord {
        val vcard = buildString {
            append("BEGIN:VCARD\r\n")
            append("VERSION:3.0\r\n")
            append("FN:$name\r\n")
            append("N:$name;;;;\r\n")
            if (!org.isNullOrBlank()) append("ORG:$org\r\n")
            if (!phone.isNullOrBlank()) append("TEL;TYPE=CELL:$phone\r\n")
            if (!email.isNullOrBlank()) append("EMAIL:$email\r\n")
            if (!url.isNullOrBlank()) append("URL:$url\r\n")
            append("END:VCARD\r\n")
        }
        return NdefRecord(
            NdefRecord.TNF_MIME_MEDIA,
            "text/vcard".toByteArray(Charsets.US_ASCII),
            ByteArray(0),
            vcard.toByteArray(Charsets.UTF_8),
        )
    }

    fun customMime(mimeType: String, payload: ByteArray): NdefRecord =
        NdefRecord(NdefRecord.TNF_MIME_MEDIA, mimeType.toByteArray(Charsets.US_ASCII), ByteArray(0), payload)

    fun message(vararg records: NdefRecord): NdefMessage = NdefMessage(records)

    private fun encode(value: String): String =
        java.net.URLEncoder.encode(value, "UTF-8").replace("+", "%20")
}

enum class WifiAuthType(val wscValue: Int) {
    OPEN(0x01),
    WPA_PERSONAL(0x02),
    WPA2_PERSONAL(0x20),
}

enum class SocialNetwork(private val urlTemplate: String) {
    FACEBOOK("https://facebook.com/%s"),
    INSTAGRAM("https://instagram.com/%s"),
    X_TWITTER("https://x.com/%s"),
    LINKEDIN("https://linkedin.com/in/%s"),
    TIKTOK("https://tiktok.com/@%s"),
    YOUTUBE("https://youtube.com/@%s"),
    GITHUB("https://github.com/%s"),
    WHATSAPP("https://wa.me/%s");

    fun profileUrl(username: String): String = urlTemplate.format(username.trim())
}
