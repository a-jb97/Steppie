package com.example.steppie.ui.guardian

import com.example.steppie.domain.model.IconRef
import com.example.steppie.domain.model.LocalizedText
import com.example.steppie.domain.model.Routine
import com.example.steppie.domain.model.RoutineColorTokens
import com.example.steppie.domain.model.RoutineSet
import com.example.steppie.domain.model.newUuidV4
import java.time.Instant

enum class RoutineTemplateId { Morning, School, Bedtime }

data class RoutineTemplateStep(
    val titleKey: String,
    val title: LocalizedText,
    val iconName: String,
    val colorToken: String,
)

data class RoutineTemplate(
    val id: RoutineTemplateId,
    val nameKey: String,
    val name: LocalizedText,
    val steps: List<RoutineTemplateStep>,
) {
    fun instantiate(now: Instant): RoutineSet {
        val routineSetId = newUuidV4()
        return RoutineSet(
            id = routineSetId,
            name = name,
            isActive = false,
            createdAt = now,
            updatedAt = now,
            routines = steps.mapIndexed { index, step ->
                Routine(
                    routineSetId = routineSetId,
                    titleKey = step.titleKey,
                    title = step.title,
                    icon = IconRef.Builtin(step.iconName),
                    colorToken = step.colorToken,
                    order = index,
                    createdAt = now,
                    updatedAt = now,
                )
            },
        )
    }
}

object RoutineTemplates {
    val all: List<RoutineTemplate> = listOf(
        RoutineTemplate(
            id = RoutineTemplateId.Morning,
            nameKey = "template.morning.name",
            name = localized("아침 루틴", "Morning routine"),
            steps = listOf(
                step("routine.wakeUp", "일어나기", "Wake up", "wake-up", "color.card.sky"),
                step("routine.washFace", "세수하기", "Wash face", "wash-face", "color.card.mint"),
                step("routine.brushTeeth", "양치하기", "Brush teeth", "brush-teeth", "color.card.lemon"),
                step("routine.getDressed", "옷 입기", "Get dressed", "get-dressed", "color.card.peach"),
                step("routine.breakfast", "아침 먹기", "Eat breakfast", "breakfast", "color.card.lavender"),
                step("routine.packBag", "가방 챙기기", "Pack bag", "pack-bag", "color.card.rose"),
            ),
        ),
        RoutineTemplate(
            id = RoutineTemplateId.School,
            nameKey = "template.school.name",
            name = localized("학교 루틴", "School routine"),
            steps = listOf(
                step("routine.goSchool", "학교 가기", "Go to school", "bus", "color.card.sky"),
                step("routine.readBook", "책 읽기", "Read book", "book", "color.card.mint"),
                step("routine.lunch", "점심 먹기", "Eat lunch", "lunch", "color.card.lemon"),
                step("routine.play", "놀이하기", "Play", "playground", "color.card.peach"),
            ),
        ),
        RoutineTemplate(
            id = RoutineTemplateId.Bedtime,
            nameKey = "template.bedtime.name",
            name = localized("취침 루틴", "Bedtime routine"),
            steps = listOf(
                step("routine.bath", "목욕하기", "Take a bath", "bath", "color.card.sky"),
                step("routine.pajamas", "잠옷 입기", "Put on pajamas", "pajamas", "color.card.mint"),
                step("routine.sleep", "잠자기", "Sleep", "sleep", "color.card.lavender"),
            ),
        ),
    )

    fun find(id: RoutineTemplateId): RoutineTemplate? = all.firstOrNull { it.id == id }
}

private fun localized(ko: String, en: String): LocalizedText = LocalizedText(mapOf("ko" to ko, "en" to en))

private fun step(
    titleKey: String,
    ko: String,
    en: String,
    iconName: String,
    colorToken: String = RoutineColorTokens.DEFAULT,
): RoutineTemplateStep = RoutineTemplateStep(
    titleKey = titleKey,
    title = localized(ko, en),
    iconName = iconName,
    colorToken = colorToken,
)
