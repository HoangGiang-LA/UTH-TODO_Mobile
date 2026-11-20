package com.example.uth_todo.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uth_todo.data.auth.AuthRepository
import com.example.uth_todo.model.AuthError
import com.example.uth_todo.model.AuthFailure
import com.example.uth_todo.model.AuthNotice
import com.example.uth_todo.model.AuthSession
import com.example.uth_todo.model.SignInCancelled
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

data class AuthUiState(
    val session: AuthSession,
    val isBusy: Boolean = false,
    val notice: AuthNotice? = null,
)

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(AuthUiState(repository.session.value))
    val uiState = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.session.collect { session -> mutableState.update { it.copy(session = session) } }
        }
    }

    fun signInWithGoogle(tokenProvider: suspend () -> String) = authenticate(AuthNotice.SignedIn) {
        repository.signInWithGoogle(tokenProvider())
    }

    fun signInWithEmail(email: String, password: String) {
        if (!validEmail(email)) return showError(AuthError.INVALID_EMAIL)
        if (password.isBlank()) return showError(AuthError.INVALID_CREDENTIALS)
        authenticate(AuthNotice.SignedIn) { repository.signInWithEmail(email.trim(), password) }
    }

    fun registerWithEmail(email: String, password: String, confirmation: String) {
        if (!validEmail(email)) return showError(AuthError.INVALID_EMAIL)
        if (password.length < 6) return showError(AuthError.WEAK_PASSWORD)
        if (password != confirmation) return showError(AuthError.PASSWORD_MISMATCH)
        authenticate(AuthNotice.Registered) { repository.registerWithEmail(email.trim(), password) }
    }

    fun sendPasswordReset(email: String) {
        if (!validEmail(email)) return showError(AuthError.INVALID_EMAIL)
        authenticate(AuthNotice.PasswordResetSent) { repository.sendPasswordReset(email.trim()) }
    }

    fun continueAsGuest() {
        if (mutableState.value.isBusy || repository.session.value != AuthSession.SignedOut) return
        mutableState.update { it.copy(notice = null) }
        repository.continueAsGuest()
    }

    fun signOut() = runOperation(AuthNotice.SignedOut) { repository.signOut() }

    fun dismissNotice(expected: AuthNotice? = mutableState.value.notice) {
        mutableState.update { if (it.notice == expected) it.copy(notice = null) else it }
    }

    private fun authenticate(success: AuthNotice, operation: suspend () -> Unit) {
        if (mutableState.value.isBusy || repository.session.value != AuthSession.SignedOut) return
        if (!repository.isConfigured) return showError(AuthError.SERVICE_UNAVAILABLE)
        runOperation(success, operation)
    }

    private fun runOperation(success: AuthNotice, operation: suspend () -> Unit) {
        if (mutableState.value.isBusy) return
        mutableState.update { it.copy(isBusy = true, notice = null) }
        viewModelScope.launch {
            try {
                withTimeout(90_000) { operation() }
                mutableState.update { it.copy(notice = success) }
            } catch (_: TimeoutCancellationException) {
                showError(AuthError.TIMEOUT)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: SignInCancelled) {
                // Closing Google's chooser is not a failed login.
            } catch (failure: AuthFailure) {
                showError(failure.reason)
            } catch (_: Exception) {
                showError(AuthError.UNKNOWN)
            } finally {
                mutableState.update { it.copy(isBusy = false) }
            }
        }
    }

    private fun showError(reason: AuthError) { mutableState.update { it.copy(notice = AuthNotice.Error(reason)) } }

    private fun validEmail(email: String): Boolean = email.trim().matches(Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))

    override fun onCleared() { repository.close() }
}
