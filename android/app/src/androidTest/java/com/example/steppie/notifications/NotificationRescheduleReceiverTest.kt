package com.example.steppie.notifications

import android.Manifest
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationRescheduleReceiverTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun manifestRegistersRestartActionsAndBootPermission() {
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS or PackageManager.GET_RECEIVERS or PackageManager.GET_SERVICES,
        )
        val receiverNames = packageInfo.receivers.orEmpty().mapTo(mutableSetOf()) { it.name }
        val requestedPermissions = packageInfo.requestedPermissions.orEmpty().toSet()
        val jobService = packageInfo.services.orEmpty()
            .first { it.name == NotificationRescheduleJobService::class.java.name }

        assertTrue(NotificationRescheduleReceiver::class.java.name in receiverNames)
        assertTrue(Manifest.permission.RECEIVE_BOOT_COMPLETED in requestedPermissions)
        assertTrue(receiverRegisteredFor(Intent.ACTION_BOOT_COMPLETED))
        assertTrue(receiverRegisteredFor(Intent.ACTION_MY_PACKAGE_REPLACED))
        assertEquals("android.permission.BIND_JOB_SERVICE", jobService.permission)
        assertTrue(jobService.exported)
    }

    @Test
    fun restartJobIsPersistentAndUsesExponentialBackoff() {
        val jobInfo = notificationRescheduleJobInfo(context)

        assertTrue(jobInfo.isPersisted)
        assertEquals(JobInfo.BACKOFF_POLICY_EXPONENTIAL, jobInfo.backoffPolicy)
        assertEquals(30_000L, jobInfo.initialBackoffMillis)
        assertEquals(
            ComponentName(context, NotificationRescheduleJobService::class.java),
            jobInfo.service,
        )
        val scheduler = context.getSystemService(JobScheduler::class.java)
        try {
            assertEquals(JobScheduler.RESULT_SUCCESS, scheduler.schedule(jobInfo))
        } finally {
            scheduler.cancel(NotificationRescheduleJobId)
        }
    }

    private fun receiverRegisteredFor(action: String): Boolean = context.packageManager
        .queryBroadcastReceivers(Intent(action).setPackage(context.packageName), PackageManager.MATCH_ALL)
        .any { it.activityInfo.name == NotificationRescheduleReceiver::class.java.name }
}
