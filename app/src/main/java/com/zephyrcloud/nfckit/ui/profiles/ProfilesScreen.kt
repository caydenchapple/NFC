package com.zephyrcloud.nfckit.ui.profiles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zephyrcloud.nfckit.data.db.ProfileEntity
import com.zephyrcloud.nfckit.ui.common.EmptyState

@Composable
fun ProfilesScreen(viewModel: ProfilesViewModel = viewModel()) {
    val profiles by viewModel.profiles.collectAsState()
    val status by viewModel.status.collectAsState()

    if (profiles.isEmpty()) {
        EmptyState(Icons.Filled.Folder, "No profiles yet", "Save a record or task set from the Write or Tasks tab")
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (val s = status) {
            is ProfileWriteStatus.Waiting -> item { Text("Hold a tag to the back of your phone…") }
            is ProfileWriteStatus.Success -> item { Text("Written successfully.") }
            is ProfileWriteStatus.Error -> item { Text("Failed: ${s.message}") }
            ProfileWriteStatus.Idle -> Unit
        }
        items(profiles, key = { it.id }) { profile ->
            ProfileRow(profile, onWrite = { viewModel.writeToTag(profile) }, onDelete = { viewModel.delete(profile) })
        }
    }
}

@Composable
private fun ProfileRow(profile: ProfileEntity, onWrite: () -> Unit, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(profile.name, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
            }
            OutlinedButton(onClick = onWrite) { Text("Write to tag") }
        }
    }
}
