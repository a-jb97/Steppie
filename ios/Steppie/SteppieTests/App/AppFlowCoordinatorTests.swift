import Testing
@testable import Steppie

@MainActor
struct AppFlowCoordinatorTests {
    @Test("보호자 진입은 PIN 존재 여부에 맞는 확인 화면을 표시한다")
    func beginsGuardianEntryWithExpectedPINPurpose() {
        let setupCoordinator = AppFlowCoordinator()
        let entryCoordinator = AppFlowCoordinator()

        setupCoordinator.beginGuardianEntry(hasGuardianPIN: false)
        entryCoordinator.beginGuardianEntry(hasGuardianPIN: true)

        #expect(
            setupCoordinator.state
                == .presenting(
                    base: .child,
                    sheet: .pin(purpose: .setup, flow: .guardianEntry)
                )
        )
        #expect(
            entryCoordinator.state
                == .presenting(
                    base: .child,
                    sheet: .pin(purpose: .enter, flow: .guardianEntry)
                )
        )
    }

    @Test("PIN 설정 완료는 복구 코드 표시 여부에 맞게 보호자 흐름을 이어간다")
    func completesPINSetupWithOptionalRecoveryCodeDisplay() {
        let coordinator = AppFlowCoordinator()
        coordinator.beginGuardianEntry(hasGuardianPIN: false)

        coordinator.completePIN(purpose: .setup, shouldDisplayRecoveryCode: true)
        #expect(
            coordinator.state
                == .presenting(base: .guardian, sheet: .recoveryCodeDisplay)
        )

        coordinator.dismissSheet()
        #expect(coordinator.state == .guardian)
    }

    @Test("PIN 변경과 복구 코드 흐름은 기존 보호자 모드를 유지한다")
    func keepsGuardianModeAcrossSecurityFlows() {
        let coordinator = AppFlowCoordinator(state: .guardian)

        coordinator.requestPINChange()
        #expect(
            coordinator.state
                == .presenting(
                    base: .guardian,
                    sheet: .pin(purpose: .change, flow: .pinChange)
                )
        )

        coordinator.completePIN(purpose: .change)
        coordinator.requestRecoveryCodeRegeneration()
        coordinator.completeRecoveryCodeRegeneration(succeeded: true)

        #expect(
            coordinator.state
                == .presenting(base: .guardian, sheet: .recoveryCodeDisplay)
        )
    }
}
