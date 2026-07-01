package com.zephyrcloud.nfckit.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import com.zephyrcloud.nfckit.tasks.ActionResult
import com.zephyrcloud.nfckit.tasks.StepResult
import com.zephyrcloud.nfckit.tasks.TaskCatalog
import com.zephyrcloud.nfckit.tasks.TaskStep

@Composable
fun TasksScreen(viewModel: TasksViewModel = viewModel()) {
    val steps by viewModel.steps.collectAsState()
    val runResults by viewModel.runResults.collectAsState()
    val writeStatus by viewModel.writeStatus.collectAsState()
    var showCatalog by rememberSaveable { mutableStateOf(false) }
    var saveDialogOpen by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showCatalog = true }) { Icon(Icons.Filled.Add, contentDescription = "Add task") }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (steps.isEmpty()) {
                    item { Text("No tasks yet. Tap + to add one.", style = MaterialTheme.typography.bodyLarge) }
                }
                stepItems(steps, viewModel)
                runResults?.let { results ->
                    item { Text("Last run", style = MaterialTheme.typography.titleMedium) }
                    items(results) { r -> ResultRow(r) }
                }
                when (val ws = writeStatus) {
                    is WriteChainStatus.WaitingForTag -> item { Text("Hold a tag to the back of your phone…") }
                    is WriteChainStatus.Success -> item { Text("Automation attached to tag.") }
                    is WriteChainStatus.Error -> item { Text("Failed: ${ws.message}") }
                    WriteChainStatus.Idle -> Unit
                }
            }

            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { saveDialogOpen = true }, enabled = steps.isNotEmpty()) { Text("Save") }
                OutlinedButton(onClick = viewModel::runNow, enabled = steps.isNotEmpty()) { Text("Run now") }
                Button(onClick = viewModel::writeToTag, enabled = steps.isNotEmpty()) { Text("Write to tag") }
            }
        }
    }

    if (showCatalog) {
        AddTaskDialog(onDismiss = { showCatalog = false }, onPick = { viewModel.addStep(it); showCatalog = false })
    }

    if (saveDialogOpen) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { saveDialogOpen = false },
            title = { Text("Save profile") },
            text = { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Profile name") }) },
            confirmButton = { TextButton(onClick = { viewModel.saveAsProfile(name); saveDialogOpen = false }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { saveDialogOpen = false }) { Text("Cancel") } },
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.stepItems(steps: List<TaskStep>, viewModel: TasksViewModel) {
    steps.forEachIndexed { index, step ->
        item(key = "step-$index") { StepCard(index, step, viewModel) }
    }
}

@Composable
private fun StepCard(index: Int, step: TaskStep, viewModel: TasksViewModel) {
    var expanded by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${index + 1}. ${step.action.label}", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
                }
                IconButton(onClick = { viewModel.removeStep(index) }) {
                    Icon(Icons.Filled.Close, contentDescription = "Remove")
                }
            }
            if (expanded) {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                TaskParamEditor(step.action) { viewModel.updateStep(index, it) }
            }
        }
    }
}

@Composable
private fun ResultRow(result: StepResult) {
    val text = when (result) {
        is StepResult.Ran -> "${result.action.label}: ${
            when (val o = result.outcome) {
                ActionResult.Success -> "done"
                is ActionResult.Failed -> o.reason
            }
        }"
        is StepResult.Skipped -> "${result.action.label}: skipped (${result.reason})"
    }
    Text("• $text", style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun AddTaskDialog(onDismiss: () -> Unit, onPick: (com.zephyrcloud.nfckit.tasks.TaskActionSpec) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a task") },
        text = {
            LazyColumn(Modifier.heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TaskCatalog.byCategory().forEach { (category, templates) ->
                    item(key = "header-$category") {
                        Text(category.name.replace('_', ' '), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
                    }
                    items(templates) { template ->
                        TextButton(onClick = { onPick(template.default) }, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.fillMaxWidth()) {
                                Text(template.title)
                                Text(template.description, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
