package com.example.uth_todo.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uth_todo.data.preferences.OnboardingPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

enum class PreferencesError { READ, SAVE }

data class OnboardingUiState(
    val isLoading: Boolean = true,
    val isCompleted: Boolean = false,
    val isSaving: Boolean = false,
    val error: PreferencesError? = null,
)

class OnboardingViewModel(private val preferences: OnboardingPreferences) : ViewModel() {
    private val mutableState = MutableStateFlow(OnboardingUiState())
    val uiState = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            val minimumSplash = launch { delay(1_000) }
            var readError: PreferencesError? = null
            val completed = try {
                withTimeout(4_000) { preferences.completed.first() }
            } catch (_: TimeoutCancellationException) {
                readError = PreferencesError.READ
                false
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                readError = PreferencesError.READ
                false
            }
            minimumSplash.join()
            mutableState.value = OnboardingUiState(isLoading = false, isCompleted = completed, error = readError)
        }
    }

    fun completeOnboarding() {
        val current = mutableState.value
        if (current.isLoading || current.isSaving || current.isCompleted) return
        mutableState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            try {
                withTimeout(4_000) { preferences.complete() }
                mutableState.update { it.copy(isCompleted = true, isSaving = false) }
            } catch (_: TimeoutCancellationException) {
                mutableState.update { it.copy(isSaving = false, error = PreferencesError.SAVE) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.update { it.copy(isSaving = false, error = PreferencesError.SAVE) }
            }
        }
    }

    fun dismissError() {
        mutableState.update { it.copy(error = null) }
    }
}
