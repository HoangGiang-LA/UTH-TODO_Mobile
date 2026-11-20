package com.example.uth_todo.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.dp
import com.example.uth_todo.R
import com.example.uth_todo.data.tasks.TaskDates
import com.example.uth_todo.data.tasks.forDate
import com.example.uth_todo.data.tasks.summary
import com.example.uth_todo.model.*
import com.example.uth_todo.ui.components.PrimaryButton
import com.example.uth_todo.ui.components.TaskBottomBar
import com.example.uth_todo.ui.components.TaskTab
import com.example.uth_todo.ui.components.TaskStorageNotice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    session: AuthSession,
    onProfile: () -> Unit,
    onOpenList: (TaskFilter) -> Unit,
    onOpenTask: (Long) -> Unit = {},
    displayName: String? = null,
    announcement: String? = null,
    state: HomeUiState = HomeUiState(error = TaskApiError.NOT_CONFIGURED),
    onRefresh: () -> Unit = {},
    onToggle: (Long) -> Unit = {},
    onSelectDate: (String) -> Unit = {},
    onCreateTask: () -> Unit = {},
    onNotifications: () -> Unit = {},
) {
    val user = (session as? AuthSession.SignedIn)?.user
    val name = displayName?.takeIf { it.isNotBlank() } ?: user?.displayName?.takeIf { it.isNotBlank() }
        ?: user?.email?.substringBefore('@') ?: stringResource(R.string.guest_name)
    val initials = name.trim().split(Regex("\\s+")).take(2).mapNotNull { it.firstOrNull() }.joinToString("").uppercase()
    val summary = remember(state.tasks, state.selectedDate) { state.tasks.summary(state.selectedDate) }
    val dayTasks = remember(state.tasks, state.selectedDate) { state.tasks.forDate(state.selectedDate) }
    var showDate by rememberSaveable { mutableStateOf(false) }
    val isToday = state.selectedDate == TaskDates.today()

    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalIconButton(onClick = onProfile, modifier = Modifier.size(44.dp).testTag("open_profile")) {
                    Text(initials, style = MaterialTheme.typography.labelLarge)
                }
                Column(Modifier.weight(1f)) {
                    TextButton(onClick = { showDate = true }, contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.testTag("home_select_date")) {
                        Text(TaskDates.heading(state.selectedDate), style = MaterialTheme.typography.labelSmall)
                    }
                    Text(stringResource(R.string.home_greeting, name), style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.testTag("home_title"))
                }
                IconButton(onClick = onNotifications,
                    modifier = Modifier.semanticsLabel(stringResource(R.string.notifications_title))) { HomeGlyph(HomeIcon.BELL) }
            }
            if (!announcement.isNullOrBlank()) Card(Modifier.fillMaxWidth().testTag("home_announcement")) {
                Text(announcement, modifier = Modifier.padding(16.dp))
            }
            TaskStorageNotice(state.dataMode, onSettings = onProfile)
            TaskProgressCard(summary, state.hasLoaded, isToday)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CategoryCard(TaskCategory.STUDY, summary.studyRemaining, state.hasLoaded,
                    onClick = { onOpenList(TaskFilter.STUDY) }, modifier = Modifier.weight(1f))
                CategoryCard(TaskCategory.LIFE, summary.lifeRemaining, state.hasLoaded,
                    onClick = { onOpenList(TaskFilter.LIFE) }, modifier = Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(if (isToday) R.string.home_tasks_today else R.string.home_tasks_day),
                    style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = onRefresh, enabled = !state.isLoading && state.changingTaskId == null,
                    modifier = Modifier.testTag("home_refresh").semanticsLabel(stringResource(R.string.tasks_refresh))) {
                    HomeGlyph(HomeIcon.REFRESH)
                }
                TextButton(onClick = { onOpenList(TaskFilter.ALL) }, modifier = Modifier.testTag("home_view_all")) {
                    Text(stringResource(R.string.home_view_all))
                }
            }
            if (state.isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().testTag("tasks_loading"))
            state.error?.let { error ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(error.messageResource()), modifier = Modifier.testTag("tasks_error"))
                        PrimaryButton(stringResource(R.string.retry), onRefresh, enabled = !state.isLoading, tag = "tasks_retry")
                    }
                }
            }
            if (state.hasLoaded) {
                if (dayTasks.isEmpty()) Text(stringResource(R.string.home_day_empty),
                    modifier = Modifier.testTag("tasks_empty").padding(vertical = 18.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                dayTasks.take(8).forEach { TaskRow(it, state.changingTaskId, onToggle,
                    onOpen = onOpenTask, enabled = !state.isLoading) }
                if (dayTasks.size > 8) Text(stringResource(R.string.home_preview_count, 8, dayTasks.size),
                    style = MaterialTheme.typography.bodyMedium)
            }
            if (session == AuthSession.Guest) Text(stringResource(R.string.guest_description),
                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
        }
        TaskBottomBar(TaskTab.HOME, onHome = {}, onList = { onOpenList(TaskFilter.ALL) },
            onNotifications = onNotifications, onProfile = onProfile, onAdd = onCreateTask)
    }
    if (showDate) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = TaskDates.toUtcMillis(state.selectedDate))
        DatePickerDialog(onDismissRequest = { showDate = false }, confirmButton = {
            TextButton(onClick = { picker.selectedDateMillis?.let { onSelectDate(TaskDates.fromUtcMillis(it)) }; showDate = false }) {
                Text(stringResource(R.string.select_action))
            }
        }, dismissButton = { TextButton(onClick = { showDate = false }) { Text(stringResource(R.string.cancel_action)) } }) {
            DatePicker(picker, showModeToggle = false)
        }
    }
}

private fun Modifier.semanticsLabel(value: String): Modifier = this.then(
    Modifier.semantics { contentDescription = value },
)
