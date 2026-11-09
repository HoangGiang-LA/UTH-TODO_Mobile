package com.example.uth_todo

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesOf
import androidx.lifecycle.ViewModelStore
import com.example.uth_todo.data.preferences.OnboardingPreferences
import com.example.uth_todo.ui.onboarding.OnboardingViewModel
import com.example.uth_todo.ui.onboarding.PreferencesError
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val viewModelStore = ViewModelStore()

    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { viewModelStore.clear(); Dispatchers.resetMain() }

    private fun model(store: TestPreferencesStore) = OnboardingViewModel(OnboardingPreferences(store)).also {
        viewModelStore.put("onboarding", it)
    }

    @Test fun newInstallDoesNotCompleteUntilUserFinishes() = runTest(dispatcher) {
        val store = TestPreferencesStore()
        val model = model(store)
        assertTrue(model.uiState.value.isLoading)
        advanceUntilIdle()
        assertFalse(model.uiState.value.isLoading)
        assertFalse(model.uiState.value.isCompleted)
        assertEquals(0, store.writeCount)
    }

    @Test fun returningUserGoesToWelcome() = runTest(dispatcher) {
        val model = model(TestPreferencesStore(completed = true))
        advanceUntilIdle()
        assertTrue(model.uiState.value.isCompleted)
    }

    @Test fun repeatedCompletionWaitsForOneSuccessfulWrite() = runTest(dispatcher) {
        val store = TestPreferencesStore(writeDelay = 300)
        val model = model(store)
        advanceUntilIdle()
        repeat(10) { model.completeOnboarding() }
        runCurrent()
        assertTrue(model.uiState.value.isSaving)
        assertFalse(model.uiState.value.isCompleted)
        advanceUntilIdle()
        assertEquals(1, store.writeCount)
        assertTrue(model.uiState.value.isCompleted)
        model.completeOnboarding()
        advanceUntilIdle()
        assertEquals(1, store.writeCount)
    }

    @Test fun failedWriteKeepsOnboardingAndCanRetry() = runTest(dispatcher) {
        val store = TestPreferencesStore(failWrite = true)
        val model = model(store)
        advanceUntilIdle()
        model.completeOnboarding()
        advanceUntilIdle()
        assertFalse(model.uiState.value.isCompleted)
        assertFalse(model.uiState.value.isSaving)
        assertEquals(PreferencesError.SAVE, model.uiState.value.error)
        store.failWrite = false
        model.completeOnboarding()
        advanceUntilIdle()
        assertTrue(model.uiState.value.isCompleted)
        assertNull(model.uiState.value.error)
    }

    @Test fun readFailureReleasesSplashAndShowsRecoverableError() = runTest(dispatcher) {
        val model = model(TestPreferencesStore(failRead = true))
        advanceUntilIdle()
        assertFalse(model.uiState.value.isLoading)
        assertFalse(model.uiState.value.isCompleted)
        assertEquals(PreferencesError.READ, model.uiState.value.error)
    }

    @Test fun stalledReadCannotKeepSplashForever() = runTest(dispatcher) {
        val model = model(TestPreferencesStore(stallRead = true))
        advanceUntilIdle()
        assertFalse(model.uiState.value.isLoading)
        assertEquals(PreferencesError.READ, model.uiState.value.error)
    }

    @Test fun stalledWriteReleasesButtonsForRetry() = runTest(dispatcher) {
        val model = model(TestPreferencesStore(writeDelay = 10_000))
        advanceUntilIdle()
        model.completeOnboarding()
        advanceUntilIdle()
        assertFalse(model.uiState.value.isSaving)
        assertFalse(model.uiState.value.isCompleted)
        assertEquals(PreferencesError.SAVE, model.uiState.value.error)
    }
}

private class TestPreferencesStore(
    completed: Boolean = false,
    private val failRead: Boolean = false,
    private val stallRead: Boolean = false,
    var failWrite: Boolean = false,
    private val writeDelay: Long = 0,
) : DataStore<Preferences> {
    private val values = MutableStateFlow(
        if (completed) preferencesOf(booleanPreferencesKey("onboarding_completed") to true) else emptyPreferences(),
    )
    var writeCount = 0
        private set
    override val data: Flow<Preferences> = flow {
        if (failRead) throw IOException("Test read failure")
        if (stallRead) awaitCancellation()
        values.collect { emit(it) }
    }
    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        writeCount++
        delay(writeDelay)
        if (failWrite) throw IOException("Test write failure")
        return transform(values.value).also { values.value = it }
    }
}
