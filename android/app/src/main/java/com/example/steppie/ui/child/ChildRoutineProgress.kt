package com.example.steppie.ui.child

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.steppie.R
import com.example.steppie.ui.theme.SteppieSpacing
import com.example.steppie.ui.theme.SteppieTheme
import com.example.steppie.ui.tutorial.TutorialTarget
import com.example.steppie.ui.tutorial.tutorialAnchor

@Composable
internal fun ChildHeader(
    title: String,
    subtitle: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(SteppieSpacing.TwoExtraSmall)) {
        Text(
            text = title,
            color = if (title == stringResource(R.string.child_feedback_title)) {
                SteppieTheme.colors.success
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            style = SteppieTheme.typography.childCardTitle,
        )
        Text(
            text = subtitle,
            color = MaterialTheme.colorScheme.onSurface,
            style = SteppieTheme.typography.guardianCaption,
        )
    }
}

@Composable
internal fun ProgressIndicator(completed: Int, total: Int) {
    val description = stringResource(R.string.a11y_progress, completed, total)
    Row(
        modifier = Modifier
            .tutorialAnchor(TutorialTarget.ChildProgress)
            .semantics(mergeDescendants = true) { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(SteppieSpacing.ExtraSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(total) { index ->
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (index < completed) {
                            SteppieTheme.colors.progressComplete
                        } else {
                            SteppieTheme.colors.progressPending
                        },
                    ),
            )
        }
        Text(
            text = stringResource(R.string.child_progress_label, completed, total),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = SteppieTheme.typography.childProgress,
        )
    }
}
