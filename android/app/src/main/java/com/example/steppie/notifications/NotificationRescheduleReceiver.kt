package com.example.steppie.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in RescheduleActions) return
        scheduleNotificationRescheduleJob(context.applicationContext)
    }
}

private val RescheduleActions = setOf(
    Intent.ACTION_BOOT_COMPLETED,
    Intent.ACTION_MY_PACKAGE_REPLACED,
)
