package com.example.steppie.data.local

import com.example.steppie.domain.model.IconRef
import com.example.steppie.domain.model.Routine
import com.example.steppie.domain.model.RoutineSet
import java.time.Instant
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val localTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

fun Routine.toEntity(): RoutineEntity {
    val builtin = icon as? IconRef.Builtin
    val photo = icon as? IconRef.Photo
    return RoutineEntity(
        id = id,
        routineSetId = routineSetId,
        titleKey = titleKey,
        localizedTitle = LocalizedTextCodec.encode(title),
        iconType = if (builtin != null) "builtin" else "photo",
        iconName = builtin?.name,
        localAssetId = photo?.localAssetId,
        backupAssetName = photo?.backupAssetName,
        colorToken = colorToken,
        sortOrder = order,
        scheduledTime = scheduledTime?.format(localTimeFormatter),
        isActive = isActive,
        createdAtEpochMillis = createdAt.toEpochMilli(),
        updatedAtEpochMillis = updatedAt.toEpochMilli(),
        deletedAtEpochMillis = deletedAt?.toEpochMilli(),
    )
}

fun RoutineEntity.toDomain(): Routine = Routine(
    id = id,
    routineSetId = routineSetId,
    titleKey = titleKey,
    title = LocalizedTextCodec.decode(localizedTitle),
    icon = when (iconType) {
        "builtin" -> IconRef.Builtin(requireNotNull(iconName))
        "photo" -> IconRef.Photo(requireNotNull(localAssetId), backupAssetName)
        else -> error("Unknown icon type: $iconType")
    },
    colorToken = colorToken,
    order = sortOrder,
    scheduledTime = scheduledTime?.let { LocalTime.parse(it, localTimeFormatter) },
    isActive = isActive,
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
    updatedAt = Instant.ofEpochMilli(updatedAtEpochMillis),
    deletedAt = deletedAtEpochMillis?.let(Instant::ofEpochMilli),
)

fun RoutineSet.toEntity(): RoutineSetEntity = RoutineSetEntity(
    id = id,
    localizedName = LocalizedTextCodec.encode(name),
    isActive = isActive,
    createdAtEpochMillis = createdAt.toEpochMilli(),
    updatedAtEpochMillis = updatedAt.toEpochMilli(),
    deletedAtEpochMillis = deletedAt?.toEpochMilli(),
)

fun RoutineSetWithRoutines.toDomain(): RoutineSet = routineSet.toDomain(
    routines = routines
        .asSequence()
        .filter { it.deletedAtEpochMillis == null }
        .sortedBy(RoutineEntity::sortOrder)
        .map(RoutineEntity::toDomain)
        .toList(),
)

fun RoutineSetEntity.toDomain(routines: List<Routine> = emptyList()): RoutineSet = RoutineSet(
    id = id,
    name = LocalizedTextCodec.decode(localizedName),
    isActive = isActive,
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
    updatedAt = Instant.ofEpochMilli(updatedAtEpochMillis),
    deletedAt = deletedAtEpochMillis?.let(Instant::ofEpochMilli),
    routines = routines,
)
