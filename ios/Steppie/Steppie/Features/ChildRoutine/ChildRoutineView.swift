import SwiftUI

struct ChildRoutineView: View {
    let viewModel: ChildRoutineViewModel
    let tutorialCoordinator: TutorialCoordinator
    let onGuardianEntryRequested: () -> Void

    init(
        viewModel: ChildRoutineViewModel,
        tutorialCoordinator: TutorialCoordinator,
        onGuardianEntryRequested: @escaping () -> Void = {}
    ) {
        self.viewModel = viewModel
        self.tutorialCoordinator = tutorialCoordinator
        self.onGuardianEntryRequested = onGuardianEntryRequested
    }

    var body: some View {
        GeometryReader { proxy in
            content(layout: ChildRoutineLayoutPolicy.layout(for: proxy.size.width))
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .overlay(alignment: .topTrailing) {
                    guardianEntryHotspot
                }
        }
        .background(Color.steppieBackgroundSecondary)
        .onAppear {
            viewModel.setRoutineSpeechActive(true)
        }
        .onDisappear {
            viewModel.setRoutineSpeechActive(false)
        }
        .task {
            viewModel.loadIfNeeded()
        }
        .tutorialOverlay(
            coordinator: tutorialCoordinator,
            screen: viewModel.page == .focus ? .childFocus : .childList,
            childMode: true,
            onPresentationChanged: { isPresented in
                viewModel.setRoutineSpeechActive(!isPresented)
            }
        )
    }

    private var guardianEntryHotspot: some View {
        Color.clear
            .frame(width: SteppieLayout.childMinimumTouchTarget, height: SteppieLayout.childMinimumTouchTarget)
            .contentShape(.rect)
            .gesture(
                LongPressGesture(minimumDuration: 3)
                    .onEnded { _ in onGuardianEntryRequested() }
            )
            .accessibilityHidden(true)
            .tutorialTarget(.guardianEntry)
    }

    @ViewBuilder
    private func content(layout: ChildRoutineLayout) -> some View {
        switch viewModel.loadState {
        case .idle:
            ProgressView()
                .accessibilityLabel(Text("screen.loading"))
        case .empty:
            messageState(
                title: "screen.empty.title",
                message: "screen.empty.message",
                allowsRetry: false
            )
        case .failed:
            messageState(
                title: "screen.error.title",
                message: "screen.error.message",
                allowsRetry: true
            )
        case .loaded:
            if viewModel.isWaitingForNextRoutineSet {
                ChildRoutineWaitingView(viewModel: viewModel)
            } else if viewModel.isAllCompleted && !viewModel.isShowingCompletionFeedback {
                ChildRoutineAllDoneView(viewModel: viewModel)
            } else {
                loadedContent(layout: layout)
            }
        }
    }

    @ViewBuilder
    private func loadedContent(layout: ChildRoutineLayout) -> some View {
        ChildRoutineLayoutView(
            layout: layout,
            page: viewModel.page,
            onShowList: viewModel.showList,
            onShowFocus: viewModel.showFocus
        ) {
            ChildRoutineFocusView(
                presentation: .phone,
                viewModel: viewModel
            )
        } phoneList: {
            ChildRoutineListView(
                presentation: .phone,
                viewModel: viewModel
            )
        } splitList: {
            ChildRoutineListView(
                presentation: .split,
                viewModel: viewModel
            )
        } splitFocus: {
            ChildRoutineFocusView(
                presentation: .split,
                viewModel: viewModel
            )
        }
    }

    private func messageState(
        title: LocalizedStringKey,
        message: LocalizedStringKey,
        allowsRetry: Bool
    ) -> some View {
        VStack(spacing: SteppieSpacing.large) {
            Text(title)
                .steppieTextStyle(.childScreenTitle)
                .foregroundStyle(Color.steppieTextSecondary)
                .multilineTextAlignment(.center)
            Text(message)
                .steppieTextStyle(.childSubtitle)
                .foregroundStyle(Color.steppieTextPrimary)
                .multilineTextAlignment(.center)

            if allowsRetry {
                SteppieButton(
                    "screen.action.retry",
                    size: .childLarge,
                    action: viewModel.load
                )
                .frame(maxWidth: 320)
            }
        }
        .padding(SteppieLayout.childScreenPadding)
    }

}

#Preview("Child Routine · iPhone", traits: .fixedLayout(width: 393, height: 852)) {
    let repository = try! RoutinePreviewStore.makeLightweightSampleRepository()
    ChildRoutineView(viewModel: ChildRoutineViewModel.preview(repository: repository), tutorialCoordinator: TutorialCoordinator())
}

#Preview("Child Routine · iPad Landscape", traits: .fixedLayout(width: 1194, height: 834)) {
    let repository = try! RoutinePreviewStore.makeLightweightSampleRepository()
    ChildRoutineView(viewModel: ChildRoutineViewModel.preview(repository: repository), tutorialCoordinator: TutorialCoordinator())
}

#Preview("Child Routine · Accessibility Text", traits: .fixedLayout(width: 393, height: 852)) {
    let repository = try! RoutinePreviewStore.makeLightweightSampleRepository()
    ChildRoutineView(viewModel: ChildRoutineViewModel.preview(repository: repository), tutorialCoordinator: TutorialCoordinator())
        .environment(\.dynamicTypeSize, .accessibility3)
}
