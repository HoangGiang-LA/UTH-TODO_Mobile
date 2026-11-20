package com.example.uth_todo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
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
import com.example.uth_todo.data.tasks.TaskDates
import com.example.uth_todo.data.tasks.TaskRepository
import com.example.uth_todo.model.AuthSession
import com.example.uth_todo.model.UserProfile
import com.example.uth_todo.model.TaskCategory
import com.example.uth_todo.model.TodoTask
import com.example.uth_todo.ui.auth.AuthViewModel
import com.example.uth_todo.ui.home.HomeViewModel
import com.example.uth_todo.ui.home.HomeUiState
import com.example.uth_todo.ui.tasklist.TaskListScreen
import com.example.uth_todo.ui.tasklist.TaskListUiState
import androidx.test.platform.app.InstrumentationRegistry
import com.example.uth_todo.navigation.AppDestination
import com.example.uth_todo.navigation.UthTodoApp
import com.example.uth_todo.ui.onboarding.OnboardingScreen
import com.example.uth_todo.ui.onboarding.OnboardingViewModel
import com.example.uth_todo.ui.theme.UTHTODOTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.CompletableDeferred
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

    private fun launchFlow(completed: Boolean = false, initialSession: AuthSession = AuthSession.SignedOut,
        configured: Boolean = false, taskRepository: TaskRepository? = null, authRepository: AuthRepository? = null,
        reminders: com.example.uth_todo.notifications.TaskReminderController? = null,
        reminderRequests: MutableStateFlow<com.example.uth_todo.notifications.ReminderOpenRequest?>? = null) {
        compose.setContent {
            navController = rememberNavController()
            val model = androidx.compose.runtime.remember {
                OnboardingViewModel(OnboardingPreferences(MemoryPreferences(completed))).also { models.put("onboarding", it) }
            }
            val auth = androidx.compose.runtime.remember {
                AuthViewModel(authRepository ?: UiAuthRepository(initialSession, configured)).also { models.put("auth", it) }
            }
            val home = androidx.compose.runtime.remember(taskRepository) {
                taskRepository?.let { HomeViewModel(it, reminders = reminders).also { viewModel -> models.put("home", viewModel) } }
            }
            val reminderRequest = reminderRequests?.collectAsState()?.value
            UTHTODOTheme { UthTodoApp(model, auth, navController, homeViewModel = home, reminderController = reminders,
                reminderOpenRequest = reminderRequest, onReminderRequestHandled = { reminderRequests?.value = null }) }
        }
        compose.waitUntil(6_000) {
            navController.currentDestination?.route ==
                (if (!completed) AppDestination.Onboarding.route
                    else if (initialSession is AuthSession.SignedIn || initialSession == AuthSession.Guest) AppDestination.Home.route
                    else AppDestination.Welcome.route)
        }
        compose.waitForIdle()
        if (taskRepository != null) compose.waitUntil(6_000) {
            (models["home"] as? HomeViewModel)?.uiState?.value?.hasLoaded == true
        }
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

    @Test fun studyCardOpensASeparateScreenWithItsCategoryAndBackReturnsHome() {
        launchFlow(completed = true, initialSession = AuthSession.Guest, taskRepository = UiTaskRepository())
        compose.onNodeWithTag("category_study").performScrollTo().performClick()
        compose.onNodeWithTag("task_list_title").assertIsDisplayed()
        compose.onNodeWithTag("task_filter_STUDY").assertIsSelected()
        compose.onNodeWithTag("list_task_11").assertIsDisplayed()
        compose.onNodeWithTag("list_task_12").assertDoesNotExist()
        pressBack()
        compose.onNodeWithTag("home_title").assertIsDisplayed()
        compose.runOnIdle { assertEquals(AppDestination.Home.route, navController.currentDestination?.route) }
    }

    @Test fun viewAllAndListTabUseTheSameDestinationWithoutLeavingDuplicateHomeEntries() {
        launchFlow(completed = true, initialSession = AuthSession.Guest, taskRepository = UiTaskRepository())
        compose.onNodeWithTag("home_view_all").performScrollTo().performClick()
        compose.onNodeWithTag("task_filter_ALL").assertIsSelected()
        compose.onNodeWithTag("task_tab_home").performClick()
        compose.onNodeWithTag("task_tab_list").performClick()
        compose.onNodeWithTag("task_list_title").assertIsDisplayed()
        pressBack()
        compose.runOnIdle {
            assertEquals(AppDestination.Home.route, navController.currentDestination?.route)
            assertNull(navController.previousBackStackEntry)
        }
    }

    @Test fun listCompletionWaitsForAcknowledgementAndUpdatesHomeProgress() {
        val repository = UiTaskRepository().apply { writeGate = CompletableDeferred() }
        launchFlow(completed = true, initialSession = AuthSession.Guest, taskRepository = repository)
        compose.onNodeWithTag("category_study").performScrollTo().performClick()
        compose.onNodeWithTag("tasks_search").performTextInput("on chuong")
        compose.onNodeWithTag("tasks_search").performImeAction()
        compose.onNodeWithTag("task_list_pending_count").assertTextEquals("1 việc chưa xong")
        compose.onNodeWithTag("list_toggle_11").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("task_list_pending_count").assertTextEquals("1 việc chưa xong")
        compose.runOnIdle { repository.writeGate!!.complete(Unit) }
        compose.waitForIdle()
        compose.onNodeWithTag("task_list_pending_count").assertTextEquals("0 việc chưa xong")
        compose.onNodeWithTag("task_tab_home").performClick()
        compose.onNodeWithTag("home_progress_count").assertTextEquals("2/2 việc")
    }

    @Test fun searchAndFilterSurviveAVisitToProfileAndCanBeClearedIndependently() {
        launchFlow(completed = true, initialSession = AuthSession.Guest, taskRepository = UiTaskRepository())
        compose.onNodeWithTag("task_tab_list").performClick()
        compose.onNodeWithTag("task_filter_STUDY").performClick()
        compose.onNodeWithTag("tasks_search").performTextInput("on chuong")
        compose.onNodeWithTag("tasks_search").performImeAction()
        compose.onNodeWithTag("task_tab_profile").performClick()
        compose.onNodeWithTag("profile_title").assertIsDisplayed()
        pressBack()
        compose.onNodeWithTag("tasks_search").assertTextContains("on chuong")
        compose.onNodeWithTag("task_filter_STUDY").assertIsSelected()
        compose.onNodeWithTag("tasks_clear_search").performClick()
        compose.onNodeWithTag("task_filter_STUDY").assertIsSelected()
        compose.onNodeWithTag("list_task_11").assertIsDisplayed()
    }

    @Test fun losingTheSessionWhileViewingTheListRemovesItsDataAndBackStack() {
        val auth = UiAuthRepository(AuthSession.Guest, false)
        launchFlow(completed = true, initialSession = AuthSession.Guest,
            taskRepository = UiTaskRepository(), authRepository = auth)
        compose.onNodeWithTag("task_tab_list").performClick()
        compose.onNodeWithTag("task_list_title").assertIsDisplayed()
        compose.runOnIdle { auth.session.value = AuthSession.SignedOut }
        compose.waitForIdle()
        compose.onNodeWithTag("welcome_title").assertIsDisplayed()
        compose.runOnIdle {
            assertNull(navController.previousBackStackEntry)
            assertEquals(emptyList<TodoTask>(), (models["home"] as HomeViewModel).uiState.value.tasks)
        }
    }

    @Test fun smallTaskListWithLargeFontCanScrollToTasksAndKeepNavigationReachable() {
        var toggled: Long? = null
        val task = TodoTask(21, "Công việc cần hoàn thành trong ngày", TaskCategory.STUDY, TaskDates.today(), "14:00", false)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                UTHTODOTheme {
                    Box(Modifier.requiredSize(320.dp, 480.dp)) {
                        TaskListScreen(HomeUiState(tasks = listOf(task), hasLoaded = true), TaskListUiState(),
                            onQuery = {}, onFilter = {}, onClearSearch = {}, onRefresh = {},
                            onToggle = { toggled = it }, onHome = {}, onProfile = {})
                    }
                }
            }
        }
        compose.onNodeWithTag("task_tab_home").assertIsDisplayed()
        compose.onNodeWithTag("tasks_list").performScrollToNode(hasTestTag("list_toggle_21"))
        compose.onNodeWithTag("list_toggle_21").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(21L, toggled) }
    }


    @Test fun homeTaskOpensDetailFromGetAndBackReturnsToHome() {
        val repository = UiTaskRepository()
        launchFlow(completed = true, initialSession = AuthSession.Guest, taskRepository = repository)
        compose.onNodeWithTag("home_task_11").performScrollTo().performClick()
        compose.onNodeWithTag("task_detail_task_title").assertTextEquals("Ôn chương 3")
        compose.runOnIdle { assertEquals(1, repository.detailReads) }
        pressBack()
        compose.onNodeWithTag("home_title").assertIsDisplayed()
    }

    @Test fun listSearchAndCategorySurviveDetailAndBack() {
        launchFlow(completed = true, initialSession = AuthSession.Guest, taskRepository = UiTaskRepository())
        compose.onNodeWithTag("category_study").performScrollTo().performClick()
        compose.onNodeWithTag("tasks_search").performTextInput("on chuong")
        compose.onNodeWithTag("tasks_search").performImeAction()
        compose.onNodeWithTag("list_task_11").performClick()
        compose.onNodeWithTag("task_detail_title").assertIsDisplayed()
        pressBack()
        compose.onNodeWithTag("tasks_search").assertTextContains("on chuong")
        compose.onNodeWithTag("task_filter_STUDY").assertIsSelected()
        compose.onNodeWithTag("list_task_11").assertIsDisplayed()
    }

    @Test fun detailSaveWaitsForAckThenReturnsUpdatedTaskToList() {
        val repository = UiTaskRepository().apply { writeGate = CompletableDeferred() }
        launchFlow(completed = true, initialSession = AuthSession.Guest, taskRepository = repository)
        compose.onNodeWithTag("task_tab_list").performClick()
        compose.onNodeWithTag("list_task_11").performClick()
        compose.onNodeWithTag("task_detail_edit").performClick()
        compose.onNodeWithTag("task_edit_title").performTextReplacement("Công việc đã sửa")
        compose.onNodeWithTag("task_edit_save").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("task_edit_save").assertIsNotEnabled()
        compose.runOnIdle { repository.writeGate!!.complete(Unit) }
        compose.waitForIdle()
        compose.onNodeWithTag("task_detail_task_title").performScrollTo().assertTextEquals("Công việc đã sửa")
        pressBack()
        compose.onNodeWithText("Công việc đã sửa").assertIsDisplayed()
    }

    @Test fun detailDeleteRequiresConfirmationAndKeepsRouteUntilAck() {
        val repository = UiTaskRepository().apply { writeGate = CompletableDeferred() }
        launchFlow(completed = true, initialSession = AuthSession.Guest, taskRepository = repository)
        compose.onNodeWithTag("task_tab_list").performClick()
        compose.onNodeWithTag("list_task_11").performClick()
        compose.onNodeWithTag("task_detail_delete").performScrollTo().performClick()
        pressBack()
        compose.onNodeWithTag("task_delete_confirm").assertDoesNotExist()
        compose.onNodeWithTag("task_detail_delete").performClick()
        compose.onNodeWithTag("task_delete_confirm").performClick()
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(AppDestination.TaskDetail.route, navController.currentDestination?.route) }
        compose.runOnIdle { repository.writeGate!!.complete(Unit) }
        compose.waitForIdle()
        compose.onNodeWithTag("task_list_title").assertIsDisplayed()
        compose.onNodeWithTag("list_task_11").assertDoesNotExist()
    }

    @Test fun dirtyDetailEditorAsksBeforeDiscardAndKeepsOriginalTask() {
        launchFlow(completed = true, initialSession = AuthSession.Guest, taskRepository = UiTaskRepository())
        compose.onNodeWithTag("task_tab_list").performClick()
        compose.onNodeWithTag("list_task_11").performClick()
        compose.onNodeWithTag("task_detail_edit").performClick()
        compose.onNodeWithTag("task_edit_title").performTextReplacement("Unsaved")
        compose.runOnIdle {
            val home = models["home"] as HomeViewModel
            assertEquals("Unsaved", home.uiState.value.detail.draft!!.title)
        }
        compose.onNodeWithTag("task_detail_back").performScrollTo().performClick()
        compose.onNodeWithTag("task_discard_confirm").performClick()
        compose.onNodeWithTag("task_detail_task_title").performScrollTo().assertTextEquals("Ôn chương 3")
        pressBack()
        compose.onNodeWithTag("list_task_11").assertIsDisplayed()
    }

    @Test fun detailReadFailureShowsRetryWithoutPretendingCachedListDataIsDetail() {
        val repository = UiTaskRepository().apply { detailFailure = com.example.uth_todo.model.TaskApiError.NETWORK }
        launchFlow(completed = true, initialSession = AuthSession.Guest, taskRepository = repository)
        compose.onNodeWithTag("task_tab_list").performClick()
        compose.onNodeWithTag("list_task_11").performClick()
        compose.onNodeWithTag("task_detail_error").assertIsDisplayed()
        compose.onNodeWithTag("task_detail_task_title").assertDoesNotExist()
        compose.runOnIdle { repository.detailFailure = null }
        compose.onNodeWithTag("task_detail_retry").performClick()
        compose.onNodeWithTag("task_detail_task_title").assertTextEquals("Ôn chương 3")
    }

    @Test fun sessionLossWhileEditingDetailClearsDraftAndProtectedHistory() {
        val auth = UiAuthRepository(AuthSession.Guest, false)
        launchFlow(completed = true, initialSession = AuthSession.Guest, taskRepository = UiTaskRepository(), authRepository = auth)
        compose.onNodeWithTag("task_tab_list").performClick()
        compose.onNodeWithTag("list_task_11").performClick()
        compose.onNodeWithTag("task_detail_edit").performClick()
        compose.runOnIdle { auth.session.value = AuthSession.SignedOut }
        compose.waitForIdle()
        compose.onNodeWithTag("welcome_title").assertIsDisplayed()
        compose.runOnIdle {
            assertNull(navController.previousBackStackEntry)
            assertNull((models["home"] as HomeViewModel).uiState.value.detail.draft)
        }
    }

    @Test fun smallDetailWithLargeFontKeepsDeleteAndEditorActionsReachable() {
        var requestedDelete = false
        val task = TodoTask(21, "Công việc có nội dung dài cần kiểm tra", TaskCategory.STUDY, TaskDates.today(), "14:00", false)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                UTHTODOTheme {
                    Box(Modifier.requiredSize(320.dp, 480.dp)) {
                        com.example.uth_todo.ui.taskdetail.TaskDetailScreen(
                            com.example.uth_todo.ui.taskdetail.TaskDetailUiState(task = task),
                            onBack = {}, onRetry = {}, onEdit = {}, onDraft = {}, onCancelEdit = {}, onSave = {},
                            onToggle = {}, onSubtask = {}, onDelete = { requestedDelete = true })
                    }
                }
            }
        }
        compose.onNodeWithTag("task_detail_delete").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithTag("task_delete_confirm").performClick()
        compose.runOnIdle { assertEquals(true, requestedDelete) }
    }

    @Test fun lauNhaCreationRejectsUnrelatedSampleAndKeepsDraftOnScreen() {
        val repository = UiTaskRepository().apply {
            createResult = TodoTask(99, "Ôn tập chương 4", TaskCategory.STUDY, TaskDates.today(), null, false)
        }
        launchFlow(completed = true, initialSession = AuthSession.Guest, taskRepository = repository)
        compose.onNodeWithTag("task_add_action").performClick()
        compose.onNodeWithTag("task_create_title").performTextInput("Lau nha")
        compose.onNodeWithTag("task_create_submit").performScrollTo().performClick()
        compose.onNodeWithTag("task_create_error").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("task_create_title").performScrollTo().assertTextContains("Lau nha")
        compose.runOnIdle {
            val state = (models["home"] as HomeViewModel).uiState.value
            assertEquals(AppDestination.TaskCreate.route, navController.currentDestination?.route)
            assertEquals(com.example.uth_todo.model.TaskApiError.CREATE_MISMATCH, state.creation.error)
            assertEquals(false, state.tasks.any { it.id == 99L })
            assertNull(state.createdNotice)
        }
    }

    @Test fun homePlusUsesSelectedDateAndEmptyBackDoesNotCreateATask() {
        val repository = UiTaskRepository()
        launchFlow(completed = true, initialSession = AuthSession.Guest, taskRepository = repository)
        compose.runOnIdle { (models["home"] as HomeViewModel).selectDate("2026-12-24") }
        compose.onNodeWithTag("task_add_action").performClick()
        compose.onNodeWithTag("task_create_title_heading").assertIsDisplayed()
        compose.onNodeWithTag("task_create_date").performScrollTo().assertTextContains("2026-12-24")
        compose.onNodeWithTag("task_create_back").performScrollTo().performClick()
        compose.onNodeWithTag("home_title").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, repository.createCount) }
    }

    @Test fun listPlusUsesCurrentCategoryAndCreatedTaskAppearsAfterAck() {
        val repository = UiTaskRepository().apply { writeGate = CompletableDeferred() }
        launchFlow(completed = true, initialSession = AuthSession.Guest, taskRepository = repository)
        compose.onNodeWithTag("task_tab_list").performClick()
        compose.onNodeWithTag("task_filter_LIFE").performClick()
        compose.onNodeWithTag("task_add_action").performClick()
        compose.onNodeWithTag("task_create_category_LIFE").performScrollTo().assertIsSelected()
        compose.onNodeWithTag("task_create_title").performScrollTo().performTextInput("Tưới cây")
        compose.onNodeWithTag("task_create_submit").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("task_create_submit").assertIsNotEnabled()
        compose.runOnIdle {
            assertEquals(1, repository.createCount)
            assertEquals(AppDestination.TaskCreate.route, navController.currentDestination?.route)
        }
        compose.runOnIdle { repository.writeGate!!.complete(Unit) }
        compose.waitForIdle()
        compose.onNodeWithTag("task_filter_LIFE").assertIsSelected()
        compose.onNodeWithTag("tasks_list").performScrollToNode(hasTestTag("list_task_101"))
        compose.onNodeWithText("Tưới cây").assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, repository.createCount) }
    }

    @Test fun invalidNewTaskStaysInFormAndNeverSendsPost() {
        val repository = UiTaskRepository()
        launchFlow(completed = true, initialSession = AuthSession.Guest, taskRepository = repository)
        compose.onNodeWithTag("task_add_action").performClick()
        compose.onNodeWithTag("task_create_submit").performScrollTo().performClick()
        compose.onNodeWithTag("task_create_error").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, repository.createCount) }
        compose.onNodeWithTag("task_create_title").performScrollTo().performTextInput("New parent")
        compose.onNodeWithTag("task_create_subtask_add").performScrollTo().performClick()
        compose.onNodeWithTag("task_create_submit").performScrollTo().performClick()
        compose.onNodeWithTag("task_create_error").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, repository.createCount) }
    }

    @Test fun backFromDirtyCreateFormAsksBeforeDiscardingWithoutPosting() {
        val repository = UiTaskRepository()
        launchFlow(completed = true, initialSession = AuthSession.Guest, taskRepository = repository)
        compose.onNodeWithTag("task_add_action").performClick()
        compose.onNodeWithTag("task_create_title").performTextInput("Unsaved")
        compose.onNodeWithTag("task_create_back").performScrollTo().performClick()
        compose.onNodeWithTag("task_create_discard_confirm").performClick()
        compose.onNodeWithTag("home_title").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(0, repository.createCount)
            assertNull((models["home"] as HomeViewModel).uiState.value.creation.entryKey)
        }
    }

    @Test fun uncertainCreateCanReviewListAndReturnToDraftBeforeExplicitRetry() {
        val repository = UiTaskRepository().apply { createFailure = com.example.uth_todo.model.TaskApiError.NETWORK }
        launchFlow(completed = true, initialSession = AuthSession.Guest, taskRepository = repository)
        compose.onNodeWithTag("task_tab_list").performClick()
        compose.onNodeWithTag("task_add_action").performClick()
        compose.onNodeWithTag("task_create_title").performTextInput("Check before retry")
        compose.onNodeWithTag("task_create_submit").performScrollTo().performClick()
        compose.onNodeWithTag("task_create_uncertain").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("task_create_submit").assertIsNotEnabled()
        compose.onNodeWithTag("task_create_review").performScrollTo().performClick()
        compose.onNodeWithTag("task_list_title").assertIsDisplayed()
        compose.onNodeWithTag("task_add_action").performClick()
        compose.onNodeWithTag("task_create_title").performScrollTo().assertTextContains("Check before retry")
        compose.runOnIdle { assertEquals(1, repository.createCount); repository.createFailure = null }
        compose.onNodeWithTag("task_create_retry").performScrollTo().performClick()
        compose.onNodeWithTag("task_create_retry_confirm").performClick()
        compose.onNodeWithTag("task_create_submit").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("task_list_title").assertIsDisplayed()
        compose.runOnIdle { assertEquals(2, repository.createCount) }
    }

    @Test fun losingSessionDuringPostClearsCreationAndPreventsLateSuccess() {
        val auth = UiAuthRepository(AuthSession.Guest, false)
        val repository = UiTaskRepository().apply { writeGate = CompletableDeferred() }
        launchFlow(completed = true, initialSession = AuthSession.Guest, taskRepository = repository, authRepository = auth)
        compose.onNodeWithTag("task_add_action").performClick()
        compose.onNodeWithTag("task_create_title").performTextInput("Previous session")
        compose.onNodeWithTag("task_create_submit").performScrollTo().performClick()
        compose.runOnIdle { auth.session.value = AuthSession.SignedOut }
        compose.waitForIdle()
        compose.onNodeWithTag("welcome_title").assertIsDisplayed()
        compose.runOnIdle { repository.writeGate!!.complete(Unit) }
        compose.waitForIdle()
        compose.runOnIdle {
            assertNull(navController.previousBackStackEntry)
            assertNull((models["home"] as HomeViewModel).uiState.value.creation.entryKey)
            assertNull((models["home"] as HomeViewModel).uiState.value.createdNotice)
        }
    }

    @Test fun sameCreateEntryRetainsDraftWhenRecomposed() {
        launchFlow(completed = true, initialSession = AuthSession.Guest, taskRepository = UiTaskRepository())
        compose.onNodeWithTag("task_add_action").performClick()
        compose.onNodeWithTag("task_create_title").performTextInput("Keep draft")
        compose.runOnIdle {
            val home = models["home"] as HomeViewModel
            val key = home.uiState.value.creation.entryKey!!
            home.beginTaskCreation(key)
            assertEquals("Keep draft", home.uiState.value.creation.draft.title)
        }
        compose.onNodeWithTag("task_create_title").assertTextContains("Keep draft")
    }

    @Test fun smallCreateFormWithLargeFontCanScrollToSubmit() {
        var submitted = false
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                UTHTODOTheme {
                    Box(Modifier.requiredSize(320.dp, 480.dp)) {
                        com.example.uth_todo.ui.taskcreate.TaskCreateScreen(
                            com.example.uth_todo.ui.taskcreate.TaskCreateUiState(entryKey = "test",
                                initialDraft = com.example.uth_todo.model.TaskDraft.blank().copy(title = "New")),
                            onBack = {}, onDraft = {}, onCreate = { submitted = true }, onReviewList = {}, onAllowRetry = {})
                    }
                }
            }
        }
        compose.onNodeWithTag("task_create_submit").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(true, submitted) }
    }

    @Test fun deviceCreateLauNhaRefreshAndDetailKeepTheExactSavedContent() {
        val api = StorageCourseRepository()
        val repository = com.example.uth_todo.data.tasks.SessionTaskRepository(
            com.example.uth_todo.data.tasks.DeviceTaskStorage(MemoryPreferences()), api)
        launchFlow(completed = true, initialSession = AuthSession.Guest, taskRepository = repository)
        compose.onNodeWithTag("task_add_action").performClick()
        compose.onNodeWithTag("task_create_title").performScrollTo().performTextInput("Lau nha")
        compose.onNodeWithTag("task_create_category_LIFE").performScrollTo().performClick()
        compose.onNodeWithTag("task_create_submit").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("task_tab_list").performClick()
        compose.onNodeWithTag("task_list_refresh").performScrollTo().performClick()
        compose.onNodeWithTag("tasks_list").performScrollToNode(hasTestTag("list_task_1"))
        compose.onNodeWithText("Lau nha").assertIsDisplayed()
        compose.onNodeWithTag("list_task_1").performClick()
        compose.onNodeWithTag("task_detail_task_title").performScrollTo().assertTextEquals("Lau nha")
        compose.onNodeWithText("Ôn tập chương 4").assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, api.calls) }
    }

    @Test fun deviceTaskSurvivesSigningOutAndReturningAsTheSameGuest() {
        val repository = com.example.uth_todo.data.tasks.SessionTaskRepository(
            com.example.uth_todo.data.tasks.DeviceTaskStorage(MemoryPreferences()), StorageCourseRepository())
        launchFlow(completed = true, initialSession = AuthSession.Guest, taskRepository = repository)
        compose.onNodeWithTag("task_add_action").performClick()
        compose.onNodeWithTag("task_create_title").performScrollTo().performTextInput("Lau nha")
        compose.onNodeWithTag("task_create_submit").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("open_profile").performScrollTo().performClick()
        compose.onNodeWithTag("sign_out_button").performScrollTo().performClick()
        compose.onNodeWithTag("guest_button").performClick()
        compose.onNodeWithTag("task_tab_list").performClick()
        compose.onNodeWithTag("tasks_list").performScrollToNode(hasTestTag("list_task_1"))
        compose.onNodeWithText("Lau nha").assertIsDisplayed()
    }

    @Test fun changingDataModeReturnsHomeAndPreservesDeviceTasksWhenSwitchingBack() {
        val prefs = MemoryPreferences()
        val repository = com.example.uth_todo.data.tasks.SessionTaskRepository(
            com.example.uth_todo.data.tasks.DeviceTaskStorage(prefs), StorageCourseRepository())
        launchFlow(completed = true, initialSession = AuthSession.Guest, taskRepository = repository)
        compose.onNodeWithTag("task_add_action").performClick()
        compose.onNodeWithTag("task_create_title").performScrollTo().performTextInput("Lau nha")
        compose.onNodeWithTag("task_create_submit").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("open_profile").performScrollTo().performClick()
        compose.onNodeWithTag("task_storage_mode_COURSE_API").performScrollTo().performClick()
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(AppDestination.Home.route, navController.currentDestination?.route)
            assertNull(navController.previousBackStackEntry)
        }
        compose.onNodeWithTag("task_storage_notice").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("task_tab_list").performClick()
        compose.onNodeWithTag("tasks_list").performScrollToNode(hasTestTag("list_task_1"))
        compose.onNodeWithText("Ôn tập chương 4").assertIsDisplayed()
        compose.onNodeWithTag("task_tab_profile").performClick()
        compose.onNodeWithTag("task_storage_mode_DEVICE").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("task_tab_list").performClick()
        compose.onNodeWithTag("tasks_list").performScrollToNode(hasTestTag("list_task_1"))
        compose.onNodeWithText("Lau nha").assertIsDisplayed()
    }

    @Test fun persistedDeviceEditAndDeleteStayCorrectAfterRefresh() {
        val repository = com.example.uth_todo.data.tasks.SessionTaskRepository(
            com.example.uth_todo.data.tasks.DeviceTaskStorage(MemoryPreferences()), StorageCourseRepository())
        launchFlow(completed = true, initialSession = AuthSession.Guest, taskRepository = repository)
        compose.onNodeWithTag("task_add_action").performClick()
        compose.onNodeWithTag("task_create_title").performScrollTo().performTextInput("Lau nha")
        compose.onNodeWithTag("task_create_submit").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("task_tab_list").performClick()
        compose.onNodeWithTag("tasks_list").performScrollToNode(hasTestTag("list_task_1"))
        compose.onNodeWithTag("list_task_1").performClick()
        compose.onNodeWithTag("task_detail_edit").performScrollTo().performClick()
        compose.onNodeWithTag("task_edit_title").performScrollTo().performTextReplacement("Lau nhà tầng 1")
        compose.onNodeWithTag("task_edit_save").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("task_detail_refresh").performScrollTo().performClick()
        compose.onNodeWithTag("task_detail_task_title").performScrollTo().assertTextEquals("Lau nhà tầng 1")
        compose.onNodeWithTag("task_detail_delete").performScrollTo().performClick()
        compose.onNodeWithTag("task_delete_confirm").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("task_list_refresh").performScrollTo().performClick()
        compose.onNodeWithTag("list_task_1").assertDoesNotExist()
    }

    @Test fun notificationsTabOpensSeparateScreenAndBackReturnsHome() {
        launchFlow(completed = true, initialSession = AuthSession.Guest, taskRepository = UiTaskRepository())
        compose.onNodeWithTag("task_tab_notifications").performClick()
        compose.onNodeWithTag("notifications_title").assertIsDisplayed()
        pressBack()
        compose.onNodeWithTag("home_title").assertIsDisplayed()
        compose.runOnIdle { assertNull(navController.previousBackStackEntry) }
    }

    @Test fun listSearchAndCategorySurviveNotificationsAndBack() {
        launchFlow(completed = true, initialSession = AuthSession.Guest, taskRepository = UiTaskRepository())
        compose.onNodeWithTag("category_study").performScrollTo().performClick()
        compose.onNodeWithTag("tasks_search").performTextInput("on chuong")
        compose.onNodeWithTag("tasks_search").performImeAction()
        compose.onNodeWithTag("task_tab_notifications").performClick()
        compose.onNodeWithTag("notifications_title").assertIsDisplayed()
        pressBack()
        compose.onNodeWithTag("tasks_search").assertTextContains("on chuong")
        compose.onNodeWithTag("task_filter_STUDY").assertIsSelected()
    }

    @Test fun reminderListOpensSavedTaskAndTestButtonUsesInjectedController() {
        val storage = com.example.uth_todo.data.tasks.DeviceTaskStorage(MemoryPreferences())
        kotlinx.coroutines.runBlocking {
            storage.create("guest", com.example.uth_todo.model.TaskDraft.blank(TaskDates.nextDay(TaskDates.today()))
                .copy(title = "Lau nha", time = "14:00", remind = true))
        }
        val reminders = UiReminderController()
        val repository = com.example.uth_todo.data.tasks.SessionTaskRepository(storage, UiTaskRepository())
        launchFlow(completed = true, initialSession = AuthSession.Guest, taskRepository = repository, reminders = reminders)
        compose.onNodeWithTag("task_tab_notifications").performClick()
        compose.onNodeWithTag("notifications_screen").performScrollToNode(hasTestTag("reminders_test"))
        compose.onNodeWithTag("reminders_test").performClick()
        compose.onNodeWithTag("reminders_test_result").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, reminders.testCalls) }
        compose.onNodeWithTag("notifications_screen").performScrollToNode(hasTestTag("reminder_task_1"))
        compose.onNodeWithTag("reminder_task_1").performClick()
        compose.onNodeWithTag("task_detail_task_title").performScrollTo().assertTextEquals("Lau nha")
        pressBack()
        compose.onNodeWithTag("notifications_title").performScrollTo().assertIsDisplayed()
    }

    @Test fun reminderOpenRequestLoadsTheCorrectTaskForTheActiveOwner() {
        val storage = com.example.uth_todo.data.tasks.DeviceTaskStorage(MemoryPreferences())
        kotlinx.coroutines.runBlocking { storage.create("guest", com.example.uth_todo.model.TaskDraft.blank().copy(title = "Lau nha")) }
        val requests = MutableStateFlow<com.example.uth_todo.notifications.ReminderOpenRequest?>(null)
        launchFlow(completed = true, initialSession = AuthSession.Guest,
            taskRepository = com.example.uth_todo.data.tasks.SessionTaskRepository(storage, UiTaskRepository()), reminderRequests = requests)
        compose.runOnIdle { requests.value = com.example.uth_todo.notifications.ReminderOpenRequest("guest", 1) }
        compose.waitForIdle()
        compose.onNodeWithTag("task_detail_task_title").performScrollTo().assertTextEquals("Lau nha")
        compose.runOnIdle { assertNull(requests.value) }
    }

    @Test fun reminderOpenRequestForDifferentOwnerDoesNotOpenTheirTask() {
        val requests = MutableStateFlow<com.example.uth_todo.notifications.ReminderOpenRequest?>(null)
        val storage = com.example.uth_todo.data.tasks.DeviceTaskStorage(MemoryPreferences())
        launchFlow(completed = true, initialSession = AuthSession.Guest,
            taskRepository = com.example.uth_todo.data.tasks.SessionTaskRepository(storage, UiTaskRepository()), reminderRequests = requests)
        compose.runOnIdle { requests.value = com.example.uth_todo.notifications.ReminderOpenRequest(
            com.example.uth_todo.data.tasks.TaskSessionScope.forUid("someone-else"), 1) }
        compose.waitForIdle()
        compose.onNodeWithTag("task_detail_title").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(AppDestination.Home.route, navController.currentDestination?.route)
            assertNull(requests.value)
        }
    }

    @Test fun apiModeDoesNotShowPersonalRemindersOrOpenADeviceTaskFromNotification() {
        val requests = MutableStateFlow<com.example.uth_todo.notifications.ReminderOpenRequest?>(null)
        val storage = com.example.uth_todo.data.tasks.DeviceTaskStorage(MemoryPreferences())
        kotlinx.coroutines.runBlocking {
            storage.create("guest", com.example.uth_todo.model.TaskDraft.blank().copy(title = "Personal"))
            storage.selectMode("guest", com.example.uth_todo.model.TaskDataMode.COURSE_API)
        }
        launchFlow(completed = true, initialSession = AuthSession.Guest,
            taskRepository = com.example.uth_todo.data.tasks.SessionTaskRepository(storage, UiTaskRepository()), reminderRequests = requests)
        compose.onNodeWithTag("task_tab_notifications").performClick()
        compose.onNodeWithTag("notifications_screen").performScrollToNode(androidx.compose.ui.test.hasText(
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.reminders_device_only)))
        compose.onNodeWithTag("reminder_task_1").assertDoesNotExist()
        compose.runOnIdle { requests.value = com.example.uth_todo.notifications.ReminderOpenRequest("guest", 1) }
        compose.waitForIdle()
        compose.onNodeWithTag("task_detail_title").assertDoesNotExist()
        compose.runOnIdle { assertNull(requests.value) }
    }

    @Test fun losingSessionOnNotificationsClearsTheProtectedBackStack() {
        val auth = UiAuthRepository(AuthSession.Guest, false)
        launchFlow(completed = true, initialSession = AuthSession.Guest, taskRepository = UiTaskRepository(), authRepository = auth)
        compose.onNodeWithTag("task_tab_notifications").performClick()
        compose.onNodeWithTag("notifications_title").assertIsDisplayed()
        compose.runOnIdle { auth.session.value = AuthSession.SignedOut }
        compose.waitForIdle()
        compose.onNodeWithTag("welcome_title").assertIsDisplayed()
        compose.runOnIdle { assertNull(navController.previousBackStackEntry) }
    }

    @Test fun notificationPermissionCardKeepsTestDisabledUntilPermissionStateIsEnabled() {
        var enableCalls = 0
        var testCalls = 0
        val state = MutableStateFlow(com.example.uth_todo.notifications.ReminderServiceState(available = true))
        compose.setContent {
            val service = state.collectAsState().value
            UTHTODOTheme {
                com.example.uth_todo.ui.notifications.NotificationsScreen(HomeUiState(), service,
                    onBack = {}, onRefresh = {}, onEnable = { enableCalls++ }, onExact = {}, onTest = { testCalls++ },
                    onOpenTask = {}, onToggle = {})
            }
        }
        compose.onNodeWithTag("notifications_screen").performScrollToNode(hasTestTag("reminders_test"))
        compose.onNodeWithTag("reminders_test").assertIsNotEnabled()
        compose.onNodeWithTag("reminders_enable").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, enableCalls); state.value = state.value.copy(notificationsAllowed = true, exactAllowed = true) }
        compose.onNodeWithTag("reminders_test").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, testCalls) }
    }

    @Test fun notificationsWithLargeFontCanScrollToTheTestAction() {
        var tested = false
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                UTHTODOTheme {
                    Box(Modifier.requiredSize(320.dp, 480.dp)) {
                        com.example.uth_todo.ui.notifications.NotificationsScreen(HomeUiState(),
                            com.example.uth_todo.notifications.ReminderServiceState(available = true, notificationsAllowed = true, exactAllowed = true),
                            onBack = {}, onRefresh = {}, onEnable = {}, onExact = {}, onTest = { tested = true },
                            onOpenTask = {}, onToggle = {})
                    }
                }
            }
        }
        compose.onNodeWithTag("notifications_screen").performScrollToNode(hasTestTag("reminders_test"))
        compose.onNodeWithTag("reminders_test").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(true, tested) }
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

private class StorageCourseRepository : TaskRepository {
    override val isConfigured = true
    var calls = 0
    private val sample = TodoTask(1, "Ôn tập chương 4", TaskCategory.STUDY, null, null, false)
    override suspend fun loadTasks(): List<TodoTask> { calls++; return listOf(sample) }
    override suspend fun loadTask(id: Long): TodoTask { calls++; return sample }
    override suspend fun setDone(id: Long, done: Boolean) { calls++ }
    override suspend fun updateTask(task: TodoTask) { calls++ }
    override suspend fun setSubtasks(id: Long, subtasks: List<com.example.uth_todo.model.TodoSubtask>) { calls++ }
    override suspend fun deleteTask(id: Long) { calls++ }
    override suspend fun createTask(draft: com.example.uth_todo.model.TaskDraft): TodoTask {
        calls++
        throw com.example.uth_todo.model.TaskCreateFailure(com.example.uth_todo.model.TaskApiError.CREATE_MISMATCH, true)
    }
}


private class UiReminderController : com.example.uth_todo.notifications.TaskReminderController {
    override val uiState = MutableStateFlow(com.example.uth_todo.notifications.ReminderServiceState(
        available = true, notificationsAllowed = true, exactAllowed = true))
    var testCalls = 0
    override fun bindSession(session: AuthSession) = Unit
    override fun refresh() = Unit
    override fun sendTest() { testCalls++; uiState.value = uiState.value.copy(testResult = true) }
}

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

private class UiTaskRepository : TaskRepository {
    var createCount = 0
    var createResult: TodoTask? = null
    var createFailure: com.example.uth_todo.model.TaskApiError? = null
    override suspend fun createTask(draft: com.example.uth_todo.model.TaskDraft): TodoTask {
        createCount++
        writeGate?.await()
        createFailure?.let { throw com.example.uth_todo.model.TaskCreateFailure(it, mayHaveCreated = true) }
        return createResult ?: draft.created(100L + createCount).also { tasks = tasks + it }
    }
    var detailReads = 0
    var detailFailure: com.example.uth_todo.model.TaskApiError? = null
    override suspend fun loadTask(id: Long): TodoTask {
        detailReads++
        detailFailure?.let { throw com.example.uth_todo.model.TaskApiFailure(it) }
        return tasks.single { it.id == id }
    }
    override suspend fun updateTask(task: TodoTask) {
        writeGate?.await()
        tasks = tasks.map { if (it.id == task.id) task else it }
    }
    override suspend fun setSubtasks(id: Long, subtasks: List<com.example.uth_todo.model.TodoSubtask>) {
        writeGate?.await()
        tasks = tasks.map { if (it.id == id) it.copy(subtasks = subtasks) else it }
    }
    override suspend fun deleteTask(id: Long) {
        writeGate?.await()
        tasks = tasks.filterNot { it.id == id }
    }
    override val isConfigured = true
    private var tasks = listOf(
        TodoTask(11, "Ôn chương 3", TaskCategory.STUDY, TaskDates.today(), "14:00", false),
        TodoTask(12, "Đi chợ", TaskCategory.LIFE, TaskDates.nextDay(TaskDates.today()), "17:30", false),
        TodoTask(13, "Nộp bài", TaskCategory.STUDY, TaskDates.today(), "09:00", true),
    )
    var writeGate: CompletableDeferred<Unit>? = null
    override suspend fun loadTasks(): List<TodoTask> = tasks
    override suspend fun setDone(id: Long, done: Boolean) {
        writeGate?.await()
        tasks = tasks.map { if (it.id == id) it.copy(done = done) else it }
    }
}
