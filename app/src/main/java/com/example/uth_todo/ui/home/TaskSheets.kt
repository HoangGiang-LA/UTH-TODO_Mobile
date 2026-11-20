package com.example.uth_todo.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.uth_todo.R
import com.example.uth_todo.data.tasks.TaskDates
import com.example.uth_todo.data.tasks.matching
import com.example.uth_todo.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListSheet(state: HomeUiState, initialFilter: TaskFilter, onDismiss: () -> Unit,
    onToggle: (Long) -> Unit, onOpen: (Long) -> Unit) {
    var filterName by rememberSaveable(initialFilter) { mutableStateOf(initialFilter.name) }
    var query by rememberSaveable { mutableStateOf("") }
    val filter = TaskFilter.valueOf(filterName)
    val tasks = remember(state.tasks, filter, query) { state.tasks.matching(filter, query) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.9f).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.tasks_list_title), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.tasks_pending_count, state.tasks.count { !it.done }),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(query, onValueChange = { query = it }, singleLine = true,
                label = { Text(stringResource(R.string.tasks_search)) }, leadingIcon = { HomeGlyph(HomeIcon.SEARCH) },
                modifier = Modifier.fillMaxWidth().testTag("tasks_search"))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (item in TaskFilter.entries) {
                    FilterChip(selected = filter == item, onClick = { filterName = item.name },
                        label = { Text(stringResource(when (item) {
                            TaskFilter.ALL -> R.string.tasks_all
                            TaskFilter.STUDY -> R.string.category_study
                            TaskFilter.LIFE -> R.string.category_life
                        })) })
                }
            }
            LazyColumn(Modifier.weight(1f).testTag("tasks_list"), verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)) {
                if (tasks.isEmpty()) item {
                    Text(stringResource(if (query.isBlank()) R.string.tasks_empty else R.string.tasks_search_empty),
                        modifier = Modifier.padding(vertical = 20.dp))
                }
                val pending = tasks.filter { !it.done }
                val completed = tasks.filter { it.done }
                if (pending.isNotEmpty()) item { Text(stringResource(R.string.tasks_pending), style = MaterialTheme.typography.labelLarge) }
                items(pending, key = { it.id }) { task ->
                    TaskRow(task, state.changingTaskId, onToggle, onOpen, prefix = "list", enabled = !state.isLoading)
                    task.date?.let { Text(TaskDates.shortDate(it), style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(start = 48.dp, top = 3.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                if (completed.isNotEmpty()) item { Text(stringResource(R.string.tasks_completed), style = MaterialTheme.typography.labelLarge) }
                items(completed, key = { it.id }) { TaskRow(it, state.changingTaskId, onToggle, onOpen,
                    prefix = "list", enabled = !state.isLoading) }
            }
        }
    }
}

@Composable
fun TaskPreviewDialog(task: TodoTask, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(task.title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(task.category.labelResource()))
                Text(task.date?.let { TaskDates.shortDate(it) } ?: stringResource(R.string.task_no_date))
                Text(task.time ?: stringResource(R.string.task_no_time))
                Text(stringResource(if (task.done) R.string.tasks_completed else R.string.tasks_pending))
                if (task.note.isNotBlank()) Text(task.note)
                if (task.subtasks.isNotEmpty()) {
                    Text(stringResource(R.string.task_subtasks), style = MaterialTheme.typography.titleSmall)
                    task.subtasks.forEach { subtask ->
                        Text(stringResource(if (subtask.done) R.string.task_subtask_done else R.string.task_subtask_pending, subtask.title))
                    }
                }
            }
        }, confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close_action)) } })
}
