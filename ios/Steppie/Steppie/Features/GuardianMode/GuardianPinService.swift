import CryptoKit
import Foundation

enum GuardianPinError: Error, Equatable {
    case invalidPIN
    case missingPIN
}

enum GuardianPinService {
    private static let hashVersion = "v1"
    private static let separator = "$"

    static func isValidPIN(_ pin: String) -> Bool {
        pin.count == 4 && pin.allSatisfy(\.isNumber)
    }

    static func makeHash(for pin: String, salt: String = UUID().uuidString) throws -> String {
        guard isValidPIN(pin) else { throw GuardianPinError.invalidPIN }
        let digest = SHA256.hash(data: Data("\(salt):\(pin)".utf8))
            .map { String(format: "%02x", $0) }
            .joined()
        return [hashVersion, salt, digest].joined(separator: separator)
    }

    static func verify(_ pin: String, against storedHash: String?) -> Bool {
        guard isValidPIN(pin), let storedHash else { return false }
        let parts = storedHash.components(separatedBy: separator)
        guard parts.count == 3, parts[0] == hashVersion else { return false }
        return (try? makeHash(for: pin, salt: parts[1])) == storedHash
    }
}
