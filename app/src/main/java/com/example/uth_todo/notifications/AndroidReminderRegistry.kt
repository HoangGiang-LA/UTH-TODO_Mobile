package com.example.uth_todo.notifications

import android.content.Context
import com.example.uth_todo.data.tasks.TaskSessionScope
import com.google.gson.Gson

class AndroidReminderRegistry(context: Context) : ReminderRegistry {
    private val prefs = context.applicationContext.getSharedPreferences("task_reminders", Context.MODE_PRIVATE)
    private val gson = Gson()
    fun initialScope(): String? = prefs.getString("active_scope", null)?.takeIf(TaskSessionScope::isValid)
    override suspend fun read(): ReminderRegistryState {
        val json = prefs.getString("registry", null) ?: return ReminderRegistryState(initialScope())
        val state = requireNotNull(gson.fromJson(json, ReminderRegistryState::class.java))
        require(state.activeScope == null || TaskSessionScope.isValid(state.activeScope))
        require(state.scheduled.all { TaskSessionScope.isValid(it.key.scope) && it.key.taskId > 0 && it.fireAt > 0 && it.dueAt >= it.fireAt })
        require(state.delivered.keys.all { ReminderKey.parse(it) != null })
        return state
    }
    override suspend fun write(state: ReminderRegistryState) {
        check(prefs.edit().putString("registry", gson.toJson(state)).putString("active_scope", state.activeScope).commit())
    }
}
