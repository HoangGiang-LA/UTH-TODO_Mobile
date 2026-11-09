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
import com.example.uth_todo.navigation.UthTodoApp
import com.example.uth_todo.ui.onboarding.OnboardingViewModel
import com.example.uth_todo.ui.theme.UTHTODOTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val onboardingFactory = viewModelFactory {
            initializer { OnboardingViewModel(OnboardingPreferences(applicationContext)) }
        }
        setContent {
            UTHTODOTheme {
                val onboardingViewModel: OnboardingViewModel = viewModel(factory = onboardingFactory)
                UthTodoApp(onboardingViewModel)
            }
        }
    }
}
