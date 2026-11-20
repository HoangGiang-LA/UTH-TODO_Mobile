package com.example.uth_todo.ui.tasklist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.uth_todo.data.tasks.matching
import com.example.uth_todo.model.TaskFilter
import com.example.uth_todo.model.TodoTask
import com.example.uth_todo.navigation.AppDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class TaskListUiState(val query: String = "", val filter: TaskFilter = TaskFilter.ALL) {
    fun content(tasks: List<TodoTask>): TaskListContent {
        val visible = tasks.matching(filter, query)
        return TaskListContent(visible.filter { !it.done }, visible.filter { it.done })
    }
}

data class TaskListContent(val pending: List<TodoTask>, val completed: List<TodoTask>) {
    val isEmpty: Boolean get() = pending.isEmpty() && completed.isEmpty()
}

// Only small UI inputs are saved. Task data stays in the shared HomeViewModel
// and is reloaded from the repository instead of being written to a Bundle.
class TaskListViewModel(private val savedState: SavedStateHandle) : ViewModel() {
    private val mutableState = MutableStateFlow(TaskListUiState(
        query = savedState[QUERY_KEY] ?: "",
        filter = TaskFilter.entries.firstOrNull { it.name == savedState.get<String>(AppDestination.TaskList.CATEGORY_ARGUMENT) }
            ?: TaskFilter.ALL,
    ))
    val uiState = mutableState.asStateFlow()

    fun setQuery(value: String) {
        savedState[QUERY_KEY] = value
        mutableState.update { it.copy(query = value) }
    }

    fun setFilter(value: TaskFilter) {
        savedState[AppDestination.TaskList.CATEGORY_ARGUMENT] = value.name
        mutableState.update { it.copy(filter = value) }
    }

    fun clearSearch() = setQuery("")

    companion object { const val QUERY_KEY = "task_list_search" }
}
