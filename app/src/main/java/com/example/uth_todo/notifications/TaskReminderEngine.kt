package com.example.uth_todo.notifications

import com.example.uth_todo.model.TaskDataMode
import com.example.uth_todo.model.TodoTask
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.TimeZone

interface ReminderTaskSource {
    suspend fun mode(scope: String): TaskDataMode
    suspend fun tasks(scope: String): List<TodoTask>
}
interface ReminderRegistry {
    suspend fun read(): ReminderRegistryState
    suspend fun write(state: ReminderRegistryState)
}
interface ReminderAlarmPort {
    fun canNotify(): Boolean
    fun canScheduleExact(): Boolean
    fun schedule(plan: TaskReminderPlan, exact: Boolean)
    fun cancel(key: ReminderKey)
    fun cancelNotification(key: ReminderKey)
    fun show(task: TodoTask, plan: TaskReminderPlan): Boolean
}

/** Re-read saved tasks for every delivery; API samples are never armed. */
class TaskReminderEngine(
    private val source: ReminderTaskSource,
    private val registry: ReminderRegistry,
    private val alarms: ReminderAlarmPort,
    private val activeScope: () -> String?,
    private val authorized: (String) -> Boolean = { true },
    private val now: () -> Long = System::currentTimeMillis,
    private val zone: () -> TimeZone = TimeZone::getDefault,
) {
    private val access = Mutex()

    suspend fun refresh(): Int = access.withLock {
        val previous = registry.read()
        val scope = activeScope()?.takeIf(authorized)
        val mode = scope?.let { source.mode(it) }
        val tasks = if (scope != null && mode == TaskDataMode.DEVICE) source.tasks(scope) else emptyList()
        if (activeScope() != scope && scope != null) return@withLock 0
        val byId = tasks.associateBy { it.id }
        val eligible = tasks.filter { it.remind && !it.done }
        val plans = if (scope == null || mode != TaskDataMode.DEVICE || !alarms.canNotify()) emptyList() else eligible.mapNotNull { task ->
            val due = TaskReminderPlanner.dueAt(task, zone()) ?: return@mapNotNull null
            if (previous.delivered[ReminderKey(scope, task.id).token] == due) return@mapNotNull null
            // Preserve an existing, delayed alarm instead of postponing it on each refresh.
            previous.scheduled.firstOrNull { it.key == ReminderKey(scope, task.id) && it.dueAt == due }
                ?: TaskReminderPlanner.plan(scope, task, now(), zone())
        }
        val delivered = previous.delivered.filterKeys { token ->
            val key = ReminderKey.parse(token)
            key != null && (key.scope != scope || mode != TaskDataMode.DEVICE || byId.containsKey(key.taskId))
        }
        registry.write(ReminderRegistryState(scope, plans, delivered))
        previous.scheduled.filter { it !in plans }.forEach { alarms.cancel(it.key); alarms.cancelNotification(it.key) }
        previous.delivered.keys.mapNotNull(ReminderKey::parse).forEach { key ->
            val task = byId[key.taskId]
            if (key.scope != scope || mode != TaskDataMode.DEVICE || task == null || task.done || !task.remind ||
                TaskReminderPlanner.dueAt(task, zone()) != previous.delivered[key.token]) alarms.cancelNotification(key)
        }
        plans.forEach { alarms.schedule(it, alarms.canScheduleExact()) }
        plans.size
    }

    suspend fun deliver(event: TaskReminderPlan): Boolean = access.withLock {
        val state = registry.read()
        if (state.activeScope != event.key.scope || activeScope() != event.key.scope || !authorized(event.key.scope) ||
            event !in state.scheduled || state.delivered[event.key.token] == event.dueAt || now() < event.fireAt) return@withLock false
        if (source.mode(event.key.scope) != TaskDataMode.DEVICE || !alarms.canNotify()) return@withLock false
        val task = source.tasks(event.key.scope).firstOrNull { it.id == event.key.taskId } ?: return@withLock false
        if (task.done || !task.remind || TaskReminderPlanner.dueAt(task, zone()) != event.dueAt ||
            activeScope() != event.key.scope || !authorized(event.key.scope)) return@withLock false
        if (!alarms.show(task, event)) return@withLock false
        registry.write(state.copy(scheduled = state.scheduled.filterNot { it.key == event.key },
            delivered = state.delivered + (event.key.token to event.dueAt)))
        alarms.cancel(event.key)
        true
    }
}
