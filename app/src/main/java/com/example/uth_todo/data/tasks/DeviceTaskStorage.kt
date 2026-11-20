package com.example.uth_todo.data.tasks

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.example.uth_todo.model.*
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Small, transactional task datasets. Do not replace corrupt data with an empty list. */
class DeviceTaskStorage(private val dataStore: DataStore<Preferences>) {
    // Keep reads (including mode selection) and writes in one queue. In particular,
    // a Windows reader must finish before DataStore replaces the preferences file.
    private val access = Mutex()
    private val decoder = TaskJsonDecoder()
    private val gson = GsonBuilder().serializeNulls().create()

    suspend fun mode(scope: String): TaskDataMode = storageOperation {
        val value = dataStore.data.first()[modeKey(scope)]
        if (value == null) TaskDataMode.DEVICE else TaskDataMode.valueOf(value)
    }

    suspend fun selectMode(scope: String, mode: TaskDataMode) = storageOperation {
        dataStore.edit { it[modeKey(scope)] = mode.name }
        Unit
    }

    suspend fun load(scope: String): List<TodoTask> = storageOperation {
        decode(dataStore.data.first()[tasksKey(scope)])
    }

    suspend fun load(scope: String, id: Long): TodoTask =
        load(scope).firstOrNull { it.id == id } ?: throw TaskApiFailure(TaskApiError.NOT_FOUND)

    suspend fun create(scope: String, draft: TaskDraft): TodoTask = storageOperation {
        require(draft.validationError() == null)
        var created: TodoTask? = null
        dataStore.edit { prefs ->
            val tasks = decode(prefs[tasksKey(scope)])
            val max = tasks.maxOfOrNull { it.id } ?: 0L
            require(max < Long.MAX_VALUE)
            val next = maxOf(prefs[nextKey(scope)] ?: 1L, max + 1L)
            require(next in 1 until Long.MAX_VALUE)
            val task = draft.created(next)
            prefs[tasksKey(scope)] = encode(tasks + task)
            prefs[nextKey(scope)] = next + 1L
            created = task
        }
        requireNotNull(created)
    }

    suspend fun update(scope: String, id: Long, transform: (TodoTask) -> TodoTask) = storageOperation {
        dataStore.edit { prefs ->
            val tasks = decode(prefs[tasksKey(scope)])
            val original = tasks.firstOrNull { it.id == id } ?: throw TaskApiFailure(TaskApiError.NOT_FOUND)
            val updated = transform(original)
            require(updated.id == id && TaskDraft.from(updated).validationError() == null)
            prefs[tasksKey(scope)] = encode(tasks.map { if (it.id == id) updated else it })
        }
        Unit
    }

    suspend fun delete(scope: String, id: Long) = storageOperation {
        dataStore.edit { prefs ->
            val tasks = decode(prefs[tasksKey(scope)])
            if (tasks.none { it.id == id }) throw TaskApiFailure(TaskApiError.NOT_FOUND)
            prefs[tasksKey(scope)] = encode(tasks.filterNot { it.id == id })
        }
        Unit
    }

    private fun decode(json: String?): List<TodoTask> = if (json == null) emptyList() else decoder.decodeList(json)
    private fun encode(tasks: List<TodoTask>): String = gson.toJson(tasks.map { task ->
        mapOf("id" to task.id, "title" to task.title, "category" to task.category.name.lowercase(),
            "date" to task.date, "time" to task.time, "done" to task.done, "note" to task.note,
            "priority" to task.priority.name.lowercase(), "remind" to task.remind, "subtasks" to task.subtasks)
    })

    private fun tasksKey(scope: String) = stringPreferencesKey("tasks_$scope")
    private fun modeKey(scope: String) = stringPreferencesKey("mode_$scope")
    private fun nextKey(scope: String) = longPreferencesKey("next_id_$scope")

    private suspend fun <T> storageOperation(block: suspend () -> T): T = try {
        access.withLock { block() }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: TaskApiFailure) {
        if (failure.reason == TaskApiError.NOT_FOUND) throw failure
        throw TaskApiFailure(TaskApiError.LOCAL_STORAGE, failure)
    } catch (failure: Exception) {
        throw TaskApiFailure(TaskApiError.LOCAL_STORAGE, failure)
    }
}
