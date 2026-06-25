import SwiftUI

struct ContentView: View {
    @Environment(\.scenePhase) private var scenePhase
    @State private var viewModel: ChildRoutineViewModel
    private let notificationRouter: RoutineNotificationRouter?

    init(
        repository: any RoutineRepository,
        speechGuide: (any RoutineSpeechGuiding)? = nil,
        feedbackPerformer: (any RoutineFeedbackPerforming)? = nil,
        notificationScheduler: (any RoutineNotificationScheduling)? = nil,
        isNotificationSchedulingEnabled: Bool = true,
        notificationRouter: RoutineNotificationRouter? = nil
    ) {
        self.notificationRouter = notificationRouter
        _viewModel = State(
            initialValue: ChildRoutineViewModel(
                repository: repository,
                speechGuide: speechGuide,
                feedbackPerformer: feedbackPerformer,
                notificationScheduler: notificationScheduler,
                isNotificationSchedulingEnabled: isNotificationSchedulingEnabled
            )
        )
    }

    var body: some View {
        ChildRoutineView(viewModel: viewModel)
            .onChange(of: scenePhase) { _, newValue in
                guard newValue == .active else { return }
                viewModel.appDidBecomeActive()
                consumePendingNotificationRoute()
            }
            .onChange(of: notificationRouter?.pendingRoute) { _, _ in
                consumePendingNotificationRoute()
            }
    }

    private func consumePendingNotificationRoute() {
        guard let route = notificationRouter?.consumePendingRoute() else { return }
        viewModel.openNotificationRoute(route)
    }
}

#Preview {
    let repository = try! RoutinePreviewStore.makeLightweightSampleRepository()
    ContentView(
        repository: repository,
        notificationScheduler: NoopRoutineNotificationScheduler(),
        isNotificationSchedulingEnabled: false
    )
}
