package com.example.uth_todo.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.uth_todo.R
import com.example.uth_todo.model.TaskDataMode

@Composable
fun TaskStorageNotice(mode: TaskDataMode?, onSettings: (() -> Unit)? = null) {
    if (mode == null) return
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().testTag("task_storage_notice")) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(if (mode == TaskDataMode.DEVICE) R.string.task_storage_device else R.string.task_storage_course),
                style = MaterialTheme.typography.labelLarge)
            Text(stringResource(if (mode == TaskDataMode.DEVICE) R.string.task_storage_device_description else R.string.task_storage_course_description),
                style = MaterialTheme.typography.bodySmall)
            if (onSettings != null) TextButton(onClick = onSettings, modifier = Modifier.testTag("task_storage_settings")) {
                Text(stringResource(R.string.task_storage_choose))
            }
        }
    }
}

@Composable
fun TaskStorageSettingsCard(mode: TaskDataMode?, busy: Boolean, onMode: (TaskDataMode) -> Unit,
    hasUnsavedChanges: Boolean = false) {
    var pendingMode by rememberSaveable { mutableStateOf<TaskDataMode?>(null) }
    Card(Modifier.fillMaxWidth().testTag("task_storage_card")) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.task_storage_choose), style = MaterialTheme.typography.titleMedium)
            TaskDataMode.entries.forEach { option ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = mode == option, onClick = {
                        if (option != mode && hasUnsavedChanges) pendingMode = option else onMode(option)
                    }, enabled = !busy,
                        modifier = Modifier.testTag("task_storage_mode_${option.name}"))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(if (option == TaskDataMode.DEVICE) R.string.task_storage_device else R.string.task_storage_course))
                        Text(stringResource(if (option == TaskDataMode.DEVICE) R.string.task_storage_device_description else R.string.task_storage_course_description),
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }
    if (pendingMode != null) AlertDialog(onDismissRequest = { pendingMode = null },
        title = { Text(stringResource(R.string.task_discard_title)) },
        text = { Text(stringResource(R.string.task_storage_discard_body)) },
        confirmButton = { TextButton(onClick = {
            val selected = pendingMode
            pendingMode = null
            selected?.let(onMode)
        }) { Text(stringResource(R.string.continue_action)) } },
        dismissButton = { TextButton(onClick = { pendingMode = null }) { Text(stringResource(R.string.cancel_action)) } })
}
