package com.example.uth_todo.ui.taskdetail

import com.example.uth_todo.model.*

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
