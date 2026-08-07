package com.example.steppie.notifications

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationRescheduleJobServiceTest {
    @Test
    fun `successful reconcile completes without retry`() = runTest {
        assertFalse(notificationRescheduleNeedsRetry {})
    }

    @Test
    fun `failed reconcile requests retry`() = runTest {
        assertTrue(
            notificationRescheduleNeedsRetry {
                throw IllegalStateException("temporary failure")
            },
        )
    }

    @Test
    fun `cancelled reconcile preserves coroutine cancellation`() {
        assertThrows(CancellationException::class.java) {
            runTest {
                notificationRescheduleNeedsRetry {
                    throw CancellationException("stopped")
                }
            }
        }
    }
}
