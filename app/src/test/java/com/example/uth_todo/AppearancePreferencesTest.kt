package com.example.uth_todo

import com.example.uth_todo.data.preferences.AppearancePreferences
import com.example.uth_todo.data.preferences.createAppearancePreferencesStore
import com.example.uth_todo.model.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AppearancePreferencesTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun systemFollowsDeviceWhileExplicitModesOverrideIt() {
        assertFalse(ThemeMode.SYSTEM.usesDarkColors(false))
        assertTrue(ThemeMode.SYSTEM.usesDarkColors(true))
        for (systemDark in listOf(false, true)) {
            assertFalse(ThemeMode.LIGHT.usesDarkColors(systemDark))
            assertTrue(ThemeMode.DARK.usesDarkColors(systemDark))
        }
    }

    @Test fun missingOrUnknownStoredModeUsesSystem() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStored(null))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStored("future_mode"))
    }

    @Test(timeout = 10_000) fun allModesSurviveClosingAndReopeningRealDataStore() = runBlocking {
        val file = folder.root.resolve("appearance.preferences_pb")
        var expected = ThemeMode.SYSTEM
        for (next in listOf(ThemeMode.DARK, ThemeMode.LIGHT, ThemeMode.SYSTEM, null)) {
            val job = SupervisorJob()
            val store = createAppearancePreferencesStore(file, CoroutineScope(Dispatchers.IO + job))
            try {
                val preferences = AppearancePreferences(store)
                assertEquals(expected, preferences.mode.first())
                if (next != null) {
                    preferences.save(next)
                    expected = next
                }
            } finally { job.cancelAndJoin() }
        }
    }
}
