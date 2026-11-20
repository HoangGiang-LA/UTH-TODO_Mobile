package com.example.uth_todo.ui.auth

import androidx.annotation.StringRes
import com.example.uth_todo.R
import com.example.uth_todo.model.AuthError
import com.example.uth_todo.model.AuthNotice

@StringRes
fun AuthNotice.messageResource(): Int = when (this) {
    AuthNotice.SignedIn -> R.string.auth_success
    AuthNotice.Registered -> R.string.auth_register_success
    AuthNotice.PasswordResetSent -> R.string.auth_reset_sent
    AuthNotice.SignedOut -> R.string.auth_signed_out
    is AuthNotice.Error -> when (reason) {
        AuthError.SERVICE_UNAVAILABLE -> R.string.auth_error_service
        AuthError.GOOGLE_UNAVAILABLE -> R.string.auth_error_google
        AuthError.GOOGLE_ACCOUNT_REQUIRED -> R.string.auth_error_google_account
        AuthError.NETWORK -> R.string.auth_error_network
        AuthError.INVALID_CREDENTIALS -> R.string.auth_error_credentials
        AuthError.EMAIL_IN_USE -> R.string.auth_error_email_used
        AuthError.WEAK_PASSWORD -> R.string.auth_error_weak_password
        AuthError.INVALID_EMAIL -> R.string.auth_error_email
        AuthError.PASSWORD_MISMATCH -> R.string.auth_error_confirmation
        AuthError.ACCOUNT_DISABLED -> R.string.auth_error_disabled
        AuthError.TOO_MANY_REQUESTS -> R.string.auth_error_attempts
        AuthError.PROVIDER_DISABLED -> R.string.auth_error_provider
        AuthError.TIMEOUT -> R.string.auth_error_timeout
        AuthError.UNKNOWN -> R.string.auth_error_unknown
    }
}
