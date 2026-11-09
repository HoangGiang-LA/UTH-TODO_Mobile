package com.example.uth_todo

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.uth_todo.data.preferences.OnboardingPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class OnboardingPreferencesTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test(timeout = 10_000) fun completedFlagSurvivesDataStoreRecreation() = runBlocking {
        val file = temporaryFolder.root.resolve("onboarding.preferences_pb")
        val firstJob = SupervisorJob()
        val firstStore = PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + firstJob), produceFile = { file })
        try {
            val preferences = OnboardingPreferences(firstStore)
            assertFalse(preferences.completed.first())
            preferences.complete()
            assertTrue(preferences.completed.first())
        } finally {
            firstJob.cancelAndJoin()
        }
        val secondJob = SupervisorJob()
        val secondStore = PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + secondJob), produceFile = { file })
        try {
            assertTrue(OnboardingPreferences(secondStore).completed.first())
        } finally {
            secondJob.cancelAndJoin()
        }
    }
}
