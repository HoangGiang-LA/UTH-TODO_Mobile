package com.example.uth_todo.navigation

sealed class AppDestination(val route: String) {
    data object Splash : AppDestination("splash")
    data object Onboarding : AppDestination("onboarding")
    data object Welcome : AppDestination("welcome")
}
