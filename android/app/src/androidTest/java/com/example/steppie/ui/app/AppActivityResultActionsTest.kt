package com.example.steppie.ui.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
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
}
