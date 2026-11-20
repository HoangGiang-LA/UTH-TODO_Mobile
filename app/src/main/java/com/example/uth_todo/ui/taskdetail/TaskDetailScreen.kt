package com.example.uth_todo.ui.taskdetail

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.uth_todo.R
import com.example.uth_todo.data.tasks.TaskDates
import com.example.uth_todo.model.*
import com.example.uth_todo.ui.home.*
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    state: TaskDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onEdit: () -> Unit,
    onDraft: (TaskDraft) -> Unit,
    onCancelEdit: () -> Unit,
    onSave: () -> Unit,
    onToggle: () -> Unit,
    onSubtask: (Int) -> Unit,
    onDelete: () -> Unit,
) {
    var confirmDelete by rememberSaveable(state.entryKey) { mutableStateOf(false) }
    var confirmDiscard by rememberSaveable(state.entryKey) { mutableStateOf(false) }
    val scroll = rememberScrollState()
    val focus = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    fun showEditor() {
        focus.clearFocus()
        onEdit()
        scope.launch { scroll.scrollTo(0) }
    }
    fun discardEditor() {
        focus.clearFocus()
        onCancelEdit()
        scope.launch { scroll.scrollTo(0) }
    }
    fun requestBack() {
        if (state.isSaving) return
        if (state.isEditing) {
            if (state.isDirty) confirmDiscard = true else discardEditor()
        } else onBack()
    }
    BackHandler(enabled = state.isEditing || state.isSaving) { requestBack() }
    val enabled = !state.isLoading && !state.isSaving && state.error == null && !state.deleted
    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(scroll)
        .padding(20.dp).testTag("task_detail_screen"), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { requestBack() }, enabled = !state.isSaving, modifier = Modifier.testTag("task_detail_back")) {
                Text(stringResource(R.string.auth_back))
            }
            if (state.task != null && !state.isEditing) Row {
                val refreshLabel = stringResource(R.string.tasks_refresh)
                IconButton(onClick = onRetry, enabled = enabled,
                    modifier = Modifier.testTag("task_detail_refresh").semantics { contentDescription = refreshLabel }) {
                    HomeGlyph(HomeIcon.REFRESH)
                }
                TextButton(onClick = { showEditor() }, enabled = enabled,
                    modifier = Modifier.testTag("task_detail_edit")) { Text(stringResource(R.string.task_edit)) }
            }
        }
        Text(stringResource(if (state.isEditing) R.string.task_edit_title else R.string.task_detail_title),
            style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("task_detail_title"))
        if (state.isLoading || state.isSaving) LinearProgressIndicator(
            Modifier.fillMaxWidth().testTag("task_detail_loading"))
        state.error?.let { error ->
            DetailError(stringResource(error.messageResource()))
            Button(onClick = onRetry, enabled = !state.isLoading, modifier = Modifier.testTag("task_detail_retry")) {
                Text(stringResource(R.string.retry))
            }
        }
        state.mutationError?.let { DetailError(stringResource(it.messageResource())) }
        state.validation?.let { DetailError(stringResource(it.messageResource())) }
        if (state.saved) Text(stringResource(R.string.task_saved), color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.testTag("task_detail_saved"))
        val task = state.task
        val draft = state.draft
        if (task != null && draft != null) {
            TaskEditor(draft, !state.isSaving, onDraft)
            Button(onClick = { focus.clearFocus(); onSave(); scope.launch { scroll.scrollTo(0) } },
                enabled = !state.isSaving, modifier = Modifier.fillMaxWidth().testTag("task_edit_save")) {
                Text(stringResource(if (state.isSaving) R.string.saving else R.string.task_save))
            }
            OutlinedButton(onClick = { requestBack() }, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.cancel_action))
            }
        } else if (task != null) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small) {
                    Text(stringResource(task.category.labelResource()), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small) {
                    Text(stringResource(task.priority.labelResource()), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
            Text(task.title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.testTag("task_detail_task_title"))
            Text(stringResource(if (task.done) R.string.tasks_completed else R.string.tasks_pending),
                color = MaterialTheme.colorScheme.primary)
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DetailValue(stringResource(R.string.task_due), listOfNotNull(
                        task.date?.let { TaskDates.shortDate(it) } ?: stringResource(R.string.task_no_date),
                        task.time).joinToString(", "))
                    DetailValue(stringResource(R.string.task_reminder),
                        stringResource(if (task.remind) R.string.task_reminder_on else R.string.task_reminder_off))
                }
            }
            Text(stringResource(R.string.task_note), style = MaterialTheme.typography.titleSmall)
            Text(task.note.ifBlank { stringResource(R.string.task_note_empty) })
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.task_subtasks), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(stringResource(R.string.task_subtasks_progress, task.subtasks.count { it.done }, task.subtasks.size),
                    modifier = Modifier.testTag("task_subtasks_progress"))
            }
            if (task.subtasks.isNotEmpty()) LinearProgressIndicator(
                progress = { task.subtasks.count { it.done }.toFloat() / task.subtasks.size }, modifier = Modifier.fillMaxWidth())
            task.subtasks.forEachIndexed { index, subtask ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(subtask.done, onCheckedChange = { onSubtask(index) }, enabled = enabled,
                            modifier = Modifier.testTag("detail_subtask_toggle_$index"))
                        Text(subtask.title, modifier = Modifier.weight(1f),
                            textDecoration = if (subtask.done) TextDecoration.LineThrough else null)
                    }
                }
            }
            TextButton(onClick = { showEditor() }, enabled = enabled, modifier = Modifier.testTag("task_subtasks_edit")) {
                Text(stringResource(R.string.task_manage_subtasks))
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = onToggle, enabled = enabled, modifier = Modifier.fillMaxWidth().testTag("task_detail_toggle")) {
                Text(stringResource(if (task.done) R.string.task_set_pending else R.string.task_set_done))
            }
            TextButton(onClick = { confirmDelete = true }, enabled = enabled,
                modifier = Modifier.fillMaxWidth().testTag("task_detail_delete")) {
                Text(stringResource(R.string.task_delete), color = MaterialTheme.colorScheme.error)
            }
        }
        Spacer(Modifier.height(16.dp))
    }
    if (confirmDelete && state.task != null) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text(stringResource(R.string.task_delete)) },
        text = { Text(stringResource(R.string.task_delete_confirmation, state.task.title)) },
        confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete() },
            modifier = Modifier.testTag("task_delete_confirm")) { Text(stringResource(R.string.task_delete)) } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel_action)) } })
    if (confirmDiscard) AlertDialog(onDismissRequest = { confirmDiscard = false },
        title = { Text(stringResource(R.string.task_discard_title)) },
        text = { Text(stringResource(R.string.task_discard_body)) },
        confirmButton = { TextButton(onClick = { confirmDiscard = false; discardEditor() },
            modifier = Modifier.testTag("task_discard_confirm")) { Text(stringResource(R.string.task_discard)) } },
        dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text(stringResource(R.string.task_keep_editing)) } })
}

@Composable
private fun DetailError(message: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Text(message, modifier = Modifier.fillMaxWidth().padding(16.dp).testTag("task_detail_error"))
    }
}

@Composable
private fun DetailValue(label: String, value: String) {
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskEditor(draft: TaskDraft, enabled: Boolean, onDraft: (TaskDraft) -> Unit) {
    var showDate by rememberSaveable { mutableStateOf(false) }
    var showTime by rememberSaveable { mutableStateOf(false) }
    val selectDateLabel = stringResource(R.string.task_select_date)
    OutlinedTextField(draft.title, { onDraft(draft.copy(title = it)) }, enabled = enabled,
        label = { Text(stringResource(R.string.task_title_label)) }, modifier = Modifier.fillMaxWidth().testTag("task_edit_title"))
    Text(stringResource(R.string.task_category), style = MaterialTheme.typography.titleSmall)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(TaskCategory.STUDY, TaskCategory.LIFE).forEach { category ->
            FilterChip(draft.category == category, onClick = { onDraft(draft.copy(category = category)) }, enabled = enabled,
                label = { Text(stringResource(category.labelResource())) }, modifier = Modifier.testTag("task_edit_category_${category.name}"))
        }
    }
    OutlinedTextField(draft.date, { onDraft(draft.copy(date = it)) }, enabled = enabled, singleLine = true,
        label = { Text(stringResource(R.string.task_date_label)) },
        trailingIcon = { IconButton(onClick = { showDate = true }, enabled = enabled,
            modifier = Modifier.semantics { contentDescription = selectDateLabel }) { HomeGlyph(HomeIcon.CALENDAR) } },
        modifier = Modifier.fillMaxWidth().testTag("task_edit_date"))
    OutlinedTextField(draft.time, { onDraft(draft.copy(time = it)) }, enabled = enabled, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
        label = { Text(stringResource(R.string.task_time_label)) },
        trailingIcon = { TextButton(onClick = { showTime = true }, enabled = enabled) { Text(stringResource(R.string.select_action)) } },
        modifier = Modifier.fillMaxWidth().testTag("task_edit_time"))
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
        label = { Text(stringResource(R.string.task_note)) }, modifier = Modifier.fillMaxWidth().testTag("task_edit_note"))
    Text(stringResource(R.string.task_subtasks), style = MaterialTheme.typography.titleMedium)
    draft.subtasks.forEachIndexed { index, subtask ->
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(subtask.title, { title ->
                    onDraft(draft.copy(subtasks = draft.subtasks.mapIndexed { i, item -> if (i == index) item.copy(title = title) else item }))
                }, enabled = enabled, label = { Text(stringResource(R.string.task_subtask_title)) },
                    modifier = Modifier.fillMaxWidth().testTag("task_edit_subtask_$index"))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(subtask.done, onCheckedChange = { done ->
                        onDraft(draft.copy(subtasks = draft.subtasks.mapIndexed { i, item -> if (i == index) item.copy(done = done) else item }))
                    }, enabled = enabled)
                    Text(stringResource(R.string.tasks_completed), modifier = Modifier.weight(1f))
                    TextButton(onClick = { onDraft(draft.copy(subtasks = draft.subtasks.filterIndexed { i, _ -> i != index })) },
                        enabled = enabled, modifier = Modifier.testTag("task_edit_subtask_delete_$index")) {
                        Text(stringResource(R.string.task_remove))
                    }
                }
            }
        }
    }
    OutlinedButton(onClick = { onDraft(draft.copy(subtasks = draft.subtasks + TodoSubtask("", false))) },
        enabled = enabled, modifier = Modifier.fillMaxWidth().testTag("task_edit_subtask_add")) {
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

private fun TaskPriority.labelResource(): Int = when (this) {
    TaskPriority.LOW -> R.string.task_priority_low
    TaskPriority.MEDIUM -> R.string.task_priority_medium
    TaskPriority.HIGH -> R.string.task_priority_high
    TaskPriority.OTHER -> R.string.category_other
}

private fun TaskEditError.messageResource(): Int = when (this) {
    TaskEditError.TITLE -> R.string.task_error_title
    TaskEditError.CATEGORY -> R.string.task_error_category
    TaskEditError.DATE -> R.string.task_error_date
    TaskEditError.TIME -> R.string.task_error_time
    TaskEditError.PRIORITY -> R.string.task_error_priority
    TaskEditError.SUBTASK -> R.string.task_error_subtask
}
