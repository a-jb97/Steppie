enum SteppieAppMode: Equatable {
    case child
    case guardian
}

enum GuardianPINFlow: Equatable {
    case guardianEntry
    case pinChange
    case recoveryRegeneration
    case recoveryReset
}

enum GuardianSheetDestination: Equatable {
    case pin(purpose: GuardianPINPurpose, flow: GuardianPINFlow)
    case recoveryCodeReset
    case recoveryCodeDisplay
}

enum AppFlowState: Equatable {
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
