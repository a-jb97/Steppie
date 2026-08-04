import CommonCrypto
import CryptoKit
import Foundation

enum GuardianPinError: Error, Equatable {
    case invalidPIN
    case invalidRecoveryCode
    case missingPIN
    case hashDerivationFailed
}

enum GuardianPinService {
    private static let currentHashVersion = "v2"
    private static let legacyHashVersion = "v1"
    private static let algorithm = "pbkdf2-sha256"
    private static let iterationCount: UInt32 = 600_000
    private static let derivedKeyLength = 32
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
        return try makeHash(forValidatedSecret: pin, salt: salt)
    }

    static func makeRecoveryCodeHash(for code: String, salt: String = UUID().uuidString) throws -> String {
        guard isValidRecoveryCode(code) else { throw GuardianPinError.invalidRecoveryCode }
        return try makeHash(forValidatedSecret: code, salt: salt)
    }

    static func verify(_ pin: String, against storedHash: String?) -> Bool {
        guard isValidPIN(pin), let storedHash else { return false }
        return verifyValidatedSecret(pin, against: storedHash)
    }

    static func verifyRecoveryCode(_ code: String, against storedHash: String?) -> Bool {
        guard isValidRecoveryCode(code), let storedHash else { return false }
        return verifyValidatedSecret(code, against: storedHash)
    }

    static func needsHashUpgrade(_ storedHash: String?) -> Bool {
        storedHash?.hasPrefix("\(legacyHashVersion)\(separator)") == true
    }

    private static func makeHash(forValidatedSecret secret: String, salt: String) throws -> String {
        let saltData = Data(salt.utf8)
        guard let derivedKey = deriveKey(
            secret: secret,
            salt: saltData,
            iterations: iterationCount
        ) else {
            throw GuardianPinError.hashDerivationFailed
        }
        return [
            currentHashVersion,
            algorithm,
            String(iterationCount),
            saltData.base64EncodedString(),
            derivedKey.base64EncodedString(),
        ].joined(separator: separator)
    }

    private static func verifyValidatedSecret(_ secret: String, against storedHash: String) -> Bool {
        let parts = storedHash.components(separatedBy: separator)
        switch parts.first {
        case currentHashVersion:
            guard parts.count == 5,
                  parts[1] == algorithm,
                  let iterations = UInt32(parts[2]),
                  iterations == iterationCount,
                  let salt = Data(base64Encoded: parts[3]),
                  let storedKey = Data(base64Encoded: parts[4]),
                  let candidateKey = deriveKey(
                      secret: secret,
                      salt: salt,
                      iterations: iterations
                  ) else {
                return false
            }
            return constantTimeEquals(candidateKey, storedKey)
        case legacyHashVersion:
            guard parts.count == 3, let storedDigest = data(fromHex: parts[2]) else {
                return false
            }
            let candidateDigest = Data(SHA256.hash(data: Data("\(parts[1]):\(secret)".utf8)))
            return constantTimeEquals(candidateDigest, storedDigest)
        default:
            return false
        }
    }

    private static func deriveKey(secret: String, salt: Data, iterations: UInt32) -> Data? {
        let password = Data(secret.utf8)
        var derivedKey = [UInt8](repeating: 0, count: derivedKeyLength)
        let status = password.withUnsafeBytes { passwordBytes in
            salt.withUnsafeBytes { saltBytes in
                CCKeyDerivationPBKDF(
                    CCPBKDFAlgorithm(kCCPBKDF2),
                    passwordBytes.bindMemory(to: Int8.self).baseAddress,
                    password.count,
                    saltBytes.bindMemory(to: UInt8.self).baseAddress,
                    salt.count,
                    CCPseudoRandomAlgorithm(kCCPRFHmacAlgSHA256),
                    iterations,
                    &derivedKey,
                    derivedKey.count
                )
            }
        }
        guard status == kCCSuccess else { return nil }
        return Data(derivedKey)
    }

    private static func constantTimeEquals(_ lhs: Data, _ rhs: Data) -> Bool {
        guard lhs.count == rhs.count else { return false }
        return zip(lhs, rhs).reduce(into: UInt8(0)) { difference, pair in
            difference |= pair.0 ^ pair.1
        } == 0
    }

    private static func data(fromHex value: String) -> Data? {
        guard value.count.isMultiple(of: 2) else { return nil }
        var bytes: [UInt8] = []
        bytes.reserveCapacity(value.count / 2)
        var index = value.startIndex
        while index < value.endIndex {
            let nextIndex = value.index(index, offsetBy: 2)
            guard let byte = UInt8(value[index..<nextIndex], radix: 16) else { return nil }
            bytes.append(byte)
            index = nextIndex
        }
        return Data(bytes)
    }
}
