package com.example.uth_todo.data.tasks

import com.example.uth_todo.model.TodoTask
import com.example.uth_todo.model.TodoSubtask

interface TaskRepository {
    val isConfigured: Boolean
    suspend fun loadTasks(): List<TodoTask>
    suspend fun loadTask(id: Long): TodoTask
    suspend fun setDone(id: Long, done: Boolean)
    suspend fun updateTask(task: TodoTask)
    suspend fun setSubtasks(id: Long, subtasks: List<TodoSubtask>)
    suspend fun deleteTask(id: Long)
}
