package com.example.uth_todo

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModelStore
import com.example.uth_todo.data.preferences.AppearancePreferences
import com.example.uth_todo.model.ThemeMode
import com.example.uth_todo.ui.settings.AppearanceError
import com.example.uth_todo.ui.settings.AppearanceViewModel
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppearanceViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val models = ViewModelStore()
    private var nextId = 0

    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { models.clear(); Dispatchers.resetMain() }

    private fun model(store: Store) = AppearanceViewModel(AppearancePreferences(store)).also { models.put("appearance${nextId++}", it) }

    @Test fun startupReadsSavedModeWithoutWriting() = runTest(dispatcher) {
        val store = Store(ThemeMode.LIGHT)
        val model = model(store)
        assertTrue(model.uiState.value.isLoading)
        advanceUntilIdle()
        assertFalse(model.uiState.value.isLoading)
        assertEquals(ThemeMode.LIGHT, model.uiState.value.mode)
        assertEquals(0, store.writes)
    }

    @Test fun selectionPublishesOnlyAfterOneSuccessfulWrite() = runTest(dispatcher) {
        val store = Store(ThemeMode.LIGHT, saveDelay = 300)
        val model = model(store)
        advanceUntilIdle()
        model.select(ThemeMode.DARK)
        repeat(5) { model.select(ThemeMode.SYSTEM) }
        runCurrent()
        assertTrue(model.uiState.value.isSaving)
        assertEquals(ThemeMode.LIGHT, model.uiState.value.mode)
        advanceUntilIdle()
        assertEquals(1, store.writes)
        assertEquals(ThemeMode.DARK, model.uiState.value.mode)
        assertFalse(model.uiState.value.isSaving)
        val recreated = model(store)
        advanceUntilIdle()
        assertEquals(ThemeMode.DARK, recreated.uiState.value.mode)
    }

    @Test fun failedWriteKeepsPreviousThemeAndCanRetry() = runTest(dispatcher) {
        val store = Store(ThemeMode.LIGHT, failSave = true)
        val model = model(store)
        advanceUntilIdle()
        model.select(ThemeMode.DARK)
        advanceUntilIdle()
        assertEquals(ThemeMode.LIGHT, model.uiState.value.mode)
        assertEquals(AppearanceError.SAVE, model.uiState.value.error)
        assertFalse(model.uiState.value.isSaving)
        store.failSave = false
        model.select(ThemeMode.DARK)
        advanceUntilIdle()
        assertEquals(ThemeMode.DARK, model.uiState.value.mode)
        assertNull(model.uiState.value.error)
    }

    @Test fun failedReadReleasesStartupAndRetryRestoresSavedMode() = runTest(dispatcher) {
        val store = Store(ThemeMode.DARK, failRead = true)
        val model = model(store)
        advanceUntilIdle()
        assertFalse(model.uiState.value.isLoading)
        assertEquals(ThemeMode.SYSTEM, model.uiState.value.mode)
        assertEquals(AppearanceError.READ, model.uiState.value.error)
        assertEquals(0, store.writes)
        store.failRead = false
        model.retryRead()
        advanceUntilIdle()
        assertEquals(ThemeMode.DARK, model.uiState.value.mode)
        assertNull(model.uiState.value.error)
    }

    @Test fun explicitSystemChoiceAfterReadFailureCanRetryFailedSave() = runTest(dispatcher) {
        val store = Store(ThemeMode.DARK, failRead = true, failSave = true)
        val model = model(store)
        advanceUntilIdle()
        model.select(ThemeMode.SYSTEM)
        advanceUntilIdle()
        assertEquals(AppearanceError.SAVE, model.uiState.value.error)
        store.failSave = false
        model.select(ThemeMode.SYSTEM)
        advanceUntilIdle()
        assertNull(model.uiState.value.error)
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStored(store.values.value[stringPreferencesKey("theme_mode")]))
    }

    @Test fun stalledReadCannotHoldSplashIndefinitely() = runTest(dispatcher) {
        val model = model(Store(stallRead = true))
        advanceUntilIdle()
        assertFalse(model.uiState.value.isLoading)
        assertEquals(AppearanceError.READ, model.uiState.value.error)
    }

    @Test fun stalledSaveReleasesControlsAndKeepsPreviousMode() = runTest(dispatcher) {
        val model = model(Store(ThemeMode.LIGHT, saveDelay = 10_000))
        advanceUntilIdle()
        model.select(ThemeMode.DARK)
        advanceUntilIdle()
        assertFalse(model.uiState.value.isSaving)
        assertEquals(ThemeMode.LIGHT, model.uiState.value.mode)
        assertEquals(AppearanceError.SAVE, model.uiState.value.error)
    }

    @Test fun unchangedSelectionDoesNotWriteOrResetState() = runTest(dispatcher) {
        val store = Store(ThemeMode.DARK)
        val model = model(store)
        advanceUntilIdle()
        model.select(ThemeMode.DARK)
        model.retryRead()
        advanceUntilIdle()
        assertEquals(0, store.writes)
        assertEquals(ThemeMode.DARK, model.uiState.value.mode)
    }

    private class Store(mode: ThemeMode? = null, var failRead: Boolean = false, var failSave: Boolean = false,
        val stallRead: Boolean = false, val saveDelay: Long = 0) : DataStore<Preferences> {
        val values = MutableStateFlow(mode?.let { preferencesOf(stringPreferencesKey("theme_mode") to it.name) } ?: emptyPreferences())
        var writes = 0
        override val data: Flow<Preferences> = flow {
            if (stallRead) awaitCancellation()
            if (failRead) throw IOException("read failed")
            emit(values.value)
        }
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            writes++
            delay(saveDelay)
            if (failSave) throw IOException("save failed")
            return transform(values.value).also { values.value = it }
        }
    }
}
