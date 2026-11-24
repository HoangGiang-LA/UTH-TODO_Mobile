package com.example.uth_todo.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.uth_todo.R
import com.example.uth_todo.model.*

enum class HomeIcon { HOME, LIST, PLUS, BELL, SETTINGS, STUDY, LIFE, REFRESH, CALENDAR, SEARCH, CHEVRON, CLOSE }

@Composable
fun HomeGlyph(icon: HomeIcon, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurface) {
    Canvas(modifier.size(22.dp)) {
        withTransform({ scale(size.width / 24f, size.height / 24f, pivot = Offset.Zero) }) {
            val stroke = Stroke(width = 1.7f, cap = StrokeCap.Round)
            fun lines(vararg points: Pair<Float, Float>) {
                val path = Path().apply {
                    moveTo(points.first().first, points.first().second)
                    points.drop(1).forEach { lineTo(it.first, it.second) }
                }
                drawPath(path, color, style = stroke)
            }
            when (icon) {
                HomeIcon.HOME, HomeIcon.LIFE -> {
                    lines(3f to 10f, 12f to 3f, 21f to 10f)
                    lines(5f to 9f, 5f to 21f, 10f to 21f, 10f to 14f, 14f to 14f, 14f to 21f, 19f to 21f, 19f to 9f)
                }
                HomeIcon.LIST -> for (y in listOf(6f, 12f, 18f)) {
                    drawCircle(color, 1f, Offset(4f, y)); lines(9f to y, 21f to y)
                }
                HomeIcon.PLUS -> { lines(12f to 4f, 12f to 20f); lines(4f to 12f, 20f to 12f) }
                HomeIcon.CHEVRON -> lines(9f to 6f, 15f to 12f, 9f to 18f)
                HomeIcon.CLOSE -> { lines(6f to 6f, 18f to 18f); lines(6f to 18f, 18f to 6f) }
                HomeIcon.BELL -> {
                    lines(4f to 17f, 6f to 14f, 6f to 9f, 8f to 5f, 12f to 4f, 16f to 5f, 18f to 9f, 18f to 14f, 20f to 17f, 4f to 17f)
                    drawArc(color, 0f, 180f, false, Offset(9f, 17f), Size(6f, 5f), style = stroke)
                }
                HomeIcon.SETTINGS -> {
                    for ((y, x) in listOf(6f to 8f, 12f to 16f, 18f to 10f)) {
                        lines(3f to y, (x - 3f) to y); lines((x + 3f) to y, 21f to y)
                        drawCircle(color, 2.5f, Offset(x, y), style = stroke)
                    }
                }
                HomeIcon.STUDY -> {
                    lines(2f to 8f, 12f to 3f, 22f to 8f, 12f to 13f, 2f to 8f)
                    lines(6f to 11f, 6f to 17f, 12f to 20f, 18f to 17f, 18f to 11f)
                    lines(22f to 8f, 22f to 16f)
                }
                HomeIcon.REFRESH -> {
                    drawArc(color, 40f, 290f, false, Offset(4f, 4f), Size(16f, 16f), style = stroke)
                    lines(17f to 2f, 20f to 6f, 15f to 7f)
                }
                HomeIcon.CALENDAR -> {
                    lines(4f to 5f, 20f to 5f, 20f to 21f, 4f to 21f, 4f to 5f)
                    lines(4f to 10f, 20f to 10f); lines(8f to 2f, 8f to 7f); lines(16f to 2f, 16f to 7f)
                }
                HomeIcon.SEARCH -> { drawCircle(color, 7f, Offset(10f, 10f), style = stroke); lines(15f to 15f, 21f to 21f) }
            }
        }
    }
}

@Composable
fun TaskProgressCard(summary: TaskDaySummary, hasLoaded: Boolean, isToday: Boolean) {
    val blue = MaterialTheme.colorScheme.primary
    val onBlue = MaterialTheme.colorScheme.onPrimary
    Card(colors = CardDefaults.cardColors(containerColor = blue, contentColor = onBlue),
        shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(if (isToday) R.string.home_progress_today else R.string.home_progress_day),
                    style = MaterialTheme.typography.bodyMedium)
                Text(if (hasLoaded) stringResource(R.string.home_progress_count, summary.completed, summary.total)
                    else stringResource(R.string.home_loading), style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.testTag("home_progress_count"))
                Text(stringResource(when {
                    !hasLoaded -> R.string.home_progress_waiting
                    summary.total == 0 -> R.string.home_progress_empty
                    summary.completed == summary.total -> R.string.home_progress_finished
                    else -> R.string.home_progress_encouragement
                }), style = MaterialTheme.typography.bodyMedium)
            }
            if (hasLoaded) {
                Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        val width = 6.dp.toPx()
                        val origin = Offset(width / 2, width / 2)
                        val area = Size(size.width - width, size.height - width)
                        drawArc(onBlue.copy(alpha = 0.25f), 0f, 360f, false, origin, area, style = Stroke(width))
                        drawArc(onBlue, -90f, summary.fraction * 360f, false, origin, area,
                            style = Stroke(width, cap = StrokeCap.Round))
                    }
                    Text(stringResource(R.string.home_percent, summary.percent), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CategoryCard(category: TaskCategory, remaining: Int, hasLoaded: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val study = category == TaskCategory.STUDY
    val tint = if (study) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
    Card(onClick = onClick, modifier = modifier.testTag(if (study) "category_study" else "category_life"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.background(tint.copy(alpha = 0.10f), RoundedCornerShape(8.dp)).padding(7.dp)) {
                HomeGlyph(if (study) HomeIcon.STUDY else HomeIcon.LIFE, color = tint)
            }
            Text(stringResource(category.labelResource()), style = MaterialTheme.typography.titleMedium)
            Text(if (hasLoaded) stringResource(R.string.home_remaining, remaining) else stringResource(R.string.home_loading),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun TaskRow(task: TodoTask, busyTaskId: Long?, onToggle: (Long) -> Unit, onOpen: (Long) -> Unit,
    prefix: String = "home", enabled: Boolean = true, scheduleText: String? = null) {
    val toggleLabel = stringResource(if (task.done) R.string.task_mark_pending else R.string.task_mark_done, task.title)
    Card(onClick = { onOpen(task.id) }, enabled = enabled && busyTaskId == null,
        modifier = Modifier.fillMaxWidth().testTag("${prefix}_task_${task.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.fillMaxWidth().padding(end = 10.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            if (busyTaskId == task.id) {
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                }
            } else {
                Checkbox(task.done, onCheckedChange = { onToggle(task.id) }, enabled = enabled && busyTaskId == null,
                    modifier = Modifier.testTag("${prefix}_toggle_${task.id}").semantics { contentDescription = toggleLabel })
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(task.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                    textDecoration = if (task.done) TextDecoration.LineThrough else null,
                    color = if (task.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(task.category.labelResource()),
                        color = if (task.category == TaskCategory.LIFE) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.07f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
                    Text(scheduleText ?: task.time ?: stringResource(R.string.task_no_time), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (!task.done && task.priority == TaskPriority.HIGH) {
                Text(stringResource(R.string.task_high_priority), color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp))
            }
            HomeGlyph(HomeIcon.CHEVRON, Modifier.size(16.dp), MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

fun TaskCategory.labelResource(): Int = when (this) {
    TaskCategory.STUDY -> R.string.category_study
    TaskCategory.LIFE -> R.string.category_life
    TaskCategory.OTHER -> R.string.category_other
}

fun TaskApiError.messageResource(): Int = when (this) {
    TaskApiError.NOT_CONFIGURED -> R.string.tasks_not_configured
    TaskApiError.NOT_FOUND -> R.string.task_not_found
    TaskApiError.UNAUTHORIZED -> R.string.tasks_unauthorized
    TaskApiError.NETWORK -> R.string.auth_error_network
    TaskApiError.TIMEOUT -> R.string.auth_error_timeout
    TaskApiError.RATE_LIMITED -> R.string.auth_error_attempts
    TaskApiError.INVALID_RESPONSE -> R.string.tasks_invalid_response
    TaskApiError.CREATE_MISMATCH -> R.string.task_create_mismatch
    TaskApiError.LOCAL_STORAGE -> R.string.task_storage_error
    TaskApiError.SERVER, TaskApiError.UNKNOWN -> R.string.tasks_server_error
}
