package com.example.uth_todo.data.auth

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.uth_todo.model.AuthError
import com.example.uth_todo.model.AuthFailure
import com.example.uth_todo.model.SignInCancelled
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

class GoogleCredentialClient {
    suspend fun getIdToken(activity: ComponentActivity): String {
        val resourceId = activity.resources.getIdentifier("default_web_client_id", "string", activity.packageName)
        val clientId = if (resourceId == 0) "" else activity.getString(resourceId)
        if (clientId.isBlank()) throw AuthFailure(AuthError.SERVICE_UNAVAILABLE)
        // This is an explicit button action, not the passive bottom-sheet flow.
        // It can offer account addition/reauthentication when necessary.
        val option = GetSignInWithGoogleOption.Builder(clientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        try {
            val credential = CredentialManager.create(activity).getCredential(activity, request).credential
            if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                throw AuthFailure(AuthError.GOOGLE_UNAVAILABLE)
            }
            return GoogleIdTokenCredential.createFrom(credential.data).idToken
        } catch (_: GetCredentialCancellationException) {
            throw SignInCancelled()
        } catch (_: NoCredentialException) {
            Log.w("UthGoogleAuth", "No available Google credential")
            throw AuthFailure(AuthError.GOOGLE_ACCOUNT_REQUIRED)
        } catch (failure: GetCredentialException) {
            // Log only the failure type, never the token or account details.
            Log.w("UthGoogleAuth", "Credential request failed: ${failure.type}")
            throw AuthFailure(AuthError.GOOGLE_UNAVAILABLE)
        }
    }
}
