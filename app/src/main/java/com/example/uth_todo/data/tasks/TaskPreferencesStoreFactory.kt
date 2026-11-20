package com.example.uth_todo.data.tasks

import androidx.datastore.core.DataStore
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okio.Path.Companion.toOkioPath
import okio.FileSystem
import java.io.File

/** Shared Android/JVM factory; the owner must keep one active store per file. */
fun createTaskPreferencesStore(
    file: File,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
): DataStore<Preferences> = PreferenceDataStoreFactory.create(scope = scope,
    storage = OkioStorage(fileSystem = FileSystem.SYSTEM, serializer = PreferencesSerializer,
        producePath = { file.absoluteFile.toOkioPath() }))
