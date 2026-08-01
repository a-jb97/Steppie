import SwiftUI

struct ChildRoutineFocusView: View {
    @Environment(\.locale) private var locale
    let presentation: ChildRoutinePanePresentation
    let viewModel: ChildRoutineViewModel

    @ViewBuilder
    var body: some View {
        switch presentation {
        case .phone:
            phoneFocus
        case .split:
            splitFocus
        }
    }

    private var phoneFocus: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                phoneHeader

                progress
                    .padding(.top, ChildRoutinePresentationMetrics.progressTopPadding)
                    .tutorialTarget(.secondary)

                focusContent(minimumHeight: 448)
                    .padding(.top, 40)
                    .tutorialTarget(.primary)

                if viewModel.isShowingCompletionFeedback {
                    ChildRoutineFeedbackUndoButton(viewModel: viewModel)
                        .padding(.top, SteppieSpacing.small)
                    ChildRoutineNextPreview(viewModel: viewModel)
                        .padding(.top, SteppieSpacing.extraSmall)
                } else {
                    ChildRoutinePaneSwitchButton(
                        systemImage: "chevron.down.2",
                        title: "screen.action.showList",
                        action: viewModel.showList
                    )
                    .padding(.top, SteppieSpacing.small)
                    .tutorialTarget(.tertiary)
                }
            }
            .frame(maxWidth: SteppieLayout.focusCardTabletMaximumWidth)
            .frame(maxWidth: .infinity)
            .padding(.horizontal, SteppieLayout.childScreenPadding)
            .padding(.bottom, SteppieSpacing.large)
        }
        .background(Color.steppieBackgroundSecondary)
    }

    private var splitFocus: some View {
        ScrollView {
            VStack(spacing: 28) {
                splitHeader
                focusContent(minimumHeight: 520)

                if viewModel.isShowingCompletionFeedback {
                    ChildRoutineFeedbackUndoButton(viewModel: viewModel)
                    ChildRoutineNextPreview(viewModel: viewModel)
                }
            }
            .frame(maxWidth: SteppieLayout.focusCardTabletMaximumWidth)
            .frame(maxWidth: .infinity, minHeight: 722)
            .padding(56)
        }
        .background(Color.steppieBackgroundPrimary)
    }

    @ViewBuilder
    private var phoneHeader: some View {
        if viewModel.isShowingCompletionFeedback {
            ChildRoutineHeader(
                title: "screen.feedback.title",
                subtitle: "screen.feedback.subtitle",
                titleStyle: .childFeedbackTitle,
                titleColor: .steppieSuccess
            )
        } else {
            ChildRoutineHeader(
                title: "screen.focus.title",
                subtitle: "screen.focus.subtitle",
                titleStyle: .childScreenTitle
            )
        }
    }

    private var splitHeader: some View {
        Text(splitHeaderTitle)
            .steppieTextStyle(.childPaneTitle)
            .foregroundStyle(
                viewModel.isShowingCompletionFeedback
                    ? Color.steppieSuccess
                    : Color.steppieTextSecondary
            )
            .multilineTextAlignment(.center)
            .accessibilityAddTraits(.isHeader)
    }

    private var splitHeaderTitle: LocalizedStringKey {
        viewModel.isShowingCompletionFeedback ? "screen.feedback.title" : "screen.focus.title"
    }

    private var progress: some View {
        ChildRoutineProgressView(
            completedCount: viewModel.completedCount,
            totalCount: viewModel.totalCount
        )
    }

    @ViewBuilder
    private func focusContent(minimumHeight: CGFloat) -> some View {
        if viewModel.isShowingCompletionFeedback, let routine = viewModel.selectedRoutine {
            ChildRoutineCompletedCard(
                routine: routine,
                minimumHeight: minimumHeight,
                settings: viewModel.settings
            )
        } else if let routine = viewModel.selectedRoutine {
            RoutineCard(
                title: Text(verbatim: localizedTitle(for: routine)),
                metadata: metadata(for: routine),
                presentation: .focus,
                state: viewModel.cardState(for: routine),
                cardColor: SteppieCardColor(colorToken: routine.colorToken),
                isActionEnabled: viewModel.cardState(for: routine) == .current,
                focusMinimumHeight: minimumHeight
            ) {
                viewModel.completeSelectedRoutine()
            } visual: {
                RoutineVisualView(icon: routine.icon, size: .card)
            }
        }
    }

    private func metadata(for routine: Routine) -> Text {
        viewModel.cardState(for: routine) == .current
            ? Text("screen.focus.tapHint")
            : Text("screen.focus.previewHint")
    }

    private func localizedTitle(for routine: Routine) -> String {
        routine.title.resolved(
            appLocale: locale.identifier,
            systemLanguages: [locale.identifier]
        )
    }
}
