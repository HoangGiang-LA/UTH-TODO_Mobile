package com.example.uth_todo.data.tasks

import com.example.uth_todo.model.AuthSession
import java.security.MessageDigest

object TaskSessionScope {
    fun from(session: AuthSession): String? = when (session) {
        AuthSession.Guest -> "guest"
        is AuthSession.SignedIn -> forUid(session.user.uid)
        else -> null
    }
    fun forUid(uid: String): String = "user_" + MessageDigest.getInstance("SHA-256")
        .digest(uid.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it.toInt() and 255) }
    fun isValid(scope: String): Boolean = scope == "guest" || scope.matches(Regex("user_[0-9a-f]{64}"))
}
