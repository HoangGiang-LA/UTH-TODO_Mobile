package com.example.uth_todo.navigation

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.WindowInsets
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.uth_todo.R
import com.example.uth_todo.ui.onboarding.OnboardingScreen
import com.example.uth_todo.ui.onboarding.OnboardingViewModel
import com.example.uth_todo.ui.onboarding.PreferencesError
import com.example.uth_todo.ui.splash.SplashScreen
import com.example.uth_todo.ui.welcome.WelcomeScreen
import kotlinx.coroutines.launch

@Composable
fun UthTodoApp(viewModel: OnboardingViewModel, navController: NavHostController = rememberNavController()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val entry by navController.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val googleMessage = stringResource(R.string.google_coming_soon)
    val guestMessage = stringResource(R.string.guest_coming_soon)
    val readErrorMessage = stringResource(R.string.preferences_read_error)
    val saveErrorMessage = stringResource(R.string.preferences_save_error)
    val retryLabel = stringResource(R.string.retry)
    val splashVisible = route == null || route == AppDestination.Splash.route

    SideEffect {
        (context as? ComponentActivity)?.enableEdgeToEdge(
            statusBarStyle = if (splashVisible) SystemBarStyle.dark(Color.Transparent.toArgb())
                else SystemBarStyle.light(Color.Transparent.toArgb(), Color.Transparent.toArgb()),
            navigationBarStyle = if (splashVisible) SystemBarStyle.dark(Color.Transparent.toArgb())
                else SystemBarStyle.light(Color.Transparent.toArgb(), Color.Transparent.toArgb()),
        )
    }

    // Wait for DataStore before choosing a destination. Remove the previous flow
    // from the stack so Back never reveals Splash or completed onboarding.
    LaunchedEffect(state.isLoading, state.isCompleted, route) {
        if (!state.isLoading) {
            val target = when {
                state.isCompleted -> AppDestination.Welcome
                route == AppDestination.Splash.route -> AppDestination.Onboarding
                else -> null
            }
            if (target != null && route != null && route != target.route) {
                navController.navigate(target.route) {
                    popUpTo(navController.graph.id) { inclusive = false }
                    launchSingleTop = true
                }
            }
        }
    }

    LaunchedEffect(state.error) {
        val error = state.error ?: return@LaunchedEffect
        val result = snackbar.showSnackbar(
            message = if (error == PreferencesError.READ) readErrorMessage else saveErrorMessage,
            actionLabel = if (error == PreferencesError.SAVE) retryLabel else null,
            withDismissAction = true,
        )
        viewModel.dismissError()
        if (error == PreferencesError.SAVE && result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
            viewModel.completeOnboarding()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        NavHost(navController, startDestination = AppDestination.Splash.route, modifier = Modifier.padding(padding)) {
            composable(AppDestination.Splash.route) { SplashScreen() }
            composable(AppDestination.Onboarding.route) {
                OnboardingScreen(isSaving = state.isSaving, onComplete = viewModel::completeOnboarding)
            }
            composable(AppDestination.Welcome.route) {
                WelcomeScreen(
                    onGoogleClick = { scope.launch { snackbar.currentSnackbarData?.dismiss(); snackbar.showSnackbar(googleMessage) } },
                    onGuestClick = { scope.launch { snackbar.currentSnackbarData?.dismiss(); snackbar.showSnackbar(guestMessage) } },
                )
            }
        }
    }
}
