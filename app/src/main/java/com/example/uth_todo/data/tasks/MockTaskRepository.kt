package com.example.uth_todo.data.tasks

import com.example.uth_todo.model.TaskApiError
import com.example.uth_todo.model.TaskApiFailure
import com.example.uth_todo.model.TodoTask
import com.google.gson.Gson
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
        require(id > 0)
        execute(request("https://share.tryapps.store/mock/teach/tasks/$id")
            .patch(Gson().toJson(mapOf("done" to done)).toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build())
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
