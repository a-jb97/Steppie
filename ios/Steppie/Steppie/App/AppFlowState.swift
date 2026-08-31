nonisolated enum SteppieAppMode: Equatable {
    case child
    case guardian
}

nonisolated enum GuardianPINFlow: Equatable {
    case initialSetup
    case guardianEntry
    case pinChange
    case recoveryRegeneration
    case recoveryReset
}

nonisolated enum GuardianSheetDestination: Equatable {
    case pin(purpose: GuardianPINPurpose, flow: GuardianPINFlow)
    case recoveryCodeReset
    case recoveryCodeDisplay

    var allowsDismissal: Bool {
        guard case let .pin(_, flow) = self else { return true }
        return flow != .initialSetup
    }
}

nonisolated enum AppFlowState: Equatable {
    case child
    case guardian
    case presenting(base: SteppieAppMode, sheet: GuardianSheetDestination)

    var mode: SteppieAppMode {
        switch self {
        case .child:
            .child
        case .guardian:
            .guardian
        case let .presenting(base, _):
            base
        }
    }

    var sheet: GuardianSheetDestination? {
        guard case let .presenting(_, sheet) = self else { return nil }
        return sheet
    }

    mutating func dismissSheet() {
        self = mode == .child ? .child : .guardian
    }
}
