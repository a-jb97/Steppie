package com.example.steppie.ui.guardian

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.steppie.R
import com.example.steppie.ui.theme.SteppieLayout
import com.example.steppie.ui.theme.SteppieSpacing
import com.example.steppie.ui.theme.SteppieTheme
import com.example.steppie.ui.tutorial.TutorialTarget
import com.example.steppie.ui.tutorial.tutorialAnchor

@Composable
internal fun GuardianPinScreen(
    state: GuardianModeUiState,
    onDigit: (Int) -> Unit,
    onDeletePinDigit: () -> Unit,
    onOpenRecoveryPinReset: () -> Unit,
) {
    val pinProgressDescription = stringResource(R.string.a11y_pin_progress, state.pinDigits.length)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("guardian_pin")
            .verticalScroll(rememberScrollState())
            .padding(
                start = SteppieLayout.ChildScreenPadding,
                top = SteppieLayout.GuardianScreenPadding,
                end = SteppieLayout.ChildScreenPadding,
                bottom = SteppieLayout.ChildScreenPadding,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GuardianTopBar(
            title = when (state.pinMode) {
                GuardianPinMode.Enter -> stringResource(R.string.guardian_pin_title)
                GuardianPinMode.Setup -> stringResource(R.string.guardian_pin_setup_title)
                GuardianPinMode.SetupConfirm -> stringResource(R.string.guardian_pin_setup_confirm_title)
                GuardianPinMode.ChangeCurrent -> stringResource(R.string.guardian_pin_change_current_title)
                GuardianPinMode.ChangeNew -> stringResource(R.string.guardian_pin_change_new_title)
                GuardianPinMode.RecoveryRegenerateConfirm -> stringResource(R.string.guardian_recovery_confirm_pin_title)
                GuardianPinMode.RecoveryResetNew -> stringResource(R.string.guardian_recovery_new_pin_title)
            },
            subtitle = when (state.pinMode) {
                GuardianPinMode.Enter -> stringResource(R.string.guardian_pin_subtitle)
                GuardianPinMode.Setup -> stringResource(R.string.guardian_pin_setup_subtitle)
                GuardianPinMode.SetupConfirm -> stringResource(R.string.guardian_pin_setup_confirm_subtitle)
                GuardianPinMode.ChangeCurrent -> stringResource(R.string.guardian_pin_change_current_subtitle)
                GuardianPinMode.ChangeNew -> stringResource(R.string.guardian_pin_change_new_subtitle)
                GuardianPinMode.RecoveryRegenerateConfirm -> stringResource(R.string.guardian_recovery_confirm_pin_subtitle)
                GuardianPinMode.RecoveryResetNew -> stringResource(R.string.guardian_recovery_new_pin_subtitle)
            },
        )
        Spacer(Modifier.height(SteppieSpacing.Large))
        Row(
            modifier = Modifier.semantics {
                contentDescription = pinProgressDescription
            },
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            if (index < state.pinDigits.length) {
                                SteppieTheme.colors.progressComplete
                            } else {
                                SteppieTheme.colors.progressPending
                            },
                        ),
                )
            }
        }
        Spacer(Modifier.height(SteppieSpacing.Large))
        Box(Modifier.tutorialAnchor(TutorialTarget.GuardianMain)) {
            GuardianNumberKeypad(
                onDigit = onDigit,
                onDelete = onDeletePinDigit,
                hapticEnabled = state.appSettings.hapticEnabled,
            )
        }
        if (state.pinError != null) {
            Spacer(Modifier.height(28.dp))
            ErrorMessage(state.pinError)
        }
        if (state.pinMode == GuardianPinMode.Enter) {
            Spacer(Modifier.height(SteppieSpacing.Small))
            Text(
                text = stringResource(R.string.guardian_recovery_reset_pin_action),
                modifier = Modifier
                    .heightIn(min = SteppieLayout.GuardianMinimumTouchTarget)
                    .clickable(role = Role.Button, onClick = onOpenRecoveryPinReset)
                    .padding(horizontal = SteppieSpacing.Small, vertical = SteppieSpacing.ExtraSmall),
                color = MaterialTheme.colorScheme.primary,
                style = SteppieTheme.typography.button,
            )
        }
    }
}
