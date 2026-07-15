package com.example.steppie.ui.tutorial

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.steppie.R
import com.example.steppie.ui.theme.SteppieCornerRadius
import com.example.steppie.ui.theme.SteppieSpacing
import com.example.steppie.ui.theme.SteppieTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val Context.tutorialDataStore by preferencesDataStore(name = "tutorial_progress")

enum class TutorialScreen(val storageKey: String, val version: Int = 1) {
    ChildFocus("child_focus"),
    ChildList("child_list"),
    GuardianPin("guardian_pin"),
    GuardianHome("guardian_home"),
    RoutineManagement("routine_management"),
    RoutineSetCreate("routine_set_create"),
    TemplateSelect("template_select"),
    CardEdit("card_edit"),
    EnvironmentSettings("environment_settings"),
    Records("records"),
    RecordsCalendar("records_calendar"),
    Security("security"),
    RecoveryCode("recovery_code"),
    BackupRestore("backup_restore"),
}

data class TutorialStep(
    val titleRes: Int,
    val bodyRes: Int,
    val target: TutorialTarget,
)

enum class TutorialTarget { ChildCard, ChildProgress, ChildListNavigation, GuardianEntry, ChildList, GuardianContent, GuardianMain, GuardianBottom }

class TutorialAnchorRegistry internal constructor() {
    internal val bounds = mutableStateMapOf<TutorialTarget, Rect>()

    fun clear() = bounds.clear()
}

val LocalTutorialAnchorRegistry = compositionLocalOf<TutorialAnchorRegistry?> { null }

@Composable
fun rememberTutorialAnchorRegistry(): TutorialAnchorRegistry = remember { TutorialAnchorRegistry() }

@Composable
fun Modifier.tutorialAnchor(target: TutorialTarget): Modifier {
    val registry = LocalTutorialAnchorRegistry.current ?: return this
    return onGloballyPositioned { registry.bounds[target] = it.boundsInRoot() }
}

data class TutorialUiState(
    val screen: TutorialScreen? = null,
    val steps: List<TutorialStep> = emptyList(),
    val stepIndex: Int = 0,
    val visible: Boolean = false,
) {
    val currentStep: TutorialStep? get() = steps.getOrNull(stepIndex)
}

class TutorialProgressRepository(context: Context) {
    private val dataStore = context.applicationContext.tutorialDataStore

    val completedVersions: Flow<Map<TutorialScreen, Int>> = dataStore.data.map { preferences ->
        TutorialScreen.entries.associateWith { preferences[intPreferencesKey(it.storageKey)] ?: 0 }
    }

    suspend fun complete(screen: TutorialScreen) {
        dataStore.edit { it[intPreferencesKey(screen.storageKey)] = screen.version }
    }

    suspend fun reset() {
        dataStore.edit { it.clear() }
    }
}

class TutorialCoordinatorViewModel(
    private val repository: TutorialProgressRepository,
) : ViewModel() {
    private val currentScreen = MutableStateFlow<TutorialScreen?>(null)
    private val stepIndex = MutableStateFlow(0)
    private val restartToken = MutableStateFlow(0)

    val uiState = combine(
        currentScreen,
        stepIndex,
        repository.completedVersions,
        restartToken,
    ) { screen, index, completed, _ ->
        val steps = screen?.let(::tutorialStepsFor).orEmpty()
        TutorialUiState(
            screen = screen,
            steps = steps,
            stepIndex = index.coerceIn(0, (steps.lastIndex).coerceAtLeast(0)),
            visible = screen != null && steps.isNotEmpty() && (completed[screen] ?: 0) < screen.version,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TutorialUiState())

    fun showFor(screen: TutorialScreen?) {
        if (currentScreen.value != screen) {
            currentScreen.value = screen
            stepIndex.value = 0
        }
    }

    fun previous() {
        stepIndex.value = (stepIndex.value - 1).coerceAtLeast(0)
    }

    fun next() {
        val state = uiState.value
        if (state.stepIndex < state.steps.lastIndex) {
            stepIndex.value += 1
        } else {
            finishCurrent()
        }
    }

    fun finishCurrent() {
        val screen = currentScreen.value ?: return
        viewModelScope.launch { repository.complete(screen) }
    }

    fun resetAll() {
        viewModelScope.launch {
            repository.reset()
            stepIndex.value = 0
            restartToken.value += 1
        }
    }

    companion object {
        fun factory(repository: TutorialProgressRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    TutorialCoordinatorViewModel(repository) as T
            }
    }
}

internal fun tutorialStepsFor(screen: TutorialScreen): List<TutorialStep> = when (screen) {
    TutorialScreen.ChildFocus -> listOf(
        TutorialStep(R.string.tutorial_child_card_title, R.string.tutorial_child_card_body, TutorialTarget.ChildCard),
        TutorialStep(R.string.tutorial_progress_title, R.string.tutorial_progress_body, TutorialTarget.ChildProgress),
        TutorialStep(R.string.tutorial_child_list_title, R.string.tutorial_child_list_body, TutorialTarget.ChildListNavigation),
        TutorialStep(R.string.tutorial_guardian_entry_title, R.string.tutorial_guardian_entry_body, TutorialTarget.GuardianEntry),
    )
    TutorialScreen.ChildList -> listOf(
        TutorialStep(R.string.tutorial_list_readonly_title, R.string.tutorial_list_readonly_body, TutorialTarget.ChildList),
        TutorialStep(R.string.tutorial_list_select_title, R.string.tutorial_list_select_body, TutorialTarget.ChildList),
    )
    TutorialScreen.GuardianPin -> listOf(
        TutorialStep(R.string.tutorial_pin_title, R.string.tutorial_pin_body, TutorialTarget.GuardianMain),
    )
    TutorialScreen.GuardianHome -> listOf(
        TutorialStep(R.string.tutorial_guardian_home_title, R.string.tutorial_guardian_home_body, TutorialTarget.GuardianMain),
        TutorialStep(R.string.tutorial_guardian_done_title, R.string.tutorial_guardian_done_body, TutorialTarget.GuardianBottom),
    )
    TutorialScreen.RoutineManagement -> listOf(
        TutorialStep(R.string.tutorial_routine_manage_title, R.string.tutorial_routine_manage_body, TutorialTarget.GuardianMain),
        TutorialStep(R.string.tutorial_routine_order_title, R.string.tutorial_routine_order_body, TutorialTarget.GuardianMain),
    )
    TutorialScreen.RoutineSetCreate -> listOf(TutorialStep(R.string.tutorial_set_create_title, R.string.tutorial_set_create_body, TutorialTarget.GuardianMain))
    TutorialScreen.TemplateSelect -> listOf(TutorialStep(R.string.tutorial_template_title, R.string.tutorial_template_body, TutorialTarget.GuardianMain))
    TutorialScreen.CardEdit -> listOf(TutorialStep(R.string.tutorial_card_edit_title, R.string.tutorial_card_edit_body, TutorialTarget.GuardianMain))
    TutorialScreen.EnvironmentSettings -> listOf(
        TutorialStep(R.string.tutorial_environment_title, R.string.tutorial_environment_body, TutorialTarget.GuardianMain),
        TutorialStep(R.string.tutorial_notifications_title, R.string.tutorial_notifications_body, TutorialTarget.GuardianMain),
    )
    TutorialScreen.Records -> listOf(TutorialStep(R.string.tutorial_records_title, R.string.tutorial_records_body, TutorialTarget.GuardianMain))
    TutorialScreen.RecordsCalendar -> listOf(TutorialStep(R.string.tutorial_calendar_title, R.string.tutorial_calendar_body, TutorialTarget.GuardianMain))
    TutorialScreen.Security -> listOf(TutorialStep(R.string.tutorial_security_title, R.string.tutorial_security_body, TutorialTarget.GuardianMain))
    TutorialScreen.RecoveryCode -> listOf(TutorialStep(R.string.tutorial_recovery_title, R.string.tutorial_recovery_body, TutorialTarget.GuardianContent))
    TutorialScreen.BackupRestore -> listOf(TutorialStep(R.string.tutorial_backup_title, R.string.tutorial_backup_body, TutorialTarget.GuardianMain))
}

@Composable
fun TutorialOverlay(
    state: TutorialUiState,
    anchorRegistry: TutorialAnchorRegistry,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
) {
    val step = state.currentStep ?: return
    val title = stringResource(step.titleRes)
    val body = stringResource(step.bodyRes)
    val progress = stringResource(R.string.tutorial_step_progress, state.stepIndex + 1, state.steps.size)
    val semanticsDescription = "$title. $body. $progress"

    BackHandler { if (state.stepIndex > 0) onPrevious() else onSkip() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("tutorial_overlay")
            .background(Color.Black.copy(alpha = 0.62f))
            .semantics { contentDescription = semanticsDescription }
            .clickable(onClick = {}),
        contentAlignment = Alignment.Center,
    ) {
        val targetBounds = anchorRegistry.bounds[step.target]
        if (targetBounds != null) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 3.dp.toPx()
                val halfStroke = stroke / 2f
                val left = targetBounds.left.coerceIn(halfStroke, size.width - halfStroke)
                val top = targetBounds.top.coerceIn(halfStroke, size.height - halfStroke)
                val right = targetBounds.right.coerceIn(halfStroke, size.width - halfStroke)
                val bottom = targetBounds.bottom.coerceIn(halfStroke, size.height - halfStroke)
                if (right > left && bottom > top) {
                    drawRoundRect(
                        color = Color(0xFFF8FAFC),
                        topLeft = androidx.compose.ui.geometry.Offset(left, top),
                        size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
                        style = Stroke(width = stroke),
                    )
                }
            }
        }
        Column(
            modifier = Modifier
                .padding(SteppieSpacing.Large)
                .widthIn(max = 520.dp)
                .clip(RoundedCornerShape(SteppieCornerRadius.Sheet))
                .background(MaterialTheme.colorScheme.surface)
                .padding(SteppieSpacing.Large),
            verticalArrangement = Arrangement.spacedBy(SteppieSpacing.Medium),
        ) {
                Text(text = progress, color = MaterialTheme.colorScheme.primary, style = SteppieTheme.typography.guardianCaption)
                Text(
                    text = title,
                    modifier = Modifier.clearAndSetSemantics { heading() },
                    color = MaterialTheme.colorScheme.onSurface,
                    style = SteppieTheme.typography.guardianTitle,
                )
                Text(text = body, color = MaterialTheme.colorScheme.onSurfaceVariant, style = SteppieTheme.typography.guardianBody)
                Spacer(Modifier.height(SteppieSpacing.ExtraSmall))
                TutorialAction(
                    label = stringResource(R.string.tutorial_skip_screen),
                    onClick = onSkip,
                    modifier = Modifier.align(Alignment.End),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SteppieSpacing.Small, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (state.stepIndex > 0) {
                        TutorialAction(
                            label = stringResource(R.string.tutorial_previous),
                            onClick = onPrevious,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    TutorialAction(
                        if (state.stepIndex == state.steps.lastIndex) stringResource(R.string.tutorial_finish)
                        else stringResource(R.string.tutorial_next),
                        onNext,
                        modifier = Modifier.weight(1f),
                        primary = true,
                    )
                }
        }
    }
}

@Composable
private fun TutorialAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(SteppieCornerRadius.Control))
            .background(if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            .clearAndSetSemantics {
                contentDescription = label
                role = Role.Button
            }
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            maxLines = 1,
            softWrap = false,
            color = if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            style = SteppieTheme.typography.button,
        )
    }
}
