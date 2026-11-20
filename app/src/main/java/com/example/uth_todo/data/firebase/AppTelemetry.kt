package com.example.uth_todo.data.firebase

import com.example.uth_todo.model.FirebaseServiceError

// Only fixed event names and error categories cross this boundary.
// Account identifiers, names, credentials and SDK exception messages do not.
interface AppTelemetry {
    fun screen(name: String)
    fun profileSaved()
    fun failure(operation: String, reason: FirebaseServiceError)

    object None : AppTelemetry {
        override fun screen(name: String) = Unit
        override fun profileSaved() = Unit
        override fun failure(operation: String, reason: FirebaseServiceError) = Unit
    }
}
