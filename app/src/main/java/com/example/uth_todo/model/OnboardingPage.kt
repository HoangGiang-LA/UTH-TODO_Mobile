package com.example.uth_todo.model

import androidx.annotation.StringRes
import com.example.uth_todo.R

enum class OnboardingIllustration { TASKS, CATEGORIES, REMINDERS }

data class OnboardingPage(
    @param:StringRes val title: Int,
    @param:StringRes val description: Int,
    val illustration: OnboardingIllustration,
) {
    companion object {
        val pages = listOf(
            OnboardingPage(R.string.onboarding_title_tasks, R.string.onboarding_body_tasks, OnboardingIllustration.TASKS),
            OnboardingPage(R.string.onboarding_title_categories, R.string.onboarding_body_categories, OnboardingIllustration.CATEGORIES),
            OnboardingPage(R.string.onboarding_title_reminders, R.string.onboarding_body_reminders, OnboardingIllustration.REMINDERS),
        )
    }
}
