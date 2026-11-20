package com.example.uth_todo.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uth_todo.data.tasks.TaskDates
import com.example.uth_todo.data.tasks.TaskRepository
import com.example.uth_todo.data.tasks.TaskDataController
import com.example.uth_todo.model.AuthSession
import com.example.uth_todo.model.TaskApiError
import com.example.uth_todo.model.TaskApiFailure
import com.example.uth_todo.model.TodoTask
import com.example.uth_todo.ui.taskdetail.TaskDetailUiState
import com.example.uth_todo.model.TaskDraft
import com.example.uth_todo.model.TaskCategory
import com.example.uth_todo.model.TaskFilter
import com.example.uth_todo.model.TaskCreateFailure
import com.example.uth_todo.model.TaskDataMode
import com.example.uth_todo.ui.taskcreate.TaskCreateUiState
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
    val detail: TaskDetailUiState = TaskDetailUiState(),
    val creation: TaskCreateUiState = TaskCreateUiState(),
    val createdNotice: TodoTask? = null,
    val dataMode: TaskDataMode? = null,
    val isChangingDataMode: Boolean = false,
    val dataSourceRevision: Int = 0,
    val pendingDataModeNavigation: Boolean = false,
)

class HomeViewModel(private val repository: TaskRepository, private val today: () -> String = TaskDates::today) : ViewModel() {
    val hasDataModeSettings: Boolean get() = repository is TaskDataController
    private val mutableState = MutableStateFlow(HomeUiState(selectedDate = today()))
    val uiState = mutableState.asStateFlow()
    private var sessionKey: String? = null
    private var generation = 0
    private var listGeneration = 0
    private var loading: Job? = null
    private var changing: Job? = null
    private var creating: Job? = null
    private var changingDataMode: Job? = null
    private var detailLoading: Job? = null
    private var detailGeneration = 0
    private var skipNextScreenRefresh = false

    fun bindSession(session: AuthSession) {
        val key = when (session) {
            is AuthSession.SignedIn -> "user:${session.user.uid}"
            AuthSession.Guest -> "guest"
            else -> null
        }
        if (key == sessionKey) return
        generation++
        listGeneration++
        loading?.cancel()
        changing?.cancel()
        creating?.cancel()
        changingDataMode?.cancel()
        detailLoading?.cancel()
        detailGeneration++
        skipNextScreenRefresh = false
        sessionKey = key
        (repository as? TaskDataController)?.bindSession(session)
        mutableState.value = HomeUiState(selectedDate = today())
        if (key != null) refresh()
    }

    fun onTaskScreenOpened() {
        if (mutableState.value.detail.isLoading) {
            detailGeneration++
            detailLoading?.cancel()
            mutableState.update { it.copy(detail = it.detail.copy(isLoading = false)) }
        }
        if (skipNextScreenRefresh) {
            skipNextScreenRefresh = false
            if (mutableState.value.hasLoaded) return
        }
        if (sessionKey != null) refresh()
    }

    fun refresh() {
        val state = mutableState.value
        if (sessionKey == null || state.isLoading || state.changingTaskId != null || state.detail.isLoading || state.creation.isSaving || state.isChangingDataMode) return
        if (!repository.isConfigured) {
            mutableState.update { it.copy(error = TaskApiError.NOT_CONFIGURED) }
            return
        }
        val current = generation
        val requestGeneration = ++listGeneration
        mutableState.update { it.copy(isLoading = true, error = null, notice = null) }
        loading = viewModelScope.launch {
            try {
                val mode = (repository as? TaskDataController)?.currentMode()
                if (current == generation && requestGeneration == listGeneration)
                    mutableState.update { it.copy(dataMode = mode) }
                val tasks = withTimeout(25_000) { repository.loadTasks() }
                if (current == generation && requestGeneration == listGeneration)
                    mutableState.update { it.copy(tasks = tasks, hasLoaded = true) }
            } catch (_: TimeoutCancellationException) {
                if (current == generation && requestGeneration == listGeneration)
                    mutableState.update { it.copy(error = TaskApiError.TIMEOUT) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                if (current == generation && requestGeneration == listGeneration)
                    mutableState.update { it.copy(error = failure.reason()) }
            } finally {
                if (current == generation && requestGeneration == listGeneration)
                    mutableState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun toggleDone(id: Long) {
        val state = mutableState.value
        if (sessionKey == null || state.isLoading || state.changingTaskId != null || state.creation.isSaving || state.isChangingDataMode) return
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

    fun openTask(id: Long, entryKey: String) {
        if (sessionKey == null || mutableState.value.changingTaskId != null || mutableState.value.creation.isSaving || mutableState.value.isChangingDataMode) return
        val detail = mutableState.value.detail
        if (detail.entryKey == entryKey && detail.id == id) return
        // A restored detail route may arrive while the initial list GET is pending.
        // Its older snapshot must not overwrite the freshly fetched detail.
        listGeneration++
        loading?.cancel()
        detailLoading?.cancel()
        detailGeneration++
        mutableState.update { it.copy(isLoading = false, detail = TaskDetailUiState(entryKey = entryKey, id = id)) }
        retryTaskDetail()
    }

    fun retryTaskDetail() {
        val detail = mutableState.value.detail
        val id = detail.id ?: return
        if (sessionKey == null || detail.isLoading || detail.isSaving || detail.isEditing || mutableState.value.creation.isSaving || mutableState.value.isChangingDataMode) return
        if (id <= 0 || !repository.isConfigured) {
            mutableState.update { it.copy(detail = it.detail.copy(error =
                if (id <= 0) TaskApiError.NOT_FOUND else TaskApiError.NOT_CONFIGURED)) }
            return
        }
        val current = generation
        val requestGeneration = ++detailGeneration
        mutableState.update { it.copy(detail = it.detail.copy(isLoading = true, error = null, mutationError = null, saved = false)) }
        detailLoading = viewModelScope.launch {
            try {
                val task = withTimeout(25_000) { repository.loadTask(id) }
                if (task.id != id) throw TaskApiFailure(TaskApiError.INVALID_RESPONSE)
                if (current == generation && requestGeneration == detailGeneration) {
                    skipNextScreenRefresh = true
                    mutableState.update { it.copy(tasks = it.tasks.upsert(task),
                        detail = it.detail.copy(task = task, isLoading = false)) }
                }
            } catch (_: TimeoutCancellationException) {
                if (current == generation && requestGeneration == detailGeneration)
                    mutableState.update { it.copy(detail = it.detail.copy(error = TaskApiError.TIMEOUT)) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                if (current == generation && requestGeneration == detailGeneration)
                    mutableState.update { it.copy(detail = it.detail.copy(error = failure.reason())) }
            } finally {
                if (current == generation && requestGeneration == detailGeneration)
                    mutableState.update { it.copy(detail = it.detail.copy(isLoading = false)) }
            }
        }
    }

    fun beginTaskEdit() {
        val detail = mutableState.value.detail
        val task = detail.task ?: return
        if (detail.isLoading || detail.isSaving || detail.error != null) return
        mutableState.update { it.copy(detail = detail.copy(draft = TaskDraft.from(task),
            validation = null, mutationError = null, saved = false)) }
    }

    fun changeTaskDraft(draft: TaskDraft) {
        if (mutableState.value.detail.isEditing && !mutableState.value.detail.isSaving)
            mutableState.update { it.copy(detail = it.detail.copy(draft = draft, validation = null, mutationError = null)) }
    }

    fun cancelTaskEdit() {
        if (!mutableState.value.detail.isSaving)
            mutableState.update { it.copy(detail = it.detail.copy(draft = null, validation = null, mutationError = null)) }
    }

    fun saveTaskEdit() {
        val detail = mutableState.value.detail
        val draft = detail.draft ?: return
        val task = detail.task ?: return
        val error = draft.validationError()
        if (error != null) {
            mutableState.update { it.copy(detail = it.detail.copy(validation = error)) }
            return
        }
        val updated = draft.applyTo(task)
        mutateDetail(updated, finishEdit = true) { repository.updateTask(updated) }
    }

    fun toggleDetailDone() {
        val task = mutableState.value.detail.task ?: return
        val updated = task.copy(done = !task.done)
        mutateDetail(updated) { repository.setDone(task.id, updated.done) }
    }

    fun toggleSubtask(index: Int) {
        val task = mutableState.value.detail.task ?: return
        val subtask = task.subtasks.getOrNull(index) ?: return
        val subtasks = task.subtasks.mapIndexed { i, value -> if (i == index) subtask.copy(done = !subtask.done) else value }
        mutateDetail(task.copy(subtasks = subtasks)) { repository.setSubtasks(task.id, subtasks) }
    }

    fun deleteTask() {
        val task = mutableState.value.detail.task ?: return
        if (mutableState.value.detail.isEditing) return
        mutateDetail(task, deleted = true) { repository.deleteTask(task.id) }
    }

    private fun mutateDetail(updated: TodoTask, finishEdit: Boolean = false, deleted: Boolean = false,
        command: suspend () -> Unit) {
        val state = mutableState.value
        val detail = state.detail
        if (sessionKey == null || state.isLoading || state.changingTaskId != null || detail.isLoading || state.creation.isSaving || state.isChangingDataMode ||
            detail.deleted || detail.error != null || (detail.isEditing && !finishEdit)) return
        val current = generation
        val entryKey = detail.entryKey
        mutableState.update { it.copy(changingTaskId = updated.id,
            detail = it.detail.copy(isSaving = true, mutationError = null, saved = false)) }
        changing = viewModelScope.launch {
            try {
                withTimeout(25_000) { command() }
                if (current == generation) {
                    skipNextScreenRefresh = true
                    mutableState.update { value ->
                        value.copy(tasks = if (deleted) value.tasks.filterNot { it.id == updated.id } else value.tasks.upsert(updated),
                            detail = if (value.detail.entryKey == entryKey) value.detail.copy(task = updated,
                                draft = if (finishEdit) null else value.detail.draft, validation = null,
                                deleted = deleted, saved = finishEdit) else value.detail)
                    }
                }
            } catch (_: TimeoutCancellationException) {
                if (current == generation && mutableState.value.detail.entryKey == entryKey)
                    mutableState.update { it.copy(detail = it.detail.copy(mutationError = TaskApiError.TIMEOUT)) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                if (current == generation && mutableState.value.detail.entryKey == entryKey)
                    mutableState.update { it.copy(detail = it.detail.copy(mutationError = failure.reason())) }
            } finally {
                if (current == generation) mutableState.update { it.copy(changingTaskId = null,
                    detail = if (it.detail.entryKey == entryKey) it.detail.copy(isSaving = false) else it.detail) }
            }
        }
    }

    fun beginTaskCreation(entryKey: String, filter: TaskFilter = TaskFilter.ALL, date: String = today()) {
        val state = mutableState.value
        if (sessionKey == null || state.changingTaskId != null || state.creation.isSaving || state.creation.entryKey == entryKey || state.isChangingDataMode) return
        cancelReadsForCreation()
        val draft = TaskDraft.blank(date.takeIf(TaskDates::isValidDate) ?: today(),
            if (filter == TaskFilter.LIFE) TaskCategory.LIFE else TaskCategory.STUDY)
        mutableState.update { it.copy(creation = TaskCreateUiState(entryKey = entryKey, initialDraft = draft)) }
    }

    fun changeCreationDraft(draft: TaskDraft) {
        val creation = mutableState.value.creation
        if (creation.entryKey != null && !creation.isSaving && creation.created == null)
            mutableState.update { it.copy(creation = it.creation.copy(draft = draft, validation = null,
                error = if (it.creation.requiresReview) it.creation.error else null)) }
    }

    fun allowCreationRetry() {
        if (!mutableState.value.creation.isSaving)
            mutableState.update { it.copy(creation = it.creation.copy(requiresReview = false, error = null)) }
    }

    fun closeTaskCreation(entryKey: String) {
        val creation = mutableState.value.creation
        if (creation.entryKey == entryKey && !creation.isSaving)
            mutableState.update { it.copy(creation = TaskCreateUiState()) }
    }

    fun createTask() {
        val state = mutableState.value
        val creation = state.creation
        if (sessionKey == null || creation.entryKey == null || creation.isSaving || creation.created != null ||
            creation.requiresReview || state.changingTaskId != null || state.isChangingDataMode) return
        val validation = creation.draft.validationError()
        if (validation != null) {
            mutableState.update { it.copy(creation = it.creation.copy(validation = validation)) }
            return
        }
        if (!repository.isConfigured) {
            mutableState.update { it.copy(creation = it.creation.copy(error = TaskApiError.NOT_CONFIGURED)) }
            return
        }
        cancelReadsForCreation()
        val current = generation
        val entryKey = creation.entryKey
        mutableState.update { it.copy(creation = it.creation.copy(isSaving = true, error = null, validation = null)) }
        creating = viewModelScope.launch {
            try {
                val task = withTimeout(25_000) { repository.createTask(creation.draft) }
                if (current == generation && mutableState.value.creation.entryKey == entryKey) {
                    // A mock response reusing an existing ID must never overwrite that task.
                    if (task.id <= 0 || mutableState.value.tasks.any { it.id == task.id })
                        throw TaskCreateFailure(TaskApiError.INVALID_RESPONSE, mayHaveCreated = true)
                    if (!creation.draft.matchesCreatedTask(task))
                        throw TaskCreateFailure(TaskApiError.CREATE_MISMATCH, mayHaveCreated = true)
                    skipNextScreenRefresh = true
                    mutableState.update { it.copy(tasks = it.tasks + task, createdNotice = task,
                        creation = it.creation.copy(created = task)) }
                }
            } catch (_: TimeoutCancellationException) {
                if (current == generation) mutableState.update { it.copy(creation = it.creation.copy(
                    error = TaskApiError.TIMEOUT, requiresReview = true)) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                if (current == generation) {
                    val error = (failure as? TaskCreateFailure)?.reason ?: failure.reason()
                    val uncertain = (failure as? TaskCreateFailure)?.mayHaveCreated ?: (error in setOf(
                        TaskApiError.NETWORK, TaskApiError.TIMEOUT, TaskApiError.SERVER, TaskApiError.INVALID_RESPONSE,
                        TaskApiError.CREATE_MISMATCH, TaskApiError.UNKNOWN))
                    mutableState.update { it.copy(creation = it.creation.copy(error = error, requiresReview = uncertain)) }
                }
            } finally {
                if (current == generation) mutableState.update { it.copy(creation = it.creation.copy(isSaving = false)) }
            }
        }
    }

    fun selectTaskDataMode(mode: TaskDataMode) {
        val controller = repository as? TaskDataController ?: return
        val original = mutableState.value
        if (sessionKey == null || original.dataMode == mode || original.isChangingDataMode ||
            original.changingTaskId != null || original.creation.isSaving) return
        val current = ++generation
        listGeneration++
        detailGeneration++
        loading?.cancel()
        detailLoading?.cancel()
        skipNextScreenRefresh = false
        mutableState.update { it.copy(isLoading = false, isChangingDataMode = true) }
        changingDataMode = viewModelScope.launch {
            try {
                controller.selectMode(mode)
                if (current == generation) {
                    mutableState.value = HomeUiState(selectedDate = original.selectedDate, dataMode = mode,
                        dataSourceRevision = original.dataSourceRevision + 1, pendingDataModeNavigation = true)
                    refresh()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                if (current == generation) mutableState.value = original.copy(isLoading = false,
                    isChangingDataMode = false, notice = failure.reason())
            }
        }
    }

    fun dataModeNavigationHandled() {
        mutableState.update { it.copy(pendingDataModeNavigation = false) }
    }

    private fun cancelReadsForCreation() {
        listGeneration++
        detailGeneration++
        loading?.cancel()
        detailLoading?.cancel()
        mutableState.update { it.copy(isLoading = false, detail = it.detail.copy(isLoading = false)) }
    }

    fun dismissCreatedNotice(id: Long) {
        mutableState.update { if (it.createdNotice?.id == id) it.copy(createdNotice = null) else it }
    }

    private fun List<TodoTask>.upsert(task: TodoTask): List<TodoTask> =
        if (any { it.id == task.id }) map { if (it.id == task.id) task else it } else this + task

    fun dismissNotice(expected: TaskApiError) {
        mutableState.update { if (it.notice == expected) it.copy(notice = null) else it }
    }

    private fun Exception.reason() = (this as? TaskApiFailure)?.reason ?: TaskApiError.UNKNOWN
}
