package com.example.uth_todo.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.uth_todo.data.tasks.TaskSessionScope

class TaskReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val scope = intent.getStringExtra(AndroidReminderAlarms.EXTRA_SCOPE) ?: return
        val id = intent.getLongExtra(AndroidReminderAlarms.EXTRA_ID, 0)
        val due = intent.getLongExtra(AndroidReminderAlarms.EXTRA_DUE, 0)
        val fire = intent.getLongExtra(AndroidReminderAlarms.EXTRA_FIRE, 0)
        if (!TaskSessionScope.isValid(scope) || id <= 0 || fire <= 0 || due < fire) return
        val result = goAsync()
        TaskReminderRuntime.get(context).background({
            deliver(TaskReminderPlan(ReminderKey(scope, id), due, fire))
        }, result::finish)
    }
}

class TaskReminderRestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED,
                Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED,
                "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED")) return
        val result = goAsync()
        TaskReminderRuntime.get(context).background({ restore() }, result::finish)
    }
}

