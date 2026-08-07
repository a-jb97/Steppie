package com.example.steppie.notifications

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import com.example.steppie.di.AppContainer
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

internal const val NotificationRescheduleJobId = 0x53544550
private val NotificationRescheduleBackoffMillis = TimeUnit.SECONDS.toMillis(30)

internal fun scheduleNotificationRescheduleJob(context: Context): Int {
    val scheduler = context.getSystemService(JobScheduler::class.java)
    return scheduler.schedule(notificationRescheduleJobInfo(context))
}

internal fun notificationRescheduleJobInfo(context: Context): JobInfo = JobInfo.Builder(
    NotificationRescheduleJobId,
    ComponentName(context, NotificationRescheduleJobService::class.java),
)
    .setPersisted(true)
    .setMinimumLatency(TimeUnit.SECONDS.toMillis(1))
    .setBackoffCriteria(
        NotificationRescheduleBackoffMillis,
        JobInfo.BACKOFF_POLICY_EXPONENTIAL,
    )
    .build()

class NotificationRescheduleJobService : JobService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var runningJob: Job? = null

    override fun onStartJob(params: JobParameters): Boolean {
        runningJob = scope.launch {
            val shouldRetry = notificationRescheduleNeedsRetry {
                AppContainer(applicationContext).notificationRescheduler.reconcileToday()
            }
            jobFinished(params, shouldRetry)
            runningJob = null
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        runningJob?.cancel()
        runningJob = null
        return true
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}

internal suspend fun notificationRescheduleNeedsRetry(
    reconcile: suspend () -> Unit,
): Boolean = try {
    reconcile()
    false
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (_: Exception) {
    true
}
