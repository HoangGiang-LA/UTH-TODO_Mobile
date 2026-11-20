package com.example.uth_todo.ui.taskcreate

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.uth_todo.R
import com.example.uth_todo.model.TaskDraft
import com.example.uth_todo.model.TaskDataMode
import com.example.uth_todo.ui.components.TaskStorageNotice
import com.example.uth_todo.ui.components.TaskEditor
import com.example.uth_todo.ui.components.messageResource
import com.example.uth_todo.ui.home.messageResource
import kotlinx.coroutines.launch

@Composable
fun TaskCreateScreen(
    state: TaskCreateUiState,
    onBack: () -> Unit,
    onDraft: (TaskDraft) -> Unit,
    onCreate: () -> Unit,
    onReviewList: () -> Unit,
    onAllowRetry: () -> Unit,
    dataMode: TaskDataMode? = null,
) {
    var confirmDiscard by rememberSaveable(state.entryKey) { mutableStateOf(false) }
    var confirmRetry by rememberSaveable(state.entryKey) { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    fun requestBack() {
        if (state.isSaving) return
        focus.clearFocus()
        if (state.isDirty) confirmDiscard = true else onBack()
    }
    BackHandler { requestBack() }
    val enabled = state.entryKey != null && !state.isSaving && state.created == null
    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(scroll)
        .padding(20.dp).testTag("task_create_screen"), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TextButton(onClick = { requestBack() }, enabled = !state.isSaving, modifier = Modifier.testTag("task_create_back")) {
            Text(stringResource(R.string.auth_back))
        }
        Text(stringResource(R.string.task_add), style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.testTag("task_create_title_heading"))
        if (state.isSaving) LinearProgressIndicator(Modifier.fillMaxWidth().testTag("task_create_loading"))
        TaskStorageNotice(dataMode)
        state.validation?.let { error ->
            CreateError(stringResource(error.messageResource()))
        }
        state.error?.let { error ->
            CreateError(stringResource(error.messageResource()))
        }
        if (state.requiresReview) {
            Text(stringResource(R.string.task_create_uncertain), modifier = Modifier.testTag("task_create_uncertain"))
            OutlinedButton(onClick = { focus.clearFocus(); onReviewList() }, enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth().testTag("task_create_review")) {
                Text(stringResource(R.string.task_create_review))
            }
            TextButton(onClick = { focus.clearFocus(); confirmRetry = true }, enabled = !state.isSaving,
                modifier = Modifier.testTag("task_create_retry")) { Text(stringResource(R.string.task_create_retry)) }
        }
        TaskEditor(state.draft, enabled, onDraft, tagPrefix = "task_create")
        Button(onClick = {
            focus.clearFocus()
            onCreate()
            scope.launch { scroll.scrollTo(0) }
        }, enabled = enabled && !state.requiresReview, modifier = Modifier.fillMaxWidth().testTag("task_create_submit")) {
            Text(stringResource(if (state.isSaving) R.string.saving else R.string.task_create_submit))
        }
        OutlinedButton(onClick = { requestBack() }, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.cancel_action))
        }
        Spacer(Modifier.height(16.dp))
    }
    if (confirmDiscard) AlertDialog(onDismissRequest = { confirmDiscard = false },
        title = { Text(stringResource(R.string.task_discard_title)) },
        text = { Text(stringResource(R.string.task_discard_body)) },
        confirmButton = { TextButton(onClick = { confirmDiscard = false; onBack() },
            modifier = Modifier.testTag("task_create_discard_confirm")) { Text(stringResource(R.string.task_discard)) } },
        dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text(stringResource(R.string.task_keep_editing)) } })
    if (confirmRetry) AlertDialog(onDismissRequest = { confirmRetry = false },
        title = { Text(stringResource(R.string.task_create_retry)) },
        text = { Text(stringResource(R.string.task_create_retry_body)) },
        confirmButton = { TextButton(onClick = { confirmRetry = false; onAllowRetry() },
            modifier = Modifier.testTag("task_create_retry_confirm")) { Text(stringResource(R.string.continue_action)) } },
        dismissButton = { TextButton(onClick = { confirmRetry = false }) { Text(stringResource(R.string.cancel_action)) } })
}

@Composable
private fun CreateError(message: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Text(message, modifier = Modifier.fillMaxWidth().padding(16.dp).testTag("task_create_error"))
    }
}
