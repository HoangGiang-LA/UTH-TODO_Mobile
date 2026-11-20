package com.example.uth_todo.ui.tasklist

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.uth_todo.R
import com.example.uth_todo.data.tasks.TaskDates
import com.example.uth_todo.model.TaskFilter
import com.example.uth_todo.model.TodoTask
import com.example.uth_todo.ui.components.PrimaryButton
import com.example.uth_todo.ui.components.TaskBottomBar
import com.example.uth_todo.ui.components.TaskTab
import com.example.uth_todo.ui.home.*
import com.example.uth_todo.ui.profile.NotificationPreferenceCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    tasksState: HomeUiState,
    listState: TaskListUiState,
    onQuery: (String) -> Unit,
    onFilter: (TaskFilter) -> Unit,
    onClearSearch: () -> Unit,
    onRefresh: () -> Unit,
    onToggle: (Long) -> Unit,
    onHome: () -> Unit,
    onProfile: () -> Unit,
    onOpenTask: (Long) -> Unit = {},
    onCreateTask: () -> Unit = {},
) {
    val content = remember(tasksState.tasks, listState) { listState.content(tasksState.tasks) }
    val scroll = rememberLazyListState()
    var showNotifications by rememberSaveable { mutableStateOf(false) }
    val refreshLabel = stringResource(R.string.tasks_refresh)
    val clearLabel = stringResource(R.string.tasks_clear_search)
    val today = TaskDates.today()
    val focus = LocalFocusManager.current

    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        LazyColumn(state = scroll, modifier = Modifier.weight(1f).fillMaxWidth().testTag("tasks_list"),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item(key = "list_header") {
                Column(Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.tasks_list_title), style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.testTag("task_list_title"))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (tasksState.hasLoaded) Text(stringResource(R.string.tasks_pending_count, content.pending.size),
                                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.testTag("task_list_pending_count"))
                            IconButton(onClick = onRefresh,
                                enabled = !tasksState.isLoading && tasksState.changingTaskId == null,
                                modifier = Modifier.testTag("task_list_refresh").semantics { contentDescription = refreshLabel }) {
                                HomeGlyph(HomeIcon.REFRESH)
                            }
                        }
                    }
                    OutlinedTextField(listState.query, onValueChange = onQuery, singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
                        label = { Text(stringResource(R.string.tasks_search)) },
                        leadingIcon = { HomeGlyph(HomeIcon.SEARCH) },
                        trailingIcon = {
                            if (listState.query.isNotEmpty()) IconButton(onClick = onClearSearch,
                                modifier = Modifier.testTag("tasks_clear_search").semantics { contentDescription = clearLabel }) {
                                HomeGlyph(HomeIcon.CLOSE)
                            }
                        }, modifier = Modifier.fillMaxWidth().testTag("tasks_search"))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TaskFilter.entries.forEach { filter ->
                            FilterChip(selected = listState.filter == filter, onClick = { onFilter(filter) },
                                shape = RoundedCornerShape(50),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.onSurface,
                                    selectedLabelColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.testTag("task_filter_${filter.name}"),
                                label = { Text(stringResource(when (filter) {
                                    TaskFilter.ALL -> R.string.tasks_all
                                    TaskFilter.STUDY -> R.string.category_study
                                    TaskFilter.LIFE -> R.string.category_life
                                })) })
                        }
                    }
                    if (tasksState.isLoading) LinearProgressIndicator(Modifier.fillMaxWidth().testTag("task_list_loading"))
                }
            }
            tasksState.error?.let { error -> item(key = "load_error") {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(error.messageResource()), modifier = Modifier.testTag("task_list_error"))
                        PrimaryButton(stringResource(R.string.retry), onRefresh,
                            enabled = !tasksState.isLoading && tasksState.changingTaskId == null, tag = "task_list_retry")
                    }
                }
            } }
            if (tasksState.hasLoaded) {
                if (content.isEmpty) item(key = "empty") {
                    Text(stringResource(when {
                        tasksState.tasks.isEmpty() -> R.string.tasks_none
                        listState.query.isNotBlank() -> R.string.tasks_search_empty
                        else -> R.string.tasks_empty
                    }), modifier = Modifier.padding(vertical = 20.dp).testTag("task_list_empty"))
                }
                if (content.pending.isNotEmpty()) item(key = "pending_header") {
                    Text(stringResource(R.string.tasks_pending), style = MaterialTheme.typography.labelLarge)
                }
                items(content.pending, key = { it.id }) { task ->
                    TaskRow(task, tasksState.changingTaskId, onToggle, onOpen = { focus.clearFocus(); onOpenTask(it) }, prefix = "list",
                        enabled = !tasksState.isLoading, scheduleText = taskSchedule(task, today))
                }
                if (content.completed.isNotEmpty()) item(key = "completed_header") {
                    Text(stringResource(R.string.tasks_completed), style = MaterialTheme.typography.labelLarge)
                }
                items(content.completed, key = { it.id }) { task ->
                    TaskRow(task, tasksState.changingTaskId, onToggle, onOpen = { focus.clearFocus(); onOpenTask(it) }, prefix = "list",
                        enabled = !tasksState.isLoading, scheduleText = taskSchedule(task, today))
                }
            } else if (tasksState.error == null) item(key = "initial_loading") {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
        TaskBottomBar(TaskTab.LIST, onHome = { focus.clearFocus(); onHome() }, onList = {},
            onNotifications = { focus.clearFocus(); showNotifications = true },
            onProfile = { focus.clearFocus(); onProfile() },
            onAdd = { focus.clearFocus(); onCreateTask() })
    }
    if (showNotifications) ModalBottomSheet(onDismissRequest = { showNotifications = false }) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp)) { NotificationPreferenceCard() }
    }
}

@Composable
private fun taskSchedule(task: TodoTask, today: String): String {
    val date = when (task.date) {
        null -> stringResource(R.string.task_no_date)
        today -> stringResource(R.string.task_today)
        TaskDates.nextDay(today) -> stringResource(R.string.task_tomorrow)
        else -> TaskDates.shortDate(task.date)
    }
    return task.time?.let { stringResource(R.string.task_schedule, date, it) } ?: date
}
