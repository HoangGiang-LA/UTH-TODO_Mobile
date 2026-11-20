package com.example.uth_todo.ui.profile

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.uth_todo.R
import com.example.uth_todo.notifications.UthNotifications
import com.example.uth_todo.ui.components.PrimaryButton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

@Composable
fun NotificationPreferenceCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val configured = remember(context) { UthNotifications.isConfigured(context) }
    var enabled by remember { mutableStateOf(UthNotifications.canPost(context)) }
    var subscribed by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var requested by rememberSaveable { mutableStateOf(false) }

    fun subscribe() {
        if (!configured || busy) return
        busy = true
        failed = false
        scope.launch {
            try {
                withTimeout(15_000) { UthNotifications.subscribe(context) }
                subscribed = true
            } catch (_: TimeoutCancellationException) {
                failed = true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                failed = true
            } finally { busy = false }
        }
    }

    fun openSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                .putExtra(Settings.EXTRA_CHANNEL_ID, UthNotifications.CHANNEL_ID)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(android.net.Uri.parse("package:${context.packageName}"))
        }
        context.startActivity(intent)
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        enabled = granted && UthNotifications.canPost(context)
        if (enabled) subscribe()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        enabled = UthNotifications.canPost(context)
        if (enabled && !subscribed) subscribe()
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.notifications_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(when {
                !configured -> R.string.notifications_unavailable
                !enabled -> R.string.notifications_description
                busy -> R.string.notifications_connecting
                failed -> R.string.notifications_failed
                subscribed -> R.string.notifications_enabled
                else -> R.string.notifications_description
            }))
            PrimaryButton(
                text = stringResource(if (busy) R.string.auth_processing else if (enabled && subscribed)
                    R.string.notifications_settings else if (failed) R.string.retry else R.string.notifications_enable),
                onClick = {
                    when {
                        enabled && subscribed -> openSettings()
                        enabled -> subscribe()
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !requested -> {
                            requested = true
                            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        else -> openSettings()
                    }
                },
                enabled = configured && !busy, tag = "enable_notifications",
            )
            if (!enabled) {
                TextButton(onClick = ::openSettings) { Text(stringResource(R.string.notifications_settings)) }
            }
        }
    }
}
