package com.example.uth_todo.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.onboardingDataStore by preferencesDataStore(
    name = "onboarding",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

class OnboardingPreferences(private val dataStore: DataStore<Preferences>) {
    constructor(context: Context) : this(context.applicationContext.onboardingDataStore)

    private val completedKey = booleanPreferencesKey("onboarding_completed")
    val completed: Flow<Boolean> = dataStore.data.map { it[completedKey] ?: false }

    suspend fun complete() {
        dataStore.edit { it[completedKey] = true }
    }
}
