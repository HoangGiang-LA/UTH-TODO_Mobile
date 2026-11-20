package com.example.uth_todo.data.firebase

import android.content.Context
import com.example.uth_todo.BuildConfig
import com.example.uth_todo.R
import com.example.uth_todo.model.FirebaseServiceError
import com.example.uth_todo.model.HomeConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

class FirebaseHomeConfigRepository(context: Context, private val telemetry: AppTelemetry) : HomeConfigRepository {
    private val app = FirebaseApp.getApps(context.applicationContext)
        .firstOrNull { it.name == FirebaseApp.DEFAULT_APP_NAME }

    override suspend fun load(): HomeConfig {
        val config = app?.let(FirebaseRemoteConfig::getInstance) ?: return HomeConfig()
        try {
            config.setConfigSettingsAsync(FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(if (BuildConfig.DEBUG) 60 else 43_200)
                .setFetchTimeoutInSeconds(10)
                .build()).await()
            config.setDefaultsAsync(R.xml.remote_config_defaults).await()
            withTimeout(12_000) { config.fetchAndActivate().await() }
        } catch (_: TimeoutCancellationException) {
            // Keep previously activated values, or local defaults, while offline.
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            telemetry.failure("remote_config", FirebaseServiceError.UNKNOWN)
        }
        val text = config.getString("home_announcement_text").trim().take(300)
        return HomeConfig(text.takeIf { config.getBoolean("home_announcement_enabled") && it.isNotEmpty() })
    }
}
