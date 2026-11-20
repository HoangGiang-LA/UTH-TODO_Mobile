package com.example.uth_todo

import androidx.lifecycle.ViewModelStore
import com.example.uth_todo.data.tasks.TaskJsonDecoder
import com.example.uth_todo.data.tasks.TaskRepository
import com.example.uth_todo.data.tasks.summary
import com.example.uth_todo.model.*
import com.example.uth_todo.ui.home.HomeViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.*
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val models = ViewModelStore()
    private val repository = HomeFakeRepository()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { models.clear(); Dispatchers.resetMain() }
    private fun model() = HomeViewModel(repository) { "2026-10-20" }.also { models.put("home", it) }
    private fun signedIn(uid: String) = AuthSession.SignedIn(UserProfile(uid, null, null, false))

    @Test fun waitsForAnActiveSessionAndSupportsGuest() = runTest(dispatcher) {
        val model = model()
        model.refresh()
        runCurrent()
        assertEquals(0, repository.loadCount)
        model.bindSession(AuthSession.Guest)
        advanceUntilIdle()
        assertEquals(1, repository.loadCount)
        assertTrue(model.uiState.value.hasLoaded)
    }

    @Test fun repeatedRefreshesWaitForOneRequestAndDoNotCreateAnEmptySuccess() = runTest(dispatcher) {
        repository.loadGate = CompletableDeferred()
        val model = model()
        model.bindSession(signedIn("alice"))
        repeat(5) { model.refresh() }
        runCurrent()
        assertEquals(1, repository.loadCount)
        assertTrue(model.uiState.value.isLoading)
        assertFalse(model.uiState.value.hasLoaded)
        repository.loadGate!!.complete(Unit)
        runCurrent()
        assertTrue(model.uiState.value.hasLoaded)
        assertEquals(6, model.uiState.value.tasks.size)
    }

    @Test fun aRealEmptyResponseIsASuccessfulEmptyState() = runTest(dispatcher) {
        repository.tasks = emptyList()
        val model = model()
        model.bindSession(AuthSession.Guest)
        advanceUntilIdle()
        assertTrue(model.uiState.value.hasLoaded)
        assertTrue(model.uiState.value.tasks.isEmpty())
        assertNull(model.uiState.value.error)
    }

    @Test fun missingConfigDoesNotCallTheApiOrBlockTheSession() = runTest(dispatcher) {
        repository.isConfigured = false
        val model = model()
        model.bindSession(signedIn("alice"))
        advanceUntilIdle()
        assertEquals(TaskApiError.NOT_CONFIGURED, model.uiState.value.error)
        assertFalse(model.uiState.value.isLoading)
        assertEquals(0, repository.loadCount)
    }

    @Test fun initialFailureCanBeRetriedWithoutSigningInAgain() = runTest(dispatcher) {
        repository.loadFailure = TaskApiError.NETWORK
        val model = model()
        model.bindSession(AuthSession.Guest)
        advanceUntilIdle()
        assertFalse(model.uiState.value.hasLoaded)
        assertEquals(TaskApiError.NETWORK, model.uiState.value.error)
        repository.loadFailure = null
        model.refresh()
        advanceUntilIdle()
        assertTrue(model.uiState.value.hasLoaded)
        assertNull(model.uiState.value.error)
    }

    @Test fun refreshFailurePreservesThePreviouslyLoadedList() = runTest(dispatcher) {
        val model = model()
        model.bindSession(AuthSession.Guest)
        advanceUntilIdle()
        val tasks = model.uiState.value.tasks
        repository.loadFailure = TaskApiError.SERVER
        model.refresh()
        advanceUntilIdle()
        assertEquals(tasks, model.uiState.value.tasks)
        assertTrue(model.uiState.value.hasLoaded)
        assertEquals(TaskApiError.SERVER, model.uiState.value.error)
    }

    @Test fun completionAndProgressChangeOnlyAfterPatchAcknowledgement() = runTest(dispatcher) {
        repository.writeGate = CompletableDeferred()
        val model = model()
        model.bindSession(AuthSession.Guest)
        advanceUntilIdle()
        repeat(5) { model.toggleDone(2) }
        runCurrent()
        assertEquals(listOf(2L to true), repository.writes)
        assertFalse(model.uiState.value.tasks.single { it.id == 2L }.done)
        assertEquals(50, model.uiState.value.tasks.summary("2026-10-20").percent)
        assertEquals(2L, model.uiState.value.changingTaskId)
        repository.writeGate!!.complete(Unit)
        runCurrent()
        assertTrue(model.uiState.value.tasks.single { it.id == 2L }.done)
        assertEquals(75, model.uiState.value.tasks.summary("2026-10-20").percent)
        assertNull(model.uiState.value.changingTaskId)
    }

    @Test fun failedPatchLeavesDataIntactAndReleasesTheCheckbox() = runTest(dispatcher) {
        repository.writeFailure = TaskApiError.UNAUTHORIZED
        val model = model()
        model.bindSession(AuthSession.Guest)
        advanceUntilIdle()
        val original = model.uiState.value.tasks
        model.toggleDone(2)
        advanceUntilIdle()
        assertEquals(original, model.uiState.value.tasks)
        assertEquals(TaskApiError.UNAUTHORIZED, model.uiState.value.notice)
        assertNull(model.uiState.value.changingTaskId)
    }

    @Test fun hangingLoadsAndWritesHaveTimeoutsAndAllowRetry() = runTest(dispatcher) {
        repository.loadGate = CompletableDeferred()
        val model = model()
        model.bindSession(AuthSession.Guest)
        advanceUntilIdle()
        assertEquals(TaskApiError.TIMEOUT, model.uiState.value.error)
        assertFalse(model.uiState.value.isLoading)
        repository.loadGate = null
        model.refresh()
        advanceUntilIdle()
        repository.writeGate = CompletableDeferred()
        model.toggleDone(2)
        advanceUntilIdle()
        assertFalse(model.uiState.value.tasks.single { it.id == 2L }.done)
        assertEquals(TaskApiError.TIMEOUT, model.uiState.value.notice)
        assertNull(model.uiState.value.changingTaskId)
    }

    @Test fun aLateLoadFromThePreviousAccountCannotReplaceTheNewAccountsData() = runTest(dispatcher) {
        val oldGate = CompletableDeferred<Unit>()
        repository.loadGate = oldGate
        repository.ignoreLoadCancellation = true
        val model = model()
        model.bindSession(signedIn("alice"))
        runCurrent()
        repository.loadGate = null
        repository.tasks = listOf(repository.tasks.first().copy(title = "New session data"))
        model.bindSession(signedIn("bob"))
        runCurrent()
        oldGate.complete(Unit)
        advanceUntilIdle()
        assertEquals("New session data", model.uiState.value.tasks.single().title)
    }

    @Test fun signingOutCancelsPendingCompletionAndClearsTheList() = runTest(dispatcher) {
        repository.writeGate = CompletableDeferred()
        val model = model()
        model.bindSession(signedIn("alice"))
        advanceUntilIdle()
        model.toggleDone(2)
        runCurrent()
        model.bindSession(AuthSession.SignedOut)
        repository.writeGate!!.complete(Unit)
        advanceUntilIdle()
        assertTrue(model.uiState.value.tasks.isEmpty())
        assertFalse(model.uiState.value.hasLoaded)
        assertNull(model.uiState.value.changingTaskId)
    }

    @Test fun choosingADayDoesNotAlterDataOrRefetchAndTheSameSessionKeepsIt() = runTest(dispatcher) {
        val model = model()
        model.bindSession(signedIn("alice"))
        advanceUntilIdle()
        model.selectDate("2026-10-21")
        model.bindSession(signedIn("alice"))
        model.selectDate("not-a-date")
        assertEquals("2026-10-21", model.uiState.value.selectedDate)
        assertEquals(1, repository.loadCount)
        assertEquals(6, model.uiState.value.tasks.size)
    }
}

private class HomeFakeRepository : TaskRepository {
    override suspend fun loadTask(id: Long): TodoTask = tasks.single { it.id == id }
    override suspend fun updateTask(task: TodoTask) { tasks = tasks.map { if (it.id == task.id) task else it } }
    override suspend fun setSubtasks(id: Long, subtasks: List<TodoSubtask>) {
        tasks = tasks.map { if (it.id == id) it.copy(subtasks = subtasks) else it }
    }
    override suspend fun deleteTask(id: Long) { tasks = tasks.filterNot { it.id == id } }
    override var isConfigured = true
    var tasks = TaskJsonDecoder().decodeList(taskFixture())
    var loadCount = 0
    val writes = mutableListOf<Pair<Long, Boolean>>()
    var loadGate: CompletableDeferred<Unit>? = null
    var writeGate: CompletableDeferred<Unit>? = null
    var ignoreLoadCancellation = false
    var loadFailure: TaskApiError? = null
    var writeFailure: TaskApiError? = null
    override suspend fun loadTasks(): List<TodoTask> {
        loadCount++
        val snapshot = tasks
        val gate = loadGate
        if (ignoreLoadCancellation) withContext(NonCancellable) { gate?.await() } else gate?.await()
        loadFailure?.let { throw TaskApiFailure(it) }
        return snapshot
    }
    override suspend fun setDone(id: Long, done: Boolean) {
        writes += id to done
        writeGate?.await()
        writeFailure?.let { throw TaskApiFailure(it) }
    }
}
