package com.example.steppie.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
    tableName = "routine_sets",
    indices = [Index(value = ["isActive", "deletedAtEpochMillis"])],
)
data class RoutineSetEntity(
    @PrimaryKey val id: String,
    val localizedName: String,
    val isActive: Boolean,
    val startTime: String?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val deletedAtEpochMillis: Long?,
)

@Entity(
    tableName = "routines",
    foreignKeys = [
        ForeignKey(
            entity = RoutineSetEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineSetId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["routineSetId"]),
        Index(value = ["routineSetId", "sortOrder", "deletedAtEpochMillis"]),
    ],
)
data class RoutineEntity(
    @PrimaryKey val id: String,
    val routineSetId: String,
    val titleKey: String?,
    val localizedTitle: String,
    val iconType: String,
    val iconName: String?,
    val localAssetId: String?,
    val backupAssetName: String?,
    val colorToken: String,
    val sortOrder: Int,
    val scheduledTime: String?,
    val isActive: Boolean,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val deletedAtEpochMillis: Long?,
)

@Entity(
    tableName = "daily_logs",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = RoutineSetEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineSetId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["date"]),
        Index(value = ["routineId"]),
        Index(value = ["routineSetId"]),
        Index(value = ["date", "routineId"], unique = true),
    ],
)
data class DailyLogEntity(
    @PrimaryKey val id: String,
    val date: String,
    val routineId: String,
    val routineSetId: String,
    val status: String,
    val completedAtEpochMillis: Long?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "daily_routine_selections",
    foreignKeys = [
        ForeignKey(
            entity = RoutineSetEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineSetId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index(value = ["routineSetId"])],
)
data class DailyRoutineSelectionEntity(
    @PrimaryKey val date: String,
    val routineSetId: String,
    val selectedAtEpochMillis: Long,
)

data class RoutineSetWithRoutines(
    @Embedded val routineSet: RoutineSetEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "routineSetId",
    )
    val routines: List<RoutineEntity>,
)
