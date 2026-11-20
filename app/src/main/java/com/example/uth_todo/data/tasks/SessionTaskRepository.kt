package com.example.uth_todo.data.tasks

import com.example.uth_todo.model.*
import java.security.MessageDigest

/** Device data and course API samples are separate datasets; never merge or silently fall back. */
class SessionTaskRepository(
    private val storage: DeviceTaskStorage,
    private val courseApi: TaskRepository,
) : TaskRepository, TaskDataController {
    @Volatile private var scope: String? = null
    override val isConfigured = true

    override fun bindSession(session: AuthSession) {
        scope = when (session) {
            AuthSession.Guest -> "guest"
            is AuthSession.SignedIn -> "user_" + MessageDigest.getInstance("SHA-256")
                .digest(session.user.uid.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it.toInt() and 255) }
            else -> null
        }
    }

    private fun sessionScope(): String = scope ?: throw TaskApiFailure(TaskApiError.UNAUTHORIZED)
    override suspend fun currentMode(): TaskDataMode = storage.mode(sessionScope())
    override suspend fun selectMode(mode: TaskDataMode) {
        val key = sessionScope()
        storage.selectMode(key, mode)
    }

    private data class Selection(val scope: String, val mode: TaskDataMode)
    private suspend fun selection(): Selection {
        val key = sessionScope() // capture before suspension: a session change cannot redirect an old request
        return Selection(key, storage.mode(key))
    }

    override suspend fun loadTasks(): List<TodoTask> {
        val selection = selection()
        return if (selection.mode == TaskDataMode.DEVICE) storage.load(selection.scope) else courseApi.loadTasks()
    }
    override suspend fun loadTask(id: Long): TodoTask {
        val selection = selection()
        return if (selection.mode == TaskDataMode.DEVICE) storage.load(selection.scope, id) else courseApi.loadTask(id)
    }
    override suspend fun createTask(draft: TaskDraft): TodoTask {
        val selection = selection()
        return if (selection.mode == TaskDataMode.DEVICE) storage.create(selection.scope, draft) else courseApi.createTask(draft)
    }
    override suspend fun setDone(id: Long, done: Boolean) {
        val selection = selection()
        if (selection.mode == TaskDataMode.DEVICE) storage.update(selection.scope, id) { it.copy(done = done) }
        else courseApi.setDone(id, done)
    }
    override suspend fun updateTask(task: TodoTask) {
        val selection = selection()
        if (selection.mode == TaskDataMode.DEVICE) storage.update(selection.scope, task.id) { task }
        else courseApi.updateTask(task)
    }
    override suspend fun setSubtasks(id: Long, subtasks: List<TodoSubtask>) {
        val selection = selection()
        if (selection.mode == TaskDataMode.DEVICE) storage.update(selection.scope, id) { it.copy(subtasks = subtasks) }
        else courseApi.setSubtasks(id, subtasks)
    }
    override suspend fun deleteTask(id: Long) {
        val selection = selection()
        if (selection.mode == TaskDataMode.DEVICE) storage.delete(selection.scope, id) else courseApi.deleteTask(id)
    }
}
