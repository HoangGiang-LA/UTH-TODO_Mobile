package com.example.uth_todo.navigation

sealed class AppDestination(val route: String) {
    data object Splash : AppDestination("splash")
    data object Onboarding : AppDestination("onboarding")
    data object Welcome : AppDestination("welcome")
    data object EmailAuth : AppDestination("email_auth")
    data object Home : AppDestination("home")
    data object Profile : AppDestination("profile")
}
