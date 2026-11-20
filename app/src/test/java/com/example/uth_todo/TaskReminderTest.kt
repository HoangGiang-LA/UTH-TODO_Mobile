package com.example.uth_todo

import androidx.lifecycle.ViewModelStore
import com.example.uth_todo.data.tasks.*
import com.example.uth_todo.model.*
import com.example.uth_todo.notifications.*
import com.example.uth_todo.ui.home.HomeViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
class TaskReminderTest {
    private val utc = TimeZone.getTimeZone("UTC")
    private var time = instant("2026-11-20 09:00")
    private var owner: String? = "guest"
    private var allowed = true
    private val source = ReminderSourceFake()
    private val registry = ReminderRegistryFake()
    private val port = ReminderPortFake()
    private val dispatcher = StandardTestDispatcher()
    private val models = ViewModelStore()
    @Before fun setup() { Dispatchers.setMain(dispatcher); source.values["guest"] = listOf(task()) }
    @After fun cleanup() { models.clear(); Dispatchers.resetMain() }
    private fun instant(value: String, zone: TimeZone = TimeZone.getTimeZone("UTC")) =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT).apply { timeZone = zone }.parse(value)!!.time
    private fun task(time: String? = "10:00", remind: Boolean = true, done: Boolean = false) =
        TodoTask(1, "Lau nha", TaskCategory.LIFE, "2026-11-20", time, done, remind = remind)
    private fun engine(zone: TimeZone = utc) = TaskReminderEngine(source, registry, port, { owner }, { allowed }, { time }, { zone })

    @Test fun plansFifteenMinutesBeforeTheDeadline() {
        val plan = TaskReminderPlanner.plan("guest", task(), time, utc)!!
        assertEquals(instant("2026-11-20 09:45"), plan.fireAt)
        assertEquals(instant("2026-11-20 10:00"), plan.dueAt)
    }
    @Test fun closeDeadlineUsesDueTimeInsteadOfAnImmediateNotification() {
        val plan = TaskReminderPlanner.plan("guest", task("09:05"), time, utc)!!
        assertEquals(instant("2026-11-20 09:05"), plan.fireAt)
        assertEquals(plan.dueAt, plan.fireAt)
    }
    @Test fun unselectedCompletedUnscheduledOrPastTasksDoNotCreateNewAlarms() {
        for (value in listOf(task(remind = false), task(done = true), task(time = null),
            task("08:00"), task().copy(date = null), task().copy(date = "2026-02-30"))) {
            assertNull(TaskReminderPlanner.plan("guest", value, time, utc))
        }
        assertNull(TaskReminderPlanner.plan("invalid", task(), time, utc))
    }
    @Test fun deadlineFollowsTheCurrentTimeZone() {
        val zone = TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
        assertEquals(instant("2026-11-20 03:00"), TaskReminderPlanner.dueAt(task(), zone))
        assertEquals(instant("2026-11-20 10:00"), TaskReminderPlanner.dueAt(task(), utc))
    }
    @Test fun nonexistentDaylightSavingTimeIsNotSilentlyMoved() {
        val task = task("02:30").copy(date = "2026-03-08")
        assertNull(TaskReminderPlanner.dueAt(task, TimeZone.getTimeZone("America/New_York")))
    }
    @Test fun selectingReminderRequiresBothDateAndTimeInTheEditor() {
        val draft = TaskDraft.blank().copy(title = "Lau nha", remind = true)
        assertEquals(TaskEditError.REMINDER_SCHEDULE, draft.validationError())
        assertEquals(TaskEditError.REMINDER_SCHEDULE, draft.copy(date = "", time = "10:00").validationError())
        assertNull(draft.copy(time = "10:00").validationError())
    }
    @Test fun deviceTasksAreScheduledAndApiSamplesAreNeverScheduled() = runTest {
        val engine = engine()
        assertEquals(1, engine.refresh())
        assertEquals("guest", registry.state.activeScope)
        assertEquals(1, port.scheduled.size)
        source.mode = TaskDataMode.COURSE_API
        assertEquals(0, engine.refresh())
        assertTrue(registry.state.scheduled.isEmpty())
        assertTrue(port.cancelled.contains(ReminderKey("guest", 1)))
    }
    @Test fun duplicateAlarmDeliveryPostsAtMostOnceAndStaysConsumedAfterEngineRecreation() = runTest {
        val engine = engine(); engine.refresh()
        val event = registry.state.scheduled.single(); time = event.fireAt
        assertTrue(engine.deliver(event))
        assertFalse(engine.deliver(event))
        assertEquals(1, port.shown.size)
        assertEquals(event.dueAt, registry.state.delivered[event.key.token])
        assertEquals(0, engine().refresh())
        assertFalse(engine().deliver(event))
    }
    @Test fun completedDeletedAndDisabledTasksCancelExistingAlarms() = runTest {
        for (tasks in listOf(listOf(task(done = true)), emptyList(), listOf(task(remind = false)))) {
            source.values["guest"] = listOf(task())
            registry.state = ReminderRegistryState()
            val engine = engine(); engine.refresh()
            val event = registry.state.scheduled.single()
            source.values["guest"] = tasks
            assertEquals(0, engine.refresh())
            time = event.fireAt
            assertFalse(engine.deliver(event))
            time = instant("2026-11-20 09:00")
        }
        assertTrue(port.cancelled.contains(ReminderKey("guest", 1)))
        assertTrue(port.shown.isEmpty())
    }
    @Test fun changedDeadlineRejectsTheOldIntentAndReplacesItsAlarm() = runTest {
        val engine = engine(); engine.refresh()
        val old = registry.state.scheduled.single()
        source.values["guest"] = listOf(task("11:00"))
        engine.refresh()
        val updated = registry.state.scheduled.single()
        assertNotEquals(old, updated)
        time = old.fireAt
        assertFalse(engine.deliver(old))
        time = updated.fireAt
        assertTrue(engine.deliver(updated))
    }
    @Test fun changingAccountCancelsOldAlarmsAndDoesNotShowTheirContents() = runTest {
        val engine = engine(); engine.refresh()
        val old = registry.state.scheduled.single()
        owner = TaskSessionScope.forUid("bob")
        source.values[owner!!] = listOf(task().copy(title = "Bob task"))
        engine.refresh()
        time = old.fireAt
        assertFalse(engine.deliver(old))
        assertTrue(port.shown.isEmpty())
        val current = registry.state.scheduled.single()
        assertTrue(engine.deliver(current))
        assertEquals("Bob task", port.shown.single().title)
    }
    @Test fun signOutOrInvalidAuthorizationCancelsAllPendingTaskAlarms() = runTest {
        val engine = engine(); engine.refresh()
        val event = registry.state.scheduled.single()
        allowed = false
        assertEquals(0, engine.refresh())
        assertNull(registry.state.activeScope)
        time = event.fireAt
        assertFalse(engine.deliver(event))
        owner = null
        assertEquals(0, engine.refresh())
    }
    @Test fun notificationPermissionOffDefersSchedulingUntilItIsGranted() = runTest {
        val engine = engine()
        port.notifications = false
        assertEquals(0, engine.refresh())
        port.notifications = true
        assertEquals(1, engine.refresh())
    }
    @Test fun exactPermissionControlsPrecisionWithoutBlockingApproximateReminders() = runTest {
        val engine = engine()
        port.exact = false
        engine.refresh()
        assertFalse(port.scheduled.last().second)
        port.exact = true
        engine.refresh()
        assertTrue(port.scheduled.last().second)
    }
    @Test fun delayedExistingAlarmIsNotPostponedAndCanDeliverAfterTheDeadline() = runTest {
        val engine = engine(); engine.refresh()
        val event = registry.state.scheduled.single()
        time = instant("2026-11-20 10:05")
        assertEquals(1, engine.refresh())
        assertEquals(event, registry.state.scheduled.single())
        assertTrue(engine.deliver(event))
        assertEquals(0, engine.refresh())
    }
    @Test fun restoreUsesSavedTaskDataWithoutRequiringTheUiOrRecreatingTasks() = runTest {
        val engine = engine(); engine.refresh()
        val saved = registry.state
        port.scheduled.clear()
        assertEquals(1, engine().refresh())
        assertEquals(saved, registry.state)
        assertEquals(saved.scheduled.single(), port.scheduled.single().first)
        assertEquals("Lau nha", source.values["guest"]!!.single().title)
    }
    @Test fun timeZoneChangeReplacesTheOldInstantForTheSameLocalDeadline() = runTest {
        time = instant("2026-11-20 00:00")
        engine().refresh()
        val old = registry.state.scheduled.single()
        engine(TimeZone.getTimeZone("Asia/Ho_Chi_Minh")).refresh()
        val current = registry.state.scheduled.single()
        assertEquals(old.dueAt - 7 * 60 * 60_000L, current.dueAt)
        assertTrue(port.cancelled.contains(old.key))
    }
    @Test fun deliveryReadsTheLatestTitleAndDoesNotTrustIntentText() = runTest {
        val engine = engine(); engine.refresh()
        val event = registry.state.scheduled.single()
        source.values["guest"] = listOf(task().copy(title = "Lau nhà tầng 1", note = "Mới"))
        time = event.fireAt
        assertTrue(engine.deliver(event))
        assertEquals("Lau nhà tầng 1", port.shown.single().title)
        assertEquals("Mới", port.shown.single().note)
    }
    @Test fun permissionRevokedAtPostDoesNotConsumeTheReminder() = runTest {
        val engine = engine(); engine.refresh()
        val event = registry.state.scheduled.single()
        time = event.fireAt
        port.postResult = false
        assertFalse(engine.deliver(event))
        assertTrue(registry.state.delivered.isEmpty())
        port.postResult = true
        assertTrue(engine.deliver(event))
    }
    @Test fun malformedKeysAndIncorrectEventsCannotDeliverAnAlarm() = runTest {
        for (token in listOf("", "guest:0", "guest:-1", "unknown:1", "guest:x", "user_bob:1")) assertNull(ReminderKey.parse(token))
        assertEquals(ReminderKey("guest", 1), ReminderKey.parse("guest:1"))
        val engine = engine(); engine.refresh()
        val event = registry.state.scheduled.single()
        time = event.fireAt
        assertFalse(engine.deliver(event.copy(dueAt = event.dueAt + 1)))
        assertFalse(engine.deliver(event.copy(key = ReminderKey("guest", 2))))
        assertTrue(port.shown.isEmpty())
    }
    @Test fun aLateReadForAnOldAccountCannotScheduleAfterScopeChanges() = runTest {
        val gate = CompletableDeferred<Unit>()
        source.gate = gate
        val job = async { engine().refresh() }
        runCurrent()
        owner = TaskSessionScope.forUid("bob")
        gate.complete(Unit); job.await()
        assertTrue(port.scheduled.isEmpty())
    }
    @Test fun viewModelBindsAndClearsTheReminderSessionAlongWithTheRepository() = runTest(dispatcher) {
        val sessions = mutableListOf<AuthSession>()
        val controller = object : TaskReminderController {
            override val uiState = MutableStateFlow(ReminderServiceState())
            override fun bindSession(session: AuthSession) { sessions += session }
            override fun refresh() = Unit
            override fun sendTest() = Unit
        }
        val model = HomeViewModel(ReminderRepositoryFake(), reminders = controller)
        models.put("tasks", model)
        model.bindSession(AuthSession.Guest); advanceUntilIdle()
        model.bindSession(AuthSession.Guest)
        model.bindSession(AuthSession.SignedOut); advanceUntilIdle()
        assertEquals(listOf(AuthSession.Guest, AuthSession.SignedOut), sessions)
        assertNull(model.uiState.value.sessionScope)
    }
}
private class ReminderRegistryFake : ReminderRegistry {
    var state = ReminderRegistryState()
    override suspend fun read() = state
    override suspend fun write(state: ReminderRegistryState) { this.state = state }
}
private class ReminderSourceFake : ReminderTaskSource {
    var mode = TaskDataMode.DEVICE
    val values = mutableMapOf<String, List<TodoTask>>()
    var gate: CompletableDeferred<Unit>? = null
    override suspend fun mode(scope: String) = mode
    override suspend fun tasks(scope: String): List<TodoTask> { val snapshot = values[scope].orEmpty(); gate?.await(); return snapshot }
}
private class ReminderPortFake : ReminderAlarmPort {
    var notifications = true
    var exact = true
    var postResult = true
    val scheduled = mutableListOf<Pair<TaskReminderPlan, Boolean>>()
    val cancelled = mutableListOf<ReminderKey>()
    val shown = mutableListOf<TodoTask>()
    override fun canNotify() = notifications
    override fun canScheduleExact() = exact
    override fun schedule(plan: TaskReminderPlan, exact: Boolean) { scheduled += plan to exact }
    override fun cancel(key: ReminderKey) { cancelled += key }
    override fun cancelNotification(key: ReminderKey) = Unit
    override fun show(task: TodoTask, plan: TaskReminderPlan): Boolean { if (postResult) shown += task; return postResult }
}
private class ReminderRepositoryFake : TaskRepository {
    override val isConfigured = true
    override suspend fun loadTasks() = emptyList<TodoTask>()
    override suspend fun loadTask(id: Long): TodoTask = error("Unused")
    override suspend fun createTask(draft: TaskDraft): TodoTask = error("Unused")
    override suspend fun setDone(id: Long, done: Boolean) = Unit
    override suspend fun updateTask(task: TodoTask) = Unit
    override suspend fun setSubtasks(id: Long, subtasks: List<TodoSubtask>) = Unit
    override suspend fun deleteTask(id: Long) = Unit
}
