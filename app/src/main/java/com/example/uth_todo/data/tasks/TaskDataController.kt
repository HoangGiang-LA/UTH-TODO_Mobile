package com.example.uth_todo.data.tasks

import com.example.uth_todo.model.AuthSession
import com.example.uth_todo.model.TaskDataMode

interface TaskDataController {
    fun bindSession(session: AuthSession)
    suspend fun currentMode(): TaskDataMode
    suspend fun selectMode(mode: TaskDataMode)
}
