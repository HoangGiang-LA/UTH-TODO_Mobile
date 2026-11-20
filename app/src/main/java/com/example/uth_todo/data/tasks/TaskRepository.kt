package com.example.uth_todo.data.tasks

import com.example.uth_todo.model.TodoTask

interface TaskRepository {
    val isConfigured: Boolean
    suspend fun loadTasks(): List<TodoTask>
    suspend fun setDone(id: Long, done: Boolean)
}
