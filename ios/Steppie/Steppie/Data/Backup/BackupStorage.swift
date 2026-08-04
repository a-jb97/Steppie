import Foundation

protocol BackupAssetStore {
    func data(forBackupAssetName name: String) throws -> Data?
    func saveAssetData(_ data: Data, backupAssetName name: String) throws
    func removeAssetData(backupAssetName name: String) throws
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

    func removeAssetData(backupAssetName name: String) throws {
        let url = try assetURL(for: name)
        guard fileManager.fileExists(atPath: url.path) else { return }
        try fileManager.removeItem(at: url)
    }

    private func assetURL(for name: String) throws -> URL {
        guard !name.contains("/"), !name.contains("\\") else {
            throw BackupError.invalidData
        }
        return directoryURL.appendingPathComponent(name, isDirectory: false)
    }
}
