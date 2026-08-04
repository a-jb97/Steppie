package com.example.steppie.ui.guardian

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.steppie.R
import com.example.steppie.ui.components.SteppieButton

@Composable
internal fun GuardianHomeScreen(
    onCloseToChild: () -> Unit,
    onOpenRoutineSetCreate: () -> Unit,
    onOpenRoutineEdit: () -> Unit,
    onOpenTemplateSelect: () -> Unit,
    onOpenEnvironmentSettings: () -> Unit,
    onOpenRecords: () -> Unit,
    onOpenSecurity: () -> Unit,
    onShowOutOfScopeNotice: () -> Unit,
) {
    GuardianScaffold(
        title = stringResource(R.string.guardian_home_title),
        subtitle = stringResource(R.string.guardian_home_subtitle),
        bottom = {
            SteppieButton(
                label = stringResource(R.string.guardian_done),
                onClick = onCloseToChild,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        GuardianMenuCard(R.drawable.ic_guardian_menu_routine_set_create, stringResource(R.string.guardian_menu_create_routine_set), stringResource(R.string.guardian_menu_create_routine_set_desc), onOpenRoutineSetCreate)
        GuardianMenuCard(R.drawable.ic_guardian_menu_template, stringResource(R.string.guardian_template_action), stringResource(R.string.guardian_template_home_desc), onOpenTemplateSelect)
        GuardianMenuCard(R.drawable.ic_guardian_menu_routine, stringResource(R.string.guardian_menu_routine), stringResource(R.string.guardian_menu_routine_desc), onOpenRoutineEdit)
        GuardianMenuCard(R.drawable.ic_guardian_menu_settings, stringResource(R.string.guardian_menu_feedback), stringResource(R.string.guardian_menu_feedback_desc), onOpenEnvironmentSettings)
        GuardianMenuCard(R.drawable.ic_guardian_menu_records, stringResource(R.string.guardian_menu_records), stringResource(R.string.guardian_menu_records_desc), onOpenRecords)
        GuardianMenuCard(R.drawable.ic_guardian_menu_security, stringResource(R.string.guardian_menu_security), stringResource(R.string.guardian_menu_security_desc), onOpenSecurity)
    }
}
