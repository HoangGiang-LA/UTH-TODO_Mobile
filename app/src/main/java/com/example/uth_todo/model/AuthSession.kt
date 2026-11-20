package com.example.uth_todo.model

data class UserProfile(
    val uid: String,
    val displayName: String?,
    val email: String?,
    val isEmailVerified: Boolean,
)

sealed interface AuthSession {
    data object Loading : AuthSession
    data object SignedOut : AuthSession
    // Guest is local to this app process; it is not a Firebase user.
    data object Guest : AuthSession
    data class SignedIn(val user: UserProfile) : AuthSession
}

enum class AuthError {
    SERVICE_UNAVAILABLE, GOOGLE_UNAVAILABLE, GOOGLE_ACCOUNT_REQUIRED, NETWORK, INVALID_CREDENTIALS,
    EMAIL_IN_USE, WEAK_PASSWORD, INVALID_EMAIL, PASSWORD_MISMATCH,
    ACCOUNT_DISABLED, TOO_MANY_REQUESTS, PROVIDER_DISABLED, TIMEOUT, UNKNOWN,
}

class AuthFailure(val reason: AuthError) : Exception(reason.name)
class SignInCancelled : Exception()

sealed interface AuthNotice {
    data object SignedIn : AuthNotice
    data object Registered : AuthNotice
    data object PasswordResetSent : AuthNotice
    data object SignedOut : AuthNotice
    data class Error(val reason: AuthError) : AuthNotice
}
