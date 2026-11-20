package com.example.uth_todo.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uth_todo.data.firebase.AppTelemetry
import com.example.uth_todo.data.firebase.HomeConfigRepository
import com.example.uth_todo.data.firebase.ProfileRepository
import com.example.uth_todo.model.AuthSession
import com.example.uth_todo.model.FirebaseServiceError
import com.example.uth_todo.model.FirebaseServiceFailure
import com.example.uth_todo.model.HomeConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

data class FirebaseServicesState(
    val uid: String? = null,
    val displayName: String? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val loadError: FirebaseServiceError? = null,
    val saveError: FirebaseServiceError? = null,
    val saved: Boolean = false,
    val homeConfig: HomeConfig = HomeConfig(),
)

class FirebaseServicesViewModel(
    private val profiles: ProfileRepository,
    private val homeConfig: HomeConfigRepository,
    private val telemetry: AppTelemetry = AppTelemetry.None,
) : ViewModel() {
    private val mutableState = MutableStateFlow(FirebaseServicesState())
    val uiState = mutableState.asStateFlow()
    private var observation: Job? = null
    private var loadingTimeout: Job? = null
    private var saving: Job? = null
    private var generation = 0

    init {
        viewModelScope.launch {
            try {
                val config = homeConfig.load()
                mutableState.update { it.copy(homeConfig = config) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Remote configuration never blocks startup or authentication.
                telemetry.failure("remote_config", FirebaseServiceError.UNKNOWN)
            }
        }
    }

    fun bindSession(session: AuthSession) {
        val uid = (session as? AuthSession.SignedIn)?.user?.uid
        if (uid == mutableState.value.uid) return
        generation++
        observation?.cancel()
        loadingTimeout?.cancel()
        saving?.cancel()
        mutableState.update { FirebaseServicesState(uid = uid, homeConfig = it.homeConfig) }
        if (uid != null) observeProfile(uid)
    }

    fun retryProfile() {
        val uid = mutableState.value.uid ?: return
        if (mutableState.value.isSaving) return
        observation?.cancel()
        loadingTimeout?.cancel()
        observeProfile(uid)
    }

    private fun observeProfile(uid: String) {
        if (!profiles.isConfigured) {
            mutableState.update { it.copy(loadError = FirebaseServiceError.NOT_CONFIGURED) }
            return
        }
        val currentGeneration = generation
        mutableState.update { it.copy(isLoading = true, loadError = null, saved = false) }
        loadingTimeout = viewModelScope.launch {
            delay(15_000)
            if (generation == currentGeneration && mutableState.value.isLoading) {
                mutableState.update { it.copy(isLoading = false, loadError = FirebaseServiceError.TIMEOUT) }
            }
        }
        observation = viewModelScope.launch {
            profiles.observe(uid).catch { failure ->
                if (generation == currentGeneration) {
                    loadingTimeout?.cancel()
                    val reason = (failure as? FirebaseServiceFailure)?.reason ?: FirebaseServiceError.UNKNOWN
                    mutableState.update { it.copy(isLoading = false, loadError = reason) }
                    telemetry.failure("profile_read", reason)
                }
            }.collect { profile ->
                if (generation == currentGeneration) {
                    loadingTimeout?.cancel()
                    mutableState.update { it.copy(
                        displayName = profile?.displayName, isLoading = false,
                        // A listener may fire for a local pending write. It does
                        // not acknowledge the write or clear a save failure.
                        loadError = null,
                    ) }
                }
            }
        }
    }

    fun saveDisplayName(value: String) {
        val state = mutableState.value
        val uid = state.uid ?: return
        if (state.isSaving || state.isLoading || !profiles.isConfigured) return
        val name = value.trim()
        if (name.isEmpty() || name.length > 60 || name.any { it.isISOControl() }) {
            mutableState.update { it.copy(saveError = FirebaseServiceError.INVALID_NAME, saved = false) }
            return
        }
        val currentGeneration = generation
        mutableState.update { it.copy(isSaving = true, saved = false, saveError = null) }
        saving = viewModelScope.launch {
            try {
                withTimeout(15_000) { profiles.saveDisplayName(uid, name) }
                if (generation == currentGeneration) {
                    mutableState.update { it.copy(displayName = name, saved = true) }
                    telemetry.profileSaved()
                }
            } catch (_: TimeoutCancellationException) {
                if (generation == currentGeneration) {
                    mutableState.update { it.copy(saveError = FirebaseServiceError.TIMEOUT) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                if (generation == currentGeneration) {
                    val reason = (failure as? FirebaseServiceFailure)?.reason ?: FirebaseServiceError.UNKNOWN
                    mutableState.update { it.copy(saveError = reason) }
                    telemetry.failure("profile_save", reason)
                }
            } finally {
                if (generation == currentGeneration) mutableState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun screenOpened(route: String) { telemetry.screen(route) }
    fun dismissSaveResult() { mutableState.update { it.copy(saved = false, saveError = null) } }
}
