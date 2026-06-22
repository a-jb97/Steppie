import Foundation

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
        guard isKebabCase(name) else {
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

    private static func isKebabCase(_ value: String) -> Bool {
        guard !value.isEmpty,
              value.first != "-",
              value.last != "-" else {
            return false
        }
        return value.allSatisfy { character in
            character.isLowercase || character.isNumber || character == "-"
        } && !value.contains("--")
    }

    private static func isValidBackupAssetName(_ value: String) -> Bool {
        guard value.hasPrefix("routine-photo-") else { return false }
        let lowercased = value.lowercased()
        return lowercased.hasSuffix(".jpg") || lowercased.hasSuffix(".png")
    }
}
