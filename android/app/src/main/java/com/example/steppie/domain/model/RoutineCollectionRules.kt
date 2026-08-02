package com.example.steppie.domain.model

val Routine.isVisible: Boolean
    get() = deletedAt == null

val Routine.isAvailableToChild: Boolean
    get() = isActive && isVisible

val RoutineSet.isVisible: Boolean
    get() = deletedAt == null

val RoutineSet.isAvailableToChild: Boolean
    get() = isActive && isVisible

fun Iterable<Routine>.inRoutineOrder(): List<Routine> = sortedBy(Routine::order)

fun Iterable<Routine>.visibleRoutinesInOrder(): List<Routine> =
    filter(Routine::isVisible).inRoutineOrder()

fun Iterable<Routine>.childRoutinesInOrder(): List<Routine> =
    filter(Routine::isAvailableToChild).inRoutineOrder()

fun Iterable<RoutineSet>.childRoutineSetsInScheduleOrder(): List<RoutineSet> =
    filter(RoutineSet::isAvailableToChild)
        .sortedWith(
            compareBy<RoutineSet> { it.startTime != null }
                .thenBy { it.startTime }
                .thenBy { it.createdAt }
                .thenBy { it.id },
        )
