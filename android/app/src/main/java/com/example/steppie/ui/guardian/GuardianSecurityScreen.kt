package com.example.steppie.ui.guardian

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.steppie.R
import com.example.steppie.ui.components.SteppieButton
import com.example.steppie.ui.components.SteppieButtonState
import com.example.steppie.ui.components.SteppieButtonStyle
import com.example.steppie.ui.theme.SteppieCornerRadius
import com.example.steppie.ui.theme.SteppieLayout
import com.example.steppie.ui.theme.SteppieSpacing
import com.example.steppie.ui.theme.SteppieStroke
import com.example.steppie.ui.theme.SteppieTheme

@Composable
internal fun GuardianSecurityScreen(
    onNavigateBack: () -> Unit,
    onOpenPinChange: () -> Unit,
    onOpenRecoveryCode: () -> Unit,
    onOpenBackupRestore: () -> Unit,
) {
    GuardianScaffold(
        title = stringResource(R.string.guardian_security_title),
        subtitle = stringResource(R.string.guardian_security_subtitle),
        onBack = onNavigateBack,
    ) {
        GuardianMenuCard(R.drawable.ic_guardian_security_warning, stringResource(R.string.guardian_pin_change), stringResource(R.string.guardian_pin_change_desc), onOpenPinChange)
        GuardianMenuCard(R.drawable.ic_guardian_security_warning, stringResource(R.string.guardian_recovery_code), stringResource(R.string.guardian_recovery_code_desc), onOpenRecoveryCode)
        GuardianMenuCard(R.drawable.ic_guardian_security_backup, stringResource(R.string.guardian_backup), stringResource(R.string.guardian_backup_desc), onOpenBackupRestore)
        GuardianPrivacyNote()
    }
}

@Composable
internal fun GuardianRecoveryCodeScreen(
    state: GuardianModeUiState,
    onCancelRecoveryPinReset: () -> Unit,
    onRecoveryCodeChange: (String) -> Unit,
    onConfirmRecoveryCode: () -> Unit,
    onCloseRecoveryCode: () -> Unit,
) {
    when (state.recoveryStep) {
        GuardianRecoveryStep.ShowCode -> Box(Modifier.fillMaxSize())
        GuardianRecoveryStep.EnterCodeForPinReset -> Box(Modifier.fillMaxSize())
        null -> GuardianScaffold(
            title = stringResource(R.string.guardian_recovery_show_title),
            subtitle = stringResource(R.string.guardian_recovery_show_subtitle),
            onBack = onCancelRecoveryPinReset,
        ) {
            ErrorMessage(stringResource(R.string.guardian_recovery_unavailable))
        }
    }
}

@Composable
internal fun BoxScope.GuardianRecoveryCodeInputSheet(
    recoveryDigits: String,
    recoveryError: GuardianRecoveryError?,
    onRecoveryCodeChange: (String) -> Unit,
    onConfirmRecoveryCode: () -> Unit,
    onCancelRecoveryPinReset: () -> Unit,
) {
    val inputDescription = stringResource(R.string.a11y_recovery_code_input)
    val recoveryErrorMessage = recoveryError?.let { recoveryErrorMessage(it) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.32f)),
    )
    Column(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .heightIn(max = 560.dp)
            .clip(RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp))
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(
                start = SteppieLayout.ChildScreenPadding,
                top = SteppieSpacing.Small,
                end = SteppieLayout.ChildScreenPadding,
                bottom = SteppieLayout.ChildScreenPadding,
            ),
        verticalArrangement = Arrangement.spacedBy(SteppieSpacing.Large),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(width = 84.dp, height = 6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.outline),
        )
        Column(verticalArrangement = Arrangement.spacedBy(SteppieSpacing.ExtraSmall)) {
            Text(
                text = stringResource(R.string.guardian_recovery_enter_title),
                color = MaterialTheme.colorScheme.onSurface,
                style = SteppieTheme.typography.guardianTitle,
            )
            Text(
                text = stringResource(R.string.guardian_recovery_enter_subtitle),
                color = MaterialTheme.colorScheme.onSurface,
                style = SteppieTheme.typography.guardianBody,
            )
        }
        OutlinedTextField(
            value = recoveryDigits,
            onValueChange = onRecoveryCodeChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 92.dp)
                .semantics {
                    contentDescription = inputDescription
                },
            placeholder = {
                Text(
                    text = stringResource(R.string.guardian_recovery_code_placeholder),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = SteppieTheme.typography.childCardTitle,
                )
            },
            singleLine = true,
            textStyle = SteppieTheme.typography.childCardTitle.copy(textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            isError = recoveryError != null,
        )
        recoveryErrorMessage?.let { ErrorMessage(it) }
        SteppieButton(
            label = stringResource(R.string.guardian_recovery_set_new_pin),
            onClick = onConfirmRecoveryCode,
            modifier = Modifier.fillMaxWidth(),
            state = if (recoveryDigits.length == 6) {
                SteppieButtonState.Enabled
            } else {
                SteppieButtonState.Disabled
            },
        )
        SteppieButton(
            label = stringResource(R.string.action_cancel),
            onClick = onCancelRecoveryPinReset,
            modifier = Modifier.fillMaxWidth(),
            style = SteppieButtonStyle.Secondary,
        )
    }
}

@Composable
private fun recoveryErrorMessage(error: GuardianRecoveryError): String = when (error) {
    GuardianRecoveryError.CodeMismatch -> stringResource(R.string.guardian_recovery_code_mismatch)
}

@Composable
@Suppress("DEPRECATION")
internal fun BoxScope.GuardianRecoveryCodeSheet(
    recoveryCode: String,
    onCloseRecoveryCode: () -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current
    var showCopiedDialog by remember { mutableStateOf(false) }
    val recoveryCodeDescription = stringResource(R.string.a11y_recovery_code_value, recoveryCode)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.54f)),
    )
    Column(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .heightIn(max = 620.dp)
            .clip(RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp))
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(
                start = SteppieLayout.ChildScreenPadding,
                top = SteppieSpacing.Small,
                end = SteppieLayout.ChildScreenPadding,
                bottom = SteppieLayout.ChildScreenPadding,
            ),
        verticalArrangement = Arrangement.spacedBy(SteppieSpacing.Large),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(width = 96.dp, height = 6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.outline),
        )
        Column(verticalArrangement = Arrangement.spacedBy(SteppieSpacing.ExtraSmall)) {
            Text(
                text = stringResource(R.string.guardian_recovery_show_title),
                color = MaterialTheme.colorScheme.onSurface,
                style = SteppieTheme.typography.guardianTitle,
            )
            Text(
                text = stringResource(R.string.guardian_recovery_show_subtitle),
                color = MaterialTheme.colorScheme.onSurface,
                style = SteppieTheme.typography.guardianBody,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 116.dp)
                .clip(RoundedCornerShape(SteppieCornerRadius.Card))
                .border(SteppieStroke.Divider, MaterialTheme.colorScheme.outline, RoundedCornerShape(SteppieCornerRadius.Card))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .semantics {
                    contentDescription = recoveryCodeDescription
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = recoveryCode.chunked(1).joinToString(" "),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                style = SteppieTheme.typography.childCardTitle,
            )
        }
        Text(
            text = stringResource(R.string.guardian_recovery_one_time_notice),
            color = MaterialTheme.colorScheme.onSurface,
            style = SteppieTheme.typography.guardianBody,
        )
        SteppieButton(
            label = stringResource(R.string.action_copy),
            onClick = {
                clipboardManager.setText(AnnotatedString(recoveryCode))
                showCopiedDialog = true
            },
            modifier = Modifier.fillMaxWidth(),
            style = SteppieButtonStyle.Secondary,
        )
        SteppieButton(
            label = stringResource(R.string.guardian_recovery_acknowledge),
            onClick = onCloseRecoveryCode,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (showCopiedDialog) {
        AlertDialog(
            onDismissRequest = { showCopiedDialog = false },
            title = { Text(stringResource(R.string.guardian_recovery_copy_done_title)) },
            text = { Text(stringResource(R.string.guardian_recovery_copy_done_body)) },
            confirmButton = {
                SteppieButton(
                    label = stringResource(R.string.action_ok),
                    onClick = { showCopiedDialog = false },
                )
            },
        )
    }
}

@Composable
internal fun GuardianBackupRestoreScreen(
    state: GuardianModeUiState,
    onOpenSecurity: () -> Unit,
    onCreateBackupFile: () -> Unit,
    onOpenRestoreFile: () -> Unit,
) {
    GuardianScaffold(
        title = stringResource(R.string.guardian_backup_title),
        subtitle = stringResource(R.string.guardian_backup_subtitle),
        onBack = onOpenSecurity,
    ) {
        WarningMessage(stringResource(R.string.guardian_backup_privacy_notice))
        SteppieButton(
            label = stringResource(R.string.guardian_backup_create),
            onClick = onCreateBackupFile,
            modifier = Modifier.fillMaxWidth(),
            state = if (state.backupInProgress) SteppieButtonState.Loading else SteppieButtonState.Enabled,
        )
        SteppieButton(
            label = stringResource(R.string.guardian_restore_select),
            onClick = onOpenRestoreFile,
            modifier = Modifier.fillMaxWidth(),
            style = SteppieButtonStyle.Secondary,
            state = if (state.backupInProgress) SteppieButtonState.Loading else SteppieButtonState.Enabled,
        )
        WarningMessage(stringResource(R.string.guardian_restore_replace_warning))
        state.backupMessage?.let { SuccessMessage(it) }
        state.backupError?.takeIf { state.pendingRestorePreview == null }?.let { ErrorMessage(it) }
    }
}

@Composable
private fun SuccessMessage(message: String) {
    Text(
        text = message,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SteppieCornerRadius.Control))
            .background(SteppieTheme.colors.cardMint)
            .border(SteppieStroke.Divider, SteppieTheme.colors.success, RoundedCornerShape(SteppieCornerRadius.Control))
            .semantics { liveRegion = LiveRegionMode.Polite }
            .padding(SteppieSpacing.Medium),
        color = MaterialTheme.colorScheme.onSurface,
        style = SteppieTheme.typography.guardianCaption,
    )
}

@Composable
private fun GuardianPrivacyNote() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 148.dp)
            .clip(RoundedCornerShape(SteppieCornerRadius.Card))
            .background(SteppieTheme.colors.cardLemon)
            .border(SteppieStroke.Divider, SteppieTheme.colors.warning, RoundedCornerShape(SteppieCornerRadius.Card))
            .padding(SteppieSpacing.Medium),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = stringResource(R.string.guardian_privacy_title),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = SteppieTheme.typography.guardianSection,
        )
        Text(
            text = stringResource(R.string.guardian_privacy_body),
            color = MaterialTheme.colorScheme.onSurface,
            style = SteppieTheme.typography.guardianCaption,
        )
    }
}
