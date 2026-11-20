package com.example.uth_todo

import androidx.lifecycle.ViewModelStore
import com.example.uth_todo.data.tasks.*
import com.example.uth_todo.model.*
import com.example.uth_todo.ui.home.HomeViewModel
import com.example.uth_todo.ui.taskdetail.*
import com.google.gson.JsonParser
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import okhttp3.Request
import okio.Buffer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TaskDetailTest {
    private val dispatcher = StandardTestDispatcher()
    private val models = ViewModelStore()
    private lateinit var repo: DetailFakeRepository
    private lateinit var model: HomeViewModel
    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        repo = DetailFakeRepository()
        model = HomeViewModel(repo) { "2026-10-20" }
        models.put("tasks", model)
    }
    @After fun tearDown() { models.clear(); Dispatchers.resetMain() }
    private suspend fun TestScope.open() {
        model.bindSession(AuthSession.Guest)
        advanceUntilIdle()
        model.openTask(1, "entry-1")
        advanceUntilIdle()
    }

    @Test fun detailDecoderRequiresOneTaskObjectAndRepositoryChecksRequestedId() = runTest {
        val decoder = TaskJsonDecoder()
        assertEquals(1L, decoder.decodeDetail(detailJson()).id)
        for (body in listOf("[]", "null", "{}", "{\"data\":" + detailJson() + "}", detailJson() + " trailing")) {
            try { decoder.decodeDetail(body); fail("Expected invalid detail") }
            catch (error: TaskApiFailure) { assertEquals(TaskApiError.INVALID_RESPONSE, error.reason) }
        }
        try {
            MockTaskRepository("test-only-token", DetailHttp(TaskHttpResponse(200, detailJson()))).loadTask(2)
            fail("Wrong ID must not be shown")
        } catch (error: TaskApiFailure) { assertEquals(TaskApiError.INVALID_RESPONSE, error.reason) }
    }

    @Test fun detailGetAndNotFoundUseDocumentedIdEndpointAndToken() = runTest {
        val http = DetailHttp(TaskHttpResponse(200, detailJson()))
        assertEquals(1L, MockTaskRepository(" test-only-token ", http).loadTask(1).id)
        assertEquals("GET", http.requests.single().method)
        assertEquals("/mock/teach/tasks/1", http.requests.single().url.encodedPath)
        assertEquals("Bearer test-only-token", http.requests.single().header("Authorization"))
        for (status in listOf(404, 401, 500)) {
            try { MockTaskRepository("test-only-token", DetailHttp(TaskHttpResponse(status, "ignored"))).loadTask(1); fail() }
            catch (error: TaskApiFailure) {
                assertEquals(when (status) { 404 -> TaskApiError.NOT_FOUND; 401 -> TaskApiError.UNAUTHORIZED; else -> TaskApiError.SERVER }, error.reason)
            }
        }
    }

    @Test fun editPatchKeepsFieldTypesAndExplicitNullsWithoutSendingId() = runTest {
        val task = DetailFakeRepository().tasks.first().copy(date = null, time = null)
        val http = DetailHttp(TaskHttpResponse(204, ""))
        MockTaskRepository("test-only-token", http).updateTask(task)
        val request = http.requests.single()
        assertEquals("PATCH", request.method)
        val json = JsonParser.parseString(request.bodyText()).asJsonObject
        assertFalse(json.has("id"))
        assertTrue(json["date"].isJsonNull)
        assertTrue(json["time"].isJsonNull)
        assertEquals("study", json["category"].asString)
        assertFalse(json["done"].asBoolean)
        assertEquals(2, json["subtasks"].asJsonArray.size())
        assertEquals(setOf("title", "done"), json["subtasks"].asJsonArray[0].asJsonObject.keySet())
    }

    @Test fun subtaskPatchOnlySendsSubtasksAndDeleteWaitsForSuccessStatus() = runTest {
        val http = DetailHttp(TaskHttpResponse(204, ""))
        val repository = MockTaskRepository("test-only-token", http)
        repository.setSubtasks(1, emptyList())
        assertEquals("{\"subtasks\":[]}", http.requests.single().bodyText())
        repository.deleteTask(1)
        assertEquals("DELETE", http.requests.last().method)
        assertEquals("/mock/teach/tasks/1", http.requests.last().url.encodedPath)
        try { MockTaskRepository("test-only-token", DetailHttp(TaskHttpResponse(500, ""))).deleteTask(1); fail() }
        catch (error: TaskApiFailure) { assertEquals(TaskApiError.SERVER, error.reason) }
    }

    @Test fun openingDetailGetsFreshDataAndSameEntryPreservesDraftWithoutDuplicateReads() = runTest(dispatcher) {
        model.bindSession(AuthSession.Guest); advanceUntilIdle()
        repo.tasks = repo.tasks.map { it.copy(note = "Fresh detail") }
        model.openTask(1, "entry-1"); advanceUntilIdle()
        assertEquals("Fresh detail", model.uiState.value.detail.task!!.note)
        model.beginTaskEdit()
        val draft = model.uiState.value.detail.draft!!.copy(title = "Unsaved")
        model.changeTaskDraft(draft)
        model.openTask(1, "entry-1"); advanceUntilIdle()
        assertEquals(1, repo.detailReads)
        assertEquals(draft, model.uiState.value.detail.draft)
    }

    @Test fun detailLoadFailureHasNoFakeCacheSuccessAndCanBeRetried() = runTest(dispatcher) {
        repo.readFailure = TaskApiError.NOT_FOUND
        open()
        assertNull(model.uiState.value.detail.task)
        assertEquals(TaskApiError.NOT_FOUND, model.uiState.value.detail.error)
        repo.readFailure = null
        model.retryTaskDetail(); advanceUntilIdle()
        assertEquals(1L, model.uiState.value.detail.task!!.id)
        assertNull(model.uiState.value.detail.error)
    }

    @Test fun saveWaitsForAckPreventsDoubleSubmissionAndUpdatesListOnce() = runTest(dispatcher) {
        open()
        model.beginTaskEdit()
        model.changeTaskDraft(model.uiState.value.detail.draft!!.copy(title = "  New title  ", category = TaskCategory.LIFE))
        repo.writeGate = CompletableDeferred()
        repeat(3) { model.saveTaskEdit() }
        runCurrent()
        assertEquals(1, repo.writes)
        assertEquals("Original", model.uiState.value.tasks.first().title)
        assertTrue(model.uiState.value.detail.isSaving)
        repo.writeGate!!.complete(Unit); runCurrent()
        assertEquals("New title", model.uiState.value.tasks.single { it.id == 1L }.title)
        assertEquals(TaskCategory.LIFE, model.uiState.value.detail.task!!.category)
        assertFalse(model.uiState.value.detail.isEditing)
        assertTrue(model.uiState.value.detail.saved)
        assertEquals(2, model.uiState.value.tasks.size)
    }

    @Test fun failedSaveKeepsDraftAndOriginalUntilRetrySucceeds() = runTest(dispatcher) {
        open(); model.beginTaskEdit()
        val draft = model.uiState.value.detail.draft!!.copy(note = "Keep this draft")
        model.changeTaskDraft(draft)
        repo.writeFailure = TaskApiError.NETWORK
        model.saveTaskEdit(); advanceUntilIdle()
        assertEquals(draft, model.uiState.value.detail.draft)
        assertEquals("", model.uiState.value.detail.task!!.note)
        assertEquals(TaskApiError.NETWORK, model.uiState.value.detail.mutationError)
        repo.writeFailure = null
        model.saveTaskEdit(); advanceUntilIdle()
        assertEquals("Keep this draft", model.uiState.value.tasks.first().note)
    }

    @Test fun invalidDraftsNeverReachTheNetworkAndCanClearSchedules() = runTest(dispatcher) {
        open(); model.beginTaskEdit()
        val original = model.uiState.value.detail.draft!!
        val invalid = listOf(
            original.copy(title = " ") to TaskEditError.TITLE,
            original.copy(date = "2026-02-30") to TaskEditError.DATE,
            original.copy(time = "24:00") to TaskEditError.TIME,
            original.copy(category = TaskCategory.OTHER) to TaskEditError.CATEGORY,
            original.copy(priority = TaskPriority.OTHER) to TaskEditError.PRIORITY,
            original.copy(subtasks = listOf(TodoSubtask(" ", false))) to TaskEditError.SUBTASK)
        for ((draft, error) in invalid) {
            model.changeTaskDraft(draft); model.saveTaskEdit(); advanceUntilIdle()
            assertEquals(error, model.uiState.value.detail.validation)
        }
        assertEquals(0, repo.writes)
        model.changeTaskDraft(original.copy(date = "", time = "", subtasks = emptyList()))
        model.saveTaskEdit(); advanceUntilIdle()
        assertNull(model.uiState.value.detail.task!!.date)
        assertNull(model.uiState.value.detail.task!!.time)
        assertTrue(model.uiState.value.detail.task!!.subtasks.isEmpty())
    }

    @Test fun editingSubtasksCanAddRenameRemoveAndPreserveParentCompletion() = runTest(dispatcher) {
        open(); model.beginTaskEdit()
        val draft = model.uiState.value.detail.draft!!
        model.changeTaskDraft(draft.copy(subtasks = listOf(draft.subtasks.last().copy(title = "Renamed"), TodoSubtask("Added", false))))
        model.saveTaskEdit(); advanceUntilIdle()
        assertEquals(listOf("Renamed", "Added"), model.uiState.value.detail.task!!.subtasks.map { it.title })
        assertFalse(model.uiState.value.detail.task!!.done)
    }

    @Test fun togglingSubtaskAndParentUpdatesBothViewsOnlyAfterAck() = runTest(dispatcher) {
        open()
        repo.writeGate = CompletableDeferred()
        model.toggleSubtask(0); runCurrent()
        assertFalse(model.uiState.value.detail.task!!.subtasks.first().done)
        repo.writeGate!!.complete(Unit); runCurrent()
        assertTrue(model.uiState.value.tasks.first().subtasks.first().done)
        assertFalse(model.uiState.value.detail.task!!.done)
        model.toggleDetailDone(); advanceUntilIdle()
        assertTrue(model.uiState.value.tasks.first().done)
        assertEquals(model.uiState.value.tasks.first(), model.uiState.value.detail.task)
    }

    @Test fun deleteFailureKeepsTaskAndSuccessfulAckRemovesItThenMarksNavigationReady() = runTest(dispatcher) {
        open()
        repo.writeFailure = TaskApiError.SERVER
        model.deleteTask(); advanceUntilIdle()
        assertEquals(2, model.uiState.value.tasks.size)
        assertFalse(model.uiState.value.detail.deleted)
        repo.writeFailure = null
        repo.writeGate = CompletableDeferred()
        repeat(2) { model.deleteTask() }; runCurrent()
        assertEquals(2, model.uiState.value.tasks.size)
        repo.writeGate!!.complete(Unit); runCurrent()
        assertEquals(listOf(2L), model.uiState.value.tasks.map { it.id })
        assertTrue(model.uiState.value.detail.deleted)
        assertFalse(model.uiState.value.detail.isSaving)
    }

    @Test fun lateDetailForPreviousEntryCannotReplaceCurrentTask() = runTest(dispatcher) {
        model.bindSession(AuthSession.Guest); advanceUntilIdle()
        val gate = CompletableDeferred<Unit>()
        repo.readGate = gate; repo.ignoreReadCancellation = true
        model.openTask(1, "old-entry"); runCurrent()
        repo.readGate = null
        model.openTask(2, "new-entry"); runCurrent()
        gate.complete(Unit); advanceUntilIdle()
        assertEquals(2L, model.uiState.value.detail.task!!.id)
        assertEquals("new-entry", model.uiState.value.detail.entryKey)
    }

    @Test fun lateDetailAndPendingWriteCannotRestoreDataAfterSignOut() = runTest(dispatcher) {
        open()
        repo.writeGate = CompletableDeferred()
        model.toggleDetailDone(); runCurrent()
        model.bindSession(AuthSession.SignedOut)
        repo.writeGate!!.complete(Unit); advanceUntilIdle()
        assertTrue(model.uiState.value.tasks.isEmpty())
        assertNull(model.uiState.value.detail.task)
        model.bindSession(AuthSession.Guest); advanceUntilIdle()
        val gate = CompletableDeferred<Unit>()
        repo.readGate = gate; repo.ignoreReadCancellation = true
        model.openTask(1, "old-account"); runCurrent()
        repo.readGate = null
        model.bindSession(AuthSession.SignedIn(UserProfile("bob", "Bob", null, false)))
        runCurrent()
        gate.complete(Unit); advanceUntilIdle()
        assertNull(model.uiState.value.detail.task)
    }

    @Test fun detailTimeoutsReleaseControlsAndPreserveUnsavedDraft() = runTest(dispatcher) {
        repo.readGate = CompletableDeferred()
        open()
        assertEquals(TaskApiError.TIMEOUT, model.uiState.value.detail.error)
        assertFalse(model.uiState.value.detail.isLoading)
        repo.readGate = null
        model.retryTaskDetail(); advanceUntilIdle()
        model.beginTaskEdit()
        val draft = model.uiState.value.detail.draft!!.copy(title = "Not acknowledged")
        model.changeTaskDraft(draft)
        repo.writeGate = CompletableDeferred()
        model.saveTaskEdit(); advanceUntilIdle()
        assertEquals(draft, model.uiState.value.detail.draft)
        assertEquals(TaskApiError.TIMEOUT, model.uiState.value.detail.mutationError)
        assertFalse(model.uiState.value.detail.isSaving)
    }

    @Test fun restoredDetailCancelsOlderListSnapshotAndBackDuringReadCanReloadList() = runTest(dispatcher) {
        val oldList = CompletableDeferred<Unit>()
        repo.listGate = oldList
        model.bindSession(AuthSession.Guest); runCurrent()
        repo.tasks = repo.tasks.map { it.copy(title = "Fresh") }
        model.openTask(1, "restored-entry"); runCurrent()
        oldList.complete(Unit); advanceUntilIdle()
        assertEquals("Fresh", model.uiState.value.tasks.single { it.id == 1L }.title)
        assertFalse(model.uiState.value.isLoading)
        repo.listGate = null
        repo.readGate = CompletableDeferred()
        model.openTask(2, "next-entry"); runCurrent()
        model.onTaskScreenOpened(); advanceUntilIdle()
        assertTrue(model.uiState.value.hasLoaded)
        assertFalse(model.uiState.value.detail.isLoading)
        assertEquals(2, model.uiState.value.tasks.size)
    }

    @Test fun returnToListDoesNotImmediatelyOverwriteAnAcknowledgedDetailMutation() = runTest(dispatcher) {
        open(); model.toggleDetailDone(); advanceUntilIdle()
        val loads = repo.listReads
        model.onTaskScreenOpened(); advanceUntilIdle()
        assertEquals(loads, repo.listReads)
        assertTrue(model.uiState.value.tasks.first().done)
        model.refresh(); advanceUntilIdle()
        assertEquals(loads + 1, repo.listReads)
    }
}

private fun detailJson() = """{"id":1,"title":"Original","category":"study","date":"2026-10-20","time":"14:00","done":false,"subtasks":[{"title":"Step","done":false}]}"""
private fun Request.bodyText(): String = Buffer().also { body!!.writeTo(it) }.readUtf8()
private class DetailHttp(private val response: TaskHttpResponse) : TaskHttpClient {
    val requests = mutableListOf<Request>()
    override suspend fun execute(request: Request): TaskHttpResponse { requests += request; return response }
}

private class DetailFakeRepository : TaskRepository {
    override val isConfigured = true
    var tasks = listOf(
        TodoTask(1, "Original", TaskCategory.STUDY, "2026-10-20", "14:00", false,
            subtasks = listOf(TodoSubtask("First", false), TodoSubtask("Second", true))),
        TodoTask(2, "Other", TaskCategory.LIFE, null, null, false))
    var detailReads = 0
    var listReads = 0
    var writes = 0
    var readFailure: TaskApiError? = null
    var writeFailure: TaskApiError? = null
    var readGate: CompletableDeferred<Unit>? = null
    var listGate: CompletableDeferred<Unit>? = null
    var writeGate: CompletableDeferred<Unit>? = null
    var ignoreReadCancellation = false
    override suspend fun loadTasks(): List<TodoTask> {
        listReads++
        val snapshot = tasks
        val gate = listGate
        withContext(NonCancellable) { gate?.await() }
        return snapshot
    }
    override suspend fun loadTask(id: Long): TodoTask {
        detailReads++
        val snapshot = tasks.single { it.id == id }
        val gate = readGate
        if (ignoreReadCancellation) withContext(NonCancellable) { gate?.await() } else gate?.await()
        readFailure?.let { throw TaskApiFailure(it) }
        return snapshot
    }
    private suspend fun acknowledge() {
        writes++
        writeGate?.await()
        writeFailure?.let { throw TaskApiFailure(it) }
    }
    override suspend fun setDone(id: Long, done: Boolean) {
        acknowledge()
        tasks = tasks.map { if (it.id == id) it.copy(done = done) else it }
    }
    override suspend fun updateTask(task: TodoTask) {
        acknowledge()
        tasks = tasks.map { if (it.id == task.id) task else it }
    }
    override suspend fun setSubtasks(id: Long, subtasks: List<TodoSubtask>) {
        acknowledge()
        tasks = tasks.map { if (it.id == id) it.copy(subtasks = subtasks) else it }
    }
    override suspend fun deleteTask(id: Long) {
        acknowledge()
        tasks = tasks.filterNot { it.id == id }
    }
}
