package com.example.steppie.ui.child

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.painter.BitmapPainter
import android.graphics.BitmapFactory
import com.example.steppie.R
import com.example.steppie.data.photo.RoutinePhotoStore
import com.example.steppie.domain.model.IconRef
import com.example.steppie.domain.model.Routine
import com.example.steppie.ui.components.RoutineCardColor

internal fun Routine.cardColor(): RoutineCardColor = when (colorToken) {
    "color.card.mint" -> RoutineCardColor.Mint
    "color.card.lemon" -> RoutineCardColor.Lemon
    "color.card.peach" -> RoutineCardColor.Peach
    "color.card.lavender" -> RoutineCardColor.Lavender
    "color.card.rose" -> RoutineCardColor.Rose
    else -> RoutineCardColor.Sky
}

@Composable
internal fun RoutineIcon(
    icon: IconRef,
    focus: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val photoStore = remember(context) { RoutinePhotoStore(context) }
    val photoBitmap = remember(icon) {
        (icon as? IconRef.Photo)?.let { photo ->
            val file = photoStore.fileFor(photo)
            if (file.isFile) BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() else null
        }
    }
    if (photoBitmap != null) {
        Image(
            painter = BitmapPainter(photoBitmap),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(RoundedCornerShape(percent = 25)),
        )
        return
    }
    val resource = when (icon) {
        is IconRef.Builtin -> builtinIconResource(icon.name, focus)
        is IconRef.Photo -> R.drawable.ic_routine_star_card
    }
    Image(
        painter = painterResource(resource),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier,
    )
}

@DrawableRes
private fun builtinIconResource(name: String, focus: Boolean): Int = when (name) {
    "wake-up" -> if (focus) R.drawable.ic_routine_wake_up_card else R.drawable.ic_routine_wake_up
    "wash-face" -> if (focus) R.drawable.ic_routine_wash_face_card else R.drawable.ic_routine_wash_face
    "brush-teeth" -> if (focus) R.drawable.ic_routine_brush_teeth_card else R.drawable.ic_routine_brush_teeth
    "get-dressed" -> if (focus) R.drawable.ic_routine_get_dressed_card else R.drawable.ic_routine_get_dressed
    "breakfast" -> if (focus) R.drawable.ic_routine_breakfast_card else R.drawable.ic_routine_breakfast
    "pack-bag" -> if (focus) R.drawable.ic_routine_pack_bag_card else R.drawable.ic_routine_pack_bag
    "school" -> if (focus) R.drawable.ic_routine_school_card else R.drawable.ic_routine_school
    "book" -> if (focus) R.drawable.ic_routine_book_card else R.drawable.ic_routine_book
    "pencil" -> if (focus) R.drawable.ic_routine_pencil_card else R.drawable.ic_routine_pencil
    "lunch" -> if (focus) R.drawable.ic_routine_lunch_card else R.drawable.ic_routine_lunch
    "playground" -> if (focus) R.drawable.ic_routine_playground_card else R.drawable.ic_routine_playground
    "bus" -> if (focus) R.drawable.ic_routine_bus_card else R.drawable.ic_routine_bus
    "bath" -> if (focus) R.drawable.ic_routine_bath_card else R.drawable.ic_routine_bath
    "pajamas" -> if (focus) R.drawable.ic_routine_pajamas_card else R.drawable.ic_routine_pajamas
    "story-book" -> if (focus) R.drawable.ic_routine_story_book_card else R.drawable.ic_routine_story_book
    "toilet" -> if (focus) R.drawable.ic_routine_toilet_card else R.drawable.ic_routine_toilet
    "sleep" -> if (focus) R.drawable.ic_routine_sleep_card else R.drawable.ic_routine_sleep
    "home" -> if (focus) R.drawable.ic_routine_home_card else R.drawable.ic_routine_home
    "meal" -> if (focus) R.drawable.ic_routine_meal_card else R.drawable.ic_routine_meal
    "snack" -> if (focus) R.drawable.ic_routine_snack_card else R.drawable.ic_routine_snack
    "medicine" -> if (focus) R.drawable.ic_routine_medicine_card else R.drawable.ic_routine_medicine
    "walk" -> if (focus) R.drawable.ic_routine_walk_card else R.drawable.ic_routine_walk
    "therapy" -> if (focus) R.drawable.ic_routine_therapy_card else R.drawable.ic_routine_therapy
    "music" -> if (focus) R.drawable.ic_routine_music_card else R.drawable.ic_routine_music
    "art" -> if (focus) R.drawable.ic_routine_art_card else R.drawable.ic_routine_art
    "clean-up" -> if (focus) R.drawable.ic_routine_clean_up_card else R.drawable.ic_routine_clean_up
    "timer" -> if (focus) R.drawable.ic_routine_timer_card else R.drawable.ic_routine_timer
    else -> if (focus) R.drawable.ic_routine_star_card else R.drawable.ic_routine_star
}
