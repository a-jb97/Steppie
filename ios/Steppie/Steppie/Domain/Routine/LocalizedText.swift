import Foundation

nonisolated struct LocalizedText: Codable, Equatable, Sendable {
    let values: [String: String]

    init(_ values: [String: String]) throws {
        guard !values.isEmpty else {
            throw RoutineDomainError.localizedTextRequiresValue
        }

        var normalized: [String: String] = [:]
        for (tag, value) in values {
            let normalizedTag = tag.replacingOccurrences(of: "_", with: "-")
            guard Self.isValidLanguageTag(normalizedTag) else {
                throw RoutineDomainError.invalidLocaleTag(tag)
            }

            let trimmedValue = value.trimmingCharacters(in: .whitespacesAndNewlines)
            if !trimmedValue.isEmpty {
                normalized[normalizedTag] = trimmedValue
            }
        }

        guard !normalized.isEmpty else {
            throw RoutineDomainError.localizedTextRequiresValue
        }
        self.values = normalized
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        try self.init(container.decode([String: String].self))
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()
        try container.encode(values)
    }

    func resolved(
        appLocale: String?,
        systemLanguages: [String] = Locale.preferredLanguages
    ) -> String {
        let candidates = [appLocale].compactMap { $0 } + systemLanguages + ["ko"]
        for candidate in candidates {
            if let value = value(matching: candidate) {
                return value
            }
        }

        return values.sorted { $0.key < $1.key }.first?.value ?? ""
    }

    private func value(matching localeIdentifier: String) -> String? {
        let normalizedIdentifier = localeIdentifier
            .replacingOccurrences(of: "_", with: "-")
            .lowercased()

        if let exact = values.first(where: { $0.key.lowercased() == normalizedIdentifier }) {
            return exact.value
        }

        let language = normalizedIdentifier.split(separator: "-").first.map(String.init)
        return values.first {
            $0.key.lowercased().split(separator: "-").first.map(String.init) == language
        }?.value
    }

    private static func isValidLanguageTag(_ tag: String) -> Bool {
        let parts = tag.split(separator: "-", omittingEmptySubsequences: false)
        guard let language = parts.first,
              (2...8).contains(language.count),
              language.allSatisfy(\.isLetter) else {
            return false
        }

        return parts.dropFirst().allSatisfy { part in
            (1...8).contains(part.count) && part.allSatisfy { $0.isLetter || $0.isNumber }
        }
    }
}
