package com.example.uth_todo.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uth_todo.data.tasks.TaskDates
import com.example.uth_todo.data.tasks.TaskRepository
import com.example.uth_todo.model.AuthSession
import com.example.uth_todo.model.TaskApiError
import com.example.uth_todo.model.TaskApiFailure
import com.example.uth_todo.model.TodoTask
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

data class HomeUiState(
    val tasks: List<TodoTask> = emptyList(),
    val hasLoaded: Boolean = false,
    val isLoading: Boolean = false,
    val error: TaskApiError? = null,
    val changingTaskId: Long? = null,
    val notice: TaskApiError? = null,
    val selectedDate: String = TaskDates.today(),
)

class HomeViewModel(private val repository: TaskRepository, private val today: () -> String = TaskDates::today) : ViewModel() {
    private val mutableState = MutableStateFlow(HomeUiState(selectedDate = today()))
    val uiState = mutableState.asStateFlow()
    private var sessionKey: String? = null
    private var generation = 0
    private var loading: Job? = null
    private var changing: Job? = null

    fun bindSession(session: AuthSession) {
        val key = when (session) {
            is AuthSession.SignedIn -> "user:${session.user.uid}"
            AuthSession.Guest -> "guest"
            else -> null
        }
        if (key == sessionKey) return
        generation++
        loading?.cancel()
        changing?.cancel()
        sessionKey = key
        mutableState.value = HomeUiState(selectedDate = today())
        if (key != null) refresh()
    }

    fun onHomeOpened() { if (sessionKey != null) refresh() }

    fun refresh() {
        val state = mutableState.value
        if (sessionKey == null || state.isLoading || state.changingTaskId != null) return
        if (!repository.isConfigured) {
            mutableState.update { it.copy(error = TaskApiError.NOT_CONFIGURED) }
            return
        }
        val current = generation
        mutableState.update { it.copy(isLoading = true, error = null, notice = null) }
        loading = viewModelScope.launch {
            try {
                val tasks = withTimeout(25_000) { repository.loadTasks() }
                if (current == generation) mutableState.update { it.copy(tasks = tasks, hasLoaded = true) }
            } catch (_: TimeoutCancellationException) {
                if (current == generation) mutableState.update { it.copy(error = TaskApiError.TIMEOUT) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                if (current == generation) mutableState.update { it.copy(error = failure.reason()) }
            } finally {
                if (current == generation) mutableState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun toggleDone(id: Long) {
        val state = mutableState.value
        if (sessionKey == null || state.isLoading || state.changingTaskId != null) return
        val task = state.tasks.firstOrNull { it.id == id } ?: return
        val current = generation
        mutableState.update { it.copy(changingTaskId = id, notice = null) }
        changing = viewModelScope.launch {
            try {
                withTimeout(25_000) { repository.setDone(id, !task.done) }
                // Apply the command only after the server acknowledges PATCH.
                if (current == generation) mutableState.update { value ->
                    value.copy(tasks = value.tasks.map { if (it.id == id) it.copy(done = !task.done) else it })
                }
            } catch (_: TimeoutCancellationException) {
                if (current == generation) mutableState.update { it.copy(notice = TaskApiError.TIMEOUT) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                if (current == generation) mutableState.update { it.copy(notice = failure.reason()) }
            } finally {
                if (current == generation) mutableState.update { it.copy(changingTaskId = null) }
            }
        }
    }

    fun selectDate(value: String) {
        if (TaskDates.isValidDate(value)) mutableState.update { it.copy(selectedDate = value) }
    }

    fun dismissNotice(expected: TaskApiError) {
        mutableState.update { if (it.notice == expected) it.copy(notice = null) else it }
    }

    private fun Exception.reason() = (this as? TaskApiFailure)?.reason ?: TaskApiError.UNKNOWN
}
