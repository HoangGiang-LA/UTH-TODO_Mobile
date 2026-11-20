package com.example.uth_todo.navigation

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.LocalActivity
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.WindowInsets
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel as composeViewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.uth_todo.R
import com.example.uth_todo.data.auth.GoogleCredentialClient
import com.example.uth_todo.model.AuthError
import com.example.uth_todo.model.AuthFailure
import com.example.uth_todo.model.AuthSession
import com.example.uth_todo.model.TaskFilter
import com.example.uth_todo.ui.auth.AuthViewModel
import com.example.uth_todo.ui.auth.EmailAuthScreen
import com.example.uth_todo.ui.auth.messageResource
import com.example.uth_todo.ui.home.HomeScreen
import com.example.uth_todo.ui.home.HomeViewModel
import com.example.uth_todo.ui.home.HomeUiState
import com.example.uth_todo.ui.home.messageResource
import com.example.uth_todo.ui.tasklist.TaskListScreen
import com.example.uth_todo.ui.tasklist.TaskListViewModel
import com.example.uth_todo.ui.taskdetail.TaskDetailScreen
import com.example.uth_todo.ui.taskdetail.TaskDetailUiState
import com.example.uth_todo.ui.profile.ProfileScreen
import com.example.uth_todo.ui.profile.FirebaseServicesViewModel
import com.example.uth_todo.ui.onboarding.OnboardingScreen
import com.example.uth_todo.ui.onboarding.OnboardingViewModel
import com.example.uth_todo.ui.onboarding.PreferencesError
import com.example.uth_todo.ui.splash.SplashScreen
import com.example.uth_todo.ui.welcome.WelcomeScreen

@Composable
fun UthTodoApp(
    viewModel: OnboardingViewModel,
    authViewModel: AuthViewModel,
    navController: NavHostController = rememberNavController(),
    servicesViewModel: FirebaseServicesViewModel? = null,
    homeViewModel: HomeViewModel? = null,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val servicesState = servicesViewModel?.uiState?.collectAsStateWithLifecycle()?.value
    val homeState = homeViewModel?.uiState?.collectAsStateWithLifecycle()?.value
    val entry by navController.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val activity = LocalActivity.current as? ComponentActivity
    val googleClient = remember { GoogleCredentialClient() }
    val readErrorMessage = stringResource(R.string.preferences_read_error)
    val saveErrorMessage = stringResource(R.string.preferences_save_error)
    val retryLabel = stringResource(R.string.retry)
    val authNotice = authState.notice
    val authNoticeMessage = authNotice?.let { stringResource(it.messageResource()) }
    val taskNotice = homeState?.notice
    val taskNoticeMessage = taskNotice?.let { stringResource(it.messageResource()) }
    val splashVisible = route == null || route == AppDestination.Splash.route

    LaunchedEffect(authState.session, servicesViewModel) {
        servicesViewModel?.bindSession(authState.session)
    }
    LaunchedEffect(authState.session, homeViewModel) { homeViewModel?.bindSession(authState.session) }
    LaunchedEffect(taskNotice, taskNoticeMessage) {
        val notice = taskNotice ?: return@LaunchedEffect
        val message = taskNoticeMessage ?: return@LaunchedEffect
        snackbar.showSnackbar(message, withDismissAction = true)
        homeViewModel?.dismissNotice(notice)
    }
    LaunchedEffect(route, servicesViewModel) {
        route?.let { servicesViewModel?.screenOpened(it) }
    }

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
    LaunchedEffect(state.isLoading, state.isCompleted, authState.session, authState.isBusy, route) {
        if (!state.isLoading && authState.session != AuthSession.Loading && !authState.isBusy) {
            val hasSession = authState.session is AuthSession.SignedIn || authState.session == AuthSession.Guest
            val target = when {
                route == AppDestination.Splash.route && !state.isCompleted -> AppDestination.Onboarding
                route == AppDestination.Splash.route && hasSession -> AppDestination.Home
                route == AppDestination.Splash.route -> AppDestination.Welcome
                route == AppDestination.Onboarding.route && state.isCompleted ->
                    if (hasSession) AppDestination.Home else AppDestination.Welcome
                hasSession && (route == AppDestination.Welcome.route || route == AppDestination.EmailAuth.route) -> AppDestination.Home
                !hasSession && AppDestination.requiresSession(route) -> AppDestination.Welcome
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

    LaunchedEffect(authNotice, authNoticeMessage) {
        val notice = authNotice ?: return@LaunchedEffect
        val message = authNoticeMessage ?: return@LaunchedEffect
        snackbar.currentSnackbarData?.dismiss()
        snackbar.showSnackbar(message, withDismissAction = true)
        authViewModel.dismissNotice(notice)
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
                    isBusy = authState.isBusy,
                    onGoogleClick = {
                        authViewModel.signInWithGoogle {
                            googleClient.getIdToken(activity ?: throw AuthFailure(AuthError.SERVICE_UNAVAILABLE))
                        }
                    },
                    onGuestClick = authViewModel::continueAsGuest,
                    onEmailClick = {
                        navController.navigate(AppDestination.EmailAuth.route) { launchSingleTop = true }
                    },
                )
            }
            composable(AppDestination.EmailAuth.route) {
                EmailAuthScreen(
                    isBusy = authState.isBusy,
                    onBack = { navController.popBackStack() },
                    onSignIn = authViewModel::signInWithEmail,
                    onRegister = authViewModel::registerWithEmail,
                    onResetPassword = authViewModel::sendPasswordReset,
                )
            }
            composable(AppDestination.Home.route) {
                LaunchedEffect(homeViewModel) { homeViewModel?.onTaskScreenOpened() }
                val uid = (authState.session as? AuthSession.SignedIn)?.user?.uid
                HomeScreen(authState.session,
                    onProfile = { navController.navigate(AppDestination.Profile.route) { launchSingleTop = true } },
                    onOpenList = { filter ->
                        if (navController.currentDestination?.route == AppDestination.Home.route) {
                            navController.navigate(AppDestination.TaskList.location(filter)) { launchSingleTop = true }
                        }
                    },
                    displayName = servicesState?.takeIf { uid != null && it.uid == uid }?.displayName,
                    onOpenTask = { id ->
                        if (navController.currentDestination?.route == AppDestination.Home.route)
                            navController.navigate(AppDestination.TaskDetail.location(id)) { launchSingleTop = true }
                    },
                    announcement = servicesState?.homeConfig?.announcement,
                    state = homeState ?: HomeUiState(error = com.example.uth_todo.model.TaskApiError.NOT_CONFIGURED),
                    onRefresh = { homeViewModel?.refresh() },
                    onToggle = { homeViewModel?.toggleDone(it) },
                    onSelectDate = { homeViewModel?.selectDate(it) })
            }
            composable(AppDestination.TaskList.route, arguments = listOf(
                navArgument(AppDestination.TaskList.CATEGORY_ARGUMENT) {
                    type = NavType.StringType
                    defaultValue = TaskFilter.ALL.name
                },
            )) {
                // The NavBackStackEntry's default factory supplies its SavedStateHandle,
                // including the initial category argument and restored search state.
                val listViewModel: TaskListViewModel = composeViewModel()
                val listState by listViewModel.uiState.collectAsStateWithLifecycle()
                LaunchedEffect(homeViewModel) { homeViewModel?.onTaskScreenOpened() }
                TaskListScreen(
                    tasksState = homeState ?: HomeUiState(error = com.example.uth_todo.model.TaskApiError.NOT_CONFIGURED),
                    listState = listState,
                    onQuery = listViewModel::setQuery,
                    onOpenTask = { id ->
                        if (navController.currentDestination?.route == AppDestination.TaskList.route)
                            navController.navigate(AppDestination.TaskDetail.location(id)) { launchSingleTop = true }
                    },
                    onFilter = listViewModel::setFilter,
                    onClearSearch = listViewModel::clearSearch,
                    onRefresh = { homeViewModel?.refresh() },
                    onToggle = { homeViewModel?.toggleDone(it) },
                    onHome = {
                        if (!navController.popBackStack(AppDestination.Home.route, inclusive = false)) {
                            navController.navigate(AppDestination.Home.route) {
                                popUpTo(navController.graph.id) { inclusive = false }
                                launchSingleTop = true
                            }
                        }
                    },
                    onProfile = { navController.navigate(AppDestination.Profile.route) { launchSingleTop = true } },
                )
            }
            composable(AppDestination.TaskDetail.route, arguments = listOf(
                navArgument(AppDestination.TaskDetail.ID_ARGUMENT) { type = NavType.LongType },
            )) { detailEntry ->
                val id = detailEntry.arguments?.getLong(AppDestination.TaskDetail.ID_ARGUMENT) ?: 0L
                val detail = homeState?.detail?.takeIf { it.entryKey == detailEntry.id }
                    ?: TaskDetailUiState(isLoading = homeViewModel != null,
                        error = if (homeViewModel == null) com.example.uth_todo.model.TaskApiError.NOT_CONFIGURED else null)
                val goBack: () -> Unit = {
                    if (!navController.popBackStack()) navController.navigate(AppDestination.Home.route) {
                        popUpTo(navController.graph.id) { inclusive = false }
                        launchSingleTop = true
                    }
                }
                LaunchedEffect(detailEntry.id, authState.session) { homeViewModel?.openTask(id, detailEntry.id) }
                LaunchedEffect(detail.deleted) { if (detail.deleted) goBack() }
                TaskDetailScreen(detail, onBack = goBack,
                    onRetry = { homeViewModel?.retryTaskDetail() }, onEdit = { homeViewModel?.beginTaskEdit() },
                    onDraft = { homeViewModel?.changeTaskDraft(it) }, onCancelEdit = { homeViewModel?.cancelTaskEdit() },
                    onSave = { homeViewModel?.saveTaskEdit() }, onToggle = { homeViewModel?.toggleDetailDone() },
                    onSubtask = { homeViewModel?.toggleSubtask(it) }, onDelete = { homeViewModel?.deleteTask() })
            }
            composable(AppDestination.Profile.route) {
                ProfileScreen(authState.session, authState.isBusy,
                    onBack = { navController.popBackStack() }, onSignOut = authViewModel::signOut,
                    servicesState = servicesState?.takeIf {
                        it.uid == (authState.session as? AuthSession.SignedIn)?.user?.uid
                    },
                    onSaveName = { servicesViewModel?.saveDisplayName(it) },
                    onRetryProfile = { servicesViewModel?.retryProfile() },
                    onNameEdited = { servicesViewModel?.dismissSaveResult() },
                    showNotifications = servicesViewModel != null)
            }
        }
    }
}
