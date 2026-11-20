package com.example.uth_todo.model

data class CloudProfile(val displayName: String, val updatedAt: Long?)

enum class FirebaseServiceError {
    NOT_CONFIGURED, PERMISSION_DENIED, NETWORK, TIMEOUT, INVALID_NAME, UNKNOWN,
}

class FirebaseServiceFailure(val reason: FirebaseServiceError) : Exception(reason.name)

data class HomeConfig(val announcement: String? = null)
