import CryptoKit
import Foundation

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
            dailyRoutineAssignments: snapshot.dailyRoutineAssignments,
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
        return try BackupPackageValidator().restorePayload(
            from: backupData,
            entries: entries,
            assetDirectory: manifest.assetDirectory
        )
    }

    func restorePackage(_ archiveData: Data) throws {
        try restorePayload(validatePackagePayload(archiveData))
    }

    func restorePayload(_ payload: BackupRestorePayload) throws {
        var previousAssets: [(name: String, data: Data?)] = []
        do {
            for (name, data) in payload.assets {
                previousAssets.append((name, try assetStore.data(forBackupAssetName: name)))
                try assetStore.saveAssetData(data, backupAssetName: name)
            }
            try repository.replaceAll(with: payload.snapshot)
        } catch {
            for previousAsset in previousAssets.reversed() {
                if let data = previousAsset.data {
                    try? assetStore.saveAssetData(data, backupAssetName: previousAsset.name)
                } else {
                    try? assetStore.removeAssetData(backupAssetName: previousAsset.name)
                }
            }
            throw error
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
