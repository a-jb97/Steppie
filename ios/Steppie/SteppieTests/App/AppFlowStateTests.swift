import Testing
@testable import Steppie

nonisolated struct AppFlowStateTests {
    @Test("표시 상태는 바탕 앱 모드와 시트 목적을 함께 유지한다")
    func presentingStateKeepsBaseModeAndSheetDestination() {
        let state = AppFlowState.presenting(
            base: .guardian,
            sheet: .pin(purpose: .change, flow: .pinChange)
        )

        #expect(state.mode == .guardian)
        #expect(state.sheet == .pin(purpose: .change, flow: .pinChange))
    }

    @Test("시트를 닫으면 표시 전 앱 모드로 돌아간다")
    func dismissSheetReturnsToBaseMode() {
        var childState = AppFlowState.presenting(
            base: .child,
            sheet: .recoveryCodeReset
        )
        var guardianState = AppFlowState.presenting(
            base: .guardian,
            sheet: .recoveryCodeDisplay
        )

        childState.dismissSheet()
        guardianState.dismissSheet()

        #expect(childState == .child)
        #expect(guardianState == .guardian)
    }
}
