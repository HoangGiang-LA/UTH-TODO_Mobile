package com.example.uth_todo

import androidx.lifecycle.ViewModelStore
import com.example.uth_todo.data.firebase.AppTelemetry
import com.example.uth_todo.data.firebase.HomeConfigRepository
import com.example.uth_todo.data.firebase.ProfileRepository
import com.example.uth_todo.model.AuthSession
import com.example.uth_todo.model.CloudProfile
import com.example.uth_todo.model.FirebaseServiceError
import com.example.uth_todo.model.FirebaseServiceFailure
import com.example.uth_todo.model.HomeConfig
import com.example.uth_todo.model.UserProfile
import com.example.uth_todo.ui.profile.FirebaseServicesViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FirebaseServicesViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val models = ViewModelStore()
    private val profiles = FakeProfiles()
    private val telemetry = FakeTelemetry()
    private var config = HomeConfig()
    private var configFails = false

    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { models.clear(); Dispatchers.resetMain() }

    private fun model() = FirebaseServicesViewModel(profiles, HomeConfigRepository {
        if (configFails) error("test configuration unavailable")
        config
    }, telemetry).also { models.put("services", it) }

    private fun session(uid: String) = AuthSession.SignedIn(UserProfile(uid, "Original name", null, false))

    @Test fun guestAndSignedOutNeverReadOrWriteCloudProfiles() = runTest(dispatcher) {
        val model = model()
        model.bindSession(AuthSession.Guest)
        model.saveDisplayName("Guest name")
        model.bindSession(AuthSession.SignedOut)
        advanceUntilIdle()
        assertTrue(profiles.observed.isEmpty())
        assertTrue(profiles.writes.isEmpty())
        assertNull(model.uiState.value.uid)
    }

    @Test fun missingDatabaseDoesNotPreventAnAuthenticatedSession() = runTest(dispatcher) {
        profiles.isConfigured = false
        val model = model()
        model.bindSession(session("alice"))
        advanceUntilIdle()
        assertEquals("alice", model.uiState.value.uid)
        assertEquals(FirebaseServiceError.NOT_CONFIGURED, model.uiState.value.loadError)
        assertFalse(model.uiState.value.isLoading)
        model.saveDisplayName("Alice")
        assertTrue(profiles.writes.isEmpty())
    }

    @Test fun cloudNameContinuesUpdatingWithoutRebindingTheSameUser() = runTest(dispatcher) {
        val model = model()
        model.bindSession(session("alice"))
        runCurrent()
        profiles.emit("alice", "First name")
        runCurrent()
        model.bindSession(session("alice"))
        profiles.emit("alice", "Updated on another device")
        runCurrent()
        assertEquals("Updated on another device", model.uiState.value.displayName)
        assertEquals(listOf("alice"), profiles.observed)
    }

    @Test fun saveWaitsForServerAcknowledgementAndIgnoresRepeatedTaps() = runTest(dispatcher) {
        profiles.saveGate = CompletableDeferred()
        val model = model()
        model.bindSession(session("alice"))
        runCurrent()
        profiles.emit("alice", null)
        runCurrent()
        repeat(5) { model.saveDisplayName("  Nguyễn Giang  ") }
        runCurrent()
        profiles.emit("alice", "Nguyễn Giang") // RTDB local pending-write event.
        runCurrent()
        assertTrue(model.uiState.value.isSaving)
        assertFalse(model.uiState.value.saved)
        assertEquals(listOf("alice" to "Nguyễn Giang"), profiles.writes)
        assertEquals(0, telemetry.savedCount)
        profiles.saveGate!!.complete(Unit)
        runCurrent()
        assertTrue(model.uiState.value.saved)
        assertFalse(model.uiState.value.isSaving)
        assertEquals(1, telemetry.savedCount)
    }

    @Test fun invalidNamesNeverReachTheDatabase() = runTest(dispatcher) {
        val model = model()
        model.bindSession(session("alice"))
        runCurrent()
        profiles.emit("alice", null)
        runCurrent()
        for (value in listOf("   ", "a".repeat(61), "Alice\nSmith")) {
            model.saveDisplayName(value)
            assertEquals(FirebaseServiceError.INVALID_NAME, model.uiState.value.saveError)
        }
        assertTrue(profiles.writes.isEmpty())
    }

    @Test fun rejectedSaveDoesNotReportSuccessAndCanBeRetried() = runTest(dispatcher) {
        profiles.saveFailure = FirebaseServiceError.PERMISSION_DENIED
        val model = model()
        model.bindSession(session("alice"))
        runCurrent()
        profiles.emit("alice", "Old name")
        runCurrent()
        model.saveDisplayName("New name")
        runCurrent()
        assertFalse(model.uiState.value.saved)
        assertEquals("Old name", model.uiState.value.displayName)
        assertEquals(FirebaseServiceError.PERMISSION_DENIED, model.uiState.value.saveError)
        profiles.saveFailure = null
        model.saveDisplayName("New name")
        runCurrent()
        assertTrue(model.uiState.value.saved)
        assertNull(model.uiState.value.saveError)
    }

    @Test fun changingAccountClearsOldProfileAndCancelsOldWritesAndListeners() = runTest(dispatcher) {
        profiles.saveGate = CompletableDeferred()
        val model = model()
        model.bindSession(session("alice"))
        runCurrent()
        profiles.emit("alice", "Alice private name")
        runCurrent()
        model.saveDisplayName("Pending Alice name")
        runCurrent()
        model.bindSession(session("bob"))
        assertNull(model.uiState.value.displayName)
        assertFalse(model.uiState.value.isSaving)
        runCurrent()
        profiles.emit("alice", "Late Alice event")
        profiles.saveGate!!.complete(Unit)
        profiles.emit("bob", "Bob name")
        runCurrent()
        assertEquals("bob", model.uiState.value.uid)
        assertEquals("Bob name", model.uiState.value.displayName)
        assertFalse(model.uiState.value.saved)
        assertEquals(setOf("bob"), profiles.active)
        assertEquals(0, telemetry.savedCount)
    }

    @Test fun signingOutDropsAccountDataAndDetachesItsListener() = runTest(dispatcher) {
        val model = model()
        model.bindSession(session("alice"))
        runCurrent()
        profiles.emit("alice", "Private name")
        runCurrent()
        model.bindSession(AuthSession.SignedOut)
        runCurrent()
        assertNull(model.uiState.value.uid)
        assertNull(model.uiState.value.displayName)
        assertTrue(profiles.active.isEmpty())
    }

    @Test fun aReadFailureCanBeRetriedWithoutAnotherLogin() = runTest(dispatcher) {
        profiles.readFailure = FirebaseServiceError.PERMISSION_DENIED
        val model = model()
        model.bindSession(session("alice"))
        runCurrent()
        assertEquals(FirebaseServiceError.PERMISSION_DENIED, model.uiState.value.loadError)
        profiles.readFailure = null
        model.retryProfile()
        runCurrent()
        profiles.emit("alice", "Recovered")
        runCurrent()
        assertEquals("Recovered", model.uiState.value.displayName)
        assertNull(model.uiState.value.loadError)
    }

    @Test fun slowReadsReleaseTheSpinnerButKeepListeningForRecovery() = runTest(dispatcher) {
        val model = model()
        model.bindSession(session("alice"))
        runCurrent()
        advanceTimeBy(15_001)
        runCurrent()
        assertFalse(model.uiState.value.isLoading)
        assertEquals(FirebaseServiceError.TIMEOUT, model.uiState.value.loadError)
        profiles.emit("alice", "Arrived later")
        runCurrent()
        assertEquals("Arrived later", model.uiState.value.displayName)
        assertNull(model.uiState.value.loadError)
    }

    @Test fun saveTimeoutIsNotAcknowledgedByALaterLocalListenerEvent() = runTest(dispatcher) {
        profiles.saveGate = CompletableDeferred()
        val model = model()
        model.bindSession(session("alice"))
        runCurrent()
        profiles.emit("alice", "Old name")
        runCurrent()
        model.saveDisplayName("Pending name")
        advanceTimeBy(15_001)
        runCurrent()
        profiles.emit("alice", "Pending name")
        runCurrent()
        assertFalse(model.uiState.value.isSaving)
        assertFalse(model.uiState.value.saved)
        assertEquals(FirebaseServiceError.TIMEOUT, model.uiState.value.saveError)
        assertEquals(0, telemetry.savedCount)
    }

    @Test fun remoteConfigurationFailureKeepsTheDefaultAndAllowsProfilesToLoad() = runTest(dispatcher) {
        configFails = true
        val model = model()
        model.bindSession(session("alice"))
        runCurrent()
        profiles.emit("alice", "Alice")
        runCurrent()
        assertEquals(HomeConfig(), model.uiState.value.homeConfig)
        assertEquals("Alice", model.uiState.value.displayName)
    }

    @Test fun publicAnnouncementCanLoadWithoutReadingAPrivateProfile() = runTest(dispatcher) {
        config = HomeConfig("Chúc bạn một ngày học tập hiệu quả!")
        val model = model()
        model.bindSession(AuthSession.Guest)
        advanceUntilIdle()
        assertEquals(config, model.uiState.value.homeConfig)
        assertTrue(profiles.observed.isEmpty())
    }

    @Test fun clearingViewModelDetachesTheDatabaseListener() = runTest(dispatcher) {
        val model = model()
        model.bindSession(session("alice"))
        runCurrent()
        assertEquals(setOf("alice"), profiles.active)
        models.clear()
        runCurrent()
        assertTrue(profiles.active.isEmpty())
    }
}

private class FakeProfiles : ProfileRepository {
    override var isConfigured = true
    val observed = mutableListOf<String>()
    val active = mutableSetOf<String>()
    val writes = mutableListOf<Pair<String, String>>()
    var readFailure: FirebaseServiceError? = null
    var saveFailure: FirebaseServiceError? = null
    var saveGate: CompletableDeferred<Unit>? = null
    private val values = mutableMapOf<String, MutableSharedFlow<CloudProfile?>>()
    private fun stream(uid: String) = values.getOrPut(uid) { MutableSharedFlow(replay = 1) }

    override fun observe(uid: String): Flow<CloudProfile?> = flow {
        observed += uid
        readFailure?.let { throw FirebaseServiceFailure(it) }
        active += uid
        try { emitAll(stream(uid)) } finally { active -= uid }
    }

    fun emit(uid: String, name: String?) { stream(uid).tryEmit(name?.let { CloudProfile(it, 123L) }) }

    override suspend fun saveDisplayName(uid: String, displayName: String) {
        writes += uid to displayName
        saveGate?.await()
        saveFailure?.let { throw FirebaseServiceFailure(it) }
    }
}

private class FakeTelemetry : AppTelemetry {
    var savedCount = 0
    override fun screen(name: String) = Unit
    override fun profileSaved() { savedCount++ }
    override fun failure(operation: String, reason: FirebaseServiceError) = Unit
}
