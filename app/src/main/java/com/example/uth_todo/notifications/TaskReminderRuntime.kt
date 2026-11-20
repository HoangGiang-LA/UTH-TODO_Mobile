package com.example.uth_todo.notifications

import android.content.Context
import com.example.uth_todo.data.tasks.TaskSessionScope
import com.example.uth_todo.data.tasks.androidDeviceTaskStorage
import com.example.uth_todo.model.AuthSession
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.atomic.AtomicReference

class TaskReminderRuntime private constructor(context: Context) : TaskReminderController {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val registry = AndroidReminderRegistry(appContext)
    private val active = AtomicReference(registry.initialScope())
    private val storage = androidDeviceTaskStorage(appContext)
    private val alarms = AndroidReminderAlarms(appContext)
    private val mutableState = MutableStateFlow(ReminderServiceState(available = true))
    override val uiState = mutableState.asStateFlow()
    private val engine = TaskReminderEngine(object : ReminderTaskSource {
        override suspend fun mode(scope: String) = storage.mode(scope)
        override suspend fun tasks(scope: String) = storage.load(scope)
    }, registry, alarms, active::get, ::authorized)

    init {
        val initialVersion = storage.changes.value
        scope.launch { storage.changes.filter { it != initialVersion }.collect { refreshNow() } }
    }
    private fun authorized(key: String): Boolean {
        if (active.get() != key) return false
        if (key == "guest") return true
        val app = FirebaseApp.getApps(appContext).firstOrNull { it.name == FirebaseApp.DEFAULT_APP_NAME } ?: return false
        val uid = FirebaseAuth.getInstance(app).currentUser?.uid ?: return false
        return TaskSessionScope.forUid(uid) == key
    }
    override fun bindSession(session: AuthSession) {
        if (session == AuthSession.Loading) return
        active.set(TaskSessionScope.from(session))
        mutableState.update { it.copy(plans = emptyList(), delivered = emptyMap(), scheduledCount = 0, testResult = null) }
        refresh()
    }
    override fun refresh() { scope.launch { refreshNow() } }
    suspend fun restore() { refreshNow() }
    private suspend fun refreshNow() {
        mutableState.update { it.copy(isBusy = true, failed = false) }
        try {
            val count = engine.refresh()
            val saved = registry.read()
            mutableState.update { it.copy(scheduledCount = count, plans = saved.scheduled, delivered = saved.delivered) }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { mutableState.update { it.copy(failed = true) } }
        finally {
            mutableState.update { it.copy(isBusy = false, notificationsAllowed = alarms.canNotify(), exactAllowed = alarms.canScheduleExact()) }
        }
    }
    override fun sendTest() {
        scope.launch {
            val sent = try { alarms.showTest() } catch (_: Exception) { false }
            mutableState.update { it.copy(testResult = sent, notificationsAllowed = alarms.canNotify(), exactAllowed = alarms.canScheduleExact()) }
        }
    }
    suspend fun deliver(plan: TaskReminderPlan) {
        try { if (engine.deliver(plan)) refreshNow() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { mutableState.update { it.copy(failed = true) } }
    }
    fun background(operation: suspend TaskReminderRuntime.() -> Unit, onFinished: () -> Unit) {
        scope.launch { try { withTimeout(8_000) { operation() } } finally { onFinished() } }
    }
    companion object {
        @Volatile private var instance: TaskReminderRuntime? = null
        fun get(context: Context): TaskReminderRuntime = instance ?: synchronized(this) {
            instance ?: TaskReminderRuntime(context).also { instance = it }
        }
    }
}
