import SwiftUI

struct ChildRoutineView: View {
    @Environment(\.locale) private var locale
    let viewModel: ChildRoutineViewModel

    var body: some View {
        GeometryReader { proxy in
            content(layout: ChildRoutineLayoutPolicy.layout(for: proxy.size.width))
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        .background(Color.steppieBackgroundSecondary)
        .task {
            viewModel.loadIfNeeded()
        }
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
            loadedContent(layout: layout)
        }
    }

    @ViewBuilder
    private func loadedContent(layout: ChildRoutineLayout) -> some View {
        switch layout {
        case .splitPane:
            HStack(spacing: 0) {
                splitRoutineList
                    .frame(width: SteppieLayout.splitListWidth)

                Rectangle()
                    .fill(Color.steppieBorderSubtle)
                    .frame(width: SteppieStroke.divider)
                    .accessibilityHidden(true)

                splitFocusView
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        case .singlePane:
            switch viewModel.page {
            case .focus:
                phoneFocusView
                    .contentShape(.rect)
                    .gesture(verticalSwipe(up: viewModel.showList))
            case .list:
                phoneRoutineList
                    .contentShape(.rect)
                    .simultaneousGesture(verticalSwipe(down: viewModel.showFocus))
            }
        }
    }

    private var phoneFocusView: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                childHeader(
                    title: "screen.focus.title",
                    subtitle: "screen.focus.subtitle",
                    titleStyle: .childScreenTitle
                )

                progressDots
                    .padding(.top, 18)

                focusCard(minimumHeight: 448)
                    .padding(.top, 40)

                paneSwitchButton(
                    systemImage: "chevron.down.2",
                    title: "screen.action.showList",
                    action: viewModel.showList
                )
                .padding(.top, SteppieSpacing.small)
            }
            .frame(maxWidth: SteppieLayout.focusCardTabletMaximumWidth)
            .frame(maxWidth: .infinity)
            .padding(.horizontal, SteppieLayout.childScreenPadding)
            .padding(.bottom, SteppieSpacing.large)
        }
        .background(Color.steppieBackgroundSecondary)
    }

    private var phoneRoutineList: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 0) {
                paneSwitchButton(
                    systemImage: "chevron.up.2",
                    title: "screen.action.showFocus",
                    action: viewModel.showFocus
                )

                childHeader(
                    title: "screen.list.title",
                    subtitle: "screen.list.subtitle",
                    titleStyle: .childScreenTitle
                )

                progressDots
                    .padding(.top, 18)

                routineCards(compactMetadata: false)
                    .padding(.top, SteppieSpacing.extraLarge)
            }
            .frame(maxWidth: SteppieLayout.focusCardTabletMaximumWidth)
            .frame(maxWidth: .infinity)
            .padding(.horizontal, SteppieLayout.childScreenPadding)
            .padding(.bottom, SteppieSpacing.extraLarge)
        }
        .background(Color.steppieBackgroundSecondary)
    }

    private var splitRoutineList: some View {
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

    private var splitFocusView: some View {
        ScrollView {
            VStack(spacing: 28) {
                Text("screen.focus.title")
                    .steppieTextStyle(.childPaneTitle)
                    .foregroundStyle(Color.steppieTextSecondary)
                    .multilineTextAlignment(.center)
                    .accessibilityAddTraits(.isHeader)

                focusCard(minimumHeight: 520)
            }
            .frame(maxWidth: SteppieLayout.focusCardTabletMaximumWidth)
            .frame(maxWidth: .infinity, minHeight: 722)
            .padding(56)
        }
        .background(Color.steppieBackgroundPrimary)
    }

    private func childHeader(
        title: LocalizedStringKey,
        subtitle: LocalizedStringKey,
        titleStyle: SteppieTextStyle
    ) -> some View {
        VStack(alignment: .leading, spacing: SteppieSpacing.twoExtraSmall) {
            Text(title)
                .steppieTextStyle(titleStyle)
                .foregroundStyle(Color.steppieTextSecondary)
                .fixedSize(horizontal: false, vertical: true)
                .accessibilityAddTraits(.isHeader)

            Text(subtitle)
                .steppieTextStyle(.childSubtitle)
                .foregroundStyle(Color.steppieTextPrimary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var progressDots: some View {
        HStack(spacing: SteppieSpacing.extraSmall) {
            ForEach(0..<viewModel.totalCount, id: \.self) { index in
                Circle()
                    .fill(
                        index < viewModel.completedCount
                            ? Color.steppieFocusRing
                            : Color.steppieBorderSubtle
                    )
                    .frame(width: 24, height: 24)
                    .accessibilityHidden(true)
            }

            Text(verbatim: "\(viewModel.completedCount)/\(viewModel.totalCount)")
                .steppieTextStyle(.childProgress)
                .foregroundStyle(Color.steppieTextSecondary)
                .fixedSize()
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(
            Text("screen.progress.accessibility")
            + Text(verbatim: " \(viewModel.completedCount)/\(viewModel.totalCount)")
        )
    }

    @ViewBuilder
    private func focusCard(minimumHeight: CGFloat) -> some View {
        if let routine = viewModel.selectedRoutine {
            RoutineCard(
                title: Text(verbatim: localizedTitle(for: routine)),
                metadata: focusMetadata(for: routine),
                presentation: .focus,
                state: viewModel.cardState(for: routine),
                cardColor: SteppieCardColor(colorToken: routine.colorToken),
                isActionEnabled: false,
                focusMinimumHeight: minimumHeight
            ) {} visual: {
                RoutineVisualView(icon: routine.icon, size: .card)
            }
        }
    }

    private func routineCards(compactMetadata: Bool) -> some View {
        LazyVStack(spacing: compactMetadata ? SteppieSpacing.medium : 14) {
            ForEach(viewModel.routines) { routine in
                let isSelected = routine.id == viewModel.selectedRoutine?.id
                RoutineCard(
                    title: Text(verbatim: localizedTitle(for: routine)),
                    metadata: listMetadata(
                        for: routine,
                        compact: compactMetadata
                    ),
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

    private func paneSwitchButton(
        systemImage: String,
        title: LocalizedStringKey,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            VStack(spacing: SteppieSpacing.extraSmall) {
                Image(systemName: systemImage)
                    .font(.system(size: 24, weight: .bold))
                    .accessibilityHidden(true)
                Text(title)
                    .steppieTextStyle(.childNavigation)
            }
            .foregroundStyle(Color.steppieTextSecondary)
            .frame(maxWidth: .infinity, minHeight: SteppieLayout.childMinimumTouchTarget)
            .contentShape(.rect)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(Text(title))
    }

    private func focusMetadata(for routine: Routine) -> Text {
        viewModel.cardState(for: routine) == .current
            ? Text("screen.focus.tapHint")
            : Text("screen.focus.previewHint")
    }

    private func listMetadata(for routine: Routine, compact: Bool) -> Text {
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

    private func verticalSwipe(
        up action: @escaping () -> Void
    ) -> some Gesture {
        DragGesture(minimumDistance: 60)
            .onEnded { value in
                guard abs(value.translation.height) > abs(value.translation.width),
                      value.translation.height < -60 else { return }
                action()
            }
    }

    private func verticalSwipe(
        down action: @escaping () -> Void
    ) -> some Gesture {
        DragGesture(minimumDistance: 60)
            .onEnded { value in
                guard abs(value.translation.height) > abs(value.translation.width),
                      value.translation.height > 60 else { return }
                action()
            }
    }
}

#Preview("Child Routine · iPhone", traits: .fixedLayout(width: 393, height: 852)) {
    let repository = try! RoutinePreviewStore.makeSampleRepository()
    ChildRoutineView(viewModel: ChildRoutineViewModel(repository: repository))
}

#Preview("Child Routine · iPad Landscape", traits: .fixedLayout(width: 1194, height: 834)) {
    let repository = try! RoutinePreviewStore.makeSampleRepository()
    ChildRoutineView(viewModel: ChildRoutineViewModel(repository: repository))
}

#Preview("Child Routine · Accessibility Text", traits: .fixedLayout(width: 393, height: 852)) {
    let repository = try! RoutinePreviewStore.makeSampleRepository()
    ChildRoutineView(viewModel: ChildRoutineViewModel(repository: repository))
        .environment(\.dynamicTypeSize, .accessibility3)
}
