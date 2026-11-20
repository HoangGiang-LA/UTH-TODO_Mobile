package com.example.uth_todo.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.sp
import com.example.uth_todo.R
import com.example.uth_todo.data.tasks.TaskDates
import com.example.uth_todo.data.tasks.forDate
import com.example.uth_todo.data.tasks.summary
import com.example.uth_todo.model.*
import com.example.uth_todo.ui.components.PrimaryButton
import com.example.uth_todo.ui.profile.NotificationPreferenceCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    session: AuthSession,
    onProfile: () -> Unit,
    displayName: String? = null,
    announcement: String? = null,
    state: HomeUiState = HomeUiState(error = TaskApiError.NOT_CONFIGURED),
    onRefresh: () -> Unit = {},
    onToggle: (Long) -> Unit = {},
    onSelectDate: (String) -> Unit = {},
) {
    val user = (session as? AuthSession.SignedIn)?.user
    val name = displayName?.takeIf { it.isNotBlank() } ?: user?.displayName?.takeIf { it.isNotBlank() }
        ?: user?.email?.substringBefore('@') ?: stringResource(R.string.guest_name)
    val initials = name.trim().split(Regex("\\s+")).take(2).mapNotNull { it.firstOrNull() }.joinToString("").uppercase()
    val summary = remember(state.tasks, state.selectedDate) { state.tasks.summary(state.selectedDate) }
    val dayTasks = remember(state.tasks, state.selectedDate) { state.tasks.forDate(state.selectedDate) }
    var listFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var previewId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showDate by rememberSaveable { mutableStateOf(false) }
    var showNotifications by rememberSaveable { mutableStateOf(false) }
    var showCreateNotice by rememberSaveable { mutableStateOf(false) }
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
                IconButton(onClick = { showNotifications = true },
                    modifier = Modifier.semanticsLabel(stringResource(R.string.notifications_title))) { HomeGlyph(HomeIcon.BELL) }
            }
            if (!announcement.isNullOrBlank()) Card(Modifier.fillMaxWidth().testTag("home_announcement")) {
                Text(announcement, modifier = Modifier.padding(16.dp))
            }
            TaskProgressCard(summary, state.hasLoaded, isToday)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CategoryCard(TaskCategory.STUDY, summary.studyRemaining, state.hasLoaded,
                    onClick = { listFilter = TaskFilter.STUDY.name }, modifier = Modifier.weight(1f))
                CategoryCard(TaskCategory.LIFE, summary.lifeRemaining, state.hasLoaded,
                    onClick = { listFilter = TaskFilter.LIFE.name }, modifier = Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(if (isToday) R.string.home_tasks_today else R.string.home_tasks_day),
                    style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = onRefresh, enabled = !state.isLoading && state.changingTaskId == null,
                    modifier = Modifier.testTag("home_refresh").semanticsLabel(stringResource(R.string.tasks_refresh))) {
                    HomeGlyph(HomeIcon.REFRESH)
                }
                TextButton(onClick = { listFilter = TaskFilter.ALL.name }, enabled = state.hasLoaded) {
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
                    onOpen = { id -> previewId = id }, enabled = !state.isLoading) }
                if (dayTasks.size > 8) Text(stringResource(R.string.home_preview_count, 8, dayTasks.size),
                    style = MaterialTheme.typography.bodyMedium)
            }
            if (session == AuthSession.Guest) Text(stringResource(R.string.guest_description),
                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
        }
        Surface(shadowElevation = 5.dp) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                HomeNavItem(HomeIcon.HOME, stringResource(R.string.home_title), true, { listFilter = null }, Modifier.weight(1f))
                HomeNavItem(HomeIcon.LIST, stringResource(R.string.tasks_list_title), false,
                    { listFilter = TaskFilter.ALL.name }, Modifier.weight(1f), enabled = state.hasLoaded)
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    FloatingActionButton(onClick = { showCreateNotice = true }, shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(50.dp).semanticsLabel(stringResource(R.string.task_add))) {
                        HomeGlyph(HomeIcon.PLUS, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
                HomeNavItem(HomeIcon.BELL, stringResource(R.string.notifications_title), false,
                    { showNotifications = true }, Modifier.weight(1f))
                HomeNavItem(HomeIcon.SETTINGS, stringResource(R.string.settings_title), false, onProfile, Modifier.weight(1f))
            }
        }
    }
    listFilter?.let { filter -> TaskListSheet(state, TaskFilter.valueOf(filter), onDismiss = { listFilter = null },
        onToggle = onToggle, onOpen = { previewId = it }) }
    state.tasks.firstOrNull { it.id == previewId }?.let { TaskPreviewDialog(it, onDismiss = { previewId = null }) }
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
    if (showNotifications) ModalBottomSheet(onDismissRequest = { showNotifications = false }) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp)) { NotificationPreferenceCard() }
    }
    if (showCreateNotice) AlertDialog(onDismissRequest = { showCreateNotice = false },
        title = { Text(stringResource(R.string.task_add)) }, text = { Text(stringResource(R.string.task_add_unavailable)) },
        confirmButton = { TextButton(onClick = { showCreateNotice = false }) { Text(stringResource(R.string.close_action)) } })
}

@Composable
private fun HomeNavItem(icon: HomeIcon, label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier, enabled: Boolean = true) {
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    TextButton(onClick = onClick, modifier = modifier, enabled = enabled, contentPadding = PaddingValues(horizontal = 1.dp, vertical = 2.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            HomeGlyph(icon, color = color)
            Text(label, fontSize = 10.sp, lineHeight = 12.sp, color = color, maxLines = 2)
        }
    }
}

private fun Modifier.semanticsLabel(value: String): Modifier = this.then(
    Modifier.semantics { contentDescription = value },
)
