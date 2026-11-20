package com.example.uth_todo.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.uth_todo.R
import com.example.uth_todo.data.tasks.TaskDates
import com.example.uth_todo.model.*
import com.example.uth_todo.ui.home.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditor(draft: TaskDraft, enabled: Boolean, onDraft: (TaskDraft) -> Unit, tagPrefix: String = "task_edit") {
    var showDate by rememberSaveable { mutableStateOf(false) }
    var showTime by rememberSaveable { mutableStateOf(false) }
    val selectDateLabel = stringResource(R.string.task_select_date)
    OutlinedTextField(draft.title, { onDraft(draft.copy(title = it)) }, enabled = enabled,
        label = { Text(stringResource(R.string.task_title_label)) }, modifier = Modifier.fillMaxWidth().testTag("${tagPrefix}_title"))
    Text(stringResource(R.string.task_category), style = MaterialTheme.typography.titleSmall)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(TaskCategory.STUDY, TaskCategory.LIFE).forEach { category ->
            FilterChip(draft.category == category, onClick = { onDraft(draft.copy(category = category)) }, enabled = enabled,
                label = { Text(stringResource(category.labelResource())) }, modifier = Modifier.testTag("${tagPrefix}_category_${category.name}"))
        }
    }
    OutlinedTextField(draft.date, { onDraft(draft.copy(date = it)) }, enabled = enabled, singleLine = true,
        label = { Text(stringResource(R.string.task_date_label)) },
        trailingIcon = { IconButton(onClick = { showDate = true }, enabled = enabled,
            modifier = Modifier.semantics { contentDescription = selectDateLabel }) { HomeGlyph(HomeIcon.CALENDAR) } },
        modifier = Modifier.fillMaxWidth().testTag("${tagPrefix}_date"))
    OutlinedTextField(draft.time, { onDraft(draft.copy(time = it)) }, enabled = enabled, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
        label = { Text(stringResource(R.string.task_time_label)) },
        trailingIcon = { TextButton(onClick = { showTime = true }, enabled = enabled) { Text(stringResource(R.string.select_action)) } },
        modifier = Modifier.fillMaxWidth().testTag("${tagPrefix}_time"))
    Text(stringResource(R.string.task_priority), style = MaterialTheme.typography.titleSmall)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(TaskPriority.LOW, TaskPriority.MEDIUM, TaskPriority.HIGH).forEach { priority ->
            FilterChip(draft.priority == priority, onClick = { onDraft(draft.copy(priority = priority)) }, enabled = enabled,
                label = { Text(stringResource(priority.labelResource())) })
        }
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.task_reminder), modifier = Modifier.weight(1f))
        Switch(draft.remind, onCheckedChange = { onDraft(draft.copy(remind = it)) }, enabled = enabled)
    }
    Text(stringResource(R.string.task_reminder_hint), style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    OutlinedTextField(draft.note, { onDraft(draft.copy(note = it)) }, enabled = enabled, minLines = 3,
        label = { Text(stringResource(R.string.task_note)) }, modifier = Modifier.fillMaxWidth().testTag("${tagPrefix}_note"))
    Text(stringResource(R.string.task_subtasks), style = MaterialTheme.typography.titleMedium)
    draft.subtasks.forEachIndexed { index, subtask ->
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(subtask.title, { title ->
                    onDraft(draft.copy(subtasks = draft.subtasks.mapIndexed { i, item -> if (i == index) item.copy(title = title) else item }))
                }, enabled = enabled, label = { Text(stringResource(R.string.task_subtask_title)) },
                    modifier = Modifier.fillMaxWidth().testTag("${tagPrefix}_subtask_$index"))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(subtask.done, onCheckedChange = { done ->
                        onDraft(draft.copy(subtasks = draft.subtasks.mapIndexed { i, item -> if (i == index) item.copy(done = done) else item }))
                    }, enabled = enabled)
                    Text(stringResource(R.string.tasks_completed), modifier = Modifier.weight(1f))
                    TextButton(onClick = { onDraft(draft.copy(subtasks = draft.subtasks.filterIndexed { i, _ -> i != index })) },
                        enabled = enabled, modifier = Modifier.testTag("${tagPrefix}_subtask_delete_$index")) {
                        Text(stringResource(R.string.task_remove))
                    }
                }
            }
        }
    }
    OutlinedButton(onClick = { onDraft(draft.copy(subtasks = draft.subtasks + TodoSubtask("", false))) },
        enabled = enabled, modifier = Modifier.fillMaxWidth().testTag("${tagPrefix}_subtask_add")) {
        Text(stringResource(R.string.task_subtask_add))
    }
    if (showDate) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = draft.date.takeIf(TaskDates::isValidDate)?.let(TaskDates::toUtcMillis))
        DatePickerDialog(onDismissRequest = { showDate = false }, confirmButton = {
            TextButton(onClick = {
                picker.selectedDateMillis?.let { onDraft(draft.copy(date = TaskDates.fromUtcMillis(it))) }
                showDate = false
            }) { Text(stringResource(R.string.select_action)) }
        }, dismissButton = { TextButton(onClick = { showDate = false }) { Text(stringResource(R.string.cancel_action)) } }) {
            DatePicker(picker, showModeToggle = false)
        }
    }
    if (showTime) {
        val initial = draft.time.takeIf(TaskDates::isValidTime)?.split(":")
        val picker = rememberTimePickerState(initialHour = initial?.get(0)?.toInt() ?: 9,
            initialMinute = initial?.get(1)?.toInt() ?: 0, is24Hour = true)
        AlertDialog(onDismissRequest = { showTime = false },
            title = { Text(stringResource(R.string.task_time_label)) },
            text = { Column(Modifier.verticalScroll(rememberScrollState()).horizontalScroll(rememberScrollState())) { TimePicker(picker) } },
            confirmButton = { TextButton(onClick = {
                onDraft(draft.copy(time = String.format(Locale.ROOT, "%02d:%02d", picker.hour, picker.minute)))
                showTime = false
            }) { Text(stringResource(R.string.select_action)) } },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text(stringResource(R.string.cancel_action)) } })
    }
}

fun TaskPriority.labelResource(): Int = when (this) {
    TaskPriority.LOW -> R.string.task_priority_low
    TaskPriority.MEDIUM -> R.string.task_priority_medium
    TaskPriority.HIGH -> R.string.task_priority_high
    TaskPriority.OTHER -> R.string.category_other
}

fun TaskEditError.messageResource(): Int = when (this) {
    TaskEditError.TITLE -> R.string.task_error_title
    TaskEditError.CATEGORY -> R.string.task_error_category
    TaskEditError.DATE -> R.string.task_error_date
    TaskEditError.TIME -> R.string.task_error_time
    TaskEditError.PRIORITY -> R.string.task_error_priority
    TaskEditError.SUBTASK -> R.string.task_error_subtask
}
