package com.example.uth_todo.model

enum class TaskCategory { STUDY, LIFE, OTHER }
enum class TaskPriority { LOW, MEDIUM, HIGH, OTHER }
enum class TaskFilter { ALL, STUDY, LIFE }

data class TodoSubtask(val title: String, val done: Boolean)

data class TodoTask(
    val id: Long,
    val title: String,
    val category: TaskCategory,
    val date: String?,
    val time: String?,
    val done: Boolean,
    val note: String = "",
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val remind: Boolean = false,
    val subtasks: List<TodoSubtask> = emptyList(),
)

enum class TaskApiError { NOT_CONFIGURED, UNAUTHORIZED, NOT_FOUND, NETWORK, TIMEOUT, SERVER, RATE_LIMITED, INVALID_RESPONSE, CREATE_MISMATCH, LOCAL_STORAGE, UNKNOWN }
class TaskApiFailure(val reason: TaskApiError, cause: Throwable? = null) : Exception(reason.name, cause)
class TaskCreateFailure(val reason: TaskApiError, val mayHaveCreated: Boolean) : Exception(reason.name)

data class TaskDaySummary(val total: Int, val completed: Int, val studyRemaining: Int, val lifeRemaining: Int) {
    val percent: Int get() = if (total == 0) 0 else (completed.toLong() * 100 / total).toInt()
    val fraction: Float get() = if (total == 0) 0f else completed.toFloat() / total
}
