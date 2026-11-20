package com.example.uth_todo

import androidx.lifecycle.ViewModelStore
import com.example.uth_todo.data.auth.AuthRepository
import com.example.uth_todo.model.AuthError
import com.example.uth_todo.model.AuthFailure
import com.example.uth_todo.model.AuthNotice
import com.example.uth_todo.model.AuthSession
import com.example.uth_todo.model.SignInCancelled
import com.example.uth_todo.model.UserProfile
import com.example.uth_todo.ui.auth.AuthViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
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
class AuthViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val models = ViewModelStore()
    private val repository = FakeAuthRepository()

    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { models.clear(); Dispatchers.resetMain() }
    private fun model() = AuthViewModel(repository).also { models.put("auth", it) }

    @Test fun missingConfigurationDoesNotOpenGoogleOrPretendToSignIn() = runTest(dispatcher) {
        repository.isConfigured = false
        val model = model()
        var chooserOpened = false
        model.signInWithGoogle { chooserOpened = true; "test-token" }
        advanceUntilIdle()
        assertFalse(chooserOpened)
        assertEquals(0, repository.signInCalls)
        assertEquals(AuthSession.SignedOut, model.uiState.value.session)
        assertEquals(AuthNotice.Error(AuthError.SERVICE_UNAVAILABLE), model.uiState.value.notice)
    }

    @Test fun googleCancellationLeavesNoSuccessOrErrorAndReleasesButtons() = runTest(dispatcher) {
        val model = model()
        model.signInWithGoogle { throw SignInCancelled() }
        advanceUntilIdle()
        assertFalse(model.uiState.value.isBusy)
        assertNull(model.uiState.value.notice)
        assertEquals(AuthSession.SignedOut, model.uiState.value.session)
    }

    @Test fun noGoogleCredentialDoesNotCallFirebaseAndCanRetry() = runTest(dispatcher) {
        val model = model()
        model.signInWithGoogle { throw AuthFailure(AuthError.GOOGLE_ACCOUNT_REQUIRED) }
        advanceUntilIdle()
        assertFalse(model.uiState.value.isBusy)
        assertEquals(0, repository.signInCalls)
        assertEquals(AuthSession.SignedOut, model.uiState.value.session)
        assertEquals(AuthNotice.Error(AuthError.GOOGLE_ACCOUNT_REQUIRED), model.uiState.value.notice)
        model.signInWithGoogle { "test-google-token" }
        advanceUntilIdle()
        assertEquals(1, repository.signInCalls)
        assertTrue(model.uiState.value.session is AuthSession.SignedIn)
    }

    @Test fun googleTokenIsExchangedBeforeSuccessIsReported() = runTest(dispatcher) {
        repository.operationDelay = 500
        val model = model()
        model.signInWithGoogle { "test-google-token" }
        runCurrent()
        assertTrue(model.uiState.value.isBusy)
        assertEquals(AuthSession.SignedOut, model.uiState.value.session)
        assertNull(model.uiState.value.notice)
        advanceUntilIdle()
        assertEquals("test-google-token", repository.lastGoogleToken)
        assertTrue(model.uiState.value.session is AuthSession.SignedIn)
        assertEquals(AuthNotice.SignedIn, model.uiState.value.notice)
    }

    @Test fun repeatedTapsWaitForOneConfirmedSignIn() = runTest(dispatcher) {
        repository.operationDelay = 500
        val model = model()
        repeat(10) { model.signInWithEmail(" student@example.com ", "secret-password") }
        runCurrent()
        assertTrue(model.uiState.value.isBusy)
        assertEquals(AuthSession.SignedOut, model.uiState.value.session)
        advanceUntilIdle()
        assertEquals(1, repository.signInCalls)
        assertEquals("student@example.com", repository.lastEmail)
        assertTrue(model.uiState.value.session is AuthSession.SignedIn)
        assertEquals(AuthNotice.SignedIn, model.uiState.value.notice)
    }

    @Test fun networkFailureDoesNotCreateSessionAndCanRetry() = runTest(dispatcher) {
        repository.failure = AuthError.NETWORK
        val model = model()
        model.signInWithEmail("student@example.com", "password")
        advanceUntilIdle()
        assertEquals(AuthSession.SignedOut, model.uiState.value.session)
        assertEquals(AuthNotice.Error(AuthError.NETWORK), model.uiState.value.notice)
        repository.failure = null
        model.signInWithEmail("student@example.com", "password")
        advanceUntilIdle()
        assertTrue(model.uiState.value.session is AuthSession.SignedIn)
    }

    @Test fun invalidRegistrationNeverCallsFirebase() = runTest(dispatcher) {
        val model = model()
        model.registerWithEmail("bad-email", "password", "password")
        assertEquals(AuthNotice.Error(AuthError.INVALID_EMAIL), model.uiState.value.notice)
        model.registerWithEmail("student@example.com", "123", "123")
        assertEquals(AuthNotice.Error(AuthError.WEAK_PASSWORD), model.uiState.value.notice)
        model.registerWithEmail("student@example.com", "password", "different")
        assertEquals(AuthNotice.Error(AuthError.PASSWORD_MISMATCH), model.uiState.value.notice)
        assertEquals(0, repository.signInCalls)
    }

    @Test fun registrationUsesTheRepositoryConfirmedSession() = runTest(dispatcher) {
        val model = model()
        model.registerWithEmail("student@example.com", "password", "password")
        advanceUntilIdle()
        assertTrue(model.uiState.value.session is AuthSession.SignedIn)
        assertEquals(AuthNotice.Registered, model.uiState.value.notice)
    }

    @Test fun resetPasswordDoesNotSignTheUserIn() = runTest(dispatcher) {
        val model = model()
        model.sendPasswordReset("student@example.com")
        advanceUntilIdle()
        assertEquals(1, repository.resetCalls)
        assertEquals(AuthSession.SignedOut, model.uiState.value.session)
        assertEquals(AuthNotice.PasswordResetSent, model.uiState.value.notice)
    }

    @Test fun guestWorksWithoutFirebaseAndIsNeverASignedInUser() = runTest(dispatcher) {
        repository.isConfigured = false
        val model = model()
        model.continueAsGuest()
        advanceUntilIdle()
        assertEquals(AuthSession.Guest, model.uiState.value.session)
        assertEquals(0, repository.signInCalls)
        model.signOut()
        advanceUntilIdle()
        assertEquals(AuthSession.SignedOut, model.uiState.value.session)
    }

    @Test fun existingSessionIsRestoredAndLogoutClearsIt() = runTest(dispatcher) {
        repository.session.value = AuthSession.SignedIn(FakeAuthRepository.user)
        val model = model()
        advanceUntilIdle()
        assertTrue(model.uiState.value.session is AuthSession.SignedIn)
        model.signOut()
        advanceUntilIdle()
        assertEquals(AuthSession.SignedOut, model.uiState.value.session)
        assertEquals(AuthNotice.SignedOut, model.uiState.value.notice)
    }

    @Test fun hungOperationTimesOutAndReleasesBusyState() = runTest(dispatcher) {
        repository.operationDelay = 120_000
        val model = model()
        model.signInWithEmail("student@example.com", "password")
        advanceUntilIdle()
        assertFalse(model.uiState.value.isBusy)
        assertEquals(AuthSession.SignedOut, model.uiState.value.session)
        assertEquals(AuthNotice.Error(AuthError.TIMEOUT), model.uiState.value.notice)
    }
}

private class FakeAuthRepository : AuthRepository {
    companion object { val user = UserProfile("test-uid", "Student", "student@example.com", true) }
    override val session = MutableStateFlow<AuthSession>(AuthSession.SignedOut)
    override var isConfigured = true
    var operationDelay = 0L
    var failure: AuthError? = null
    var signInCalls = 0
    var resetCalls = 0
    var lastEmail: String? = null
    var lastGoogleToken: String? = null
    private suspend fun signIn() {
        signInCalls++
        delay(operationDelay)
        failure?.let { throw AuthFailure(it) }
        session.value = AuthSession.SignedIn(user)
    }
    override suspend fun signInWithGoogle(idToken: String) { lastGoogleToken = idToken; signIn() }
    override suspend fun signInWithEmail(email: String, password: String) { lastEmail = email; signIn() }
    override suspend fun registerWithEmail(email: String, password: String) { lastEmail = email; signIn() }
    override suspend fun sendPasswordReset(email: String) { resetCalls++ }
    override fun continueAsGuest() { session.value = AuthSession.Guest }
    override suspend fun signOut() { session.value = AuthSession.SignedOut }
}
