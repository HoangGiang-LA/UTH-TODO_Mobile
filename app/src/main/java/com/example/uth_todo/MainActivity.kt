package com.example.uth_todo

import android.os.Bundle
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
import com.example.uth_todo.ui.auth.AuthViewModel
import com.example.uth_todo.navigation.UthTodoApp
import com.example.uth_todo.ui.onboarding.OnboardingViewModel
import com.example.uth_todo.ui.profile.FirebaseServicesViewModel
import com.example.uth_todo.ui.theme.UTHTODOTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appFactory = viewModelFactory {
            initializer { OnboardingViewModel(OnboardingPreferences(applicationContext)) }
            initializer { AuthViewModel(FirebaseAuthRepository(applicationContext)) }
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
                UthTodoApp(onboardingViewModel, authViewModel = authViewModel, servicesViewModel = servicesViewModel)
            }
        }
    }
}
