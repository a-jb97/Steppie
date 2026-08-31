import Observation

@MainActor
@Observable
final class AppFlowCoordinator {
    private(set) var state: AppFlowState

    init(state: AppFlowState = .child) {
        self.state = state
    }

    func beginInitialPINSetup() {
        state = .presenting(
            base: .child,
            sheet: .pin(purpose: .setup, flow: .initialSetup)
        )
    }

    func beginGuardianEntry(hasGuardianPIN: Bool) {
        state = .presenting(
            base: .child,
            sheet: .pin(
                purpose: hasGuardianPIN ? .enter : .setup,
                flow: .guardianEntry
            )
        )
    }

    func requestPINChange() {
        state = .presenting(
            base: state.mode,
            sheet: .pin(purpose: .change, flow: .pinChange)
        )
    }

    func requestRecoveryCodeRegeneration() {
        state = .presenting(
            base: state.mode,
            sheet: .pin(purpose: .enter, flow: .recoveryRegeneration)
        )
    }

    func requestRecoveryCodeReset() {
        state = .presenting(base: state.mode, sheet: .recoveryCodeReset)
    }

    func completeRecoveryCodeVerification() {
        state = .presenting(
            base: state.mode,
            sheet: .pin(purpose: .setup, flow: .recoveryReset)
        )
    }

    func completePIN(
        purpose: GuardianPINPurpose,
        shouldDisplayRecoveryCode: Bool = false
    ) {
        switch purpose {
        case .enter:
            state = .guardian
        case .setup:
            state = shouldDisplayRecoveryCode
                ? .presenting(base: .guardian, sheet: .recoveryCodeDisplay)
                : .guardian
        case .change:
            state.dismissSheet()
        }
    }

    func completeRecoveryCodeRegeneration(succeeded: Bool) {
        guard succeeded else {
            state.dismissSheet()
            return
        }
        state = .presenting(base: state.mode, sheet: .recoveryCodeDisplay)
    }

    func dismissSheet() {
        guard state.sheet?.allowsDismissal != false else { return }
        state.dismissSheet()
    }

    func exitGuardianMode() {
        state = .child
    }
}
