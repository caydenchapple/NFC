package com.zephyrcloud.nfckit.ui.history

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
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zephyrcloud.nfckit.data.db.HistoryEntity
import com.zephyrcloud.nfckit.ui.common.EmptyState
import java.text.DateFormat
import java.util.Date

@Composable
fun HistoryScreen(viewModel: HistoryViewModel = viewModel()) {
    val entries by viewModel.entries.collectAsState()

    if (entries.isEmpty()) {
        EmptyState(Icons.Filled.History, "No history yet", "Reads, writes, and task runs will show up here")
        return
    }

    Column(Modifier.fillMaxSize()) {
        TextButton(onClick = viewModel::clear, modifier = Modifier.padding(8.dp)) { Text("Clear history") }
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(entries, key = { it.id }) { entry -> HistoryRow(entry) }
        }
    }
}

@Composable
private fun HistoryRow(entry: HistoryEntity) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(entry.action.name, style = MaterialTheme.typography.labelLarge)
                Text(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(entry.timestampEpochMillis)), style = MaterialTheme.typography.bodySmall)
            }
            Text(entry.summary, style = MaterialTheme.typography.bodyMedium)
            entry.tagUidHex?.let { Text("UID: $it", style = MaterialTheme.typography.bodySmall) }
        }
    }
}
