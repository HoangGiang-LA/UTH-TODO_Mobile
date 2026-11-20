package com.example.uth_todo.data.firebase

import android.content.Context
import android.os.Bundle
import com.example.uth_todo.model.FirebaseServiceError
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

class FirebaseTelemetry(context: Context) : AppTelemetry {
    private val configured = FirebaseApp.getApps(context.applicationContext)
        .any { it.name == FirebaseApp.DEFAULT_APP_NAME }
    private val analytics = if (configured) FirebaseAnalytics.getInstance(context.applicationContext) else null
    private val crashlytics = if (configured) FirebaseCrashlytics.getInstance() else null

    override fun screen(name: String) {
        // Routes are fixed application constants; never include route arguments.
        analytics?.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, name)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, "MainActivity")
        })
    }

    override fun profileSaved() { analytics?.logEvent("profile_saved", null) }

    override fun failure(operation: String, reason: FirebaseServiceError) {
        // Expected setup/offline/validation failures are not application defects.
        if (reason != FirebaseServiceError.UNKNOWN) return
        crashlytics?.recordException(IllegalStateException("uth_${operation}_${reason.name}"))
    }
}
