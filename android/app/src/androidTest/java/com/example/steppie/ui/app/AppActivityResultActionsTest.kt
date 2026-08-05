package com.example.steppie.ui.app

import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test

class AppActivityResultActionsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun featureActivityResultActionsRegisterTogether() {
        composeRule.setContent {
            rememberPhotoActivityActions(onPhotoImport = {})
            rememberBackupDocumentActions(
                onCreateDocument = {},
                onOpenDocument = {},
            )
            rememberNotificationPermissionActions(onPermissionResult = {})
            Box(
                Modifier
                    .size(1.dp)
                    .testTag("activity_result_actions_ready"),
            )
        }

        composeRule.onNodeWithTag("activity_result_actions_ready").assertIsDisplayed()
    }

    @Test
    fun photoSelectionUsesTheSystemPhotoPickerContract() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = photoPickerContract().createIntent(
            context,
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
        )

        assertEquals(MediaStore.ACTION_PICK_IMAGES, intent.action)
        assertNotEquals(Intent.ACTION_PICK, intent.action)
        assertEquals("image/*", intent.type)
    }
}
