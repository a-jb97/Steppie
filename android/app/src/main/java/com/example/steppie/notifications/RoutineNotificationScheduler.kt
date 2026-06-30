package com.example.steppie.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.steppie.MainActivity
import com.example.steppie.R
import com.example.steppie.domain.model.AppSettings
import com.example.steppie.domain.model.Routine
import java.time.Instant
import java.time.LocalDate

interface RoutineNotificationScheduler {
    fun reconcileToday(
        routines: List<Routine>,
        completedRoutineIds: Set<String>,
        settings: AppSettings,
        now: Instant = Instant.now(),
        date: LocalDate = LocalDate.now(),
    )
}

class AndroidRoutineNotificationScheduler(
    context: Context,
    private val planner: RoutineNotificationPlanner = RoutineNotificationPlanner(),
) : RoutineNotificationScheduler {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)

    override fun reconcileToday(
        routines: List<Routine>,
        completedRoutineIds: Set<String>,
        settings: AppSettings,
        now: Instant,
        date: LocalDate,
    ) {
        cancelKnownRequests(routines)
        if (!appContext.canPostNotifications()) return

        planner.requestsForToday(
            routines = routines,
            completedRoutineIds = completedRoutineIds,
            settings = settings,
            now = now,
            date = date,
        ).forEach(::schedule)
    }

    private fun schedule(request: RoutineNotificationRequest) {
        alarmManager.set(
            AlarmManager.RTC_WAKEUP,
            request.triggerAtMillis,
            pendingIntentFor(request, PendingIntent.FLAG_UPDATE_CURRENT),
        )
    }

    private fun cancelKnownRequests(routines: List<Routine>) {
        routines.forEach { routine ->
            KnownLeadMinutes.forEach { leadMinutes ->
                alarmManager.cancel(
                    pendingIntentFor(
                        routineId = routine.id,
                        leadMinutes = leadMinutes,
                        flags = PendingIntent.FLAG_NO_CREATE,
                    ) ?: return@forEach,
                )
            }
        }
    }

    private fun pendingIntentFor(
        request: RoutineNotificationRequest,
        flags: Int,
    ): PendingIntent = requireNotNull(
        pendingIntentFor(request.routineId, request.leadMinutes, flags, request.routineTitle),
    )

    private fun pendingIntentFor(
        routineId: String,
        leadMinutes: Int,
        flags: Int,
        routineTitle: String? = null,
    ): PendingIntent? {
        val intent = Intent(appContext, RoutineNotificationReceiver::class.java).apply {
            action = ACTION_ROUTINE_REMINDER
            putExtra(EXTRA_ROUTINE_ID, routineId)
            putExtra(EXTRA_LEAD_MINUTES, leadMinutes)
            routineTitle?.let { putExtra(EXTRA_ROUTINE_TITLE, it) }
        }
        return PendingIntent.getBroadcast(
            appContext,
            stableRequestCode(routineId, leadMinutes),
            intent,
            flags or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

private val KnownLeadMinutes = listOf(10, 5)

class RoutineNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_ROUTINE_REMINDER || !context.canPostNotifications()) return
        ensureNotificationChannel(context)

        val routineId = intent.getStringExtra(EXTRA_ROUTINE_ID) ?: return
        val routineTitle = intent.getStringExtra(EXTRA_ROUTINE_TITLE) ?: return
        val leadMinutes = intent.getIntExtra(EXTRA_LEAD_MINUTES, 0)
        val title = context.getString(R.string.notification_routine_reminder_title)
        val body = context.resources.getQuantityString(
            R.plurals.notification_routine_reminder_body,
            leadMinutes,
            leadMinutes,
            routineTitle,
        )
        val contentIntent = PendingIntent.getActivity(
            context,
            stableRequestCode(routineId, leadMinutes),
            Intent(context, MainActivity::class.java).apply {
                action = ACTION_OPEN_ROUTINE
                putExtra(EXTRA_ROUTINE_ID, routineId)
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, ROUTINE_REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_feedback_check)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        try {
            NotificationManagerCompat.from(context).notify(stableRequestCode(routineId, leadMinutes), notification)
        } catch (_: SecurityException) {
            return
        }
    }
}

fun Context.canPostNotifications(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

private fun ensureNotificationChannel(context: Context) {
    val manager = context.getSystemService(NotificationManager::class.java)
    val channel = NotificationChannel(
        ROUTINE_REMINDER_CHANNEL_ID,
        context.getString(R.string.notification_routine_reminder_channel_name),
        NotificationManager.IMPORTANCE_DEFAULT,
    ).apply {
        description = context.getString(R.string.notification_routine_reminder_channel_description)
    }
    manager.createNotificationChannel(channel)
}

const val ACTION_OPEN_ROUTINE = "com.example.steppie.action.OPEN_ROUTINE"
const val EXTRA_ROUTINE_ID = "com.example.steppie.extra.ROUTINE_ID"

private const val ACTION_ROUTINE_REMINDER = "com.example.steppie.action.ROUTINE_REMINDER"
private const val EXTRA_ROUTINE_TITLE = "com.example.steppie.extra.ROUTINE_TITLE"
private const val EXTRA_LEAD_MINUTES = "com.example.steppie.extra.LEAD_MINUTES"
private const val ROUTINE_REMINDER_CHANNEL_ID = "routine_reminders"
