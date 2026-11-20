package com.example.uth_todo.data.tasks

import com.example.uth_todo.model.TaskApiError
import com.example.uth_todo.model.TaskApiFailure
import com.example.uth_todo.model.TodoTask
import com.example.uth_todo.model.TodoSubtask
import com.example.uth_todo.model.TaskCategory
import com.example.uth_todo.model.TaskPriority
import com.example.uth_todo.model.TaskDraft
import com.example.uth_todo.model.TaskCreateFailure
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InterruptedIOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class TaskHttpResponse(val status: Int, val body: String)
fun interface TaskHttpClient { suspend fun execute(request: Request): TaskHttpResponse }

class OkHttpTaskClient : TaskHttpClient {
    private val client = OkHttpClient.Builder().followRedirects(false).followSslRedirects(false)
        .retryOnConnectionFailure(false)
        .connectTimeout(10, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS).callTimeout(20, TimeUnit.SECONDS).build()

    override suspend fun execute(request: Request): TaskHttpResponse = suspendCancellableCoroutine { continuation ->
        val call = client.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val result = response.use {
                        // Limit payloads and always close sockets/bodies. Never log headers or response text.
                        val bytes = ByteArrayOutputStream()
                        it.body?.byteStream()?.use { input ->
                            val buffer = ByteArray(8192)
                            while (true) {
                                val count = input.read(buffer)
                                if (count < 0) break
                                if (bytes.size() + count > 1_048_576) throw TaskApiFailure(TaskApiError.INVALID_RESPONSE)
                                bytes.write(buffer, 0, count)
                            }
                        }
                        TaskHttpResponse(it.code, bytes.toString(Charsets.UTF_8.name()))
                    }
                    if (continuation.isActive) continuation.resume(result)
                } catch (failure: Exception) {
                    if (continuation.isActive) continuation.resumeWithException(failure)
                }
            }
        })
    }
}

class MockTaskRepository(
    private val token: String,
    private val http: TaskHttpClient = OkHttpTaskClient(),
    private val decoder: TaskJsonDecoder = TaskJsonDecoder(),
) : TaskRepository {
    override val isConfigured: Boolean get() = token.isNotBlank()

    override suspend fun loadTasks(): List<TodoTask> = withContext(Dispatchers.Default) {
        decoder.decodeList(execute(request("https://share.tryapps.store/mock/teach/tasks").get().build()).body)
    }

    override suspend fun setDone(id: Long, done: Boolean) {
        patch(id, mapOf("done" to done))
    }

    override suspend fun loadTask(id: Long): TodoTask = withContext(Dispatchers.Default) {
        require(id > 0)
        val task = decoder.decodeDetail(execute(request("https://share.tryapps.store/mock/teach/tasks/$id").get().build()).body)
        if (task.id != id) throw TaskApiFailure(TaskApiError.INVALID_RESPONSE)
        task
    }

    override suspend fun updateTask(task: TodoTask) {
        require(task.title.isNotBlank() && task.subtasks.all { it.title.isNotBlank() })
        require(task.category != TaskCategory.OTHER && task.priority != TaskPriority.OTHER)
        // Use the same field names/types as GET. PATCH/DELETE accept any 2xx ACK.
        patch(task.id, mapOf("title" to task.title, "category" to task.category.name.lowercase(),
            "date" to task.date, "time" to task.time, "note" to task.note,
            "priority" to task.priority.name.lowercase(), "remind" to task.remind,
            "done" to task.done, "subtasks" to task.subtasks))
    }

    override suspend fun setSubtasks(id: Long, subtasks: List<TodoSubtask>) {
        require(subtasks.all { it.title.isNotBlank() })
        patch(id, mapOf("subtasks" to subtasks))
    }

    override suspend fun deleteTask(id: Long) {
        require(id > 0)
        execute(request("https://share.tryapps.store/mock/teach/tasks/$id").delete().build())
    }

    override suspend fun createTask(draft: TaskDraft): TodoTask {
        require(draft.validationError() == null)
        // IDs are assigned by the server. Never fabricate one from the local list.
        val fields = mapOf("title" to draft.title.trim(), "category" to draft.category.name.lowercase(),
            "date" to draft.date.trim().ifBlank { null }, "time" to draft.time.trim().ifBlank { null },
            "note" to draft.note.trim(), "priority" to draft.priority.name.lowercase(), "remind" to draft.remind,
            "done" to false, "subtasks" to draft.subtasks.map { it.copy(title = it.title.trim()) })
        val request = request("https://share.tryapps.store/mock/teach/tasks")
            .post(com.google.gson.GsonBuilder().serializeNulls().create().toJson(fields)
                .toRequestBody("application/json; charset=utf-8".toMediaType())).build()
        val response = try {
            execute(request)
        } catch (failure: TaskApiFailure) {
            throw TaskCreateFailure(failure.reason, failure.reason in setOf(TaskApiError.NETWORK,
                TaskApiError.TIMEOUT, TaskApiError.SERVER, TaskApiError.INVALID_RESPONSE))
        }
        return try { decoder.decodeCreated(response.body, draft) }
        catch (failure: TaskApiFailure) { throw TaskCreateFailure(failure.reason, mayHaveCreated = true) }
    }

    private suspend fun patch(id: Long, fields: Map<String, Any?>) {
        require(id > 0)
        val json = com.google.gson.GsonBuilder().serializeNulls().create().toJson(fields)
        execute(request("https://share.tryapps.store/mock/teach/tasks/$id")
            .patch(json.toRequestBody("application/json; charset=utf-8".toMediaType())).build())
    }

    private fun request(url: String): Request.Builder {
        if (!isConfigured) throw TaskApiFailure(TaskApiError.NOT_CONFIGURED)
        return Request.Builder().url(url).header("Accept", "application/json").header("Authorization", "Bearer ${token.trim()}")
    }

    private suspend fun execute(request: Request): TaskHttpResponse {
        try {
            val response = http.execute(request)
            if (response.status !in 200..299) throw TaskApiFailure(when (response.status) {
                401, 403 -> TaskApiError.UNAUTHORIZED
                404 -> TaskApiError.NOT_FOUND
                429 -> TaskApiError.RATE_LIMITED
                else -> TaskApiError.SERVER
            })
            return response
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (known: TaskApiFailure) {
            throw known
        } catch (_: InterruptedIOException) {
            throw TaskApiFailure(TaskApiError.TIMEOUT)
        } catch (_: IOException) {
            throw TaskApiFailure(TaskApiError.NETWORK)
        }
    }
}
