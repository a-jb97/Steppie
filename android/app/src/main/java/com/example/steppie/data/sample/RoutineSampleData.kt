package com.example.steppie.data.sample

import com.example.steppie.data.repository.InMemoryRoutineRepository
import com.example.steppie.domain.model.IconRef
import com.example.steppie.domain.model.LocalizedText
import com.example.steppie.domain.model.Routine
import com.example.steppie.domain.model.RoutineSet
import java.time.Instant
import java.time.LocalTime

object RoutineSampleData {
    private val createdAt = Instant.parse("2026-01-01T00:00:00Z")

    val morning = routineSet(
        id = "10000000-0000-4000-8000-000000000001",
        ko = "아침 루틴",
        en = "Morning routine",
        active = true,
        routines = listOf(
            routine("20000000-0000-4000-8000-000000000001", "routine.wakeUp", "일어나기", "Wake up", "wake-up", "color.card.sky", 0, "07:00"),
            routine("20000000-0000-4000-8000-000000000002", "routine.washFace", "세수하기", "Wash face", "wash-face", "color.card.mint", 1, "07:10"),
            routine("20000000-0000-4000-8000-000000000003", "routine.brushTeeth", "양치하기", "Brush teeth", "brush-teeth", "color.card.lemon", 2, "07:20"),
            routine("20000000-0000-4000-8000-000000000004", "routine.getDressed", "옷 입기", "Get dressed", "get-dressed", "color.card.peach", 3, "07:30"),
            routine("20000000-0000-4000-8000-000000000005", "routine.breakfast", "아침 먹기", "Eat breakfast", "breakfast", "color.card.lavender", 4, "07:40"),
            routine("20000000-0000-4000-8000-000000000006", "routine.packBag", "가방 챙기기", "Pack bag", "pack-bag", "color.card.rose", 5, "07:55"),
        ),
    )

    val school = routineSet(
        id = "10000000-0000-4000-8000-000000000002",
        ko = "학교 루틴",
        en = "School routine",
        routines = listOf(
            routine("20000000-0000-4000-8000-000000000011", "routine.goSchool", "학교 가기", "Go to school", "school", "color.card.sky", 0),
            routine("20000000-0000-4000-8000-000000000012", "routine.readBook", "책 읽기", "Read book", "book", "color.card.mint", 1),
            routine("20000000-0000-4000-8000-000000000013", "routine.lunch", "점심 먹기", "Eat lunch", "lunch", "color.card.lemon", 2),
            routine("20000000-0000-4000-8000-000000000014", "routine.play", "놀이하기", "Play", "playground", "color.card.peach", 3),
        ),
    )

    val bedtime = routineSet(
        id = "10000000-0000-4000-8000-000000000003",
        ko = "취침 루틴",
        en = "Bedtime routine",
        routines = listOf(
            routine("20000000-0000-4000-8000-000000000021", "routine.bath", "목욕하기", "Take a bath", "bath", "color.card.sky", 0, "20:00"),
            routine("20000000-0000-4000-8000-000000000022", "routine.pajamas", "잠옷 입기", "Put on pajamas", "pajamas", "color.card.mint", 1, "20:30"),
            routine("20000000-0000-4000-8000-000000000023", "routine.sleep", "잠자기", "Sleep", "sleep", "color.card.lavender", 2, "21:00"),
        ),
    )

    val routineSets: List<RoutineSet> = listOf(morning, school, bedtime)

    fun inMemoryRepository(): InMemoryRoutineRepository = InMemoryRoutineRepository(routineSets)

    private fun routineSet(
        id: String,
        ko: String,
        en: String,
        active: Boolean = false,
        routines: List<Routine>,
    ): RoutineSet = RoutineSet(
        id = id,
        name = LocalizedText(mapOf("ko" to ko, "en" to en)),
        isActive = active,
        createdAt = createdAt,
        updatedAt = createdAt,
        routines = routines.map { it.copy(routineSetId = id) },
    )

    private fun routine(
        id: String,
        titleKey: String,
        ko: String,
        en: String,
        icon: String,
        colorToken: String,
        order: Int,
        scheduledTime: String? = null,
    ): Routine = Routine(
        id = id,
        routineSetId = "10000000-0000-4000-8000-000000000001",
        titleKey = titleKey,
        title = LocalizedText(mapOf("ko" to ko, "en" to en)),
        icon = IconRef.Builtin(icon),
        colorToken = colorToken,
        order = order,
        scheduledTime = scheduledTime?.let(LocalTime::parse),
        createdAt = createdAt,
        updatedAt = createdAt,
    )
}
