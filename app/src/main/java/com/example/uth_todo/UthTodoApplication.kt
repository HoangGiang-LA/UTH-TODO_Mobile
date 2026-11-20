package com.example.uth_todo

import android.app.Application
import com.example.uth_todo.notifications.UthNotifications

class UthTodoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        UthNotifications.createChannel(this)
    }
}
