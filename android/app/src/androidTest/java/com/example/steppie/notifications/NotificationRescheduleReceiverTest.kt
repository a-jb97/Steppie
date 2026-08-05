package com.example.steppie.notifications

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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
            PackageManager.GET_PERMISSIONS or PackageManager.GET_RECEIVERS,
        )
        val receiverNames = packageInfo.receivers.orEmpty().mapTo(mutableSetOf()) { it.name }
        val requestedPermissions = packageInfo.requestedPermissions.orEmpty().toSet()

        assertTrue(NotificationRescheduleReceiver::class.java.name in receiverNames)
        assertTrue(Manifest.permission.RECEIVE_BOOT_COMPLETED in requestedPermissions)
        assertTrue(receiverRegisteredFor(Intent.ACTION_BOOT_COMPLETED))
        assertTrue(receiverRegisteredFor(Intent.ACTION_MY_PACKAGE_REPLACED))
    }

    private fun receiverRegisteredFor(action: String): Boolean = context.packageManager
        .queryBroadcastReceivers(Intent(action).setPackage(context.packageName), PackageManager.MATCH_ALL)
        .any { it.activityInfo.name == NotificationRescheduleReceiver::class.java.name }
}
