package com.example.uth_todo.data.auth

import com.example.uth_todo.model.AuthSession
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository : AutoCloseable {
    val session: StateFlow<AuthSession>
    val isConfigured: Boolean
    suspend fun signInWithGoogle(idToken: String)
    suspend fun signInWithEmail(email: String, password: String)
    suspend fun registerWithEmail(email: String, password: String)
    suspend fun sendPasswordReset(email: String)
    fun continueAsGuest()
    suspend fun signOut()
    override fun close() = Unit
}
