package com.example.steppie.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import java.util.UUID

private val localeTagPattern = Regex("^[A-Za-z]{2,8}(?:-[A-Za-z0-9]{1,8})*$")
private val builtinIconPattern = Regex("^[a-z0-9]+(?:-[a-z0-9]+)*$")

data class LocalizedText(val values: Map<String, String>) {
    init {
        require(values.isNotEmpty()) { "LocalizedText must contain at least one locale." }
        require(values.keys.all(localeTagPattern::matches)) { "Locale keys must be BCP 47 language tags." }
        require(values.values.any { it.isNotBlank() }) { "LocalizedText must contain a non-blank value." }
        require(values.values.none { it.isBlank() }) { "LocalizedText cannot contain blank values." }
    }

    fun resolve(appLocale: String?, systemLocale: String): String {
        fun valueFor(tag: String?): String? {
            if (tag == null) return null
            values[tag]?.let { return it }
            val language = Locale.forLanguageTag(tag).language
            return values.entries.firstOrNull {
                Locale.forLanguageTag(it.key).language == language
            }?.value
        }

        return valueFor(appLocale)
            ?: valueFor(systemLocale)
            ?: valueFor("ko")
            ?: values.values.first()
    }
}

sealed interface IconRef {
    data class Builtin(val name: String) : IconRef {
        init {
            require(name.matches(builtinIconPattern)) { "Builtin icon names must use lowercase kebab-case." }
            require(name in BuiltinIconNames.all) { "Unknown builtin icon: $name" }
        }
    }

    data class Photo(
        val localAssetId: String,
        val backupAssetName: String? = null,
    ) : IconRef {
        init {
            requireUuidV4(localAssetId, "localAssetId")
            require(backupAssetName?.let { it.isNotBlank() && '/' !in it && '\\' !in it } != false) {
                "backupAssetName must be a non-blank file name."
            }
        }
    }
}

object BuiltinIconNames {
    val all: Set<String> = setOf(
        "wake-up", "wash-face", "brush-teeth", "get-dressed", "breakfast", "pack-bag",
        "school", "book", "pencil", "lunch", "playground", "bus",
        "bath", "pajamas", "story-book", "toilet", "sleep", "star",
        "home", "meal", "snack", "medicine", "walk", "therapy", "music", "art",
        "clean-up", "timer",
    )
}

object RoutineColorTokens {
    const val DEFAULT = "color.card.sky"

    val all: Set<String> = setOf(
        DEFAULT,
        "color.card.mint",
        "color.card.lemon",
        "color.card.peach",
        "color.card.lavender",
        "color.card.rose",
    )
}

data class Routine(
    val id: String = newUuidV4(),
    val routineSetId: String,
    val titleKey: String? = null,
    val title: LocalizedText,
    val icon: IconRef = IconRef.Builtin("star"),
    val colorToken: String = RoutineColorTokens.DEFAULT,
    val order: Int,
    val scheduledTime: LocalTime? = null,
    val isActive: Boolean = true,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = createdAt,
    val deletedAt: Instant? = null,
) {
    init {
        requireUuidV4(id, "Routine.id")
        requireUuidV4(routineSetId, "Routine.routineSetId")
        require(titleKey?.isNotBlank() != false) { "titleKey cannot be blank." }
        require(colorToken in RoutineColorTokens.all) { "Unknown routine color token: $colorToken" }
        require(order >= 0) { "Routine.order must be zero or greater." }
        require(scheduledTime == null || (scheduledTime.second == 0 && scheduledTime.nano == 0)) {
            "Routine.scheduledTime must use HH:mm precision."
        }
        require(updatedAt >= createdAt) { "Routine.updatedAt cannot precede createdAt." }
        require(deletedAt == null || deletedAt >= createdAt) { "Routine.deletedAt cannot precede createdAt." }
    }
}

data class RoutineSet(
    val id: String = newUuidV4(),
    val name: LocalizedText,
    val isActive: Boolean = false,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = createdAt,
    val deletedAt: Instant? = null,
    val routines: List<Routine> = emptyList(),
) {
    init {
        requireUuidV4(id, "RoutineSet.id")
        require(updatedAt >= createdAt) { "RoutineSet.updatedAt cannot precede createdAt." }
        require(deletedAt == null || deletedAt >= createdAt) { "RoutineSet.deletedAt cannot precede createdAt." }
        require(!(isActive && deletedAt != null)) { "An active RoutineSet cannot be deleted." }
        require(routines.all { it.routineSetId == id }) { "Every routine must belong to this RoutineSet." }
        val visible = routines.filter { it.deletedAt == null }
        require(visible.map(Routine::order).distinct().size == visible.size) {
            "Visible routine orders must be unique within a RoutineSet."
        }
    }
}

enum class LogStatus(val storageValue: String) {
    Completed("completed"),
    Undone("undone");

    companion object {
        fun fromStorageValue(value: String): LogStatus = entries.firstOrNull { it.storageValue == value }
            ?: error("Unknown LogStatus: $value")
    }
}

data class DailyLog(
    val id: String = newUuidV4(),
    val date: LocalDate,
    val routineId: String,
    val routineSetId: String,
    val status: LogStatus = LogStatus.Undone,
    val completedAt: Instant? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = createdAt,
) {
    init {
        requireUuidV4(id, "DailyLog.id")
        requireUuidV4(routineId, "DailyLog.routineId")
        requireUuidV4(routineSetId, "DailyLog.routineSetId")
        require((status == LogStatus.Completed) == (completedAt != null)) {
            "DailyLog.completedAt must be present only when status is completed."
        }
        require(updatedAt >= createdAt) { "DailyLog.updatedAt cannot precede createdAt." }
    }
}

fun newUuidV4(): String = UUID.randomUUID().toString()

fun requireUuidV4(value: String, fieldName: String) {
    val uuid = runCatching { UUID.fromString(value) }.getOrNull()
    require(uuid != null && uuid.version() == 4 && uuid.toString().equals(value, ignoreCase = true)) {
        "$fieldName must be a UUID v4 string."
    }
}
