package com.example.uth_todo

import androidx.lifecycle.ViewModelStore
import com.example.uth_todo.data.tasks.*
import com.example.uth_todo.model.*
import com.example.uth_todo.ui.home.HomeViewModel
import com.google.gson.JsonParser
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import okhttp3.Request
import okio.Buffer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class TaskCreateTest {
    private val dispatcher = StandardTestDispatcher()
    private val models = ViewModelStore()
    private lateinit var repo: CreateFakeRepository
    private lateinit var model: HomeViewModel
    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        repo = CreateFakeRepository()
        model = HomeViewModel(repo) { "2026-10-20" }
        models.put("tasks", model)
    }
    @After fun tearDown() { models.clear(); Dispatchers.resetMain() }
    private fun validDraft() = TaskDraft.blank("2026-10-20").copy(title = "  New task  ", note = " Note ",
        subtasks = listOf(TodoSubtask(" Step ", false)))
    private fun TestScope.open() {
        model.bindSession(AuthSession.Guest); advanceUntilIdle()
        model.beginTaskCreation("create-entry")
        model.changeCreationDraft(validDraft())
    }

    @Test fun postSendsNormalizedFieldsWithoutClientIdAndPreservesTypes() = runTest {
        val http = CreateHttp(TaskHttpResponse(201, "{\"id\":9}"))
        val draft = validDraft().copy(date = "", time = "", remind = true)
        val task = MockTaskRepository(" test-only-token ", http).createTask(draft)
        val request = http.requests.single()
        assertEquals("POST", request.method)
        assertEquals("https://share.tryapps.store/mock/teach/tasks", request.url.toString())
        assertEquals("Bearer test-only-token", request.header("Authorization"))
        val json = JsonParser.parseString(request.jsonBody()).asJsonObject
        assertFalse(json.has("id"))
        assertEquals("New task", json["title"].asString)
        assertEquals("study", json["category"].asString)
        assertTrue(json["date"].isJsonNull)
        assertTrue(json["time"].isJsonNull)
        assertFalse(json["done"].asBoolean)
        assertTrue(json["remind"].asBoolean)
        assertEquals("Step", json["subtasks"].asJsonArray[0].asJsonObject["title"].asString)
        assertEquals(9L, task.id)
        assertFalse(task.done)
    }

    @Test fun fullCreatedResponseMustMatchTheSubmittedDraft() = runTest {
        val body = """{"id":12,"title":" New task ","category":"study","done":false,"date":"2026-10-20","note":"Note","subtasks":[{"title":"Step","done":false}]}"""
        val task = MockTaskRepository("test-only-token", CreateHttp(TaskHttpResponse(201, body))).createTask(validDraft())
        assertEquals(12L, task.id)
        assertEquals("New task", task.title.trim())
        assertEquals(TaskCategory.STUDY, task.category)
        assertEquals("Note", task.note)
    }

    @Test fun submittingLauNhaMustNeverAppendAnUnrelatedStudySample() = runTest(dispatcher) {
        val response = """{"id":99,"title":"Ôn tập chương 4","category":"study","date":"2026-10-20","done":false}"""
        val requests = mutableListOf<Request>()
        val repository = MockTaskRepository("test-only-token", TaskHttpClient { request ->
            requests += request
            if (request.method == "GET") TaskHttpResponse(200, "[]") else TaskHttpResponse(201, response)
        })
        val tasks = HomeViewModel(repository) { "2026-10-20" }
        models.put("regression", tasks)
        tasks.bindSession(AuthSession.Guest); advanceUntilIdle()
        tasks.beginTaskCreation("lau-nha", TaskFilter.LIFE)
        tasks.changeCreationDraft(tasks.uiState.value.creation.draft.copy(title = "Lau nha"))
        tasks.createTask(); advanceUntilIdle()
        assertEquals("Lau nha", JsonParser.parseString(requests.single { it.method == "POST" }.jsonBody()).asJsonObject["title"].asString)
        assertTrue(tasks.uiState.value.tasks.isEmpty())
        assertNull(tasks.uiState.value.creation.created)
        assertNull(tasks.uiState.value.createdNotice)
        assertEquals("Lau nha", tasks.uiState.value.creation.draft.title)
        assertEquals(TaskApiError.CREATE_MISMATCH, tasks.uiState.value.creation.error)
        assertTrue(tasks.uiState.value.creation.requiresReview)
    }

    @Test fun matchingTitleDoesNotHideChangesToOtherSubmittedFields() = runTest {
        val baseline = """{"id":12,"title":"New task","category":"study","date":"2026-10-20","time":null,"note":"Note","priority":"medium","remind":false,"done":false,"subtasks":[{"title":"Step","done":false}]}"""
        val samples = listOf(baseline.replace("study", "life"), baseline.replace("2026-10-20", "2026-10-21"),
            baseline.replace("\"time\":null", "\"time\":\"14:00\""), baseline.replace("Note", "Different"),
            baseline.replace("medium", "high"), baseline.replace("\"remind\":false", "\"remind\":true"),
            baseline.replace("\"done\":false", "\"done\":true"), baseline.replace("Step", "Different step"))
        for (body in samples) {
            try { MockTaskRepository("test-only-token", CreateHttp(TaskHttpResponse(201, body))).createTask(validDraft()); fail() }
            catch (failure: TaskCreateFailure) {
                assertEquals(TaskApiError.CREATE_MISMATCH, failure.reason)
                assertTrue(failure.mayHaveCreated)
            }
        }
    }

    @Test fun sharedStateAlsoRejectsAnUnrelatedResultFromAnyRepository() = runTest(dispatcher) {
        open()
        repo.overrideResult = validDraft().created(9).copy(title = "Unrelated sample")
        model.createTask(); advanceUntilIdle()
        assertEquals(listOf(1L), model.uiState.value.tasks.map { it.id })
        assertNull(model.uiState.value.creation.created)
        assertEquals(TaskApiError.CREATE_MISMATCH, model.uiState.value.creation.error)
        assertEquals(validDraft(), model.uiState.value.creation.draft)
    }

    @Test fun unconfirmedSuccessWithoutAValidServerIdNeverFabricatesATask() = runTest {
        for (body in listOf("", "{}", "[]", "{\"ok\":true}", "{\"id\":0}", "{\"id\":-1}",
            "{\"id\":\"9\"}", "{\"id\":1.5}", "{\"id\":9223372036854775808}", "{\"id\":9} trailing")) {
            try {
                MockTaskRepository("test-only-token", CreateHttp(TaskHttpResponse(201, body))).createTask(validDraft())
                fail("Unconfirmed result must not create a local task")
            } catch (failure: TaskCreateFailure) {
                assertEquals(TaskApiError.INVALID_RESPONSE, failure.reason)
                assertTrue(failure.mayHaveCreated)
            }
        }
    }

    @Test fun requestFailuresDistinguishRejectionFromUncertainDeliveryAndPreserveCancellation() = runTest {
        for ((status, expected, uncertain) in listOf(
            Triple(401, TaskApiError.UNAUTHORIZED, false), Triple(429, TaskApiError.RATE_LIMITED, false),
            Triple(500, TaskApiError.SERVER, true))) {
            val http = CreateHttp(TaskHttpResponse(status, "ignored"))
            try { MockTaskRepository("test-only-token", http).createTask(validDraft()); fail() }
            catch (failure: TaskCreateFailure) {
                assertEquals(expected, failure.reason); assertEquals(uncertain, failure.mayHaveCreated)
                assertEquals(1, http.requests.size)
            }
        }
        try { MockTaskRepository("test-only-token", TaskHttpClient { throw IOException() }).createTask(validDraft()); fail() }
        catch (failure: TaskCreateFailure) { assertTrue(failure.mayHaveCreated); assertEquals(TaskApiError.NETWORK, failure.reason) }
        try { MockTaskRepository("test-only-token", TaskHttpClient { throw CancellationException() }).createTask(validDraft()); fail() }
        catch (_: CancellationException) { }
    }

    @Test fun missingConfigurationAndInvalidDraftMakeNoRequest() = runTest {
        val http = CreateHttp(TaskHttpResponse(201, "{\"id\":9}"))
        try { MockTaskRepository("", http).createTask(validDraft()); fail() }
        catch (failure: TaskApiFailure) { assertEquals(TaskApiError.NOT_CONFIGURED, failure.reason) }
        try { MockTaskRepository("test-only-token", http).createTask(validDraft().copy(title = "")); fail() }
        catch (_: IllegalArgumentException) { }
        assertTrue(http.requests.isEmpty())
    }

    @Test fun initialCategoryAndDateAreAppliedOnceAndNewEntryGetsACleanDraft() = runTest(dispatcher) {
        model.bindSession(AuthSession.Guest); advanceUntilIdle()
        model.beginTaskCreation("one", TaskFilter.LIFE, "2026-10-23")
        assertEquals(TaskCategory.LIFE, model.uiState.value.creation.draft.category)
        assertEquals("2026-10-23", model.uiState.value.creation.draft.date)
        model.changeCreationDraft(validDraft())
        model.beginTaskCreation("one", TaskFilter.STUDY, "invalid")
        assertEquals(validDraft(), model.uiState.value.creation.draft)
        model.beginTaskCreation("two", TaskFilter.ALL, "invalid")
        assertEquals("", model.uiState.value.creation.draft.title)
        assertEquals("2026-10-20", model.uiState.value.creation.draft.date)
        assertEquals(TaskCategory.STUDY, model.uiState.value.creation.draft.category)
    }

    @Test fun invalidTitleScheduleOrSubtaskDoesNotSubmitAndKeepsInput() = runTest(dispatcher) {
        open()
        for ((draft, error) in listOf(validDraft().copy(title = " ") to TaskEditError.TITLE,
            validDraft().copy(date = "2026-02-30") to TaskEditError.DATE,
            validDraft().copy(time = "25:00") to TaskEditError.TIME,
            validDraft().copy(subtasks = listOf(TodoSubtask("", false))) to TaskEditError.SUBTASK)) {
            model.changeCreationDraft(draft); model.createTask(); advanceUntilIdle()
            assertEquals(error, model.uiState.value.creation.validation)
            assertEquals(draft, model.uiState.value.creation.draft)
        }
        assertEquals(0, repo.creates)
    }

    @Test fun creationWaitsForAckBlocksRepeatedSubmissionAndAppendsOneServerAssignedTask() = runTest(dispatcher) {
        open()
        repo.createGate = CompletableDeferred()
        repeat(4) { model.createTask() }; runCurrent()
        assertEquals(1, repo.creates)
        assertEquals(1, model.uiState.value.tasks.size)
        assertNull(model.uiState.value.creation.created)
        assertTrue(model.uiState.value.creation.isSaving)
        model.changeCreationDraft(validDraft().copy(title = "Changed while sending"))
        assertEquals(validDraft(), model.uiState.value.creation.draft)
        repo.createGate!!.complete(Unit); runCurrent()
        assertEquals(listOf(1L, 9L), model.uiState.value.tasks.map { it.id })
        assertEquals("New task", model.uiState.value.creation.created!!.title)
        assertEquals(9L, model.uiState.value.createdNotice!!.id)
        model.createTask(); advanceUntilIdle()
        assertEquals(1, repo.creates)
    }

    @Test fun uncertainFailureKeepsDraftAndRequiresExplicitPermissionBeforeAnotherPost() = runTest(dispatcher) {
        open()
        repo.failure = TaskCreateFailure(TaskApiError.NETWORK, true)
        model.createTask(); advanceUntilIdle()
        assertEquals(validDraft(), model.uiState.value.creation.draft)
        assertTrue(model.uiState.value.creation.requiresReview)
        assertEquals(1, model.uiState.value.tasks.size)
        repeat(2) { model.createTask() }; advanceUntilIdle()
        assertEquals(1, repo.creates)
        model.allowCreationRetry()
        repo.failure = null
        model.createTask(); advanceUntilIdle()
        assertEquals(2, repo.creates)
        assertEquals(9L, model.uiState.value.creation.created!!.id)
    }

    @Test fun definitiveRejectionCanBeRetriedWithoutLosingTheDraft() = runTest(dispatcher) {
        open()
        repo.failure = TaskCreateFailure(TaskApiError.UNAUTHORIZED, false)
        model.createTask(); advanceUntilIdle()
        assertFalse(model.uiState.value.creation.requiresReview)
        assertEquals(TaskApiError.UNAUTHORIZED, model.uiState.value.creation.error)
        repo.failure = null
        model.createTask(); advanceUntilIdle()
        assertNotNull(model.uiState.value.creation.created)
    }

    @Test fun reusedIdDoesNotOverwriteExistingTaskOrClaimCreationSucceeded() = runTest(dispatcher) {
        open()
        repo.resultId = 1
        model.createTask(); advanceUntilIdle()
        assertEquals("Existing", model.uiState.value.tasks.single().title)
        assertNull(model.uiState.value.creation.created)
        assertNull(model.uiState.value.createdNotice)
        assertTrue(model.uiState.value.creation.requiresReview)
    }

    @Test fun lateCreationForPreviousSessionCannotAppendDataOrSuccessNotice() = runTest(dispatcher) {
        open()
        val gate = CompletableDeferred<Unit>()
        repo.createGate = gate; repo.ignoreCreateCancellation = true
        model.createTask(); runCurrent()
        repo.createGate = null
        model.bindSession(AuthSession.SignedIn(UserProfile("bob", "Bob", null, false)))
        runCurrent()
        gate.complete(Unit); advanceUntilIdle()
        assertNull(model.uiState.value.creation.created)
        assertNull(model.uiState.value.createdNotice)
        assertEquals(listOf(1L), model.uiState.value.tasks.map { it.id })
    }

    @Test fun lateListSnapshotCannotEraseCreatedTaskAndReturningDoesNotImmediatelyRefetch() = runTest(dispatcher) {
        model.bindSession(AuthSession.Guest); advanceUntilIdle()
        val gate = CompletableDeferred<Unit>()
        repo.listGate = gate
        model.refresh(); runCurrent()
        model.beginTaskCreation("entry"); model.changeCreationDraft(validDraft())
        model.createTask(); runCurrent()
        gate.complete(Unit); advanceUntilIdle()
        assertEquals(listOf(1L, 9L), model.uiState.value.tasks.map { it.id })
        val reads = repo.listReads
        model.onTaskScreenOpened(); advanceUntilIdle()
        assertEquals(reads, repo.listReads)
    }

    @Test fun timeoutUnblocksControlsKeepsDraftAndStopsAutomaticResubmission() = runTest(dispatcher) {
        open()
        repo.createGate = CompletableDeferred()
        model.createTask(); advanceUntilIdle()
        assertEquals(TaskApiError.TIMEOUT, model.uiState.value.creation.error)
        assertEquals(validDraft(), model.uiState.value.creation.draft)
        assertFalse(model.uiState.value.creation.isSaving)
        assertTrue(model.uiState.value.creation.requiresReview)
        model.createTask(); advanceUntilIdle()
        assertEquals(1, repo.creates)
    }

    @Test fun reviewingListRetainsDraftAndClosingWithoutSubmissionClearsIt() = runTest(dispatcher) {
        open()
        repo.failure = TaskCreateFailure(TaskApiError.INVALID_RESPONSE, true)
        model.createTask(); advanceUntilIdle()
        model.onTaskScreenOpened(); advanceUntilIdle()
        model.beginTaskCreation("create-entry")
        assertEquals(validDraft(), model.uiState.value.creation.draft)
        assertTrue(model.uiState.value.creation.requiresReview)
        model.closeTaskCreation("create-entry")
        assertNull(model.uiState.value.creation.entryKey)
        assertFalse(model.uiState.value.creation.isDirty)
        assertEquals(1, repo.creates)
    }
}

private fun Request.jsonBody(): String = Buffer().also { body!!.writeTo(it) }.readUtf8()
private class CreateHttp(private val response: TaskHttpResponse) : TaskHttpClient {
    val requests = mutableListOf<Request>()
    override suspend fun execute(request: Request): TaskHttpResponse { requests += request; return response }
}
private class CreateFakeRepository : TaskRepository {
    override val isConfigured = true
    var tasks = listOf(TodoTask(1, "Existing", TaskCategory.STUDY, "2026-10-20", "09:00", false))
    var resultId = 9L
    var creates = 0
    var listReads = 0
    var failure: TaskCreateFailure? = null
    var overrideResult: TodoTask? = null
    var createGate: CompletableDeferred<Unit>? = null
    var listGate: CompletableDeferred<Unit>? = null
    var ignoreCreateCancellation = false
    override suspend fun loadTasks(): List<TodoTask> {
        listReads++
        val snapshot = tasks
        val gate = listGate
        withContext(NonCancellable) { gate?.await() }
        return snapshot
    }
    override suspend fun createTask(draft: TaskDraft): TodoTask {
        creates++
        val gate = createGate
        if (ignoreCreateCancellation) withContext(NonCancellable) { gate?.await() } else gate?.await()
        failure?.let { throw it }
        return overrideResult ?: draft.created(resultId)
    }
    override suspend fun loadTask(id: Long): TodoTask = tasks.single { it.id == id }
    override suspend fun setDone(id: Long, done: Boolean) = error("Unused")
    override suspend fun updateTask(task: TodoTask) = error("Unused")
    override suspend fun setSubtasks(id: Long, subtasks: List<TodoSubtask>) = error("Unused")
    override suspend fun deleteTask(id: Long) = error("Unused")
}
