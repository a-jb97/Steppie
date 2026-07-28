package com.example.steppie.data.backup

import com.example.steppie.data.local.DailyLogEntity
import com.example.steppie.data.local.LocalizedTextCodec
import com.example.steppie.data.local.RoutineEntity
import com.example.steppie.data.local.RoutineSetEntity
import com.example.steppie.data.local.toEntity
import com.example.steppie.domain.model.AppSettings
import com.example.steppie.domain.model.DailyLog
import com.example.steppie.domain.model.FeedbackIntensity
import com.example.steppie.domain.model.IconRef
import com.example.steppie.domain.model.LocalizedText
import com.example.steppie.domain.model.LogStatus
import com.example.steppie.domain.model.Routine
import com.example.steppie.domain.model.RoutineSet
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import org.json.JSONArray
import org.json.JSONObject

internal object BackupJson {
    fun encodeData(snapshot: BackupSnapshot): String {
        val root = JSONObject()
            .put("schemaVersion", BackupSchemaVersion)
            .put("exportedAt", formatInstant(snapshot.exportedAt))
            .put("routineSets", JSONArray(snapshot.routineSets.map(::encodeRoutineSet)))
            .put("routines", JSONArray(snapshot.routines.map(::encodeRoutine)))
            .put("dailyLogs", JSONArray(snapshot.dailyLogs.map(::encodeDailyLog)))
            .put("appSettings", encodeAppSettings(snapshot.appSettings))
        return root.toString()
    }

    fun decodeData(json: String): BackupSnapshot {
        val root = runCatching { JSONObject(json) }.getOrElse {
            throw BackupValidationException("data.json 형식이 올바르지 않습니다.")
        }
        val schemaVersion = root.optInt("schemaVersion", -1)
        if (schemaVersion !in 1..BackupSchemaVersion) {
            throw BackupValidationException("지원하지 않는 백업 데이터 버전입니다.")
        }
        val exportedAt = parseInstant(root.requiredString("exportedAt"), "exportedAt")
        val routineSets = root.requiredArray("routineSets").mapObjects(::decodeRoutineSet)
        val routines = root.requiredArray("routines").mapObjects(::decodeRoutine)
        val dailyLogs = root.requiredArray("dailyLogs").mapObjects(::decodeDailyLog)
        val settings = decodeAppSettings(root.requiredObject("appSettings"))
        validateReferences(routineSets, routines, dailyLogs)
        return BackupSnapshot(
            exportedAt = exportedAt,
            routineSets = if (schemaVersion == 1) normalizeActiveRoutineSet(routineSets) else routineSets,
            routines = routines,
            dailyLogs = dailyLogs,
            appSettings = settings,
        )
    }

    fun encodeManifest(
        createdAt: Instant,
        appVersion: String,
        dataChecksum: String,
    ): String = JSONObject()
        .put("app", BackupAppName)
        .put("backupSchemaVersion", BackupSchemaVersion)
        .put("createdAt", formatInstant(createdAt))
        .put("sourcePlatform", "android")
        .put("appVersion", appVersion)
        .put("dataFile", BackupDataFileName)
        .put("assetDirectory", BackupAssetDirectory)
        .put(
            "checksum",
            JSONObject()
                .put("algorithm", "sha256")
                .put("dataJson", dataChecksum),
        )
        .toString()

    fun decodeManifest(json: String): BackupManifest {
        val root = runCatching { JSONObject(json) }.getOrElse {
            throw BackupValidationException("manifest.json 형식이 올바르지 않습니다.")
        }
        if (root.optString("app") != BackupAppName) {
            throw BackupValidationException("Steppie 백업 파일이 아닙니다.")
        }
        if (root.optInt("backupSchemaVersion", -1) !in 1..BackupSchemaVersion) {
            throw BackupValidationException("지원하지 않는 백업 버전입니다.")
        }
        if (root.optString("dataFile") != BackupDataFileName) {
            throw BackupValidationException("백업 데이터 파일 이름이 올바르지 않습니다.")
        }
        if (root.optString("assetDirectory") != BackupAssetDirectory) {
            throw BackupValidationException("백업 에셋 폴더 이름이 올바르지 않습니다.")
        }
        val checksum = root.requiredObject("checksum")
        if (checksum.optString("algorithm") != "sha256") {
            throw BackupValidationException("지원하지 않는 checksum 방식입니다.")
        }
        return BackupManifest(
            createdAt = parseInstant(root.requiredString("createdAt"), "createdAt"),
            sourcePlatform = root.requiredString("sourcePlatform"),
            dataChecksum = checksum.requiredString("dataJson"),
        )
    }

    private fun encodeRoutineSet(entity: RoutineSetEntity): JSONObject = JSONObject()
        .put("id", entity.id)
        .put("name", encodeLocalizedText(entity.localizedName))
        .put("isActive", entity.isActive)
        .putNullable("startTime", entity.startTime)
        .put("createdAt", formatEpochMillis(entity.createdAtEpochMillis))
        .put("updatedAt", formatEpochMillis(entity.updatedAtEpochMillis))
        .putNullable("deletedAt", entity.deletedAtEpochMillis?.let(::formatEpochMillis))

    private fun decodeRoutineSet(json: JSONObject): RoutineSetEntity {
        val domain = RoutineSet(
            id = json.requiredString("id"),
            name = decodeLocalizedText(json.requiredObject("name")),
            isActive = json.requiredBoolean("isActive"),
            startTime = json.optionalString("startTime")?.let(LocalTime::parse),
            createdAt = parseInstant(json.requiredString("createdAt"), "RoutineSet.createdAt"),
            updatedAt = parseInstant(json.requiredString("updatedAt"), "RoutineSet.updatedAt"),
            deletedAt = json.optionalString("deletedAt")?.let { parseInstant(it, "RoutineSet.deletedAt") },
        )
        return domain.toEntity()
    }

    private fun encodeRoutine(entity: RoutineEntity): JSONObject {
        val icon = if (entity.iconType == "photo") {
            JSONObject()
                .put("type", "photo")
                .put("localAssetId", entity.localAssetId)
                .putNullable("backupAssetName", entity.backupAssetName)
        } else {
            JSONObject()
                .put("type", "builtin")
                .put("name", entity.iconName)
        }
        return JSONObject()
            .put("id", entity.id)
            .put("routineSetId", entity.routineSetId)
            .putNullable("titleKey", entity.titleKey)
            .put("title", encodeLocalizedText(entity.localizedTitle))
            .put("icon", icon)
            .put("colorToken", entity.colorToken)
            .put("order", entity.sortOrder)
            .putNullable("scheduledTime", entity.scheduledTime)
            .put("isActive", entity.isActive)
            .put("createdAt", formatEpochMillis(entity.createdAtEpochMillis))
            .put("updatedAt", formatEpochMillis(entity.updatedAtEpochMillis))
            .putNullable("deletedAt", entity.deletedAtEpochMillis?.let(::formatEpochMillis))
    }

    private fun decodeRoutine(json: JSONObject): RoutineEntity {
        val icon = decodeIcon(json.requiredObject("icon"))
        val domain = Routine(
            id = json.requiredString("id"),
            routineSetId = json.requiredString("routineSetId"),
            titleKey = json.optionalString("titleKey"),
            title = decodeLocalizedText(json.requiredObject("title")),
            icon = icon,
            colorToken = json.requiredString("colorToken"),
            order = json.requiredInt("order"),
            scheduledTime = json.optionalString("scheduledTime")?.let(LocalTime::parse),
            isActive = json.requiredBoolean("isActive"),
            createdAt = parseInstant(json.requiredString("createdAt"), "Routine.createdAt"),
            updatedAt = parseInstant(json.requiredString("updatedAt"), "Routine.updatedAt"),
            deletedAt = json.optionalString("deletedAt")?.let { parseInstant(it, "Routine.deletedAt") },
        )
        return domain.toEntity()
    }

    private fun encodeDailyLog(entity: DailyLogEntity): JSONObject = JSONObject()
        .put("id", entity.id)
        .put("date", entity.date)
        .put("routineId", entity.routineId)
        .put("routineSetId", entity.routineSetId)
        .put("status", entity.status)
        .putNullable("completedAt", entity.completedAtEpochMillis?.let(::formatEpochMillis))
        .put("createdAt", formatEpochMillis(entity.createdAtEpochMillis))
        .put("updatedAt", formatEpochMillis(entity.updatedAtEpochMillis))

    private fun decodeDailyLog(json: JSONObject): DailyLogEntity {
        val status = LogStatus.fromStorageValue(json.requiredString("status"))
        val domain = DailyLog(
            id = json.requiredString("id"),
            date = LocalDate.parse(json.requiredString("date")),
            routineId = json.requiredString("routineId"),
            routineSetId = json.requiredString("routineSetId"),
            status = status,
            completedAt = json.optionalString("completedAt")?.let { parseInstant(it, "DailyLog.completedAt") },
            createdAt = parseInstant(json.requiredString("createdAt"), "DailyLog.createdAt"),
            updatedAt = parseInstant(json.requiredString("updatedAt"), "DailyLog.updatedAt"),
        )
        return domain.toEntity()
    }

    private fun encodeAppSettings(settings: AppSettings): JSONObject = JSONObject()
        .putNullable("guardianPinHash", settings.guardianPinHash)
        .putNullable("recoveryCodeHash", settings.recoveryCodeHash)
        .put("feedbackIntensity", settings.feedbackIntensity.storageValue)
        .put("soundEnabled", settings.soundEnabled)
        .put("ttsEnabled", settings.ttsEnabled)
        .put("ttsRate", settings.ttsRate)
        .put("ttsVolume", settings.ttsVolume)
        .put("hapticEnabled", settings.hapticEnabled)
        .put("undoDurationSeconds", settings.undoDurationSeconds)
        .put("notificationLeadTimes", JSONArray(settings.notificationLeadTimes))
        .putNullable("quietHoursStart", settings.quietHoursStart?.toString())
        .putNullable("quietHoursEnd", settings.quietHoursEnd?.toString())
        .putNullable("locale", settings.locale)

    private fun decodeAppSettings(json: JSONObject): AppSettings {
        val default = AppSettings()
        return AppSettings(
            guardianPinHash = json.optionalString("guardianPinHash"),
            recoveryCodeHash = json.optionalString("recoveryCodeHash"),
            feedbackIntensity = json.optionalString("feedbackIntensity")
                ?.let(FeedbackIntensity::fromStorageValue)
                ?: default.feedbackIntensity,
            soundEnabled = json.optionalBoolean("soundEnabled") ?: default.soundEnabled,
            ttsEnabled = json.optionalBoolean("ttsEnabled") ?: default.ttsEnabled,
            ttsRate = json.optionalDouble("ttsRate") ?: default.ttsRate,
            ttsVolume = json.optionalDouble("ttsVolume") ?: default.ttsVolume,
            hapticEnabled = json.optionalBoolean("hapticEnabled") ?: default.hapticEnabled,
            undoDurationSeconds = json.optionalInt("undoDurationSeconds") ?: default.undoDurationSeconds,
            notificationLeadTimes = json.optionalArray("notificationLeadTimes")
                ?.mapInts()
                ?.takeIf(List<Int>::isNotEmpty)
                ?: default.notificationLeadTimes,
            quietHoursStart = json.optionalString("quietHoursStart")?.let(LocalTime::parse),
            quietHoursEnd = json.optionalString("quietHoursEnd")?.let(LocalTime::parse),
            locale = json.optionalString("locale"),
        )
    }

    private fun validateReferences(
        routineSets: List<RoutineSetEntity>,
        routines: List<RoutineEntity>,
        dailyLogs: List<DailyLogEntity>,
    ) {
        requireUnique("RoutineSet", routineSets.map { it.id })
        requireUnique("Routine", routines.map { it.id })
        requireUnique("DailyLog", dailyLogs.map { it.id })
        requireUnique("DailyLog date/routine", dailyLogs.map { "${it.date}|${it.routineId}" })
        val routineSetIds = routineSets.map { it.id }.toSet()
        val routineIds = routines.map { it.id }.toSet()
        routines.forEach {
            if (it.routineSetId !in routineSetIds) {
                throw BackupValidationException("존재하지 않는 루틴 세트를 참조하는 루틴이 있습니다.")
            }
        }
        dailyLogs.forEach {
            if (it.routineSetId !in routineSetIds || it.routineId !in routineIds) {
                throw BackupValidationException("존재하지 않는 루틴 또는 루틴 세트를 참조하는 기록이 있습니다.")
            }
        }
        routines.groupBy { it.routineSetId }
            .forEach { (_, setRoutines) ->
                val visibleOrders = setRoutines.filter { it.deletedAtEpochMillis == null }.map { it.sortOrder }
                if (visibleOrders.size != visibleOrders.distinct().size) {
                    throw BackupValidationException("한 루틴 세트 안에 중복된 순서가 있습니다.")
                }
            }
    }

    private fun normalizeActiveRoutineSet(sets: List<RoutineSetEntity>): List<RoutineSetEntity> {
        val visible = sets.filter { it.deletedAtEpochMillis == null }
        val selectedActive = visible
            .filter { it.isActive }
            .maxWithOrNull(compareBy<RoutineSetEntity> { it.updatedAtEpochMillis }.thenBy { it.id })
            ?: visible.maxWithOrNull(compareBy<RoutineSetEntity> { it.updatedAtEpochMillis }.thenBy { it.id })
        return sets.map { set ->
            if (set.deletedAtEpochMillis != null) {
                set.copy(isActive = false)
            } else {
                set.copy(isActive = set.id == selectedActive?.id)
            }
        }
    }

    private fun requireUnique(name: String, ids: List<String>) {
        if (ids.size != ids.distinct().size) {
            throw BackupValidationException("$name ID가 중복된 백업입니다.")
        }
    }

    private fun encodeLocalizedText(encoded: String): JSONObject {
        val text = LocalizedTextCodec.decode(encoded)
        return JSONObject(text.values)
    }

    private fun decodeLocalizedText(json: JSONObject): LocalizedText {
        val values = json.keys().asSequence().associateWith { json.getString(it) }
        return LocalizedText(values)
    }

    private fun decodeIcon(json: JSONObject): IconRef = when (json.requiredString("type")) {
        "builtin" -> runCatching { IconRef.Builtin(json.requiredString("name")) }
            .getOrElse { IconRef.Builtin("star") }
        "photo" -> IconRef.Photo(
            localAssetId = json.requiredString("localAssetId"),
            backupAssetName = json.optionalString("backupAssetName"),
        )
        else -> throw BackupValidationException("지원하지 않는 아이콘 형식입니다.")
    }

    private fun parseInstant(value: String, field: String): Instant = runCatching {
        OffsetDateTime.parse(value).toInstant()
    }.getOrElse {
        throw BackupValidationException("$field 시간이 ISO 8601 형식이 아닙니다.")
    }

    private fun formatEpochMillis(value: Long): String = formatInstant(Instant.ofEpochMilli(value))

    private fun formatInstant(value: Instant): String =
        DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(value.atZone(ZoneId.systemDefault()))
}

data class BackupManifest(
    val createdAt: Instant,
    val sourcePlatform: String,
    val dataChecksum: String,
)

private fun JSONObject.putNullable(name: String, value: Any?): JSONObject =
    put(name, value ?: JSONObject.NULL)

private fun JSONObject.requiredString(name: String): String =
    optionalString(name) ?: throw BackupValidationException("$name 값이 없습니다.")

private fun JSONObject.requiredBoolean(name: String): Boolean {
    if (!has(name) || isNull(name)) throw BackupValidationException("$name 값이 없습니다.")
    return getBoolean(name)
}

private fun JSONObject.requiredInt(name: String): Int {
    if (!has(name) || isNull(name)) throw BackupValidationException("$name 값이 없습니다.")
    return getInt(name)
}

private fun JSONObject.requiredObject(name: String): JSONObject =
    optJSONObject(name) ?: throw BackupValidationException("$name 객체가 없습니다.")

private fun JSONObject.requiredArray(name: String): JSONArray =
    optJSONArray(name) ?: throw BackupValidationException("$name 배열이 없습니다.")

private fun JSONObject.optionalString(name: String): String? =
    if (!has(name) || isNull(name)) null else getString(name)

private fun JSONObject.optionalBoolean(name: String): Boolean? =
    if (!has(name) || isNull(name)) null else getBoolean(name)

private fun JSONObject.optionalDouble(name: String): Double? =
    if (!has(name) || isNull(name)) null else getDouble(name)

private fun JSONObject.optionalInt(name: String): Int? =
    if (!has(name) || isNull(name)) null else getInt(name)

private fun JSONObject.optionalArray(name: String): JSONArray? =
    if (!has(name) || isNull(name)) null else optJSONArray(name)

private fun JSONArray.mapInts(): List<Int> = (0 until length()).map { getInt(it) }

private fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> =
    (0 until length()).map { transform(getJSONObject(it)) }
