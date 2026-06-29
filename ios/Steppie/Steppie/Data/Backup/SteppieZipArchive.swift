import Foundation

enum SteppieZipError: Error, Equatable {
    case invalidArchive
    case unsupportedCompressionMethod(UInt16)
    case duplicateEntry(String)
    case missingEntry(String)
}

struct SteppieZipEntry: Equatable {
    let path: String
    let data: Data
}

enum SteppieZipArchive {
    static func makeArchive(entries: [SteppieZipEntry]) throws -> Data {
        var archive = Data()
        var centralDirectory = Data()
        var seenPaths: Set<String> = []
        var centralDirectoryRecords: [(path: String, crc: UInt32, size: UInt32, offset: UInt32)] = []

        for entry in entries {
            guard seenPaths.insert(entry.path).inserted else {
                throw SteppieZipError.duplicateEntry(entry.path)
            }
            let nameData = Data(entry.path.utf8)
            let crc = CRC32.checksum(entry.data)
            let size = UInt32(entry.data.count)
            let offset = UInt32(archive.count)

            archive.appendUInt32(0x04034b50)
            archive.appendUInt16(20)
            archive.appendUInt16(0)
            archive.appendUInt16(0)
            archive.appendUInt16(0)
            archive.appendUInt16(0)
            archive.appendUInt32(crc)
            archive.appendUInt32(size)
            archive.appendUInt32(size)
            archive.appendUInt16(UInt16(nameData.count))
            archive.appendUInt16(0)
            archive.append(nameData)
            archive.append(entry.data)

            centralDirectoryRecords.append((entry.path, crc, size, offset))
        }

        let centralDirectoryOffset = UInt32(archive.count)
        for record in centralDirectoryRecords {
            let nameData = Data(record.path.utf8)
            centralDirectory.appendUInt32(0x02014b50)
            centralDirectory.appendUInt16(20)
            centralDirectory.appendUInt16(20)
            centralDirectory.appendUInt16(0)
            centralDirectory.appendUInt16(0)
            centralDirectory.appendUInt16(0)
            centralDirectory.appendUInt16(0)
            centralDirectory.appendUInt32(record.crc)
            centralDirectory.appendUInt32(record.size)
            centralDirectory.appendUInt32(record.size)
            centralDirectory.appendUInt16(UInt16(nameData.count))
            centralDirectory.appendUInt16(0)
            centralDirectory.appendUInt16(0)
            centralDirectory.appendUInt16(0)
            centralDirectory.appendUInt16(0)
            centralDirectory.appendUInt32(0)
            centralDirectory.appendUInt32(record.offset)
            centralDirectory.append(nameData)
        }

        let centralDirectorySize = UInt32(centralDirectory.count)
        archive.append(centralDirectory)
        archive.appendUInt32(0x06054b50)
        archive.appendUInt16(0)
        archive.appendUInt16(0)
        archive.appendUInt16(UInt16(centralDirectoryRecords.count))
        archive.appendUInt16(UInt16(centralDirectoryRecords.count))
        archive.appendUInt32(centralDirectorySize)
        archive.appendUInt32(centralDirectoryOffset)
        archive.appendUInt16(0)
        return archive
    }

    static func readArchive(_ data: Data) throws -> [String: Data] {
        var offset = 0
        var entries: [String: Data] = [:]

        while offset + 4 <= data.count {
            let signature = try data.readUInt32(at: offset)
            if signature == 0x02014b50 || signature == 0x06054b50 {
                break
            }
            guard signature == 0x04034b50 else {
                throw SteppieZipError.invalidArchive
            }

            let flags = try data.readUInt16(at: offset + 6)
            let compressionMethod = try data.readUInt16(at: offset + 8)
            guard compressionMethod == 0 else {
                throw SteppieZipError.unsupportedCompressionMethod(compressionMethod)
            }
            guard flags & 0x0008 == 0 else {
                throw SteppieZipError.invalidArchive
            }

            let compressedSize = Int(try data.readUInt32(at: offset + 18))
            let fileNameLength = Int(try data.readUInt16(at: offset + 26))
            let extraLength = Int(try data.readUInt16(at: offset + 28))
            let nameStart = offset + 30
            let nameEnd = nameStart + fileNameLength
            let payloadStart = nameEnd + extraLength
            let payloadEnd = payloadStart + compressedSize
            guard nameEnd <= data.count, payloadEnd <= data.count else {
                throw SteppieZipError.invalidArchive
            }
            guard let path = String(data: data[nameStart..<nameEnd], encoding: .utf8) else {
                throw SteppieZipError.invalidArchive
            }
            guard entries[path] == nil else {
                throw SteppieZipError.duplicateEntry(path)
            }
            entries[path] = Data(data[payloadStart..<payloadEnd])
            offset = payloadEnd
        }

        return entries
    }
}

private enum CRC32 {
    static func checksum(_ data: Data) -> UInt32 {
        var crc: UInt32 = 0xffffffff
        for byte in data {
            let index = Int((crc ^ UInt32(byte)) & 0xff)
            crc = table[index] ^ (crc >> 8)
        }
        return crc ^ 0xffffffff
    }

    private static let table: [UInt32] = (0..<256).map { value in
        var crc = UInt32(value)
        for _ in 0..<8 {
            if crc & 1 == 1 {
                crc = 0xedb88320 ^ (crc >> 1)
            } else {
                crc >>= 1
            }
        }
        return crc
    }
}

private extension Data {
    mutating func appendUInt16(_ value: UInt16) {
        append(contentsOf: [
            UInt8(value & 0xff),
            UInt8((value >> 8) & 0xff),
        ])
    }

    mutating func appendUInt32(_ value: UInt32) {
        append(contentsOf: [
            UInt8(value & 0xff),
            UInt8((value >> 8) & 0xff),
            UInt8((value >> 16) & 0xff),
            UInt8((value >> 24) & 0xff),
        ])
    }

    func readUInt16(at offset: Int) throws -> UInt16 {
        guard offset + 2 <= count else { throw SteppieZipError.invalidArchive }
        return UInt16(self[offset]) | (UInt16(self[offset + 1]) << 8)
    }

    func readUInt32(at offset: Int) throws -> UInt32 {
        guard offset + 4 <= count else { throw SteppieZipError.invalidArchive }
        return UInt32(self[offset])
            | (UInt32(self[offset + 1]) << 8)
            | (UInt32(self[offset + 2]) << 16)
            | (UInt32(self[offset + 3]) << 24)
    }
}
