package com.example.uth_todo.ui.notifications

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.uth_todo.R
import com.example.uth_todo.notifications.AndroidReminderAlarms
import com.example.uth_todo.notifications.ReminderServiceState
import com.example.uth_todo.notifications.TaskReminderController
import com.example.uth_todo.ui.home.HomeUiState

@Composable
fun NotificationSettingsRoute(
    tasks: HomeUiState,
    service: ReminderServiceState,
    controller: TaskReminderController?,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onOpenTask: (Long) -> Unit,
    onToggle: (Long) -> Unit,
    showGeneralNotifications: Boolean,
) {
    val context = LocalContext.current
    var requested by rememberSaveable { mutableStateOf(false) }
    var settingsFailed by rememberSaveable { mutableStateOf(false) }
    val settingsError = stringResource(R.string.reminders_settings_error)
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { controller?.refresh() }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { controller?.refresh() }
    fun open(intent: Intent) {
        settingsFailed = false
        try { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        catch (_: ActivityNotFoundException) { settingsFailed = true }
        catch (_: SecurityException) { settingsFailed = true }
    }
    fun notificationSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            open(Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                .putExtra(Settings.EXTRA_CHANNEL_ID, AndroidReminderAlarms.CHANNEL_ID))
        } else open(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
    }
    NotificationsScreen(tasks, service, onBack = onBack, onRefresh = { controller?.refresh(); onRefresh() },
        onEnable = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !requested &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requested = true
                permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else notificationSettings()
        }, onExact = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                open(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
        }, onTest = { controller?.sendTest() }, onOpenTask = onOpenTask, onToggle = onToggle,
        showGeneralNotifications = showGeneralNotifications, settingsError = if (settingsFailed) settingsError else null)
}
