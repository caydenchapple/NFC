package com.zephyrcloud.nfckit.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zephyrcloud.nfckit.tasks.RingerMode
import com.zephyrcloud.nfckit.tasks.TaskActionSpec
import com.zephyrcloud.nfckit.tasks.VolumeStream

/** Renders the editable fields (if any) for one [TaskActionSpec], calling back with an updated copy on every change. */
@Composable
fun TaskParamEditor(action: TaskActionSpec, onChange: (TaskActionSpec) -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (action) {
            is TaskActionSpec.WifiToggle -> BoolField("Turn on", action.enable) { onChange(action.copy(enable = it)) }
            is TaskActionSpec.BluetoothToggle -> BoolField("Turn on", action.enable) { onChange(action.copy(enable = it)) }
            is TaskActionSpec.SetRingerMode -> RingerModeField(action.mode) { onChange(action.copy(mode = it)) }
            is TaskActionSpec.SetVolume -> {
                VolumeStreamField(action.stream) { onChange(action.copy(stream = it)) }
                IntField("Percent (0-100)", action.percent) { onChange(action.copy(percent = it)) }
            }
            is TaskActionSpec.ToggleFlashlight -> BoolField("Turn on", action.enable) { onChange(action.copy(enable = it)) }
            is TaskActionSpec.SetBrightness -> IntField("Percent (0-100)", action.percent) { onChange(action.copy(percent = it)) }
            is TaskActionSpec.Vibrate -> LongField("Milliseconds", action.milliseconds) { onChange(action.copy(milliseconds = it)) }
            is TaskActionSpec.ShowToast -> TextField_("Message", action.message) { onChange(action.copy(message = it)) }
            is TaskActionSpec.ShowNotification -> {
                TextField_("Title", action.title) { onChange(action.copy(title = it)) }
                TextField_("Text", action.text) { onChange(action.copy(text = it)) }
            }
            is TaskActionSpec.OpenApp -> TextField_("Package name", action.packageName) { onChange(action.copy(packageName = it)) }
            is TaskActionSpec.OpenUrl -> TextField_("URL", action.url) { onChange(action.copy(url = it)) }
            is TaskActionSpec.OpenPlayStoreListing -> TextField_("Package name", action.packageName) { onChange(action.copy(packageName = it)) }
            is TaskActionSpec.DialNumber -> TextField_("Phone number", action.phoneNumber) { onChange(action.copy(phoneNumber = it)) }
            is TaskActionSpec.ComposeSms -> {
                TextField_("Phone number", action.phoneNumber) { onChange(action.copy(phoneNumber = it)) }
                TextField_("Message", action.message.orEmpty()) { onChange(action.copy(message = it.ifBlank { null })) }
            }
            is TaskActionSpec.ComposeEmail -> {
                TextField_("Address", action.address) { onChange(action.copy(address = it)) }
                TextField_("Subject", action.subject.orEmpty()) { onChange(action.copy(subject = it.ifBlank { null })) }
                TextField_("Body", action.body.orEmpty()) { onChange(action.copy(body = it.ifBlank { null })) }
            }
            is TaskActionSpec.SetAlarm -> {
                IntField("Hour (0-23)", action.hour) { onChange(action.copy(hour = it)) }
                IntField("Minute (0-59)", action.minute) { onChange(action.copy(minute = it)) }
                TextField_("Label", action.label.orEmpty()) { onChange(action.copy(label = it.ifBlank { null })) }
            }
            is TaskActionSpec.StartTimer -> {
                IntField("Seconds", action.seconds) { onChange(action.copy(seconds = it)) }
                TextField_("Label", action.label.orEmpty()) { onChange(action.copy(label = it.ifBlank { null })) }
            }
            is TaskActionSpec.OpenMapsLocation -> {
                DoubleField("Latitude", action.latitude) { onChange(action.copy(latitude = it)) }
                DoubleField("Longitude", action.longitude) { onChange(action.copy(longitude = it)) }
                TextField_("Label", action.label.orEmpty()) { onChange(action.copy(label = it.ifBlank { null })) }
            }
            is TaskActionSpec.SendBroadcast -> TextField_("Broadcast action", action.action) { onChange(action.copy(action = it)) }
            is TaskActionSpec.CustomIntent -> {
                TextField_("Action", action.action.orEmpty()) { onChange(action.copy(action = it.ifBlank { null })) }
                TextField_("Package", action.packageName.orEmpty()) { onChange(action.copy(packageName = it.ifBlank { null })) }
                TextField_("Class", action.className.orEmpty()) { onChange(action.copy(className = it.ifBlank { null })) }
                TextField_("Data URI", action.dataUri.orEmpty()) { onChange(action.copy(dataUri = it.ifBlank { null })) }
            }
            is TaskActionSpec.Delay -> LongField("Milliseconds", action.milliseconds) { onChange(action.copy(milliseconds = it)) }
            is TaskActionSpec.RunProfile -> LongField("Profile ID", action.profileId) { onChange(action.copy(profileId = it)) }
            else -> Text("No parameters for this action.")
        }
    }
}

@Composable
private fun TextField_(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun IntField(label: String, value: Int, onChange: (Int) -> Unit) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it; it.toIntOrNull()?.let(onChange) },
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun LongField(label: String, value: Long, onChange: (Long) -> Unit) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it; it.toLongOrNull()?.let(onChange) },
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun DoubleField(label: String, value: Double, onChange: (Double) -> Unit) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it; it.toDoubleOrNull()?.let(onChange) },
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun BoolField(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = value, onCheckedChange = onChange)
    }
}

@Composable
private fun RingerModeField(value: RingerMode, onChange: (RingerMode) -> Unit) {
    Column {
        RingerMode.entries.forEach { mode ->
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                RadioButton(selected = value == mode, onClick = { onChange(mode) })
                Text(mode.name)
            }
        }
    }
}

@Composable
private fun VolumeStreamField(value: VolumeStream, onChange: (VolumeStream) -> Unit) {
    Column {
        VolumeStream.entries.forEach { stream ->
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                RadioButton(selected = value == stream, onClick = { onChange(stream) })
                Text(stream.name)
            }
        }
    }
}
