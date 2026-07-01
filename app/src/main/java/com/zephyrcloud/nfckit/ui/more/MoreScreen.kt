package com.zephyrcloud.nfckit.ui.more

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.BufferedReader
import java.io.InputStreamReader

@Composable
fun MoreScreen(
    viewModel: MoreViewModel = viewModel(),
    onOpenProfiles: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var confirmLockDialog by remember { mutableStateOf(false) }
    var pendingExportJson by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val json = pendingExportJson ?: return@rememberLauncherForActivityResult
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { out -> out.write(json.toByteArray()) }
        }
        pendingExportJson = null
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        context.contentResolver.openInputStream(uri)?.use { input ->
            val text = BufferedReader(InputStreamReader(input)).readText()
            viewModel.importProfiles(text)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { SectionHeader("Tag maintenance") }
        item {
            CloneCard(
                cloneReady = state.cloneReady,
                activeOp = state.activeOp,
                onCapture = viewModel::startCloneCapture,
                onWrite = viewModel::startCloneWrite,
            )
        }
        item { ActionRow("Format tag", "Wipes and reinitializes the NDEF structure.") { viewModel.startFormat() } }
        item { ActionRow("Erase tag", "Clears the NDEF content, keeps the tag formatted.") { viewModel.startErase() } }
        item { ActionRow("Lock tag", "Makes it permanently read-only. Cannot be undone.") { confirmLockDialog = true } }

        if (state.activeOp != MaintenanceOp.NONE || state.statusMessage != null) {
            item {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Column(Modifier.padding(12.dp)) {
                        state.statusMessage?.let { Text(it) }
                        if (state.activeOp != MaintenanceOp.NONE) {
                            TextButton(onClick = viewModel::cancelOp) { Text("Cancel") }
                        }
                    }
                }
            }
        }

        item { SectionHeader("Tag emulation (HCE)") }
        item {
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text("Emulate last scanned tag")
                        Text(
                            if (state.hceHasPayload) "Ready to emulate." else "Scan a tag first so there's something to emulate.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Switch(checked = state.hceEnabled, onCheckedChange = viewModel::setHceEnabled, enabled = state.hceHasPayload)
                }
            }
        }

        item { SectionHeader("Library") }
        item { ActionRow("Profiles", "Saved record + task combinations.", onOpenProfiles) }
        item { ActionRow("History", "Recent reads, writes, and task runs.", onOpenHistory) }

        item { SectionHeader("Backup") }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    viewModel.exportProfiles { json ->
                        pendingExportJson = json
                        exportLauncher.launch("nfckit-profiles.json")
                    }
                }) { Text("Export profiles") }
                Button(onClick = { importLauncher.launch(arrayOf("application/json")) }) { Text("Import profiles") }
            }
        }
    }

    if (confirmLockDialog) {
        AlertDialog(
            onDismissRequest = { confirmLockDialog = false },
            title = { Text("Lock tag permanently?") },
            text = { Text("This can't be undone. The tag will never be writable again.") },
            confirmButton = {
                TextButton(onClick = { viewModel.startLock(); confirmLockDialog = false }) { Text("Lock") }
            },
            dismissButton = { TextButton(onClick = { confirmLockDialog = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun ActionRow(title: String, subtitle: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth()) {
                    Text(title, style = MaterialTheme.typography.bodyLarge)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun CloneCard(cloneReady: Boolean, activeOp: MaintenanceOp, onCapture: () -> Unit, onWrite: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Copy / clone tag", style = MaterialTheme.typography.bodyLarge)
            Text("Scan a source tag, then scan a target tag to duplicate it.", style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onCapture) { Text("1. Scan source") }
                Button(onClick = onWrite, enabled = cloneReady) { Text("2. Scan target") }
            }
        }
    }
}
