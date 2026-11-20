package com.example.uth_todo

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.lifecycle.ViewModelStore
import com.example.uth_todo.data.tasks.*
import com.example.uth_todo.model.*
import com.example.uth_todo.ui.home.HomeViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class TaskPersistenceTest {
    @get:Rule val folder = TemporaryFolder()
    private val dispatcher = StandardTestDispatcher()
    private val models = ViewModelStore()
    private val scopes = mutableListOf<CoroutineScope>()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() {
        models.clear()
        runBlocking { scopes.forEach { it.coroutineContext[Job]!!.cancelAndJoin() } }
        Dispatchers.resetMain()
    }
    private fun draft(title: String = "Lau nha") = TaskDraft.blank("2026-11-20", TaskCategory.LIFE).copy(
        title = title, note = "Tầng 1", subtasks = listOf(TodoSubtask("Quét trước", false)))
    private fun disk(file: File): Pair<DeviceTaskStorage, CoroutineScope> {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scopes += scope
        return DeviceTaskStorage(createTaskPreferencesStore(file, scope)) to scope
    }
    private fun repository(prefs: TaskMemoryPreferences, api: CourseSpy = CourseSpy()) =
        SessionTaskRepository(DeviceTaskStorage(prefs), api)
    private fun user(uid: String) = AuthSession.SignedIn(UserProfile(uid, uid, null, false))

    @Test fun lauNhaSurvivesClosingAndReopeningARealDataStoreFile() = runBlocking {
        val file = File(folder.root, "tasks.preferences_pb")
        val (storage, scope) = disk(file)
        val repo = SessionTaskRepository(storage, CourseSpy())
        repo.bindSession(AuthSession.Guest)
        val task = repo.createTask(draft())
        assertTrue(file.isFile)
        scope.coroutineContext[Job]!!.cancelAndJoin()
        val (reopened, _) = disk(file)
        val fresh = SessionTaskRepository(reopened, CourseSpy())
        fresh.bindSession(AuthSession.Guest)
        assertEquals(listOf(task), fresh.loadTasks())
        assertEquals("Lau nha", fresh.loadTask(task.id).title)
        assertEquals("Tầng 1", fresh.loadTask(task.id).note)
        assertEquals(listOf(TodoSubtask("Quét trước", false)), fresh.loadTask(task.id).subtasks)
    }

    @Test fun editCompletionSubtasksAndDeletionPersistWithoutReusingIds() = runBlocking {
        val file = File(folder.root, "crud.preferences_pb")
        val (storage, scope) = disk(file)
        val repo = SessionTaskRepository(storage, CourseSpy())
        repo.bindSession(AuthSession.Guest)
        val first = repo.createTask(draft())
        repo.setDone(first.id, true)
        repo.setSubtasks(first.id, listOf(TodoSubtask("Đã lau", true)))
        repo.updateTask(repo.loadTask(first.id).copy(note = "Đã sửa", time = "18:30"))
        scope.coroutineContext[Job]!!.cancelAndJoin()
        val (freshStorage, _) = disk(file)
        val fresh = SessionTaskRepository(freshStorage, CourseSpy())
        fresh.bindSession(AuthSession.Guest)
        val task = fresh.loadTask(first.id)
        assertTrue(task.done)
        assertEquals("18:30", task.time)
        assertEquals("Đã sửa", task.note)
        assertTrue(task.subtasks.single().done)
        fresh.deleteTask(task.id)
        assertTrue(fresh.loadTasks().isEmpty())
        val next = fresh.createTask(draft("Việc tiếp theo"))
        assertTrue(next.id > task.id)
        try { fresh.loadTask(task.id); fail() } catch (error: TaskApiFailure) { assertEquals(TaskApiError.NOT_FOUND, error.reason) }
    }

    @Test fun concurrentCreatesUseTransactionalIdsAndDoNotLoseTasks() = runBlocking {
        val (storage, _) = disk(File(folder.root, "concurrent.preferences_pb"))
        val repo = SessionTaskRepository(storage, CourseSpy())
        repo.bindSession(AuthSession.Guest)
        val tasks = (1..20).map { index -> async(Dispatchers.IO) { repo.createTask(draft("Việc $index")) } }.awaitAll()
        assertEquals(20, tasks.map { it.id }.toSet().size)
        assertEquals(tasks.toSet(), repo.loadTasks().toSet())
    }

    @Test fun guestAndEachAccountHaveSeparateDataAndModes() = runTest(dispatcher) {
        val repo = repository(TaskMemoryPreferences())
        repo.bindSession(AuthSession.Guest); repo.createTask(draft("Guest"))
        repo.bindSession(user("alice")); assertTrue(repo.loadTasks().isEmpty()); repo.createTask(draft("Alice"))
        repo.selectMode(TaskDataMode.COURSE_API)
        repo.bindSession(user("bob")); assertEquals(TaskDataMode.DEVICE, repo.currentMode())
        assertTrue(repo.loadTasks().isEmpty()); repo.createTask(draft("Bob"))
        repo.bindSession(user("alice")); assertEquals(TaskDataMode.COURSE_API, repo.currentMode())
        repo.selectMode(TaskDataMode.DEVICE); assertEquals("Alice", repo.loadTasks().single().title)
        repo.bindSession(AuthSession.Guest); assertEquals("Guest", repo.loadTasks().single().title)
        repo.bindSession(user("bob")); assertEquals("Bob", repo.loadTasks().single().title)
    }

    @Test fun courseSampleNeverReplacesDeviceTasksAndModeSurvivesRepositoryRecreation() = runTest(dispatcher) {
        val prefs = TaskMemoryPreferences()
        val api = CourseSpy()
        val repo = repository(prefs, api)
        repo.bindSession(AuthSession.Guest)
        val local = repo.createTask(draft())
        assertEquals(0, api.calls)
        repo.selectMode(TaskDataMode.COURSE_API)
        assertEquals("Ôn tập chương 4", repo.loadTasks().single().title)
        assertEquals(1, api.calls)
        val fresh = repository(prefs, api)
        fresh.bindSession(AuthSession.Guest)
        assertEquals(TaskDataMode.COURSE_API, fresh.currentMode())
        fresh.selectMode(TaskDataMode.DEVICE)
        assertEquals(listOf(local), fresh.loadTasks())
    }

    @Test fun apiFailureDoesNotFallBackToDeviceOrClaimThatApiSavedAnything() = runTest(dispatcher) {
        val api = CourseSpy()
        val repo = repository(TaskMemoryPreferences(), api)
        repo.bindSession(AuthSession.Guest)
        repo.createTask(draft())
        repo.selectMode(TaskDataMode.COURSE_API)
        try { repo.createTask(draft("Khác")); fail() }
        catch (error: TaskCreateFailure) { assertEquals(TaskApiError.CREATE_MISMATCH, error.reason) }
        repo.selectMode(TaskDataMode.DEVICE)
        assertEquals(listOf("Lau nha"), repo.loadTasks().map { it.title })
    }

    @Test fun corruptStoredDataIsAnErrorAndIsNeverReplacedByAnEmptyList() = runTest(dispatcher) {
        val prefs = TaskMemoryPreferences()
        val key = stringPreferencesKey("tasks_guest")
        prefs.updateData { it.toMutablePreferences().apply { this[key] = "invalid JSON" } }
        val repo = repository(prefs)
        repo.bindSession(AuthSession.Guest)
        try { repo.loadTasks(); fail() } catch (error: TaskApiFailure) { assertEquals(TaskApiError.LOCAL_STORAGE, error.reason) }
        try { repo.createTask(draft()); fail() } catch (error: TaskApiFailure) { assertEquals(TaskApiError.LOCAL_STORAGE, error.reason) }
        assertEquals("invalid JSON", prefs.data.value[key])
        repo.bindSession(user("alice"))
        assertTrue(repo.loadTasks().isEmpty())
    }

    @Test fun signedOutRepositoryCannotReadOrWriteAnotherSessionsTasks() = runTest(dispatcher) {
        val repo = repository(TaskMemoryPreferences())
        repo.bindSession(AuthSession.Guest); repo.createTask(draft())
        repo.bindSession(AuthSession.SignedOut)
        try { repo.loadTasks(); fail() } catch (error: TaskApiFailure) { assertEquals(TaskApiError.UNAUTHORIZED, error.reason) }
        try { repo.createTask(draft()); fail() } catch (error: TaskApiFailure) { assertEquals(TaskApiError.UNAUTHORIZED, error.reason) }
        repo.bindSession(AuthSession.Guest); assertEquals("Lau nha", repo.loadTasks().single().title)
    }

    @Test fun sameNumericIdInApiAndDeviceModesDoesNotCrossUpdate() = runTest(dispatcher) {
        val api = CourseSpy()
        val repo = repository(TaskMemoryPreferences(), api)
        repo.bindSession(AuthSession.Guest)
        val task = repo.createTask(draft())
        assertEquals(1L, task.id)
        repo.selectMode(TaskDataMode.COURSE_API); repo.setDone(1, true)
        repo.selectMode(TaskDataMode.DEVICE)
        assertFalse(repo.loadTask(1).done)
        assertEquals(1, api.calls)
    }

    @Test fun viewModelCreateRefreshDetailAndNewInstanceReadTheSameSavedTask() = runTest(dispatcher) {
        val prefs = TaskMemoryPreferences()
        val repo = repository(prefs)
        val model = HomeViewModel(repo)
        models.put("one", model)
        model.bindSession(AuthSession.Guest); advanceUntilIdle()
        assertEquals(TaskDataMode.DEVICE, model.uiState.value.dataMode)
        model.beginTaskCreation("create"); model.changeCreationDraft(draft()); model.createTask(); advanceUntilIdle()
        model.refresh(); advanceUntilIdle()
        assertEquals("Lau nha", model.uiState.value.tasks.single().title)
        val fresh = HomeViewModel(repository(prefs))
        models.put("fresh", fresh)
        fresh.bindSession(AuthSession.Guest); advanceUntilIdle()
        fresh.openTask(1, "detail"); advanceUntilIdle()
        assertEquals("Lau nha", fresh.uiState.value.detail.task!!.title)
        fresh.beginTaskEdit(); fresh.changeTaskDraft(draft().copy(note = "Mới")); fresh.saveTaskEdit(); advanceUntilIdle()
        fresh.refresh(); advanceUntilIdle()
        assertEquals("Mới", fresh.uiState.value.tasks.single().note)
    }

    @Test fun diskWriteFailureKeepsOldTaskAndDraftAndDoesNotClaimSuccess() = runTest(dispatcher) {
        val prefs = TaskMemoryPreferences()
        val repo = repository(prefs)
        repo.bindSession(AuthSession.Guest); repo.createTask(draft("Old"))
        val model = HomeViewModel(repo); models.put("model", model)
        model.bindSession(AuthSession.Guest); advanceUntilIdle()
        model.beginTaskCreation("new"); model.changeCreationDraft(draft())
        prefs.failWrites = true
        model.createTask(); advanceUntilIdle()
        assertEquals(TaskApiError.LOCAL_STORAGE, model.uiState.value.creation.error)
        assertEquals("Lau nha", model.uiState.value.creation.draft.title)
        assertNull(model.uiState.value.createdNotice)
        assertEquals(listOf("Old"), model.uiState.value.tasks.map { it.title })
        prefs.failWrites = false
        assertEquals(listOf("Old"), repo.loadTasks().map { it.title })
    }

    @Test fun sourceChangeClearsOldDetailAndDraftAndCanReturnToSavedDeviceTasks() = runTest(dispatcher) {
        val repo = repository(TaskMemoryPreferences())
        repo.bindSession(AuthSession.Guest); repo.createTask(draft())
        val model = HomeViewModel(repo); models.put("model", model)
        model.bindSession(AuthSession.Guest); advanceUntilIdle()
        model.openTask(1, "detail"); advanceUntilIdle(); model.beginTaskEdit()
        model.selectTaskDataMode(TaskDataMode.COURSE_API); advanceUntilIdle()
        assertNull(model.uiState.value.detail.task)
        assertNull(model.uiState.value.detail.draft)
        assertEquals(1, model.uiState.value.dataSourceRevision)
        assertTrue(model.uiState.value.pendingDataModeNavigation)
        model.dataModeNavigationHandled()
        assertFalse(model.uiState.value.pendingDataModeNavigation)
        assertEquals("Ôn tập chương 4", model.uiState.value.tasks.single().title)
        model.selectTaskDataMode(TaskDataMode.DEVICE); advanceUntilIdle()
        assertEquals("Lau nha", model.uiState.value.tasks.single().title)
        assertEquals(2, model.uiState.value.dataSourceRevision)
    }

    @Test fun lateApiLoadCannotOverwriteDeviceTasksAfterSourceChange() = runTest(dispatcher) {
        val api = CourseSpy()
        val repo = repository(TaskMemoryPreferences(), api)
        repo.bindSession(AuthSession.Guest); repo.createTask(draft())
        val model = HomeViewModel(repo); models.put("model", model)
        model.bindSession(AuthSession.Guest); advanceUntilIdle()
        val gate = CompletableDeferred<Unit>()
        api.gate = gate
        model.selectTaskDataMode(TaskDataMode.COURSE_API); runCurrent()
        model.selectTaskDataMode(TaskDataMode.DEVICE); runCurrent()
        gate.complete(Unit); advanceUntilIdle()
        assertEquals(TaskDataMode.DEVICE, model.uiState.value.dataMode)
        assertEquals("Lau nha", model.uiState.value.tasks.single().title)
    }
}

private class TaskMemoryPreferences : DataStore<Preferences> {
    override val data = MutableStateFlow(emptyPreferences())
    private val mutex = Mutex()
    var failWrites = false
    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences = mutex.withLock {
        if (failWrites) throw IOException("Simulated disk failure")
        transform(data.value).also { data.value = it }
    }
}
private class CourseSpy : TaskRepository {
    override val isConfigured = true
    var calls = 0
    var gate: CompletableDeferred<Unit>? = null
    private val sample = TodoTask(1, "Ôn tập chương 4", TaskCategory.STUDY, null, null, false)
    override suspend fun loadTasks(): List<TodoTask> { calls++; withContext(NonCancellable) { gate?.await() }; return listOf(sample) }
    override suspend fun loadTask(id: Long): TodoTask { calls++; return sample }
    override suspend fun setDone(id: Long, done: Boolean) { calls++ }
    override suspend fun updateTask(task: TodoTask) { calls++ }
    override suspend fun setSubtasks(id: Long, subtasks: List<TodoSubtask>) { calls++ }
    override suspend fun deleteTask(id: Long) { calls++ }
    override suspend fun createTask(draft: TaskDraft): TodoTask { calls++; throw TaskCreateFailure(TaskApiError.CREATE_MISMATCH, true) }
}
