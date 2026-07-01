import CryptoKit
import Foundation

enum GuardianPinError: Error, Equatable {
    case invalidPIN
    case invalidRecoveryCode
    case missingPIN
}

enum GuardianPinService {
    private static let hashVersion = "v1"
    private static let separator = "$"

    static func isValidPIN(_ pin: String) -> Bool {
        pin.count == 4 && pin.allSatisfy(\.isNumber)
    }

    static func isValidRecoveryCode(_ code: String) -> Bool {
        code.count == 6 && code.allSatisfy(\.isNumber)
    }

    static func generateRecoveryCode() -> String {
        String(format: "%06d", Int.random(in: 0...999_999))
    }

    static func makeHash(for pin: String, salt: String = UUID().uuidString) throws -> String {
        guard isValidPIN(pin) else { throw GuardianPinError.invalidPIN }
        return makeHash(forValidatedSecret: pin, salt: salt)
    }

    static func makeRecoveryCodeHash(for code: String, salt: String = UUID().uuidString) throws -> String {
        guard isValidRecoveryCode(code) else { throw GuardianPinError.invalidRecoveryCode }
        return makeHash(forValidatedSecret: code, salt: salt)
    }

    static func verify(_ pin: String, against storedHash: String?) -> Bool {
        guard isValidPIN(pin), let storedHash else { return false }
        return verifyValidatedSecret(pin, against: storedHash)
    }

    static func verifyRecoveryCode(_ code: String, against storedHash: String?) -> Bool {
        guard isValidRecoveryCode(code), let storedHash else { return false }
        return verifyValidatedSecret(code, against: storedHash)
    }

    private static func makeHash(forValidatedSecret secret: String, salt: String) -> String {
        let digest = SHA256.hash(data: Data("\(salt):\(secret)".utf8))
            .map { String(format: "%02x", $0) }
            .joined()
        return [hashVersion, salt, digest].joined(separator: separator)
    }

    private static func verifyValidatedSecret(_ secret: String, against storedHash: String) -> Bool {
        let parts = storedHash.components(separatedBy: separator)
        guard parts.count == 3, parts[0] == hashVersion else { return false }
        return makeHash(forValidatedSecret: secret, salt: parts[1]) == storedHash
    }
}
