package com.example.uth_todo.notifications

import com.example.uth_todo.model.AuthSession
import kotlinx.coroutines.flow.StateFlow

interface TaskReminderController {
    val uiState: StateFlow<ReminderServiceState>
    fun bindSession(session: AuthSession)
    fun refresh()
    fun sendTest()
}
