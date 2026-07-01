package com.zephyrcloud.nfckit.hce

import android.content.Context
import android.nfc.NdefMessage
import android.util.Base64

/**
 * Persists the NDEF payload the HCE service should emulate. A [android.nfc.cardemulation.HostApduService]
 * is instantiated fresh by the system each time a reader taps the phone, so this can't just live
 * in memory -- it has to survive process death, hence SharedPreferences rather than a plain object.
 */
object HceEmulationStore {
    private const val PREFS = "hce_emulation"
    private const val KEY_PAYLOAD_B64 = "payload"
    private const val KEY_ENABLED = "enabled"

    fun setEmulatedMessage(context: Context, message: NdefMessage) {
        prefs(context).edit()
            .putString(KEY_PAYLOAD_B64, Base64.encodeToString(message.toByteArray(), Base64.NO_WRAP))
            .putBoolean(KEY_ENABLED, true)
            .apply()
    }

    fun getEmulatedPayload(context: Context): ByteArray? {
        val b64 = prefs(context).getString(KEY_PAYLOAD_B64, null) ?: return null
        return Base64.decode(b64, Base64.NO_WRAP)
    }

    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
