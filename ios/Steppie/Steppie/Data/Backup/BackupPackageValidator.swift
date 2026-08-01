import Foundation

struct BackupPackageValidator {
    func restorePayload(
        from data: BackupData,
        entries: [String: Data],
        assetDirectory: String
    ) throws -> BackupRestorePayload {
        BackupRestorePayload(
            snapshot: try normalizedSnapshot(
                from: data,
                entries: entries,
                assetDirectory: assetDirectory
            ),
            assets: assetPayloads(from: entries, assetDirectory: assetDirectory)
        )
    }

    private func normalizedSnapshot(
        from data: BackupData,
        entries: [String: Data],
        assetDirectory: String
    ) throws -> RoutineRepositorySnapshot {
        try validateUnique(data.routineSets.map(\.id), label: "RoutineSet")
        try validateUnique(data.routines.map(\.id), label: "Routine")
        try validateUnique(data.dailyLogs.map(\.id), label: "DailyLog")

        let routineSetsByID = Dictionary(uniqueKeysWithValues: data.routineSets.map { ($0.id, $0) })
        guard !routineSetsByID.isEmpty else {
            guard data.routines.isEmpty, data.dailyLogs.isEmpty else {
                throw BackupError.invalidReference("RoutineSet")
            }
            return RoutineRepositorySnapshot(
                routineSets: [],
                routines: [],
                dailyLogs: [],
                dailyRoutineAssignments: [],
                appSettings: data.appSettings
            )
        }

        var routines: [Routine] = []
        for routine in data.routines {
            guard routineSetsByID[routine.routineSetID] != nil else {
                throw BackupError.invalidReference("Routine.routineSetId")
            }
            routines.append(try normalizedRoutine(routine, entries: entries, assetDirectory: assetDirectory))
        }
        try validateActiveRoutineOrders(routines)

        let routineIDs = Set(routines.map(\.id))
        let routineSetIDs = Set(data.routineSets.map(\.id))
        var dailyLogKeys: Set<String> = []
        for log in data.dailyLogs {
            guard routineSetIDs.contains(log.routineSetID) else {
                throw BackupError.invalidReference("DailyLog.routineSetId")
            }
            guard routineIDs.contains(log.routineID) else {
                throw BackupError.invalidReference("DailyLog.routineId")
            }
            let key = "\(log.date)#\(log.routineID.uuidString)"
            guard dailyLogKeys.insert(key).inserted else {
                throw BackupError.duplicateID("DailyLog.date+routineId")
            }
        }

        var assignmentDates: Set<String> = []
        for assignment in data.dailyRoutineAssignments {
            guard routineSetIDs.contains(assignment.routineSetID) else {
                throw BackupError.invalidReference("DailyRoutineAssignment.routineSetId")
            }
            guard assignmentDates.insert(assignment.date).inserted else {
                throw BackupError.duplicateID("DailyRoutineAssignment.date")
            }
        }

        let routineSets = try normalizedRoutineSets(data.routineSets)
        return RoutineRepositorySnapshot(
            routineSets: routineSets,
            routines: routines,
            dailyLogs: data.dailyLogs,
            dailyRoutineAssignments: data.dailyRoutineAssignments,
            appSettings: data.appSettings
        )
    }

    private func normalizedRoutine(
        _ routine: Routine,
        entries: [String: Data],
        assetDirectory: String
    ) throws -> Routine {
        guard routine.icon.type == .photo,
              let backupAssetName = routine.icon.backupAssetName else {
            return routine
        }
        guard entries["\(assetDirectory)/\(backupAssetName)"] == nil else {
            return routine
        }
        return try Routine(
            id: routine.id,
            routineSetID: routine.routineSetID,
            titleKey: routine.titleKey,
            title: routine.title,
            icon: IconRef.builtin(name: RoutineIconName.star.rawValue),
            colorToken: routine.colorToken,
            order: routine.order,
            scheduledTime: routine.scheduledTime,
            isActive: routine.isActive,
            createdAt: routine.createdAt,
            updatedAt: routine.updatedAt,
            deletedAt: routine.deletedAt
        )
    }

    private func normalizedRoutineSets(_ routineSets: [RoutineSet]) throws -> [RoutineSet] {
        let visibleSets = routineSets.filter { $0.deletedAt == nil }
        guard !visibleSets.isEmpty else { return routineSets }

        let activeTarget = visibleSets
            .filter(\.isActive)
            .max { $0.updatedAt < $1.updatedAt }
            ?? visibleSets.max { $0.updatedAt < $1.updatedAt }

        guard let activeTarget else { return routineSets }
        return try routineSets.map { routineSet in
            let shouldBeActive = routineSet.id == activeTarget.id && routineSet.deletedAt == nil
            guard routineSet.isActive != shouldBeActive else { return routineSet }
            return try RoutineSet(
                id: routineSet.id,
                name: routineSet.name,
                isActive: shouldBeActive,
                dailyStartTime: routineSet.dailyStartTime,
                createdAt: routineSet.createdAt,
                updatedAt: routineSet.updatedAt,
                deletedAt: routineSet.deletedAt
            )
        }
    }

    private func validateUnique(_ ids: [UUID], label: String) throws {
        guard Set(ids).count == ids.count else {
            throw BackupError.duplicateID(label)
        }
    }

    private func validateActiveRoutineOrders(_ routines: [Routine]) throws {
        let grouped = Dictionary(grouping: routines) { $0.routineSetID }
        for (routineSetID, routines) in grouped {
            let activeOrders = routines
                .filter { $0.isActive && $0.deletedAt == nil }
                .map(\.order)
            guard Set(activeOrders).count == activeOrders.count else {
                throw BackupError.duplicateID("Routine.order:\(routineSetID.uuidString)")
            }
        }
    }

    private func assetPayloads(
        from entries: [String: Data],
        assetDirectory: String
    ) -> [String: Data] {
        let prefix = "\(assetDirectory)/"
        return entries.reduce(into: [String: Data]()) { result, entry in
            guard entry.key.hasPrefix(prefix), entry.key != prefix else { return }
            let name = String(entry.key.dropFirst(prefix.count))
            guard !name.isEmpty, !name.contains("/") else { return }
            result[name] = entry.value
        }
    }
}
