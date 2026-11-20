package com.example.uth_todo.data.tasks

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile

private object AndroidTaskStorage {
    private var store: DataStore<Preferences>? = null
    private var storage: DeviceTaskStorage? = null
    @Synchronized fun get(context: Context): DataStore<Preferences> = store
        ?: createTaskPreferencesStore(context.applicationContext.preferencesDataStoreFile("personal_tasks")).also { store = it }
    @Synchronized fun storage(context: Context): DeviceTaskStorage = storage
        ?: DeviceTaskStorage(get(context)).also { storage = it }
}

fun androidDeviceTaskStorage(context: Context): DeviceTaskStorage = AndroidTaskStorage.storage(context)

fun androidTaskRepository(context: Context, mockToken: String): SessionTaskRepository =
    SessionTaskRepository(androidDeviceTaskStorage(context), MockTaskRepository(mockToken))
