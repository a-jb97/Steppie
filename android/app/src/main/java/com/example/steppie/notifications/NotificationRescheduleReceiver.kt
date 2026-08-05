package com.example.steppie.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.steppie.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NotificationRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in RescheduleActions) return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                AppContainer(context.applicationContext).notificationRescheduler.reconcileToday()
            } catch (_: Exception) {
                // A restart-time reschedule failure must not crash the app process.
            } finally {
                pendingResult.finish()
            }
        }
    }
}

private val RescheduleActions = setOf(
    Intent.ACTION_BOOT_COMPLETED,
    Intent.ACTION_MY_PACKAGE_REPLACED,
)
