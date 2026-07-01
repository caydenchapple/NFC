package com.zephyrcloud.nfckit.ui.read

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zephyrcloud.nfckit.nfc.ParsedNdefRecord
import com.zephyrcloud.nfckit.nfc.TagInfo
import com.zephyrcloud.nfckit.ui.common.EmptyState
import com.zephyrcloud.nfckit.ui.common.LabeledRow

@Composable
fun ReadScreen(viewModel: ReadViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val tag = state.lastTag

    if (tag == null) {
        EmptyState(
            icon = Icons.Filled.Nfc,
            title = "Ready to scan",
            subtitle = "Hold a tag to the back of your phone",
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { TagSummaryCard(tag) }
        if (tag.records.isNotEmpty()) {
            item { Text("NDEF records", style = MaterialTheme.typography.titleMedium) }
            items(tag.records) { record -> RecordCard(record) }
        }
        item { RawDataCard(tag) }
        state.lastTaskRun?.let { results ->
            item { TaskRunCard(results) }
        }
    }
}

@Composable
private fun TagSummaryCard(tag: TagInfo) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            androidx.compose.foundation.layout.Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Filled.CreditCard, contentDescription = null)
                Text(tag.tagTypeName, style = MaterialTheme.typography.titleLarge)
            }
            LabeledRow("Manufacturer", tag.manufacturer)
            LabeledRow("UID", tag.uidHex)
            LabeledRow("Tech", tag.techList.joinToString(", "))
            LabeledRow("NDEF formatted", if (tag.isNdef) "Yes" else "No")
            if (tag.isNdef) {
                LabeledRow("Writable", if (tag.isWritable) "Yes" else "No")
                if (tag.maxSizeBytes != null) {
                    LabeledRow("Capacity", "${tag.usedSizeBytes ?: 0} / ${tag.maxSizeBytes} bytes")
                }
            }
        }
    }
}

@Composable
private fun RecordCard(record: ParsedNdefRecord) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            when (record) {
                is ParsedNdefRecord.Text -> {
                    Text("Text", style = MaterialTheme.typography.labelLarge)
                    Text(record.text)
                }
                is ParsedNdefRecord.Uri -> {
                    Text("Link", style = MaterialTheme.typography.labelLarge)
                    Text(record.uri)
                }
                is ParsedNdefRecord.VCard -> {
                    Text("Contact", style = MaterialTheme.typography.labelLarge)
                    record.name?.let { Text(it) }
                    record.phone?.let { LabeledRow("Phone", it) }
                    record.email?.let { LabeledRow("Email", it) }
                    record.org?.let { LabeledRow("Org", it) }
                }
                is ParsedNdefRecord.WifiConfig -> {
                    Text("Wi-Fi network", style = MaterialTheme.typography.labelLarge)
                    LabeledRow("SSID", record.ssid ?: "?")
                    LabeledRow("Security", record.authType ?: "Unknown")
                }
                is ParsedNdefRecord.AppLaunch -> {
                    Text("App launch", style = MaterialTheme.typography.labelLarge)
                    Text(record.packageName)
                }
                is ParsedNdefRecord.Mime -> {
                    Text("MIME: ${record.mimeType}", style = MaterialTheme.typography.labelLarge)
                    Text("${record.rawPayload.size} bytes")
                }
                is ParsedNdefRecord.External -> {
                    Text("External: ${record.domain}", style = MaterialTheme.typography.labelLarge)
                    Text(record.type)
                }
                is ParsedNdefRecord.Unknown -> {
                    Text("Unknown record", style = MaterialTheme.typography.labelLarge)
                    Text("TNF ${record.tnf}, type ${record.type}")
                }
            }
        }
    }
}

@Composable
private fun RawDataCard(tag: TagInfo) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Raw data", style = MaterialTheme.typography.titleMedium)
            val hex = tag.rawNdefBytes?.joinToString(" ") { "%02X".format(it) } ?: "(no NDEF data)"
            Text(hex, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun TaskRunCard(results: List<com.zephyrcloud.nfckit.tasks.StepResult>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Automation ran", style = MaterialTheme.typography.titleMedium)
            results.forEach { result ->
                when (result) {
                    is com.zephyrcloud.nfckit.tasks.StepResult.Ran ->
                        Text("• ${result.action.label}: ${if (result.outcome is com.zephyrcloud.nfckit.tasks.ActionResult.Success) "done" else (result.outcome as com.zephyrcloud.nfckit.tasks.ActionResult.Failed).reason}")
                    is com.zephyrcloud.nfckit.tasks.StepResult.Skipped ->
                        Text("• ${result.action.label}: skipped (${result.reason})")
                }
            }
        }
    }
}
