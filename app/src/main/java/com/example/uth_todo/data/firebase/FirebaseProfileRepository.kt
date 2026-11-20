package com.example.uth_todo.data.firebase

import android.content.Context
import com.example.uth_todo.model.CloudProfile
import com.example.uth_todo.model.FirebaseServiceError
import com.example.uth_todo.model.FirebaseServiceFailure
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirebaseProfileRepository(context: Context) : ProfileRepository {
    private val app = FirebaseApp.getApps(context.applicationContext)
        .firstOrNull { it.name == FirebaseApp.DEFAULT_APP_NAME }
    // The URL comes from the real google-services.json after creating RTDB.
    // Do not guess a URL from the project ID: regional databases differ.
    private val database = app?.options?.databaseUrl?.takeIf { it.isNotBlank() }?.let { url ->
        FirebaseDatabase.getInstance(requireNotNull(app), url)
    }
    override val isConfigured: Boolean get() = database != null

    override fun observe(uid: String): Flow<CloudProfile?> = callbackFlow {
        val reference = profileReference(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val name = snapshot.child("displayName").value as? String
                trySend(name?.let {
                    CloudProfile(it, (snapshot.child("updatedAt").value as? Number)?.toLong())
                })
            }

            override fun onCancelled(error: DatabaseError) {
                close(FirebaseServiceFailure(error.toServiceError()))
            }
        }
        reference.addValueEventListener(listener)
        awaitClose { reference.removeEventListener(listener) }
    }

    override suspend fun saveDisplayName(uid: String, displayName: String) {
        // updateChildren preserves any future fields on the profile.
        val reference = profileReference(uid)
        suspendCancellableCoroutine<Unit> { continuation ->
            reference.updateChildren(mapOf(
                "displayName" to displayName,
                "updatedAt" to ServerValue.TIMESTAMP,
            )) { error, _ ->
                if (continuation.isActive) {
                    if (error == null) continuation.resume(Unit)
                    else continuation.resumeWithException(FirebaseServiceFailure(error.toServiceError()))
                }
            }
        }
    }

    private fun profileReference(uid: String): DatabaseReference {
        val db = database ?: throw FirebaseServiceFailure(FirebaseServiceError.NOT_CONFIGURED)
        if (FirebaseAuth.getInstance(requireNotNull(app)).currentUser?.uid != uid) {
            throw FirebaseServiceFailure(FirebaseServiceError.PERMISSION_DENIED)
        }
        return db.reference.child("users").child(uid).child("profile")
    }
}

private fun DatabaseError.toServiceError() = when (code) {
    DatabaseError.PERMISSION_DENIED, DatabaseError.EXPIRED_TOKEN, DatabaseError.INVALID_TOKEN ->
        FirebaseServiceError.PERMISSION_DENIED
    DatabaseError.DISCONNECTED, DatabaseError.NETWORK_ERROR, DatabaseError.UNAVAILABLE ->
        FirebaseServiceError.NETWORK
    else -> FirebaseServiceError.UNKNOWN
}
