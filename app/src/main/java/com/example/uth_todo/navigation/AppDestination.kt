package com.example.uth_todo.navigation

import com.example.uth_todo.model.TaskFilter

sealed class AppDestination(val route: String) {
    data object Splash : AppDestination("splash")
    data object Onboarding : AppDestination("onboarding")
    data object Welcome : AppDestination("welcome")
    data object EmailAuth : AppDestination("email_auth")
    data object Home : AppDestination("home")
    data object TaskList : AppDestination("task_list?category={category}") {
        const val CATEGORY_ARGUMENT = "category"
        fun location(filter: TaskFilter): String = "task_list?category=${filter.name}"
    }
    data object Profile : AppDestination("profile")

    companion object {
        fun requiresSession(route: String?): Boolean =
            route == Home.route || route == TaskList.route || route == Profile.route
    }
}
