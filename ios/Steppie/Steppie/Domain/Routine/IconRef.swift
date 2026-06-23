import Foundation

nonisolated enum RoutineIconName: String, CaseIterable, Identifiable, Sendable {
    case wakeUp = "wake-up"
    case washFace = "wash-face"
    case brushTeeth = "brush-teeth"
    case getDressed = "get-dressed"
    case breakfast
    case packBag = "pack-bag"
    case school
    case book
    case pencil
    case lunch
    case playground
    case bus
    case bath
    case pajamas
    case storyBook = "story-book"
    case toilet
    case sleep
    case star
    case home
    case meal
    case snack
    case medicine
    case walk
    case therapy
    case music
    case art
    case cleanUp = "clean-up"
    case timer

    var id: String { rawValue }
}

nonisolated struct IconRef: Codable, Equatable, Sendable {
    nonisolated enum Kind: String, Codable, Sendable {
        case builtin
        case photo
    }

    let type: Kind
    let name: String?
    let localAssetID: UUID?
    let backupAssetName: String?

    private init(
        type: Kind,
        name: String?,
        localAssetID: UUID?,
        backupAssetName: String?
    ) {
        self.type = type
        self.name = name
        self.localAssetID = localAssetID
        self.backupAssetName = backupAssetName
    }

    static func builtin(name: String) throws -> IconRef {
        guard RoutineIconName(rawValue: name) != nil else {
            throw RoutineDomainError.invalidBuiltinIconName(name)
        }
        return IconRef(type: .builtin, name: name, localAssetID: nil, backupAssetName: nil)
    }

    static func photo(localAssetID: UUID, backupAssetName: String) throws -> IconRef {
        guard localAssetID.isVersion4 else {
            throw RoutineDomainError.invalidPhotoAssetID
        }
        guard isValidBackupAssetName(backupAssetName) else {
            throw RoutineDomainError.invalidBackupAssetName(backupAssetName)
        }
        return IconRef(
            type: .photo,
            name: nil,
            localAssetID: localAssetID,
            backupAssetName: backupAssetName
        )
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        let type = try container.decode(Kind.self, forKey: .type)
        switch type {
        case .builtin:
            self = try .builtin(name: container.decode(String.self, forKey: .name))
        case .photo:
            self = try .photo(
                localAssetID: container.decode(UUID.self, forKey: .localAssetID),
                backupAssetName: container.decode(String.self, forKey: .backupAssetName)
            )
        }
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(type, forKey: .type)
        switch type {
        case .builtin:
            try container.encode(name, forKey: .name)
        case .photo:
            try container.encode(localAssetID, forKey: .localAssetID)
            try container.encode(backupAssetName, forKey: .backupAssetName)
        }
    }

    private enum CodingKeys: String, CodingKey {
        case type
        case name
        case localAssetID = "localAssetId"
        case backupAssetName
    }

    private static func isValidBackupAssetName(_ value: String) -> Bool {
        guard value.hasPrefix("routine-photo-") else { return false }
        let lowercased = value.lowercased()
        return lowercased.hasSuffix(".jpg") || lowercased.hasSuffix(".png")
    }
}
