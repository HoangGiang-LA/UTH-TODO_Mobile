package com.example.uth_todo.model

import com.example.uth_todo.data.tasks.TaskDates

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

    fun created(id: Long): TodoTask {
        require(id > 0 && validationError() == null)
        return TodoTask(id, title.trim(), category, date.trim().ifBlank { null }, time.trim().ifBlank { null },
            done = false, note = note.trim(), priority = priority, remind = remind,
            subtasks = subtasks.map { it.copy(title = it.title.trim()) })
    }

    fun matchesCreatedTask(task: TodoTask): Boolean = task.id > 0 && validationError() == null &&
        created(task.id) == task.copy(title = task.title.trim(), note = task.note.trim(),
            subtasks = task.subtasks.map { it.copy(title = it.title.trim()) })

    companion object {
        fun blank(date: String = TaskDates.today(), category: TaskCategory = TaskCategory.STUDY) =
            TaskDraft("", category, date, "", "", TaskPriority.MEDIUM, false, emptyList())
        fun from(task: TodoTask) = TaskDraft(task.title, task.category, task.date.orEmpty(), task.time.orEmpty(),
            task.note, task.priority, task.remind, task.subtasks)
    }
}
