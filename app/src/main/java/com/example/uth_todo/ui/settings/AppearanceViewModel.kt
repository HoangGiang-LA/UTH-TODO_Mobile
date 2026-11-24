package com.example.uth_todo.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uth_todo.data.preferences.AppearancePreferences
import com.example.uth_todo.model.ThemeMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

enum class AppearanceError { READ, SAVE }

data class AppearanceUiState(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: AppearanceError? = null,
)

class AppearanceViewModel(private val preferences: AppearancePreferences) : ViewModel() {
    private val state = MutableStateFlow(AppearanceUiState())
    val uiState = state.asStateFlow()

    init { read() }

    private fun read() {
        state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val saved = withTimeout(4_000) { preferences.mode.first() }
                state.value = AppearanceUiState(mode = saved, isLoading = false)
            } catch (_: TimeoutCancellationException) {
                state.update { it.copy(isLoading = false, error = AppearanceError.READ) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                state.update { it.copy(isLoading = false, error = AppearanceError.READ) }
            }
        }
    }

    fun retryRead() {
        val current = state.value
        if (!current.isLoading && !current.isSaving && current.error == AppearanceError.READ) read()
    }

    fun select(mode: ThemeMode) {
        val current = state.value
        if (current.isLoading || current.isSaving || (current.mode == mode && current.error == null)) return
        state.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            try {
                withTimeout(4_000) { preferences.save(mode) }
                // Only publish the new theme after it has been persisted.
                state.update { it.copy(mode = mode, isSaving = false) }
            } catch (_: TimeoutCancellationException) {
                state.update { it.copy(isSaving = false, error = AppearanceError.SAVE) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                state.update { it.copy(isSaving = false, error = AppearanceError.SAVE) }
            }
        }
    }
}
