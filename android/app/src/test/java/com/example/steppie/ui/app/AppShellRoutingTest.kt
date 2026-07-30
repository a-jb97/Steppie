package com.example.steppie.ui.app

import com.example.steppie.notifications.ACTION_OPEN_ROUTINE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppShellRoutingTest {
    @Test
    fun `bootstrap blocks both app modes until initialization completes`() {
        assertEquals(
            AppMode.Bootstrap,
            resolveAppMode(bootstrapComplete = false, guardianActive = false),
        )
        assertEquals(
            AppMode.Bootstrap,
            resolveAppMode(bootstrapComplete = false, guardianActive = true),
        )
    }

    @Test
    fun `completed bootstrap selects guardian only while guardian mode is active`() {
        assertEquals(
            AppMode.Child,
            resolveAppMode(bootstrapComplete = true, guardianActive = false),
        )
        assertEquals(
            AppMode.Guardian,
            resolveAppMode(bootstrapComplete = true, guardianActive = true),
        )
    }

    @Test
    fun `notification target accepts only the open routine action with an id`() {
        assertEquals(
            "routine-id",
            resolveNotificationTarget(
                action = ACTION_OPEN_ROUTINE,
                routineId = "routine-id",
                expectedAction = ACTION_OPEN_ROUTINE,
            ),
        )
        assertNull(
            resolveNotificationTarget(
                action = "other-action",
                routineId = "routine-id",
                expectedAction = ACTION_OPEN_ROUTINE,
            ),
        )
        assertNull(
            resolveNotificationTarget(
                action = ACTION_OPEN_ROUTINE,
                routineId = null,
                expectedAction = ACTION_OPEN_ROUTINE,
            ),
        )
    }

    @Test
    fun `notification route waits for its routine then selects and consumes once`() {
        val pending = resolveNotificationRoute(
            targetRoutineId = "target",
            visibleRoutineIds = setOf("other"),
        )
        val ready = resolveNotificationRoute(
            targetRoutineId = "target",
            visibleRoutineIds = setOf("other", "target"),
        )
        val consumed = resolveNotificationRoute(
            targetRoutineId = null,
            visibleRoutineIds = setOf("other", "target"),
        )

        assertNull(pending.routineIdToSelect)
        assertFalse(pending.consumeRoute)
        assertEquals("target", ready.routineIdToSelect)
        assertTrue(ready.consumeRoute)
        assertNull(consumed.routineIdToSelect)
        assertFalse(consumed.consumeRoute)
    }

    @Test
    fun `photo picker routes recognized targets only when a uri is returned`() {
        assertEquals(
            PhotoImportRequest(PhotoTarget.RoutineDraft, "content://gallery/photo"),
            resolvePhotoPickerResult(
                targetName = PhotoTarget.RoutineDraft.name,
                uri = "content://gallery/photo",
            ),
        )
        assertEquals(
            PhotoImportRequest(PhotoTarget.RoutineSetStep, "content://gallery/step"),
            resolvePhotoPickerResult(
                targetName = PhotoTarget.RoutineSetStep.name,
                uri = "content://gallery/step",
            ),
        )
        assertNull(resolvePhotoPickerResult(targetName = "Unknown", uri = "content://gallery/photo"))
        assertNull(resolvePhotoPickerResult(targetName = PhotoTarget.RoutineDraft.name, uri = null))
    }

    @Test
    fun `camera imports only a successful result with its original target and uri`() {
        val target = PhotoTarget.RoutineDraft.name
        val uri = "content://camera/photo"

        assertEquals(
            PhotoImportRequest(PhotoTarget.RoutineDraft, uri),
            resolveCameraResult(targetName = target, uri = uri, succeeded = true),
        )
        assertNull(resolveCameraResult(targetName = target, uri = uri, succeeded = false))
        assertNull(resolveCameraResult(targetName = target, uri = null, succeeded = true))
        assertNull(resolveCameraResult(targetName = null, uri = uri, succeeded = true))
    }
}
