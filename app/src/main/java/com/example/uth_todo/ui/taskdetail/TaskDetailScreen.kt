package com.example.uth_todo.ui.taskdetail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.uth_todo.R
import com.example.uth_todo.data.tasks.TaskDates
import com.example.uth_todo.model.*
import com.example.uth_todo.ui.home.*
import com.example.uth_todo.ui.components.TaskEditor
import com.example.uth_todo.ui.components.labelResource
import com.example.uth_todo.ui.components.messageResource
import com.example.uth_todo.ui.components.TaskStorageNotice
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
    dataMode: TaskDataMode? = null,
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
        TaskStorageNotice(dataMode)
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
