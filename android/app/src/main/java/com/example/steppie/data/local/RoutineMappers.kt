package com.example.steppie.data.local

import com.example.steppie.domain.model.DailyLog
import com.example.steppie.domain.model.IconRef
import com.example.steppie.domain.model.LogStatus
import com.example.steppie.domain.model.Routine
import com.example.steppie.domain.model.RoutineSet
import com.example.steppie.domain.model.inRoutineOrder
import com.example.steppie.domain.model.visibleRoutinesInOrder
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val localDateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
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
    startTime = startTime?.format(localTimeFormatter),
    createdAtEpochMillis = createdAt.toEpochMilli(),
    updatedAtEpochMillis = updatedAt.toEpochMilli(),
    deletedAtEpochMillis = deletedAt?.toEpochMilli(),
)

fun RoutineSetWithRoutines.toDomain(): RoutineSet = routineSet.toDomain(
    routines = routines
        .map(RoutineEntity::toDomain)
        .visibleRoutinesInOrder(),
)

fun RoutineSetWithRoutines.toRecordsDomain(): RoutineSet = routineSet.toDomain(
    routines = routines
        .map(RoutineEntity::toDomain)
        .inRoutineOrder(),
)

fun RoutineSetEntity.toDomain(routines: List<Routine> = emptyList()): RoutineSet = RoutineSet(
    id = id,
    name = LocalizedTextCodec.decode(localizedName),
    isActive = isActive,
    startTime = startTime?.let { LocalTime.parse(it, localTimeFormatter) },
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
    updatedAt = Instant.ofEpochMilli(updatedAtEpochMillis),
    deletedAt = deletedAtEpochMillis?.let(Instant::ofEpochMilli),
    routines = routines,
)

fun DailyLog.toEntity(): DailyLogEntity = DailyLogEntity(
    id = id,
    date = date.format(localDateFormatter),
    routineId = routineId,
    routineSetId = routineSetId,
    status = status.storageValue,
    completedAtEpochMillis = completedAt?.toEpochMilli(),
    createdAtEpochMillis = createdAt.toEpochMilli(),
    updatedAtEpochMillis = updatedAt.toEpochMilli(),
)

fun DailyLogEntity.toDomain(): DailyLog = DailyLog(
    id = id,
    date = LocalDate.parse(date, localDateFormatter),
    routineId = routineId,
    routineSetId = routineSetId,
    status = LogStatus.fromStorageValue(status),
    completedAt = completedAtEpochMillis?.let(Instant::ofEpochMilli),
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
    updatedAt = Instant.ofEpochMilli(updatedAtEpochMillis),
)
