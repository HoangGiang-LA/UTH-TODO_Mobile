package com.example.uth_todo.ui.taskcreate

import com.example.uth_todo.model.*

data class TaskCreateUiState(
    val entryKey: String? = null,
    val initialDraft: TaskDraft = TaskDraft.blank(),
    val draft: TaskDraft = initialDraft,
    val isSaving: Boolean = false,
    val error: TaskApiError? = null,
    val validation: TaskEditError? = null,
    val requiresReview: Boolean = false,
    val created: TodoTask? = null,
) {
    val isDirty: Boolean get() = draft != initialDraft
}
