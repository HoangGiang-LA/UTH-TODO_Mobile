package com.example.uth_todo.notifications

import com.example.uth_todo.data.tasks.TaskDates
import com.example.uth_todo.data.tasks.TaskSessionScope
import com.example.uth_todo.model.TodoTask
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

data class ReminderKey(val scope: String, val taskId: Long) {
    val token: String get() = "$scope:$taskId"
    companion object {
        fun parse(token: String): ReminderKey? {
            val scope = token.substringBefore(':', "")
            val id = token.substringAfter(':', "").toLongOrNull() ?: return null
            return if (TaskSessionScope.isValid(scope) && id > 0) ReminderKey(scope, id) else null
        }
    }
}
data class TaskReminderPlan(val key: ReminderKey, val dueAt: Long, val fireAt: Long)
data class ReminderOpenRequest(val scope: String, val taskId: Long)
data class ReminderRegistryState(
    val activeScope: String? = null,
    val scheduled: List<TaskReminderPlan> = emptyList(),
    val delivered: Map<String, Long> = emptyMap(),
)
data class ReminderServiceState(
    val available: Boolean = false,
    val notificationsAllowed: Boolean = false,
    val exactAllowed: Boolean = false,
    val scheduledCount: Int = 0,
    val isBusy: Boolean = false,
    val failed: Boolean = false,
    val testResult: Boolean? = null,
    val plans: List<TaskReminderPlan> = emptyList(),
    val delivered: Map<String, Long> = emptyMap(),
)

object TaskReminderPlanner {
    const val LEAD_MILLIS = 15 * 60_000L
    fun dueAt(task: TodoTask, zone: TimeZone): Long? {
        val date = task.date ?: return null
        val time = task.time ?: return null
        if (!TaskDates.isValidDate(date) || !TaskDates.isValidTime(time)) return null
        val input = "$date $time"
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT).apply { isLenient = false; timeZone = zone }
        val position = ParsePosition(0)
        val parsed = format.parse(input, position) ?: return null
        return parsed.time.takeIf { position.index == input.length && format.format(parsed) == input }
    }
    fun plan(scope: String, task: TodoTask, now: Long, zone: TimeZone): TaskReminderPlan? {
        if (!task.remind || task.done || task.id <= 0 || !TaskSessionScope.isValid(scope)) return null
        val due = dueAt(task, zone) ?: return null
        if (due <= now) return null
        val early = due - LEAD_MILLIS
        return TaskReminderPlan(ReminderKey(scope, task.id), due, if (early > now) early else due)
    }
}
