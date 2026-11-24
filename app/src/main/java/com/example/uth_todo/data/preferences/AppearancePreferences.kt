package com.example.uth_todo.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.PreferencesSerializer
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.example.uth_todo.model.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.map
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import java.io.File

private object AndroidAppearanceStore {
    private var store: DataStore<Preferences>? = null
    @Synchronized fun get(context: Context): DataStore<Preferences> = store
        ?: createAppearancePreferencesStore(context.applicationContext.preferencesDataStoreFile("appearance")).also { store = it }
}

// Use the same file backend on Android and in persistence tests on Windows.
fun createAppearancePreferencesStore(file: File,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
): DataStore<Preferences> = PreferenceDataStoreFactory.create(scope = scope,
    storage = OkioStorage(fileSystem = FileSystem.SYSTEM, serializer = PreferencesSerializer,
        producePath = { file.absoluteFile.toOkioPath() }))

class AppearancePreferences(private val store: DataStore<Preferences>) {
    constructor(context: Context) : this(AndroidAppearanceStore.get(context))

    private val modeKey = stringPreferencesKey("theme_mode")
    val mode = store.data.map { ThemeMode.fromStored(it[modeKey]) }

    suspend fun save(mode: ThemeMode) {
        store.edit { it[modeKey] = mode.name }
    }
}
