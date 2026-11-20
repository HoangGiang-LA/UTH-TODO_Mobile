package com.example.uth_todo.data.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.ClearCredentialException
import com.example.uth_todo.model.AuthError
import com.example.uth_todo.model.AuthFailure
import com.example.uth_todo.model.AuthSession
import com.example.uth_todo.model.UserProfile
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

class FirebaseAuthRepository(context: Context) : AuthRepository {
    private val appContext = context.applicationContext
    private val auth: FirebaseAuth? = (
        FirebaseApp.getApps(appContext).firstOrNull { it.name == FirebaseApp.DEFAULT_APP_NAME }
            ?: FirebaseApp.initializeApp(appContext)
        )?.let { FirebaseAuth.getInstance(it) }
    private val mutableSession = MutableStateFlow<AuthSession>(
        if (auth == null) AuthSession.SignedOut else AuthSession.Loading,
    )
    override val session = mutableSession.asStateFlow()
    override val isConfigured: Boolean get() = auth != null

    private val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        mutableSession.value = when {
            user != null -> AuthSession.SignedIn(user.toProfile())
            mutableSession.value == AuthSession.Guest -> AuthSession.Guest
            else -> AuthSession.SignedOut
        }
    }

    init { auth?.addAuthStateListener(listener) }

    override suspend fun signInWithGoogle(idToken: String) = firebaseOperation {
        requireAuth().signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
        refreshSession()
    }

    override suspend fun signInWithEmail(email: String, password: String) = firebaseOperation {
        requireAuth().signInWithEmailAndPassword(email, password).await()
        refreshSession()
    }

    override suspend fun registerWithEmail(email: String, password: String) = firebaseOperation {
        requireAuth().createUserWithEmailAndPassword(email, password).await()
        refreshSession()
    }

    override suspend fun sendPasswordReset(email: String) = firebaseOperation {
        requireAuth().sendPasswordResetEmail(email).await()
    }

    override fun continueAsGuest() {
        if (mutableSession.value == AuthSession.SignedOut) mutableSession.value = AuthSession.Guest
    }

    override suspend fun signOut() {
        auth?.signOut()
        mutableSession.value = AuthSession.SignedOut
        try {
            withTimeoutOrNull(3_000) {
                CredentialManager.create(appContext).clearCredentialState(ClearCredentialStateRequest())
            }
        } catch (_: ClearCredentialException) {
            // Firebase/local logout is already complete. A provider cleanup
            // failure must not leave the app displaying a signed-in account.
        }
    }

    override fun close() { auth?.removeAuthStateListener(listener) }

    private fun requireAuth(): FirebaseAuth = auth ?: throw AuthFailure(AuthError.SERVICE_UNAVAILABLE)

    private fun refreshSession() {
        mutableSession.value = requireAuth().currentUser?.let { AuthSession.SignedIn(it.toProfile()) }
            ?: AuthSession.SignedOut
    }

    private suspend fun firebaseOperation(operation: suspend () -> Unit) {
        try {
            operation()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (known: AuthFailure) {
            throw known
        } catch (_: FirebaseNetworkException) {
            throw AuthFailure(AuthError.NETWORK)
        } catch (_: FirebaseTooManyRequestsException) {
            throw AuthFailure(AuthError.TOO_MANY_REQUESTS)
        } catch (failure: FirebaseAuthException) {
            throw AuthFailure(when (failure.errorCode) {
                "ERROR_INVALID_EMAIL" -> AuthError.INVALID_EMAIL
                "ERROR_EMAIL_ALREADY_IN_USE", "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" -> AuthError.EMAIL_IN_USE
                "ERROR_WEAK_PASSWORD" -> AuthError.WEAK_PASSWORD
                "ERROR_USER_DISABLED" -> AuthError.ACCOUNT_DISABLED
                "ERROR_OPERATION_NOT_ALLOWED" -> AuthError.PROVIDER_DISABLED
                "ERROR_TOO_MANY_REQUESTS" -> AuthError.TOO_MANY_REQUESTS
                "ERROR_INVALID_CREDENTIAL", "ERROR_INVALID_LOGIN_CREDENTIALS", "ERROR_WRONG_PASSWORD", "ERROR_USER_NOT_FOUND" -> AuthError.INVALID_CREDENTIALS
                else -> AuthError.UNKNOWN
            })
        }
    }
}

private fun FirebaseUser.toProfile() = UserProfile(uid, displayName, email, isEmailVerified)
