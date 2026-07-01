package com.zephyrcloud.nfckit

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.content.IntentCompat
import androidx.lifecycle.lifecycleScope
import com.zephyrcloud.nfckit.ui.LocalHistoryRepository
import com.zephyrcloud.nfckit.ui.LocalNfcController
import com.zephyrcloud.nfckit.ui.LocalProfileRepository
import com.zephyrcloud.nfckit.ui.navigation.NfcKitNavHost
import com.zephyrcloud.nfckit.ui.theme.NfcKitTheme
import kotlinx.coroutines.launch

/**
 * Hosts the whole Compose UI and owns the NFC read loop. While the activity is in the
 * foreground we use [NfcAdapter.enableReaderMode] (silent, no OS "tag discovered" sound/dialog,
 * gives us the raw [Tag] directly) rather than the older foreground-dispatch + tech-filter
 * intent flow. That older flow is still declared in the manifest so a tag tap can cold-launch
 * the app; [onNewIntent] / the initial intent handle that case.
 */
class MainActivity : ComponentActivity(), NfcAdapter.ReaderCallback {

    private var nfcAdapter: NfcAdapter? = null

    private val app get() = application as NfcKitApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        setContent {
            NfcKitTheme {
                CompositionLocalProvider(
                    LocalNfcController provides app.nfcController,
                    LocalProfileRepository provides app.profileRepository,
                    LocalHistoryRepository provides app.historyRepository,
                ) {
                    NfcKitNavHost()
                }
            }
        }

        handleTagIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        val flags = NfcAdapter.FLAG_READER_NFC_A or
            NfcAdapter.FLAG_READER_NFC_B or
            NfcAdapter.FLAG_READER_NFC_F or
            NfcAdapter.FLAG_READER_NFC_V or
            NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
        nfcAdapter?.enableReaderMode(this, this, flags, null)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleTagIntent(intent)
    }

    /** ReaderCallback: fired on a binder thread whenever a tag enters the field. */
    override fun onTagDiscovered(tag: Tag?) {
        tag ?: return
        lifecycleScope.launch {
            app.nfcController.onTagDiscovered(tag, applicationContext)
        }
    }

    private fun handleTagIntent(intent: Intent) {
        val action = intent.action ?: return
        if (action != NfcAdapter.ACTION_NDEF_DISCOVERED &&
            action != NfcAdapter.ACTION_TECH_DISCOVERED &&
            action != NfcAdapter.ACTION_TAG_DISCOVERED
        ) {
            return
        }
        val tag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            IntentCompat.getParcelableExtra(intent, NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        } ?: return
        lifecycleScope.launch {
            app.nfcController.onTagDiscovered(tag, applicationContext)
        }
    }
}
