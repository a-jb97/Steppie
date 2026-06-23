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

data class RoutineSetWithRoutines(
    @Embedded val routineSet: RoutineSetEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "routineSetId",
    )
    val routines: List<RoutineEntity>,
)
