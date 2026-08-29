package com.example.steppie.ui.app

import com.example.steppie.notifications.ACTION_OPEN_ROUTINE
import com.example.steppie.notifications.EXTRA_ROUTINE_ID
import com.example.steppie.ui.child.ChildSinglePane
import com.example.steppie.ui.guardian.GuardianDestination
import com.example.steppie.ui.guardian.GuardianPinMode
import com.example.steppie.ui.tutorial.TutorialScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppShellRoutingTest {
    @Test
    fun notificationRouteUsesReleaseApplicationIdentity() {
        assertEquals("com.jade.steppie.action.OPEN_ROUTINE", ACTION_OPEN_ROUTINE)
        assertEquals("com.jade.steppie.extra.ROUTINE_ID", EXTRA_ROUTINE_ID)
    }

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
    fun `notification permission is requested only when required by the platform and still denied`() {
        assertFalse(shouldRequestNotificationPermission(apiLevel = 32, permissionGranted = false))
        assertTrue(shouldRequestNotificationPermission(apiLevel = 33, permissionGranted = false))
        assertFalse(shouldRequestNotificationPermission(apiLevel = 33, permissionGranted = true))
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

    @Test
    fun `child tutorial waits for loading then follows the active child pane`() {
        assertNull(
            resolveTutorialScreen(
                guardianActive = false,
                guardianDestination = GuardianDestination.Home,
                guardianPinMode = GuardianPinMode.Enter,
                guardianOverlayBlocking = false,
                childLoading = true,
                childSinglePane = ChildSinglePane.Focus,
            ),
        )
        assertEquals(
            TutorialScreen.ChildFocus,
            childTutorialScreen(ChildSinglePane.Focus),
        )
        assertEquals(
            TutorialScreen.ChildList,
            childTutorialScreen(ChildSinglePane.List),
        )
    }

    @Test
    fun `guardian tutorial is suppressed by blocking content and pin setup`() {
        assertNull(
            guardianTutorialScreen(
                destination = GuardianDestination.Home,
                overlayBlocking = true,
            ),
        )
        assertNull(
            guardianTutorialScreen(
                destination = GuardianDestination.Pin,
                pinMode = GuardianPinMode.Setup,
            ),
        )
        assertEquals(
            TutorialScreen.GuardianPin,
            guardianTutorialScreen(
                destination = GuardianDestination.Pin,
                pinMode = GuardianPinMode.Enter,
            ),
        )
    }

    @Test
    fun `guardian destinations resolve to their matching tutorial screens`() {
        val expected = mapOf(
            GuardianDestination.Home to TutorialScreen.GuardianHome,
            GuardianDestination.RoutineEdit to TutorialScreen.RoutineManagement,
            GuardianDestination.TemplateSelect to TutorialScreen.TemplateSelect,
            GuardianDestination.CardEdit to TutorialScreen.CardEdit,
            GuardianDestination.RoutineSetCreate to TutorialScreen.RoutineSetCreate,
            GuardianDestination.EnvironmentSettings to TutorialScreen.EnvironmentSettings,
            GuardianDestination.Records to TutorialScreen.Records,
            GuardianDestination.RecordsCalendar to TutorialScreen.RecordsCalendar,
            GuardianDestination.Security to TutorialScreen.Security,
            GuardianDestination.RecoveryCode to TutorialScreen.RecoveryCode,
            GuardianDestination.BackupRestore to TutorialScreen.BackupRestore,
        )

        expected.forEach { (destination, tutorial) ->
            assertEquals(tutorial, guardianTutorialScreen(destination))
        }
    }

    private fun childTutorialScreen(singlePane: ChildSinglePane): TutorialScreen? =
        resolveTutorialScreen(
            guardianActive = false,
            guardianDestination = GuardianDestination.Home,
            guardianPinMode = GuardianPinMode.Enter,
            guardianOverlayBlocking = false,
            childLoading = false,
            childSinglePane = singlePane,
        )

    private fun guardianTutorialScreen(
        destination: GuardianDestination,
        pinMode: GuardianPinMode = GuardianPinMode.Enter,
        overlayBlocking: Boolean = false,
    ): TutorialScreen? = resolveTutorialScreen(
        guardianActive = true,
        guardianDestination = destination,
        guardianPinMode = pinMode,
        guardianOverlayBlocking = overlayBlocking,
        childLoading = false,
        childSinglePane = ChildSinglePane.Focus,
    )
}
