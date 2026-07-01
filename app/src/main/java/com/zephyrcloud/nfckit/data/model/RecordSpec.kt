package com.zephyrcloud.nfckit.data.model

import android.nfc.NdefRecord
import com.zephyrcloud.nfckit.nfc.NdefRecordFactory
import com.zephyrcloud.nfckit.nfc.SocialNetwork
import com.zephyrcloud.nfckit.nfc.WifiAuthType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Serializable description of one NDEF record the Write tab can build. Profiles persist a
 * list of these (as JSON) instead of raw [NdefRecord] bytes, so a saved profile can be
 * re-rendered in the editor and re-written to any tag later.
 */
@Serializable
sealed class RecordSpec {
    abstract fun toNdefRecord(): NdefRecord

    @Serializable
    @SerialName("text")
    data class TextSpec(val text: String, val locale: String = "en") : RecordSpec() {
        override fun toNdefRecord() = NdefRecordFactory.plainText(text, java.util.Locale(locale))
    }

    @Serializable
    @SerialName("uri")
    data class UriSpec(val uri: String) : RecordSpec() {
        override fun toNdefRecord() = NdefRecordFactory.uri(uri)
    }

    @Serializable
    @SerialName("url")
    data class UrlSpec(val url: String) : RecordSpec() {
        override fun toNdefRecord() = NdefRecordFactory.url(url)
    }

    @Serializable
    @SerialName("phone")
    data class PhoneSpec(val number: String) : RecordSpec() {
        override fun toNdefRecord() = NdefRecordFactory.phoneCall(number)
    }

    @Serializable
    @SerialName("sms")
    data class SmsSpec(val number: String, val message: String? = null) : RecordSpec() {
        override fun toNdefRecord() = NdefRecordFactory.sms(number, message)
    }

    @Serializable
    @SerialName("email")
    data class EmailSpec(val address: String, val subject: String? = null, val body: String? = null) : RecordSpec() {
        override fun toNdefRecord() = NdefRecordFactory.email(address, subject, body)
    }

    @Serializable
    @SerialName("geo")
    data class GeoSpec(val lat: Double, val lon: Double, val label: String? = null) : RecordSpec() {
        override fun toNdefRecord() = NdefRecordFactory.geoLocation(lat, lon, label)
    }

    @Serializable
    @SerialName("app")
    data class AppLaunchSpec(val packageName: String) : RecordSpec() {
        override fun toNdefRecord() = NdefRecordFactory.appLaunch(packageName)
    }

    @Serializable
    @SerialName("social")
    data class SocialSpec(val network: String, val username: String) : RecordSpec() {
        override fun toNdefRecord() =
            NdefRecordFactory.socialProfile(SocialNetwork.valueOf(network), username)
    }

    @Serializable
    @SerialName("video")
    data class VideoSpec(val url: String) : RecordSpec() {
        override fun toNdefRecord() = NdefRecordFactory.videoLink(url)
    }

    @Serializable
    @SerialName("wifi")
    data class WifiSpec(val ssid: String, val password: String? = null, val auth: String = "WPA2_PERSONAL") : RecordSpec() {
        override fun toNdefRecord() =
            NdefRecordFactory.wifiConfig(ssid, password, WifiAuthType.valueOf(auth))
    }

    @Serializable
    @SerialName("vcard")
    data class VCardSpec(
        val name: String,
        val phone: String? = null,
        val email: String? = null,
        val org: String? = null,
        val url: String? = null,
    ) : RecordSpec() {
        override fun toNdefRecord() = NdefRecordFactory.vCard(name, phone, email, org, url)
    }
}
