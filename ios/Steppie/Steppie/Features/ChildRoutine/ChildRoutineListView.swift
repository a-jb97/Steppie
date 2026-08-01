import SwiftUI

struct ChildRoutineListView: View {
    @Environment(\.locale) private var locale
    let presentation: ChildRoutinePanePresentation
    let viewModel: ChildRoutineViewModel

    @ViewBuilder
    var body: some View {
        switch presentation {
        case .phone:
            phoneList
        case .split:
            splitList
        }
    }

    private var phoneList: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 0) {
                ChildRoutinePaneSwitchButton(
                    systemImage: "chevron.up.2",
                    title: "screen.action.showFocus",
                    action: viewModel.showFocus
                )

                ChildRoutineHeader(
                    title: "screen.list.title",
                    subtitle: "screen.list.subtitle",
                    titleStyle: .childScreenTitle
                )
                .tutorialTarget(.primary)

                progress
                    .padding(.top, ChildRoutinePresentationMetrics.progressTopPadding)

                routineCards(compactMetadata: false)
                    .padding(.top, SteppieSpacing.extraLarge)
                    .tutorialTarget(.secondary)
            }
            .frame(maxWidth: SteppieLayout.focusCardTabletMaximumWidth)
            .frame(maxWidth: .infinity)
            .padding(.horizontal, SteppieLayout.childScreenPadding)
            .padding(.bottom, SteppieSpacing.extraLarge)
        }
        .background(Color.steppieBackgroundSecondary)
    }

    private var splitList: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: SteppieSpacing.medium) {
                Text("screen.list.title")
                    .steppieTextStyle(.childPaneTitle)
                    .foregroundStyle(Color.steppieTextSecondary)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .accessibilityAddTraits(.isHeader)

                routineCards(compactMetadata: true)
            }
            .padding(.horizontal, SteppieLayout.guardianScreenPadding)
            .padding(.vertical, SteppieSpacing.extraLarge)
        }
        .background(Color.steppieBackgroundSecondary)
    }

    private var progress: some View {
        ChildRoutineProgressView(
            completedCount: viewModel.completedCount,
            totalCount: viewModel.totalCount
        )
    }

    private func routineCards(compactMetadata: Bool) -> some View {
        LazyVStack(spacing: compactMetadata ? SteppieSpacing.medium : 14) {
            ForEach(viewModel.routines) { routine in
                let isSelected = routine.id == viewModel.selectedRoutine?.id
                RoutineCard(
                    title: Text(verbatim: localizedTitle(for: routine)),
                    metadata: metadata(for: routine, compact: compactMetadata),
                    presentation: .list,
                    state: viewModel.cardState(for: routine),
                    cardColor: SteppieCardColor(colorToken: routine.colorToken)
                ) {
                    viewModel.selectRoutine(
                        routine,
                        showFocus: !compactMetadata
                    )
                } visual: {
                    RoutineVisualView(icon: routine.icon, size: .list)
                }
                .accessibilityAddTraits(isSelected ? .isSelected : [])
            }
        }
    }

    private func metadata(for routine: Routine, compact: Bool) -> Text {
        if viewModel.cardState(for: routine) == .completed {
            return compact
                ? Text("screen.list.completed")
                : Text(verbatim: "\(routine.order + 1) · ") + Text("screen.list.completed")
        }

        if viewModel.cardState(for: routine) == .current {
            return compact
                ? Text("screen.list.current")
                : Text(verbatim: "\(routine.order + 1) · ") + Text("screen.list.current")
        }

        let order = compact ? Text("") : Text(verbatim: "\(routine.order + 1)")
        guard let scheduledTime = routine.scheduledTime else { return order }
        return compact
            ? Text(verbatim: scheduledTime.description)
            : order + Text(verbatim: " · \(scheduledTime.description)")
    }

    private func localizedTitle(for routine: Routine) -> String {
        routine.title.resolved(
            appLocale: locale.identifier,
            systemLanguages: [locale.identifier]
        )
    }
}
