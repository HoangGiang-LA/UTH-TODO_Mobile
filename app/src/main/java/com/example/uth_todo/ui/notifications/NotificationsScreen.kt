package com.example.uth_todo.ui.notifications

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.uth_todo.R
import com.example.uth_todo.data.tasks.TaskSessionScope
import com.example.uth_todo.model.TaskDataMode
import com.example.uth_todo.notifications.*
import com.example.uth_todo.ui.components.TaskStorageNotice
import com.example.uth_todo.ui.home.*
import com.example.uth_todo.ui.profile.NotificationPreferenceCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

@Composable
fun NotificationsScreen(
    tasks: HomeUiState,
    service: ReminderServiceState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onEnable: () -> Unit,
    onExact: () -> Unit,
    onTest: () -> Unit,
    onOpenTask: (Long) -> Unit,
    onToggle: (Long) -> Unit,
    showGeneralNotifications: Boolean = false,
    settingsError: String? = null,
) {
    val locale = LocalConfiguration.current.locales[0]
    val pending = tasks.tasks.filter { it.remind && !it.done }
        .sortedBy { TaskReminderPlanner.dueAt(it, TimeZone.getDefault()) ?: Long.MAX_VALUE }
    LazyColumn(Modifier.fillMaxSize().safeDrawingPadding().testTag("notifications_screen"),
        contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            TextButton(onClick = onBack, modifier = Modifier.testTag("notifications_back")) { Text(stringResource(R.string.auth_back)) }
            Text(stringResource(R.string.notifications_title), style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.testTag("notifications_title"))
        }
        item { TaskStorageNotice(tasks.dataMode) }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.reminders_title), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(R.string.reminders_policy))
                    Text(stringResource(when {
                        !service.available -> R.string.reminders_unavailable
                        !service.notificationsAllowed -> R.string.reminders_permission_needed
                        !service.exactAllowed -> R.string.reminders_approximate
                        else -> R.string.reminders_precise
                    }), modifier = Modifier.testTag("reminders_status"))
                    if (service.isBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    if (service.failed) Text(stringResource(R.string.reminders_failed), color = MaterialTheme.colorScheme.error)
                    Text(stringResource(R.string.reminders_scheduled_count, service.scheduledCount),
                        modifier = Modifier.testTag("reminders_count"))
                    OutlinedButton(onClick = onEnable, enabled = service.available, modifier = Modifier.fillMaxWidth().testTag("reminders_enable")) {
                        Text(stringResource(if (service.notificationsAllowed) R.string.notifications_settings else R.string.notifications_enable))
                    }
                    if (!service.exactAllowed) OutlinedButton(onClick = onExact, enabled = service.available,
                        modifier = Modifier.fillMaxWidth().testTag("reminders_exact")) { Text(stringResource(R.string.reminders_allow_exact)) }
                    Button(onClick = onTest, enabled = service.available && service.notificationsAllowed,
                        modifier = Modifier.fillMaxWidth().testTag("reminders_test")) { Text(stringResource(R.string.reminders_send_test)) }
                    service.testResult?.let { Text(stringResource(if (it) R.string.reminders_test_sent else R.string.reminders_test_failed),
                        modifier = Modifier.testTag("reminders_test_result")) }
                    TextButton(onClick = onRefresh, modifier = Modifier.testTag("reminders_refresh")) { Text(stringResource(R.string.tasks_refresh)) }
                    settingsError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
        }
        if (tasks.dataMode == TaskDataMode.COURSE_API) item { Text(stringResource(R.string.reminders_device_only)) }
        if (tasks.dataMode == TaskDataMode.DEVICE) {
            item { Text(stringResource(R.string.reminders_tasks_title), style = MaterialTheme.typography.titleLarge) }
            if (pending.isEmpty()) item { Text(stringResource(R.string.reminders_empty), modifier = Modifier.testTag("reminders_empty")) }
            items(pending, key = { it.id }) { task ->
                Column {
                    TaskRow(task, tasks.changingTaskId, onToggle, onOpenTask, prefix = "reminder",
                        enabled = !tasks.isLoading, scheduleText = listOfNotNull(task.date, task.time).joinToString(" "))
                    val key = tasks.sessionScope?.let { ReminderKey(it, task.id) }
                    val due = TaskReminderPlanner.dueAt(task, TimeZone.getDefault())
                    val plan = service.plans.firstOrNull { it.key == key && it.dueAt == due }
                    val status = when {
                        key != null && due != null && service.delivered[key.token] == due -> stringResource(R.string.reminders_already_sent)
                        plan != null -> stringResource(R.string.reminders_will_fire,
                            SimpleDateFormat("dd/MM/yyyy HH:mm", locale).format(Date(plan.fireAt)))
                        due == null -> stringResource(R.string.reminders_missing_schedule)
                        due <= System.currentTimeMillis() -> stringResource(R.string.reminders_past)
                        !service.notificationsAllowed -> stringResource(R.string.reminders_permission_needed)
                        else -> stringResource(R.string.reminders_waiting)
                    }
                    Text(status, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp))
                }
            }
        }
        if (showGeneralNotifications) item { NotificationPreferenceCard() }
    }
}
