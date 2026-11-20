package com.example.uth_todo.data.tasks

import com.example.uth_todo.model.TaskApiError
import com.example.uth_todo.model.TaskApiFailure
import com.example.uth_todo.model.TaskCategory
import com.example.uth_todo.model.TaskPriority
import com.example.uth_todo.model.TodoSubtask
import com.example.uth_todo.model.TodoTask
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.Strictness

class TaskJsonDecoder {
    private val gson = GsonBuilder().setStrictness(Strictness.STRICT).create()

    fun decodeList(json: String): List<TodoTask> {
        try {
            val root = gson.fromJson(json, JsonElement::class.java)
            require(root != null && root.isJsonArray)
            val tasks = root.asJsonArray.map { decodeTask(it.asJsonObject) }
            require(tasks.map { it.id }.toSet().size == tasks.size)
            return tasks
        } catch (_: Exception) {
            // A malformed row is a response error, not an empty task list.
            throw TaskApiFailure(TaskApiError.INVALID_RESPONSE)
        }
    }

    private fun decodeTask(value: JsonObject): TodoTask {
        val idValue = value.get("id")
        require(idValue != null && idValue.isJsonPrimitive && idValue.asJsonPrimitive.isNumber)
        val id = idValue.asString.takeIf { it.matches(Regex("[0-9]+")) }?.toLongOrNull()
        require(id != null && id > 0)
        val title = value.text("title", required = true)
        require(title.isNotBlank())
        val date = value.text("date").takeIf { it.isNotBlank() }
        val time = value.text("time").takeIf { it.isNotBlank() }
        require(date == null || TaskDates.isValidDate(date))
        require(time == null || TaskDates.isValidTime(time))
        val subtasksValue = value.get("subtasks")
        val subtasks = if (subtasksValue == null || subtasksValue.isJsonNull) emptyList() else {
            require(subtasksValue.isJsonArray)
            subtasksValue.asJsonArray.map { item ->
                val subtask = item.asJsonObject
                TodoSubtask(subtask.text("title", required = true), subtask.boolean("done"))
            }
        }
        return TodoTask(id, title, when (value.text("category", required = true)) {
            "study" -> TaskCategory.STUDY
            "life" -> TaskCategory.LIFE
            else -> TaskCategory.OTHER
        }, date, time, value.boolean("done"), value.text("note"), when (value.text("priority")) {
            "low" -> TaskPriority.LOW
            "medium", "" -> TaskPriority.MEDIUM
            "high" -> TaskPriority.HIGH
            else -> TaskPriority.OTHER
        }, value.boolean("remind", default = false), subtasks)
    }

    private fun JsonObject.text(key: String, required: Boolean = false): String {
        val element = get(key)
        if (element == null || element.isJsonNull) {
            require(!required)
            return ""
        }
        require(element.isJsonPrimitive && element.asJsonPrimitive.isString)
        return element.asString
    }

    private fun JsonObject.boolean(key: String, default: Boolean? = null): Boolean {
        val element = get(key)
        if (element == null || element.isJsonNull) return requireNotNull(default)
        require(element.isJsonPrimitive && element.asJsonPrimitive.isBoolean)
        return element.asBoolean
    }
}
