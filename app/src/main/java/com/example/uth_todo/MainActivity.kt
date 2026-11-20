package com.example.uth_todo

import android.os.Bundle
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.uth_todo.data.preferences.OnboardingPreferences
import com.example.uth_todo.data.auth.FirebaseAuthRepository
import com.example.uth_todo.data.firebase.FirebaseHomeConfigRepository
import com.example.uth_todo.data.firebase.FirebaseProfileRepository
import com.example.uth_todo.data.firebase.FirebaseTelemetry
import com.example.uth_todo.data.tasks.androidTaskRepository
import com.example.uth_todo.ui.auth.AuthViewModel
import com.example.uth_todo.navigation.UthTodoApp
import com.example.uth_todo.ui.onboarding.OnboardingViewModel
import com.example.uth_todo.ui.profile.FirebaseServicesViewModel
import com.example.uth_todo.ui.home.HomeViewModel
import com.example.uth_todo.ui.theme.UTHTODOTheme
import com.example.uth_todo.notifications.TaskReminderRuntime
import com.example.uth_todo.notifications.ReminderOpenRequest
import com.example.uth_todo.notifications.AndroidReminderAlarms
import com.example.uth_todo.data.tasks.TaskSessionScope

class MainActivity : ComponentActivity() {
    private var reminderRequest by mutableStateOf<ReminderOpenRequest?>(null)
    private fun readReminderIntent(intent: Intent) {
        val scope = intent.getStringExtra(AndroidReminderAlarms.EXTRA_SCOPE) ?: return
        val id = intent.getLongExtra(AndroidReminderAlarms.EXTRA_ID, 0)
        if (TaskSessionScope.isValid(scope) && id > 0) reminderRequest = ReminderOpenRequest(scope, id)
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readReminderIntent(intent)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        readReminderIntent(intent)
        val reminders = TaskReminderRuntime.get(applicationContext)
        val appFactory = viewModelFactory {
            initializer { OnboardingViewModel(OnboardingPreferences(applicationContext)) }
            initializer { AuthViewModel(FirebaseAuthRepository(applicationContext)) }
            initializer { HomeViewModel(androidTaskRepository(applicationContext, BuildConfig.MOCK_API_TOKEN), reminders = reminders) }
            initializer {
                val telemetry = FirebaseTelemetry(applicationContext)
                FirebaseServicesViewModel(FirebaseProfileRepository(applicationContext),
                    FirebaseHomeConfigRepository(applicationContext, telemetry), telemetry)
            }
        }
        setContent {
            UTHTODOTheme {
                val onboardingViewModel: OnboardingViewModel = viewModel(factory = appFactory)
                val authViewModel: AuthViewModel = viewModel(factory = appFactory)
                val servicesViewModel: FirebaseServicesViewModel = viewModel(factory = appFactory)
                val homeViewModel: HomeViewModel = viewModel(factory = appFactory)
                UthTodoApp(onboardingViewModel, authViewModel = authViewModel,
                    servicesViewModel = servicesViewModel, homeViewModel = homeViewModel, reminderController = reminders,
                    reminderOpenRequest = reminderRequest, onReminderRequestHandled = {
                        reminderRequest = null
                        intent.removeExtra(AndroidReminderAlarms.EXTRA_SCOPE)
                        intent.removeExtra(AndroidReminderAlarms.EXTRA_ID)
                    })
            }
        }
    }
}
