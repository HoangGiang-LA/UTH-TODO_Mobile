package com.example.uth_todo

import com.example.uth_todo.data.tasks.*
import com.example.uth_todo.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import okhttp3.Request
import okio.Buffer
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.io.InterruptedIOException

internal fun taskFixture(): String = requireNotNull(object {}.javaClass.getResourceAsStream("/tasks-list.json"))
    .bufferedReader(Charsets.UTF_8).use { it.readText() }

class TaskApiTest {
    private val decoder = TaskJsonDecoder()
    private fun expectFailure(reason: TaskApiError, operation: () -> Unit) {
        try { operation(); fail("Expected $reason") } catch (failure: TaskApiFailure) { assertEquals(reason, failure.reason) }
    }

    @Test fun decodesTheArrayContractWithOptionalFieldsAndSubtasks() {
        val tasks = decoder.decodeList(taskFixture())
        assertEquals(6, tasks.size)
        val task = tasks.single { it.id == 1L }
        assertEquals(TaskCategory.STUDY, task.category)
        assertTrue(task.done)
        assertTrue(task.remind)
        assertEquals(2, task.subtasks.size)
        assertFalse(task.subtasks.last().done)
        assertEquals("Nộp bài đúng hạn", task.note)
    }

    @Test fun malformedResponsesNeverBecomeAFalseEmptyState() {
        for (body in listOf("", "<html>error</html>", "{}", "null", "[null]", "[1]", "[{},]", "[] garbage")) {
            expectFailure(TaskApiError.INVALID_RESPONSE) { decoder.decodeList(body) }
        }
        assertTrue(decoder.decodeList("[]").isEmpty())
    }

    @Test fun invalidIdsAndDuplicateIdsAreRejected() {
        val row = "{\"id\":1,\"title\":\"Task\",\"category\":\"study\",\"done\":false}"
        for (id in listOf("-1", "1.5", "\"1\"", "null", "9223372036854775808")) {
            expectFailure(TaskApiError.INVALID_RESPONSE) { decoder.decodeList("[${row.replace("\"id\":1", "\"id\":$id") }]") }
        }
        expectFailure(TaskApiError.INVALID_RESPONSE) { decoder.decodeList("[$row,$row]") }
    }

    @Test fun strictTypesAndSchedulesPreventIncorrectCountsOrDates() {
        val row = "{\"id\":1,\"title\":\"Task\",\"category\":\"study\",\"done\":false,\"date\":\"2026-10-20\",\"time\":\"09:00\"}"
        for (invalid in listOf(row.replace("false", "\"false\""), row.replace("2026-10-20", "2026-02-30"),
            row.replace("09:00", "25:00"), row.replace("\"Task\"", "42"))) {
            expectFailure(TaskApiError.INVALID_RESPONSE) { decoder.decodeList("[$invalid]") }
        }
    }

    @Test fun unknownCategoryIsNotMislabelledAndUnscheduledItemsAreAllowed() {
        val tasks = decoder.decodeList("[{\"id\":1,\"title\":\"Task\",\"category\":\"unknown\",\"done\":false,\"date\":null,\"time\":null,\"extra\":123}]")
        assertEquals(TaskCategory.OTHER, tasks.single().category)
        assertNull(tasks.single().date)
        assertNull(tasks.single().time)
    }

    @Test fun progressUsesOnlyTheSelectedDayWhileCategoryCountsIncludeFuturePendingWork() {
        val tasks = decoder.decodeList(taskFixture())
        val summary = tasks.summary("2026-10-20")
        assertEquals(4, summary.total)
        assertEquals(2, summary.completed)
        assertEquals(50, summary.percent)
        assertEquals(2, summary.studyRemaining)
        assertEquals(2, summary.lifeRemaining)
        assertEquals(listOf(4L, 1L, 2L, 3L), tasks.forDate("2026-10-20").map { it.id })
        assertEquals(0, tasks.summary("2026-11-20").percent)
    }

    @Test fun filteringAndSearchSupportVietnameseWithoutChangingTheSourceList() {
        val tasks = decoder.decodeList(taskFixture())
        assertEquals(listOf(2L), tasks.matching(TaskFilter.STUDY, "on chuong 3").map { it.id })
        assertEquals(3, tasks.matching(TaskFilter.LIFE, "").size)
        assertTrue(tasks.matching(TaskFilter.LIFE, "on chuong").isEmpty())
        val sorted = tasks.matching(TaskFilter.ALL, "")
        assertFalse(sorted.first().done)
        assertTrue(sorted.last().done)
        assertEquals(6, tasks.size)
    }

    @Test fun datesValidateLeapDaysAndRetainTheSameDateInPickerUtcValues() {
        assertTrue(TaskDates.isValidDate("2024-02-29"))
        assertFalse(TaskDates.isValidDate("2026-02-29"))
        assertFalse(TaskDates.isValidDate("2026-1-2"))
        assertFalse(TaskDates.isValidTime("24:00"))
        assertEquals("2026-10-20", TaskDates.fromUtcMillis(TaskDates.toUtcMillis("2026-10-20")))
    }

    @Test fun sendsTheProvidedTokenToTheDocumentedGetEndpoint() = runTest {
        val http = RecordingHttp(TaskHttpResponse(200, taskFixture()))
        val tasks = MockTaskRepository(" test-only-token ", http).loadTasks()
        assertEquals(6, tasks.size)
        val request = http.requests.single()
        assertEquals("GET", request.method)
        assertEquals("https://share.tryapps.store/mock/teach/tasks", request.url.toString())
        assertEquals("Bearer test-only-token", request.header("Authorization"))
    }

    @Test fun patchOnlySendsTheRequestedCompletionFieldAndAcceptsNoContentAck() = runTest {
        val http = RecordingHttp(TaskHttpResponse(204, ""))
        MockTaskRepository("test-only-token", http).setDone(4, false)
        val request = http.requests.single()
        assertEquals("PATCH", request.method)
        assertEquals("/mock/teach/tasks/4", request.url.encodedPath)
        val buffer = Buffer()
        request.body!!.writeTo(buffer)
        assertEquals("{\"done\":false}", buffer.readUtf8())
    }

    @Test fun handlesUnauthorizedThrottledAndServerResponsesWithoutParsingTheirBodies() = runTest {
        for ((status, expected) in listOf(401 to TaskApiError.UNAUTHORIZED, 403 to TaskApiError.UNAUTHORIZED,
            429 to TaskApiError.RATE_LIMITED, 500 to TaskApiError.SERVER, 302 to TaskApiError.SERVER)) {
            try {
                MockTaskRepository("test-only-token", RecordingHttp(TaskHttpResponse(status, "private server body"))).loadTasks()
                fail("Expected failure")
            } catch (failure: TaskApiFailure) { assertEquals(expected, failure.reason) }
        }
    }

    @Test fun mapsTransportFailuresAndPreservesCancellation() = runTest {
        for ((error, expected) in listOf(IOException("test") to TaskApiError.NETWORK,
            InterruptedIOException("test") to TaskApiError.TIMEOUT)) {
            try {
                MockTaskRepository("test-only-token", TaskHttpClient { throw error }).loadTasks()
                fail("Expected failure")
            } catch (failure: TaskApiFailure) { assertEquals(expected, failure.reason) }
        }
        try {
            MockTaskRepository("test-only-token", TaskHttpClient { throw CancellationException("test") }).loadTasks()
            fail("Expected cancellation")
        } catch (_: CancellationException) { }
    }

    @Test fun missingTokenMakesNoHttpRequest() = runTest {
        val http = RecordingHttp(TaskHttpResponse(200, "[]"))
        try { MockTaskRepository(" ", http).loadTasks(); fail("Expected missing config") }
        catch (failure: TaskApiFailure) { assertEquals(TaskApiError.NOT_CONFIGURED, failure.reason) }
        assertTrue(http.requests.isEmpty())
    }
}

private class RecordingHttp(private val response: TaskHttpResponse) : TaskHttpClient {
    val requests = mutableListOf<Request>()
    override suspend fun execute(request: Request): TaskHttpResponse { requests += request; return response }
}
