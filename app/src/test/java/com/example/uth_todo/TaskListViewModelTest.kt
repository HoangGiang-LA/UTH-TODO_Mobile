package com.example.uth_todo

import androidx.lifecycle.SavedStateHandle
import com.example.uth_todo.data.tasks.TaskDates
import com.example.uth_todo.data.tasks.TaskJsonDecoder
import com.example.uth_todo.model.TaskFilter
import com.example.uth_todo.navigation.AppDestination
import com.example.uth_todo.ui.tasklist.TaskListViewModel
import org.junit.Assert.*
import org.junit.Test

class TaskListViewModelTest {
    private val tasks = TaskJsonDecoder().decodeList(taskFixture())

    @Test fun restoredSearchAndCategoryProduceTheSameVisibleList() {
        val saved = SavedStateHandle(mapOf(AppDestination.TaskList.CATEGORY_ARGUMENT to TaskFilter.STUDY.name))
        val first = TaskListViewModel(saved)
        first.setQuery("on chuong")
        val restored = TaskListViewModel(SavedStateHandle(mapOf(
            AppDestination.TaskList.CATEGORY_ARGUMENT to saved.get<String>(AppDestination.TaskList.CATEGORY_ARGUMENT),
            TaskListViewModel.QUERY_KEY to saved.get<String>(TaskListViewModel.QUERY_KEY),
        )))
        assertEquals(first.uiState.value.content(tasks), restored.uiState.value.content(tasks))
        assertEquals(listOf(2L), restored.uiState.value.content(tasks).pending.map { it.id })
    }

    @Test fun unknownCategoryArgumentFallsBackToAllInsteadOfCrashingOrHidingTasks() {
        val model = TaskListViewModel(SavedStateHandle(mapOf(AppDestination.TaskList.CATEGORY_ARGUMENT to "unknown")))
        val content = model.uiState.value.content(tasks)
        assertEquals(TaskFilter.ALL, model.uiState.value.filter)
        assertEquals(6, content.pending.size + content.completed.size)
    }

    @Test fun clearingSearchPreservesTheChosenCategory() {
        val model = TaskListViewModel(SavedStateHandle())
        model.setFilter(TaskFilter.LIFE)
        model.setQuery("does not match")
        assertTrue(model.uiState.value.content(tasks).isEmpty)
        model.clearSearch()
        assertEquals(TaskFilter.LIFE, model.uiState.value.filter)
        assertEquals(3, model.uiState.value.content(tasks).let { it.pending.size + it.completed.size })
    }

    @Test fun pendingCountReflectsTheSearchAndFilterInsteadOfTheGlobalCount() {
        val model = TaskListViewModel(SavedStateHandle())
        assertEquals(4, model.uiState.value.content(tasks).pending.size)
        model.setFilter(TaskFilter.STUDY)
        assertEquals(2, model.uiState.value.content(tasks).pending.size)
        model.setQuery("nop bai")
        val content = model.uiState.value.content(tasks)
        assertEquals(0, content.pending.size)
        assertEquals(listOf(1L), content.completed.map { it.id })
    }

    @Test fun acknowledgedCompletionMovesTheTaskBetweenGroupsWithoutLosingOrDuplicatingIt() {
        val model = TaskListViewModel(SavedStateHandle())
        model.setFilter(TaskFilter.STUDY)
        val updated = tasks.map { if (it.id == 2L) it.copy(done = true) else it }
        val before = model.uiState.value.content(tasks)
        val after = model.uiState.value.content(updated)
        assertEquals(before.pending.size - 1, after.pending.size)
        assertEquals(before.completed.size + 1, after.completed.size)
        assertEquals(1, (after.pending + after.completed).count { it.id == 2L })
    }

    @Test fun tomorrowLabelsRemainCorrectAcrossMonthAndYearBoundaries() {
        assertEquals("2024-02-29", TaskDates.nextDay("2024-02-28"))
        assertEquals("2026-03-01", TaskDates.nextDay("2026-02-28"))
        assertEquals("2027-01-01", TaskDates.nextDay("2026-12-31"))
    }
}
