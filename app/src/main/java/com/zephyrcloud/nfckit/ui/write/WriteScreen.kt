package com.zephyrcloud.nfckit.ui.write

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zephyrcloud.nfckit.data.model.RecordSpec
import com.zephyrcloud.nfckit.nfc.SocialNetwork
import com.zephyrcloud.nfckit.nfc.WifiAuthType

private enum class RecordType(val label: String) {
    TEXT("Plain text"), URL("URL / website"), PHONE("Phone call"), SMS("Text message"),
    EMAIL("Email"), GEO("Geo location"), APP("App launch"), SOCIAL("Social profile"),
    WIFI("Wi-Fi config"), VCARD("Contact card"), VIDEO("Video link"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WriteScreen(viewModel: WriteViewModel = viewModel()) {
    val records by viewModel.records.collectAsState()
    val status by viewModel.status.collectAsState()
    var saveDialogOpen by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { AddRecordCard(onAdd = viewModel::addRecord) }
            if (records.isNotEmpty()) {
                item { Text("Records to write", style = MaterialTheme.typography.titleMedium) }
                items(records.size) { index ->
                    RecordRow(records[index]) { viewModel.removeRecord(index) }
                }
            }
            when (val s = status) {
                is WriteStatus.WaitingForTag -> item { StatusRow("Hold a tag to the back of your phone…", loading = true) }
                is WriteStatus.Success -> item { StatusRow("Written successfully.", loading = false) }
                is WriteStatus.Error -> item { StatusRow("Failed: ${s.message}", loading = false) }
                WriteStatus.Idle -> Unit
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = { saveDialogOpen = true }, enabled = records.isNotEmpty()) {
                Text("Save as profile")
            }
            Button(onClick = viewModel::startWriting, enabled = records.isNotEmpty()) {
                Text("Write to tag")
            }
        }
    }

    if (saveDialogOpen) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { saveDialogOpen = false },
            title = { Text("Save as profile") },
            text = { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Profile name") }) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.saveAsProfile(name)
                    saveDialogOpen = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { saveDialogOpen = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun StatusRow(text: String, loading: Boolean) {
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp))
        Text(text)
    }
}

@Composable
private fun RecordRow(spec: RecordSpec, onRemove: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Text(describe(spec), modifier = Modifier.weight(1f))
            IconButton(onClick = onRemove) { Icon(Icons.Filled.Close, contentDescription = "Remove") }
        }
    }
}

private fun describe(spec: RecordSpec): String = when (spec) {
    is RecordSpec.TextSpec -> "Text: ${spec.text}"
    is RecordSpec.UriSpec -> "URI: ${spec.uri}"
    is RecordSpec.UrlSpec -> "URL: ${spec.url}"
    is RecordSpec.PhoneSpec -> "Call: ${spec.number}"
    is RecordSpec.SmsSpec -> "SMS: ${spec.number}"
    is RecordSpec.EmailSpec -> "Email: ${spec.address}"
    is RecordSpec.GeoSpec -> "Location: ${spec.lat}, ${spec.lon}"
    is RecordSpec.AppLaunchSpec -> "App: ${spec.packageName}"
    is RecordSpec.SocialSpec -> "${spec.network}: ${spec.username}"
    is RecordSpec.WifiSpec -> "Wi-Fi: ${spec.ssid}"
    is RecordSpec.VCardSpec -> "Contact: ${spec.name}"
    is RecordSpec.VideoSpec -> "Video: ${spec.url}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddRecordCard(onAdd: (RecordSpec) -> Unit) {
    var type by rememberSaveable { mutableStateOf(RecordType.TEXT) }
    var expanded by remember { mutableStateOf(false) }

    var field1 by rememberSaveable(type) { mutableStateOf("") }
    var field2 by rememberSaveable(type) { mutableStateOf("") }
    var field3 by rememberSaveable(type) { mutableStateOf("") }
    var social by rememberSaveable(type) { mutableStateOf(SocialNetwork.INSTAGRAM) }
    var wifiAuth by rememberSaveable(type) { mutableStateOf(WifiAuthType.WPA2_PERSONAL) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Add a record", style = MaterialTheme.typography.titleMedium)

            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = type.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Record type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                )
                androidx.compose.material3.ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    RecordType.entries.forEach { entry ->
                        DropdownMenuItem(text = { Text(entry.label) }, onClick = { type = entry; expanded = false })
                    }
                }
            }

            when (type) {
                RecordType.TEXT -> OutlinedTextField(field1, { field1 = it }, label = { Text("Text") }, modifier = Modifier.fillMaxWidth())
                RecordType.URL -> OutlinedTextField(field1, { field1 = it }, label = { Text("URL") }, modifier = Modifier.fillMaxWidth())
                RecordType.PHONE -> OutlinedTextField(field1, { field1 = it }, label = { Text("Phone number") }, modifier = Modifier.fillMaxWidth())
                RecordType.SMS -> {
                    OutlinedTextField(field1, { field1 = it }, label = { Text("Phone number") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(field2, { field2 = it }, label = { Text("Message (optional)") }, modifier = Modifier.fillMaxWidth())
                }
                RecordType.EMAIL -> {
                    OutlinedTextField(field1, { field1 = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(field2, { field2 = it }, label = { Text("Subject (optional)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(field3, { field3 = it }, label = { Text("Body (optional)") }, modifier = Modifier.fillMaxWidth())
                }
                RecordType.GEO -> {
                    OutlinedTextField(field1, { field1 = it }, label = { Text("Latitude") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(field2, { field2 = it }, label = { Text("Longitude") }, modifier = Modifier.fillMaxWidth())
                }
                RecordType.APP -> OutlinedTextField(field1, { field1 = it }, label = { Text("Package name") }, modifier = Modifier.fillMaxWidth())
                RecordType.SOCIAL -> {
                    SocialNetworkPicker(social) { social = it }
                    OutlinedTextField(field1, { field1 = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth())
                }
                RecordType.WIFI -> {
                    OutlinedTextField(field1, { field1 = it }, label = { Text("SSID") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(field2, { field2 = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth())
                    WifiAuthPicker(wifiAuth) { wifiAuth = it }
                }
                RecordType.VCARD -> {
                    OutlinedTextField(field1, { field1 = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(field2, { field2 = it }, label = { Text("Phone (optional)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(field3, { field3 = it }, label = { Text("Email (optional)") }, modifier = Modifier.fillMaxWidth())
                }
                RecordType.VIDEO -> OutlinedTextField(field1, { field1 = it }, label = { Text("Video URL") }, modifier = Modifier.fillMaxWidth())
            }

            Button(
                onClick = {
                    val spec = buildSpec(type, field1, field2, field3, social, wifiAuth) ?: return@Button
                    onAdd(spec)
                    field1 = ""; field2 = ""; field3 = ""
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Add record") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SocialNetworkPicker(selected: SocialNetwork, onSelect: (SocialNetwork) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected.name,
            onValueChange = {},
            readOnly = true,
            label = { Text("Network") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        androidx.compose.material3.ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SocialNetwork.entries.forEach { entry ->
                DropdownMenuItem(text = { Text(entry.name) }, onClick = { onSelect(entry); expanded = false })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WifiAuthPicker(selected: WifiAuthType, onSelect: (WifiAuthType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected.name,
            onValueChange = {},
            readOnly = true,
            label = { Text("Security") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        androidx.compose.material3.ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            WifiAuthType.entries.forEach { entry ->
                DropdownMenuItem(text = { Text(entry.name) }, onClick = { onSelect(entry); expanded = false })
            }
        }
    }
}

private fun buildSpec(
    type: RecordType,
    f1: String,
    f2: String,
    f3: String,
    social: SocialNetwork,
    wifiAuth: WifiAuthType,
): RecordSpec? = when (type) {
    RecordType.TEXT -> f1.takeIf { it.isNotBlank() }?.let { RecordSpec.TextSpec(it) }
    RecordType.URL -> f1.takeIf { it.isNotBlank() }?.let { RecordSpec.UrlSpec(it) }
    RecordType.PHONE -> f1.takeIf { it.isNotBlank() }?.let { RecordSpec.PhoneSpec(it) }
    RecordType.SMS -> f1.takeIf { it.isNotBlank() }?.let { RecordSpec.SmsSpec(it, f2.ifBlank { null }) }
    RecordType.EMAIL -> f1.takeIf { it.isNotBlank() }?.let { RecordSpec.EmailSpec(it, f2.ifBlank { null }, f3.ifBlank { null }) }
    RecordType.GEO -> {
        val lat = f1.toDoubleOrNull()
        val lon = f2.toDoubleOrNull()
        if (lat != null && lon != null) RecordSpec.GeoSpec(lat, lon) else null
    }
    RecordType.APP -> f1.takeIf { it.isNotBlank() }?.let { RecordSpec.AppLaunchSpec(it) }
    RecordType.SOCIAL -> f1.takeIf { it.isNotBlank() }?.let { RecordSpec.SocialSpec(social.name, it) }
    RecordType.WIFI -> f1.takeIf { it.isNotBlank() }?.let { RecordSpec.WifiSpec(it, f2.ifBlank { null }, wifiAuth.name) }
    RecordType.VCARD -> f1.takeIf { it.isNotBlank() }?.let { RecordSpec.VCardSpec(it, f2.ifBlank { null }, f3.ifBlank { null }) }
    RecordType.VIDEO -> f1.takeIf { it.isNotBlank() }?.let { RecordSpec.VideoSpec(it) }
}
