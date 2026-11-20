package com.example.uth_todo.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.uth_todo.MainActivity
import com.example.uth_todo.R
import com.example.uth_todo.model.TodoTask

class AndroidReminderAlarms(private val context: Context) : ReminderAlarmPort {
    companion object {
        const val CHANNEL_ID = "uth_todo_task_reminders"
        const val EXTRA_SCOPE = "reminder_scope"
        const val EXTRA_ID = "reminder_task_id"
        const val EXTRA_DUE = "reminder_due_at"
        const val EXTRA_FIRE = "reminder_fire_at"
    }
    init { createChannel() }
    private val manager get() = context.getSystemService(AlarmManager::class.java)
    fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, context.getString(R.string.reminders_channel), NotificationManager.IMPORTANCE_DEFAULT))
        }
    }
    override fun canNotify(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            context.getSystemService(NotificationManager::class.java).getNotificationChannel(CHANNEL_ID)?.importance != NotificationManager.IMPORTANCE_NONE
    }
    override fun canScheduleExact(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()
    private fun alarmIntent(key: ReminderKey): Intent = Intent(context, TaskReminderReceiver::class.java)
        .setData(Uri.parse("uth-todo://reminder/${key.scope}/${key.taskId}"))
    override fun schedule(plan: TaskReminderPlan, exact: Boolean) {
        val intent = alarmIntent(plan.key).putExtra(EXTRA_SCOPE, plan.key.scope).putExtra(EXTRA_ID, plan.key.taskId)
            .putExtra(EXTRA_DUE, plan.dueAt).putExtra(EXTRA_FIRE, plan.fireAt)
        val pending = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        if (exact && (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms())) {
            try {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, plan.fireAt, pending)
                return
            } catch (_: SecurityException) { /* permission may have changed since the UI check */ }
        }
        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, plan.fireAt, pending)
    }
    override fun cancel(key: ReminderKey) {
        val pending = PendingIntent.getBroadcast(context, 0, alarmIntent(key), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        if (pending != null) { manager.cancel(pending); pending.cancel() }
    }
    override fun cancelNotification(key: ReminderKey) { NotificationManagerCompat.from(context).cancel(key.token, 0) }
    override fun show(task: TodoTask, plan: TaskReminderPlan): Boolean {
        val intent = Intent(context, MainActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .setData(Uri.parse("uth-todo://open-reminder/${plan.key.scope}/${task.id}"))
            .putExtra(EXTRA_SCOPE, plan.key.scope).putExtra(EXTRA_ID, task.id)
        val pending = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val due = listOfNotNull(task.date, task.time).joinToString(" ")
        return post(plan.key.token, task.title, context.getString(R.string.reminder_due_message, due), pending)
    }
    fun showTest(): Boolean {
        val intent = Intent(context, MainActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pending = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return post("reminder_test", context.getString(R.string.app_name), context.getString(R.string.reminder_test_body), pending)
    }
    private fun post(tag: String, title: String, body: String, pending: PendingIntent): Boolean {
        if (!canNotify()) return false
        val notification = NotificationCompat.Builder(context, CHANNEL_ID).setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title.take(100)).setContentText(body).setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pending).setAutoCancel(true).setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE).build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false
        return try { NotificationManagerCompat.from(context).notify(tag, 0, notification); true }
        catch (_: SecurityException) { false }
    }
}
