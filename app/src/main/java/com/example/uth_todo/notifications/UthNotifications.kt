package com.example.uth_todo.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.uth_todo.MainActivity
import com.example.uth_todo.R
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

object UthNotifications {
    const val CHANNEL_ID = "uth_todo_updates"
    const val TOPIC = "uth_todo_updates"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, context.getString(R.string.notifications_channel),
                    NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = context.getString(R.string.notifications_channel_description)
                },
            )
        }
    }

    fun isConfigured(context: Context): Boolean = FirebaseApp.getApps(context.applicationContext)
        .any { it.name == FirebaseApp.DEFAULT_APP_NAME }

    fun canPost(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED) return false
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            context.getSystemService(NotificationManager::class.java).getNotificationChannel(CHANNEL_ID)
                ?.importance != NotificationManager.IMPORTANCE_NONE
    }

    suspend fun subscribe(context: Context) {
        check(isConfigured(context))
        // This topic carries public app announcements only, never user tasks.
        FirebaseMessaging.getInstance().subscribeToTopic(TOPIC).await()
    }

    fun show(context: Context, title: String, body: String) {
        if (body.isBlank() || !canPost(context)) return
        createChannel(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pending = PendingIntent.getActivity(context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title.take(100))
            .setContentText(body.take(500))
            .setStyle(NotificationCompat.BigTextStyle().bigText(body.take(500)))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        // Keep the permission check at the actual post site as well, so lint
        // can prove it and a permission revoked since canPost is respected.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED) {
            try {
                NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notification)
            } catch (_: SecurityException) {
                // Revoking notifications while a message arrives is harmless.
            }
        }
    }
}
