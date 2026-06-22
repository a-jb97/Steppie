import Foundation

nonisolated struct LocalTime: Codable, Equatable, Hashable, Sendable, CustomStringConvertible {
    let hour: Int
    let minute: Int

    init(hour: Int, minute: Int) throws {
        guard (0...23).contains(hour), (0...59).contains(minute) else {
            throw RoutineDomainError.invalidLocalTime(
                String(format: "%02d:%02d", hour, minute)
            )
        }
        self.hour = hour
        self.minute = minute
    }

    init(_ value: String) throws {
        let components = value.split(separator: ":", omittingEmptySubsequences: false)
        guard components.count == 2,
              components[0].count == 2,
              components[1].count == 2,
              let hour = Int(components[0]),
              let minute = Int(components[1]),
              (0...23).contains(hour),
              (0...59).contains(minute) else {
            throw RoutineDomainError.invalidLocalTime(value)
        }
        self.hour = hour
        self.minute = minute
    }

    var description: String {
        String(format: "%02d:%02d", hour, minute)
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        try self.init(container.decode(String.self))
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()
        try container.encode(description)
    }
}
