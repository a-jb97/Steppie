package com.example.steppie.ui.preview

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.steppie.R
import com.example.steppie.ui.components.RoutineCard
import com.example.steppie.ui.components.RoutineCardColor
import com.example.steppie.ui.components.RoutineCardPresentation
import com.example.steppie.ui.components.RoutineCardState
import com.example.steppie.ui.components.SteppieButton
import com.example.steppie.ui.components.SteppieButtonSize
import com.example.steppie.ui.theme.SteppieLayout
import com.example.steppie.ui.theme.SteppieSpacing
import com.example.steppie.ui.theme.SteppieTheme

@Preview(
    name = "Phone portrait · font scale 1.3",
    showBackground = true,
    widthDp = 393,
    heightDp = 852,
    fontScale = 1.3f,
)
@Composable
private fun PhonePortraitDesignSystemPreview() {
    SteppieTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(SteppieLayout.ChildScreenPadding),
            verticalArrangement = Arrangement.spacedBy(SteppieSpacing.Large),
        ) {
            RoutineCard(
                title = "양치하기",
                state = RoutineCardState.Current,
                onClick = {},
                presentation = RoutineCardPresentation.Focus,
                meta = "카드를 누르면 완료",
            ) {
                RoutineIcon(R.drawable.ic_routine_brush_teeth_card, 128.dp)
            }
            SteppieButton(
                label = "다 했어요",
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                size = SteppieButtonSize.ChildLarge,
            )
        }
    }
}

@Preview(
    name = "Tablet landscape · dark",
    showBackground = true,
    widthDp = 1280,
    heightDp = 800,
)
@Composable
private fun TabletLandscapeDesignSystemPreview() {
    SteppieTheme(darkTheme = true) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            Column(
                modifier = Modifier
                    .width(SteppieLayout.SplitListWidth)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(SteppieSpacing.ExtraLarge),
                verticalArrangement = Arrangement.spacedBy(SteppieSpacing.Medium),
            ) {
                RoutineCard("양치하기", RoutineCardState.Current, {}) {
                    RoutineIcon(R.drawable.ic_routine_brush_teeth, 48.dp)
                }
                RoutineCard(
                    "옷 입기",
                    RoutineCardState.Completed,
                    {},
                    cardColor = RoutineCardColor.Mint,
                ) {
                    RoutineIcon(R.drawable.ic_routine_get_dressed, 48.dp)
                }
                RoutineCard(
                    "가방 챙기기",
                    RoutineCardState.Upcoming,
                    {},
                    cardColor = RoutineCardColor.Peach,
                    meta = "3 · 8:00",
                ) {
                    RoutineIcon(R.drawable.ic_routine_pack_bag, 48.dp)
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(SteppieSpacing.ExtraLarge),
            ) {
                RoutineCard(
                    title = "양치하기",
                    state = RoutineCardState.Current,
                    onClick = {},
                    presentation = RoutineCardPresentation.Focus,
                    focusMaxWidth = SteppieLayout.FocusCardTabletMaxWidth,
                    meta = "카드를 누르면 완료",
                ) {
                    RoutineIcon(R.drawable.ic_routine_brush_teeth_card, 128.dp)
                }
            }
        }
    }
}

@Composable
private fun RoutineIcon(
    @DrawableRes drawableRes: Int,
    size: Dp,
) {
    Image(
        painter = painterResource(drawableRes),
        contentDescription = null,
        modifier = Modifier.size(size),
    )
}
