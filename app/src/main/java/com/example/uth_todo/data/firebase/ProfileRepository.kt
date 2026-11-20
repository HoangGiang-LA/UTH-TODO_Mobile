package com.example.uth_todo.data.firebase

import com.example.uth_todo.model.CloudProfile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    val isConfigured: Boolean
    fun observe(uid: String): Flow<CloudProfile?>
    suspend fun saveDisplayName(uid: String, displayName: String)
}
