package com.example.uth_todo.ui.taskdetail

import com.example.uth_todo.data.tasks.TaskDates
import com.example.uth_todo.model.*

enum class TaskEditError { TITLE, CATEGORY, DATE, TIME, PRIORITY, SUBTASK }

data class TaskDraft(
    val title: String,
    val category: TaskCategory,
    val date: String,
    val time: String,
    val note: String,
    val priority: TaskPriority,
    val remind: Boolean,
    val subtasks: List<TodoSubtask>,
) {
    fun validationError(): TaskEditError? = when {
        title.isBlank() -> TaskEditError.TITLE
        category == TaskCategory.OTHER -> TaskEditError.CATEGORY
        date.isNotBlank() && !TaskDates.isValidDate(date.trim()) -> TaskEditError.DATE
        time.isNotBlank() && !TaskDates.isValidTime(time.trim()) -> TaskEditError.TIME
        priority == TaskPriority.OTHER -> TaskEditError.PRIORITY
        subtasks.any { it.title.isBlank() } -> TaskEditError.SUBTASK
        else -> null
    }

    fun applyTo(task: TodoTask): TodoTask {
        require(validationError() == null)
        return task.copy(title = title.trim(), category = category, date = date.trim().ifBlank { null },
            time = time.trim().ifBlank { null }, note = note.trim(), priority = priority, remind = remind,
            subtasks = subtasks.map { it.copy(title = it.title.trim()) })
    }

    companion object {
        fun from(task: TodoTask) = TaskDraft(task.title, task.category, task.date.orEmpty(), task.time.orEmpty(),
            task.note, task.priority, task.remind, task.subtasks)
    }
}

data class TaskDetailUiState(
    val entryKey: String? = null,
    val id: Long? = null,
    val task: TodoTask? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: TaskApiError? = null,
    val mutationError: TaskApiError? = null,
    val draft: TaskDraft? = null,
    val validation: TaskEditError? = null,
    val deleted: Boolean = false,
    val saved: Boolean = false,
) {
    val isEditing: Boolean get() = draft != null
    val isDirty: Boolean get() = task?.let { draft != null && draft != TaskDraft.from(it) } ?: false
}
