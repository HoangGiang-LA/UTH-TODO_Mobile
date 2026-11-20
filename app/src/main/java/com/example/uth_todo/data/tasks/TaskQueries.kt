package com.example.uth_todo.data.tasks

import com.example.uth_todo.model.TaskCategory
import com.example.uth_todo.model.TaskDaySummary
import com.example.uth_todo.model.TaskFilter
import com.example.uth_todo.model.TodoTask
import java.text.Normalizer
import java.util.Locale

private val scheduleOrder = compareBy<TodoTask> { it.date ?: "9999-99-99" }
    .thenBy { it.time ?: "99:99" }.thenBy { it.id }

fun List<TodoTask>.forDate(date: String): List<TodoTask> = filter { it.date == date }.sortedWith(scheduleOrder)

fun List<TodoTask>.summary(date: String): TaskDaySummary {
    val day = filter { it.date == date }
    return TaskDaySummary(day.size, day.count { it.done },
        count { !it.done && it.category == TaskCategory.STUDY },
        count { !it.done && it.category == TaskCategory.LIFE })
}

fun List<TodoTask>.matching(filter: TaskFilter, query: String): List<TodoTask> {
    val search = query.searchKey()
    return filter { task ->
        (filter == TaskFilter.ALL || filter == TaskFilter.STUDY && task.category == TaskCategory.STUDY ||
            filter == TaskFilter.LIFE && task.category == TaskCategory.LIFE) &&
            (search.isEmpty() || task.title.searchKey().contains(search) || task.note.searchKey().contains(search))
    }.sortedWith(compareBy<TodoTask> { it.done }.then(scheduleOrder))
}

private fun String.searchKey(): String = Normalizer.normalize(this, Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "").replace('đ', 'd').replace('Đ', 'D').lowercase(Locale.ROOT).trim()
