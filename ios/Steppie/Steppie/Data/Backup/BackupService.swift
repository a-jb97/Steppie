import CryptoKit
import Foundation

enum BackupError: Error, Equatable {
    case invalidPackage
    case missingFile(String)
    case invalidManifest
    case invalidChecksum
    case unsupportedSchemaVersion(Int)
    case unsupportedPlatform(String)
    case duplicateID(String)
    case invalidReference(String)
    case invalidData
}

struct BackupChecksum: Codable, Equatable, Sendable {
    let algorithm: String
    let dataJson: String
}

struct BackupManifest: Codable, Equatable, Sendable {
    let app: String
    let backupSchemaVersion: Int
    let createdAt: Date
    let sourcePlatform: String
    let appVersion: String
    let dataFile: String
    let assetDirectory: String
    let checksum: BackupChecksum
}

struct BackupData: Codable, Equatable, Sendable {
    let schemaVersion: Int
    let exportedAt: Date
    let routineSets: [RoutineSet]
    let routines: [Routine]
    let dailyLogs: [DailyLog]
    let appSettings: AppSettings
}

struct BackupPackage: Equatable, Sendable {
    let fileName: String
    let archiveData: Data
    let manifest: BackupManifest
}

struct BackupRestorePayload: Equatable, Sendable {
    let snapshot: RoutineRepositorySnapshot
    let assets: [String: Data]
}

protocol BackupAssetStore {
    func data(forBackupAssetName name: String) throws -> Data?
    func saveAssetData(_ data: Data, backupAssetName name: String) throws
}

struct FileBackupAssetStore: BackupAssetStore {
    private let directoryURL: URL
    private let fileManager: FileManager

    init(fileManager: FileManager = .default) {
        self.fileManager = fileManager
        let baseURL = fileManager.urls(for: .applicationSupportDirectory, in: .userDomainMask).first
            ?? fileManager.temporaryDirectory
        directoryURL = baseURL.appendingPathComponent("SteppieRoutineAssets", isDirectory: true)
    }

    func data(forBackupAssetName name: String) throws -> Data? {
        let url = try assetURL(for: name)
        guard fileManager.fileExists(atPath: url.path) else { return nil }
        return try Data(contentsOf: url)
    }

    func saveAssetData(_ data: Data, backupAssetName name: String) throws {
        try fileManager.createDirectory(at: directoryURL, withIntermediateDirectories: true)
        try data.write(to: assetURL(for: name), options: .atomic)
    }

    private func assetURL(for name: String) throws -> URL {
        guard !name.contains("/"), !name.contains("\\") else {
            throw BackupError.invalidData
        }
        return directoryURL.appendingPathComponent(name, isDirectory: false)
    }
}

protocol BackupStorageProvider {
    var name: String { get }
}

struct LocalFileBackupProvider: BackupStorageProvider {
    let name = "local-file"
}

protocol CloudBackupProvider: BackupStorageProvider {}

@MainActor
struct BackupService {
    private let repository: any RoutineRepository
    private let now: () -> Date
    private let appVersion: () -> String
    private let platform: String
    private let assetStore: any BackupAssetStore

    init(
        repository: any RoutineRepository,
        now: @escaping () -> Date = Date.init,
        appVersion: @escaping () -> String = {
            Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0.0"
        },
        platform: String = "ios",
        assetStore: (any BackupAssetStore)? = nil
    ) {
        self.repository = repository
        self.now = now
        self.appVersion = appVersion
        self.platform = platform
        self.assetStore = assetStore ?? FileBackupAssetStore()
    }

    func exportPackage() throws -> BackupPackage {
        let exportedAt = now()
        let snapshot = try repository.backupSnapshot()
        let backupData = BackupData(
            schemaVersion: 1,
            exportedAt: exportedAt,
            routineSets: snapshot.routineSets,
            routines: snapshot.routines,
            dailyLogs: snapshot.dailyLogs,
            appSettings: snapshot.appSettings
        )
        let dataJson = try Self.jsonEncoder.encode(backupData)
        let checksum = BackupChecksum(
            algorithm: "sha256",
            dataJson: Self.sha256Hex(dataJson)
        )
        let manifest = BackupManifest(
            app: "Steppie",
            backupSchemaVersion: 1,
            createdAt: exportedAt,
            sourcePlatform: platform,
            appVersion: appVersion(),
            dataFile: "data.json",
            assetDirectory: "assets",
            checksum: checksum
        )
        let manifestJson = try Self.jsonEncoder.encode(manifest)
        var entries = [
            SteppieZipEntry(path: "manifest.json", data: manifestJson),
            SteppieZipEntry(path: "data.json", data: dataJson),
            SteppieZipEntry(path: "assets/", data: Data()),
        ]
        for asset in try assetEntries(from: snapshot.routines, assetDirectory: manifest.assetDirectory) {
            entries.append(asset)
        }
        let archive = try SteppieZipArchive.makeArchive(entries: entries)

        return BackupPackage(
            fileName: Self.fileName(for: exportedAt),
            archiveData: archive,
            manifest: manifest
        )
    }

    func validatePackage(_ archiveData: Data) throws -> RoutineRepositorySnapshot {
        let payload = try validatePackagePayload(archiveData)
        return payload.snapshot
    }

    func validatePackagePayload(_ archiveData: Data) throws -> BackupRestorePayload {
        let entries: [String: Data]
        do {
            entries = try SteppieZipArchive.readArchive(archiveData)
        } catch {
            throw BackupError.invalidPackage
        }

        guard let manifestData = entries["manifest.json"] else {
            throw BackupError.missingFile("manifest.json")
        }
        let manifest: BackupManifest
        do {
            manifest = try Self.jsonDecoder.decode(BackupManifest.self, from: manifestData)
        } catch {
            throw BackupError.invalidManifest
        }
        guard manifest.app == "Steppie",
              manifest.dataFile == "data.json",
              manifest.assetDirectory == "assets",
              manifest.checksum.algorithm == "sha256" else {
            throw BackupError.invalidManifest
        }
        guard manifest.backupSchemaVersion == 1 else {
            throw BackupError.unsupportedSchemaVersion(manifest.backupSchemaVersion)
        }
        guard manifest.sourcePlatform == "ios" else {
            throw BackupError.unsupportedPlatform(manifest.sourcePlatform)
        }
        guard let dataJson = entries[manifest.dataFile] else {
            throw BackupError.missingFile(manifest.dataFile)
        }
        guard Self.sha256Hex(dataJson) == manifest.checksum.dataJson else {
            throw BackupError.invalidChecksum
        }

        let backupData: BackupData
        do {
            backupData = try Self.jsonDecoder.decode(BackupData.self, from: dataJson)
        } catch {
            throw BackupError.invalidData
        }
        guard backupData.schemaVersion == 1 else {
            throw BackupError.unsupportedSchemaVersion(backupData.schemaVersion)
        }
        let assets = assetPayloads(from: entries, assetDirectory: manifest.assetDirectory)
        return BackupRestorePayload(
            snapshot: try normalizedSnapshot(from: backupData, entries: entries, assetDirectory: manifest.assetDirectory),
            assets: assets
        )
    }

    func restorePackage(_ archiveData: Data) throws {
        try restorePayload(validatePackagePayload(archiveData))
    }

    func restorePayload(_ payload: BackupRestorePayload) throws {
        for (name, data) in payload.assets {
            try assetStore.saveAssetData(data, backupAssetName: name)
        }
        try repository.replaceAll(with: payload.snapshot)
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

        let routineSets = try normalizedRoutineSets(data.routineSets)
        return RoutineRepositorySnapshot(
            routineSets: routineSets,
            routines: routines,
            dailyLogs: data.dailyLogs,
            appSettings: data.appSettings
        )
    }

    private func normalizedRoutine(_ routine: Routine, entries: [String: Data], assetDirectory: String) throws -> Routine {
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

    private func assetEntries(from routines: [Routine], assetDirectory: String) throws -> [SteppieZipEntry] {
        var entries: [SteppieZipEntry] = []
        var addedNames: Set<String> = []
        for routine in routines where routine.icon.type == .photo {
            guard let backupAssetName = routine.icon.backupAssetName,
                  addedNames.insert(backupAssetName).inserted,
                  let data = try assetStore.data(forBackupAssetName: backupAssetName) else {
                continue
            }
            entries.append(SteppieZipEntry(path: "\(assetDirectory)/\(backupAssetName)", data: data))
        }
        return entries
    }

    private func assetPayloads(from entries: [String: Data], assetDirectory: String) -> [String: Data] {
        let prefix = "\(assetDirectory)/"
        return entries.reduce(into: [String: Data]()) { result, entry in
            guard entry.key.hasPrefix(prefix), entry.key != prefix else { return }
            let name = String(entry.key.dropFirst(prefix.count))
            guard !name.isEmpty, !name.contains("/") else { return }
            result[name] = entry.value
        }
    }

    private static func sha256Hex(_ data: Data) -> String {
        SHA256.hash(data: data)
            .map { String(format: "%02x", $0) }
            .joined()
    }

    private static func fileName(for date: Date) -> String {
        let formatter = DateFormatter()
        formatter.calendar = Calendar(identifier: .gregorian)
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = .current
        formatter.dateFormat = "yyyyMMdd-HHmmss"
        return "steppie-backup-\(formatter.string(from: date)).zip"
    }

    private static let jsonEncoder: JSONEncoder = {
        let encoder = JSONEncoder()
        encoder.outputFormatting = [.prettyPrinted, .sortedKeys]
        encoder.dateEncodingStrategy = .iso8601
        return encoder
    }()

    private static let jsonDecoder: JSONDecoder = {
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        return decoder
    }()
}
