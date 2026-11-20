package com.example.uth_todo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesOf
import androidx.lifecycle.ViewModelStore
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.espresso.Espresso.pressBack
import com.example.uth_todo.data.preferences.OnboardingPreferences
import com.example.uth_todo.data.auth.AuthRepository
import com.example.uth_todo.model.AuthSession
import com.example.uth_todo.model.UserProfile
import com.example.uth_todo.ui.auth.AuthViewModel
import androidx.test.platform.app.InstrumentationRegistry
import com.example.uth_todo.navigation.AppDestination
import com.example.uth_todo.navigation.UthTodoApp
import com.example.uth_todo.ui.onboarding.OnboardingScreen
import com.example.uth_todo.ui.onboarding.OnboardingViewModel
import com.example.uth_todo.ui.theme.UTHTODOTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class OnboardingFlowTest {
    @get:Rule val compose = createComposeRule()
    private val models = ViewModelStore()
    private lateinit var navController: NavHostController

    @After fun clearViewModels() { compose.runOnIdle { models.clear() } }

    private fun launchFlow(completed: Boolean = false, initialSession: AuthSession = AuthSession.SignedOut, configured: Boolean = false) {
        compose.setContent {
            navController = rememberNavController()
            val model = androidx.compose.runtime.remember {
                OnboardingViewModel(OnboardingPreferences(MemoryPreferences(completed))).also { models.put("onboarding", it) }
            }
            val auth = androidx.compose.runtime.remember {
                AuthViewModel(UiAuthRepository(initialSession, configured)).also { models.put("auth", it) }
            }
            UTHTODOTheme { UthTodoApp(model, auth, navController) }
        }
        compose.waitUntil(6_000) {
            navController.currentDestination?.route ==
                (if (!completed) AppDestination.Onboarding.route
                    else if (initialSession is AuthSession.SignedIn || initialSession == AuthSession.Guest) AppDestination.Home.route
                    else AppDestination.Welcome.route)
        }
        compose.waitForIdle()
    }

    @Test fun nextSwipeAndBackFollowTheThreePages() {
        launchFlow()
        compose.onNodeWithTag("page_title_0").assertIsDisplayed()
        compose.onNodeWithTag("primary_button").performClick()
        compose.onNodeWithTag("page_title_1").assertIsDisplayed()
        compose.onNodeWithTag("onboarding_pager").performTouchInput { swipeLeft() }
        compose.onNodeWithTag("page_title_2").assertIsDisplayed()
        compose.onNodeWithTag("page_indicator").assertContentDescriptionEquals("Trang 3 trên 3")
        pressBack()
        compose.onNodeWithTag("page_title_1").assertIsDisplayed()
        pressBack()
        compose.onNodeWithTag("page_title_0").assertIsDisplayed()
        compose.runOnIdle { assertNull(navController.previousBackStackEntry) }
    }

    @Test fun skipClearsIntroductionAndMissingConfigurationReportsAnError() {
        launchFlow()
        compose.onNodeWithTag("skip_button").performClick()
        compose.onNodeWithTag("welcome_title").assertIsDisplayed()
        compose.runOnIdle { assertNull(navController.previousBackStackEntry) }
        compose.onNodeWithTag("primary_button").performClick()
        compose.onNodeWithText(InstrumentationRegistry.getInstrumentation().targetContext
            .getString(R.string.auth_error_service)).assertIsDisplayed()
        compose.runOnIdle { assertEquals(AppDestination.Welcome.route, navController.currentDestination?.route) }
    }

    @Test fun getStartedClearsIntroduction() {
        launchFlow()
        repeat(3) { compose.onNodeWithTag("primary_button").performClick(); compose.waitForIdle() }
        compose.onNodeWithTag("welcome_title").assertIsDisplayed()
        compose.runOnIdle { assertNull(navController.previousBackStackEntry) }
    }

    @Test fun savedCompletionSkipsOnboarding() {
        launchFlow(completed = true)
        compose.onNodeWithTag("welcome_title").assertIsDisplayed()
        compose.runOnIdle { assertEquals(AppDestination.Welcome.route, navController.currentDestination?.route) }
    }

    @Test fun pageSurvivesSavedStateRestoration() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { UTHTODOTheme { OnboardingScreen(isSaving = false, onComplete = {}) } }
        compose.onNodeWithTag("primary_button").performClick()
        compose.onNodeWithTag("page_title_1").assertIsDisplayed()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("page_title_1").assertIsDisplayed()
    }

    @Test fun guestCanOpenProfileAndReturnToLoginWithNoProtectedBackStack() {
        launchFlow(completed = true)
        compose.onNodeWithTag("guest_button").performClick()
        compose.onNodeWithTag("home_title").assertIsDisplayed()
        compose.onNodeWithTag("open_profile").performScrollTo().performClick()
        compose.onNodeWithTag("profile_title").assertIsDisplayed()
        compose.onNodeWithTag("sign_out_button").performScrollTo().performClick()
        compose.onNodeWithTag("welcome_title").assertIsDisplayed()
        compose.runOnIdle { assertNull(navController.previousBackStackEntry) }
    }

    @Test fun restoredUserSeesHomeProfileAndCanSignOut() {
        val user = UserProfile("test-user", "Student", "student@example.com", true)
        launchFlow(completed = true, initialSession = AuthSession.SignedIn(user))
        compose.onNodeWithTag("home_title").assertIsDisplayed()
        compose.onNodeWithTag("open_profile").performScrollTo().performClick()
        compose.onNodeWithText(user.email!!).assertIsDisplayed()
        compose.onNodeWithTag("sign_out_button").performScrollTo().performClick()
        compose.onNodeWithTag("welcome_title").assertIsDisplayed()
        compose.runOnIdle { assertNull(navController.previousBackStackEntry) }
    }

    @Test fun emailFormIsConnectedToRepositoryAndHomeNavigation() {
        launchFlow(completed = true, configured = true)
        compose.onNodeWithTag("email_button").performClick()
        compose.onNodeWithTag("auth_email").performTextInput("student@example.com")
        compose.onNodeWithTag("auth_password").performTextInput("test-password")
        compose.onNodeWithTag("primary_button").performScrollTo().performClick()
        compose.onNodeWithTag("home_title").assertIsDisplayed()
        compose.runOnIdle { assertNull(navController.previousBackStackEntry) }
    }

    @Test fun smallViewportWithLargeFontKeepsActionsReachable() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                UTHTODOTheme {
                    Box(Modifier.requiredSize(320.dp, 480.dp)) {
                        OnboardingScreen(isSaving = false, onComplete = {})
                    }
                }
            }
        }
        compose.onNodeWithTag("skip_button").assertIsDisplayed()
        compose.onNodeWithTag("primary_button").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithTag("page_indicator").assertContentDescriptionEquals("Trang 2 trên 3")
    }

}

private class MemoryPreferences(completed: Boolean = false) : DataStore<Preferences> {
    override val data = MutableStateFlow(
        if (completed) preferencesOf(booleanPreferencesKey("onboarding_completed") to true) else emptyPreferences(),
    )
    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        return transform(data.value).also { data.value = it }
    }
}

// UI-only fake: no Firebase calls, account chooser, or user's app data.
private class UiAuthRepository(initial: AuthSession, override val isConfigured: Boolean) : AuthRepository {
    override val session = MutableStateFlow(initial)
    override suspend fun signInWithGoogle(idToken: String) { signInWithEmail("student@example.com", "unused") }
    override suspend fun signInWithEmail(email: String, password: String) {
        session.value = AuthSession.SignedIn(UserProfile("test-user", "Student", email, true))
    }
    override suspend fun registerWithEmail(email: String, password: String) { signInWithEmail(email, password) }
    override suspend fun sendPasswordReset(email: String) = Unit
    override fun continueAsGuest() { session.value = AuthSession.Guest }
    override suspend fun signOut() { session.value = AuthSession.SignedOut }
}
