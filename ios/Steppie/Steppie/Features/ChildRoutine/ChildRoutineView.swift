import SwiftUI

struct ChildRoutineView: View {
    @Environment(\.locale) private var locale
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    let viewModel: ChildRoutineViewModel
    let onGuardianEntryRequested: () -> Void

    init(
        viewModel: ChildRoutineViewModel,
        onGuardianEntryRequested: @escaping () -> Void = {}
    ) {
        self.viewModel = viewModel
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
        .task {
            viewModel.loadIfNeeded()
        }
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
            if viewModel.isAllCompleted && !viewModel.isShowingCompletionFeedback {
                allDoneView
            } else {
                loadedContent(layout: layout)
            }
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
                if viewModel.isShowingCompletionFeedback {
                    childHeader(
                        title: "screen.feedback.title",
                        subtitle: "screen.feedback.subtitle",
                        titleStyle: .childFeedbackTitle,
                        titleColor: .steppieSuccess
                    )
                } else {
                    childHeader(
                        title: "screen.focus.title",
                        subtitle: "screen.focus.subtitle",
                        titleStyle: .childScreenTitle
                    )
                }

                progressDots
                    .padding(.top, 18)

                focusContent(minimumHeight: 448)
                    .padding(.top, 40)

                if viewModel.isShowingCompletionFeedback {
                    feedbackUndoButton
                        .padding(.top, SteppieSpacing.small)
                    nextRoutinePreview
                        .padding(.top, SteppieSpacing.extraSmall)
                } else {
                    paneSwitchButton(
                        systemImage: "chevron.down.2",
                        title: "screen.action.showList",
                        action: viewModel.showList
                    )
                    .padding(.top, SteppieSpacing.small)
                }
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
                if viewModel.isShowingCompletionFeedback {
                    Text("screen.feedback.title")
                        .steppieTextStyle(.childPaneTitle)
                        .foregroundStyle(Color.steppieSuccess)
                        .multilineTextAlignment(.center)
                        .accessibilityAddTraits(.isHeader)
                } else {
                    Text("screen.focus.title")
                        .steppieTextStyle(.childPaneTitle)
                        .foregroundStyle(Color.steppieTextSecondary)
                        .multilineTextAlignment(.center)
                        .accessibilityAddTraits(.isHeader)
                }

                focusContent(minimumHeight: 520)

                if viewModel.isShowingCompletionFeedback {
                    feedbackUndoButton
                    nextRoutinePreview
                }
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
        titleStyle: SteppieTextStyle,
        titleColor: Color = .steppieTextSecondary
    ) -> some View {
        VStack(alignment: .leading, spacing: SteppieSpacing.twoExtraSmall) {
            Text(title)
                .steppieTextStyle(titleStyle)
                .foregroundStyle(titleColor)
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
        VStack(alignment: .leading, spacing: SteppieSpacing.extraSmall) {
            if !dynamicTypeSize.isAccessibilitySize {
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
                }
            }

            Text(verbatim: "\(viewModel.completedCount)/\(viewModel.totalCount)")
                .steppieTextStyle(.childProgress)
                .foregroundStyle(Color.steppieTextSecondary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(
            Text("screen.progress.accessibility")
            + Text(verbatim: " \(viewModel.completedCount)/\(viewModel.totalCount)")
        )
    }

    @ViewBuilder
    private func focusContent(minimumHeight: CGFloat) -> some View {
        if viewModel.isShowingCompletionFeedback, let routine = viewModel.selectedRoutine {
            completedFeedbackCard(for: routine, minimumHeight: minimumHeight)
        } else {
            focusCard(minimumHeight: minimumHeight)
        }
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
                isActionEnabled: viewModel.cardState(for: routine) == .current,
                focusMinimumHeight: minimumHeight
            ) {
                viewModel.completeSelectedRoutine()
            } visual: {
                RoutineVisualView(icon: routine.icon, size: .card)
            }
        }
    }

    private func completedFeedbackCard(for routine: Routine, minimumHeight: CGFloat) -> some View {
        VStack(spacing: SteppieSpacing.large) {
            PraiseFeedbackMark(
                imageName: "feedback-check",
                size: CGSize(width: 150, height: 150),
                intensity: effectiveFeedbackIntensity,
                reduceMotion: reduceMotion,
                showsParticles: true
            )

            Text(verbatim: localizedTitle(for: routine) + localizedCompleteSuffix)
                .steppieTextStyle(.childCardTitle)
                .foregroundStyle(Color.steppieTextPrimary)
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(36)
        .frame(maxWidth: SteppieLayout.focusCardPhoneMaximumWidth)
        .frame(maxWidth: .infinity)
        .frame(minHeight: minimumHeight)
        .background(Color.steppieCardMint)
        .overlay {
            PraiseFeedbackOutline(
                cornerRadius: SteppieCornerRadius.card,
                intensity: effectiveFeedbackIntensity,
                reduceMotion: reduceMotion,
                lineWidth: 4
            )
        }
        .clipShape(.rect(cornerRadius: SteppieCornerRadius.card))
        .id(routine.id)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Text(verbatim: localizedTitle(for: routine) + localizedCompleteSuffix))
        .accessibilityValue(Text("a11y.status.completed"))
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

    private var feedbackUndoButton: some View {
        Button(action: viewModel.undoLastCompletion) {
            HStack(spacing: SteppieSpacing.extraSmall) {
                Image(feedbackUndoIconName)
                    .resizable()
                    .scaledToFit()
                    .frame(width: 18, height: 18)
                    .accessibilityHidden(true)
                Text("screen.feedback.undo")
                    .steppieTextStyle(.childUndo)
                    .lineLimit(2)
                    .multilineTextAlignment(.center)
            }
            .foregroundStyle(Color.steppieTextSecondary)
            .padding(.horizontal, SteppieSpacing.medium)
            .frame(minHeight: 39)
            .background(Color.steppieBackgroundSecondary)
            .overlay {
                Capsule()
                    .stroke(Color.steppieTextSecondary, lineWidth: 2)
            }
            .clipShape(.capsule)
            .shadow(color: Color.steppieTextSecondary.opacity(0.14), radius: 2, y: 3)
        }
        .buttonStyle(.plain)
        .frame(maxWidth: .infinity, minHeight: SteppieLayout.childMinimumTouchTarget)
        .opacity(viewModel.canUndoCompletion ? 1 : 0)
        .disabled(!viewModel.canUndoCompletion)
        .accessibilityLabel(Text("screen.feedback.undo"))
    }

    private var feedbackUndoIconName: String {
        colorScheme == .dark ? "feedback-undo-arrow-dark" : "feedback-undo-arrow"
    }

    @ViewBuilder
    private var nextRoutinePreview: some View {
        if let nextRoutine = viewModel.nextRoutineAfterFeedback {
            Button(action: viewModel.proceedAfterCompletionFeedback) {
                adaptivePreviewStack {
                    RoutineVisualView(icon: nextRoutine.icon, size: .list)
                        .frame(width: 52, height: 52)
                        .accessibilityHidden(true)

                    Text("screen.feedback.nextPrefix")
                        + Text(verbatim: localizedTitle(for: nextRoutine))
                }
                .steppieTextStyle(.childListTitle)
                .foregroundStyle(Color.steppieTextPrimary)
                .lineLimit(dynamicTypeSize.isAccessibilitySize ? nil : 2)
                .fixedSize(horizontal: false, vertical: true)
                .frame(maxWidth: .infinity, minHeight: SteppieLayout.childMinimumTouchTarget, alignment: .leading)
                .padding(.horizontal, SteppieSpacing.medium)
                .background(Color.steppieCardPeach)
                .clipShape(.rect(cornerRadius: SteppieCornerRadius.card))
            }
            .buttonStyle(.plain)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(Text("screen.feedback.nextPrefix") + Text(verbatim: localizedTitle(for: nextRoutine)))
        } else if viewModel.isAllCompleted {
            Button(action: viewModel.proceedAfterCompletionFeedback) {
                adaptivePreviewStack {
                    Image("routine-all-done-stamp")
                        .resizable()
                        .scaledToFit()
                        .frame(width: 52, height: 52)
                        .accessibilityHidden(true)

                    VStack(alignment: .leading, spacing: SteppieSpacing.twoExtraSmall) {
                        Text("screen.allDone.title")
                        Text("screen.allDone.subtitle")
                            .steppieTextStyle(.childCaption)
                            .foregroundStyle(Color.steppieTextSecondary)
                    }
                }
                .steppieTextStyle(.childListTitle)
                .foregroundStyle(Color.steppieTextPrimary)
                .lineLimit(dynamicTypeSize.isAccessibilitySize ? nil : 2)
                .fixedSize(horizontal: false, vertical: true)
                .frame(maxWidth: .infinity, minHeight: SteppieLayout.childMinimumTouchTarget, alignment: .leading)
                .padding(.horizontal, SteppieSpacing.medium)
                .background(Color.steppieCardSky)
                .clipShape(.rect(cornerRadius: SteppieCornerRadius.card))
            }
            .buttonStyle(.plain)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(Text("screen.allDone.title"))
            .accessibilityValue(Text("screen.allDone.subtitle"))
        }
    }

    private var allDoneView: some View {
        ScrollView {
            VStack {
                Spacer(minLength: 72)

                VStack(spacing: SteppieSpacing.large) {
                    PraiseFeedbackMark(
                        imageName: allDoneStampImageName,
                        size: CGSize(width: 168, height: 164),
                        intensity: effectiveFeedbackIntensity,
                        reduceMotion: reduceMotion,
                        showsParticles: false
                    )

                    Text("screen.allDone.title")
                        .steppieTextStyle(.childCardTitle)
                        .foregroundStyle(Color.steppieTextPrimary)
                        .multilineTextAlignment(.center)
                        .fixedSize(horizontal: false, vertical: true)

                    Text("screen.allDone.subtitle")
                        .steppieTextStyle(.childAllDoneSubtitle)
                        .foregroundStyle(Color.steppieFocusRing)
                        .multilineTextAlignment(.center)
                        .fixedSize(horizontal: false, vertical: true)
                }
                .padding(SteppieSpacing.extraLarge)
                .frame(maxWidth: SteppieLayout.focusCardPhoneMaximumWidth)
                .frame(maxWidth: .infinity)
                .frame(minHeight: 620)
                .background(Color.steppieCardSky)
                .overlay {
                    if effectiveFeedbackIntensity == .strong && !reduceMotion {
                        AllDoneFireworks()
                            .padding(SteppieSpacing.medium)
                            .accessibilityHidden(true)
                    }
                }
                .clipShape(.rect(cornerRadius: SteppieCornerRadius.sheet))
                .accessibilityElement(children: .combine)

                Spacer(minLength: 72)
            }
            .frame(maxWidth: SteppieLayout.focusCardTabletMaximumWidth)
            .frame(maxWidth: .infinity)
            .padding(.horizontal, SteppieLayout.childScreenPadding)
        }
        .background(Color.steppieBackgroundSecondary)
    }

    private func localizedTitle(for routine: Routine) -> String {
        routine.title.resolved(
            appLocale: locale.identifier,
            systemLanguages: [locale.identifier]
        )
    }

    private var localizedCompleteSuffix: String {
        locale.language.languageCode?.identifier == "en" ? " complete!" : " 완료!"
    }

    private var allDoneStampImageName: String {
        colorScheme == .dark ? "routine-all-done-stamp-dark" : "routine-all-done-stamp"
    }

    private var effectiveFeedbackIntensity: FeedbackIntensity {
        let intensity = viewModel.settings?.feedbackIntensity ?? .normal
        if reduceMotion && (intensity == .strong || intensity == .normal) {
            return .quiet
        }
        return intensity
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

    @ViewBuilder
    private func adaptivePreviewStack<Content: View>(
        @ViewBuilder content: () -> Content
    ) -> some View {
        if dynamicTypeSize.isAccessibilitySize {
            VStack(alignment: .leading, spacing: SteppieSpacing.small, content: content)
        } else {
            HStack(spacing: SteppieSpacing.small, content: content)
        }
    }
}

private struct PraiseFeedbackMark: View {
    let imageName: String
    let size: CGSize
    let intensity: FeedbackIntensity
    let reduceMotion: Bool
    let showsParticles: Bool

    @State private var hasAnimated = false

    var body: some View {
        Image(imageName)
            .resizable()
            .scaledToFit()
            .frame(width: size.width, height: size.height)
            .scaleEffect(markScale)
            .opacity(markOpacity)
            .overlay {
                if shouldShowParticles {
                    PraiseParticles(isExpanded: hasAnimated)
                        .frame(width: size.width + 96, height: size.height + 96)
                        .accessibilityHidden(true)
                }
            }
            .accessibilityHidden(true)
            .onAppear {
                startAnimationIfNeeded()
            }
    }

    private var shouldAnimate: Bool {
        !reduceMotion && (intensity == .strong || intensity == .normal)
    }

    private var shouldShowParticles: Bool {
        shouldAnimate && intensity == .strong && showsParticles
    }

    private var markScale: CGFloat {
        guard shouldAnimate else { return 1 }
        if hasAnimated { return 1 }
        return intensity == .strong ? 0.74 : 0.86
    }

    private var markOpacity: Double {
        guard shouldAnimate else { return 1 }
        return hasAnimated ? 1 : 0
    }

    private func startAnimationIfNeeded() {
        guard shouldAnimate, !hasAnimated else { return }
        let animation: Animation = intensity == .strong
            ? .spring(response: 0.42, dampingFraction: 0.58)
            : .easeOut(duration: 0.28)
        withAnimation(animation) {
            hasAnimated = true
        }
    }
}

private struct PraiseFeedbackOutline: View {
    let cornerRadius: CGFloat
    let intensity: FeedbackIntensity
    let reduceMotion: Bool
    let lineWidth: CGFloat

    @State private var hasAnimated = false

    var body: some View {
        RoundedRectangle(cornerRadius: cornerRadius)
            .stroke(Color.steppieSuccess.opacity(primaryOpacity), lineWidth: lineWidth)
            .overlay {
                if shouldPulse {
                    RoundedRectangle(cornerRadius: cornerRadius)
                        .stroke(Color.steppieSuccess.opacity(hasAnimated ? 0 : 0.38), lineWidth: lineWidth)
                        .scaleEffect(hasAnimated ? 1.035 : 1)
                }
            }
            .onAppear {
                startAnimationIfNeeded()
            }
    }

    private var shouldPulse: Bool {
        !reduceMotion && (intensity == .strong || intensity == .normal)
    }

    private var primaryOpacity: Double {
        intensity == .off ? 0 : 1
    }

    private func startAnimationIfNeeded() {
        guard shouldPulse, !hasAnimated else { return }
        withAnimation(.easeOut(duration: intensity == .strong ? 0.52 : 0.34)) {
            hasAnimated = true
        }
    }
}

private struct PraiseParticles: View {
    let isExpanded: Bool

    private let particles: [PraiseParticle] = [
        PraiseParticle(id: 0, x: -44, y: -36, size: 12, color: .steppieCardPeach),
        PraiseParticle(id: 1, x: -18, y: -58, size: 9, color: .steppieFocusRing),
        PraiseParticle(id: 2, x: 24, y: -54, size: 10, color: .steppieCardLemon),
        PraiseParticle(id: 3, x: 50, y: -22, size: 11, color: .steppieCardRose),
        PraiseParticle(id: 4, x: 42, y: 34, size: 8, color: .steppieChildAction),
        PraiseParticle(id: 5, x: 4, y: 58, size: 10, color: .steppieCardLavender),
        PraiseParticle(id: 6, x: -38, y: 42, size: 9, color: .steppieSuccess),
        PraiseParticle(id: 7, x: -58, y: 8, size: 8, color: .steppieCardSky)
    ]

    var body: some View {
        ZStack {
            ForEach(particles) { particle in
                Circle()
                    .fill(particle.color)
                    .frame(width: particle.size, height: particle.size)
                    .offset(
                        x: isExpanded ? particle.x : particle.x * 0.18,
                        y: isExpanded ? particle.y : particle.y * 0.18
                    )
                    .opacity(isExpanded ? 0 : 0.9)
                    .scaleEffect(isExpanded ? 1 : 0.35)
            }
        }
    }
}

private struct PraiseParticle: Identifiable {
    let id: Int
    let x: CGFloat
    let y: CGFloat
    let size: CGFloat
    let color: Color
}

private struct AllDoneFireworks: View {
    private let bursts: [FireworkBurstSpec] = [
        FireworkBurstSpec(
            id: 0,
            xRatio: 0.22,
            yRatio: 0.22,
            radius: 74,
            delay: 0,
            colors: [.steppieCardRose, .steppieCardLemon, .steppieFocusRing]
        ),
        FireworkBurstSpec(
            id: 1,
            xRatio: 0.78,
            yRatio: 0.26,
            radius: 82,
            delay: 0.16,
            colors: [.steppieCardPeach, .steppieChildAction, .steppieCardLavender]
        ),
        FireworkBurstSpec(
            id: 2,
            xRatio: 0.50,
            yRatio: 0.18,
            radius: 62,
            delay: 0.32,
            colors: [.steppieSuccess, .steppieCardSky, .steppieCardLemon]
        )
    ]

    var body: some View {
        GeometryReader { proxy in
            ZStack {
                ForEach(bursts) { burst in
                    FireworkBurst(
                        spec: burst,
                        center: CGPoint(
                            x: proxy.size.width * burst.xRatio,
                            y: proxy.size.height * burst.yRatio
                        )
                    )
                    .frame(width: proxy.size.width, height: proxy.size.height)
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .clipped()
        }
        .allowsHitTesting(false)
    }
}

private struct FireworkBurst: View {
    let spec: FireworkBurstSpec
    let center: CGPoint

    @State private var phase: CGFloat = 0
    @State private var isVisible = false

    var body: some View {
        ZStack {
            ForEach(0..<12, id: \.self) { index in
                fireworkSpark(at: index)
            }

            Circle()
                .stroke(spec.colors[0].opacity(centerOpacity), lineWidth: 3)
                .frame(width: centerRingSize, height: centerRingSize)
                .position(center)
        }
        .onAppear {
            Task {
                await sleep(for: spec.delay)
                isVisible = true

                await sleep(for: 0.08)
                withAnimation(.easeOut(duration: 0.72)) {
                    phase = 1
                }

                await sleep(for: 0.58)
                withAnimation(.easeOut(duration: 0.28)) {
                    isVisible = false
                }
            }
        }
    }

    private func fireworkSpark(at index: Int) -> some View {
        let angle = (Double(index) / 12.0) * Double.pi * 2
        let distance = spec.radius * phase
        let x = CGFloat(cos(angle)) * distance
        let y = CGFloat(sin(angle)) * distance
        let isLongSpark = index.isMultiple(of: 3)

        return Capsule()
            .fill(spec.colors[index % spec.colors.count])
            .frame(width: isLongSpark ? 7 : 9, height: isLongSpark ? 22 : 12)
            .rotationEffect(.radians(angle + Double.pi / 2))
            .scaleEffect(0.45 + phase * 0.65)
            .opacity(sparkOpacity(for: index))
            .position(x: center.x + x, y: center.y + y)
    }

    private func sparkOpacity(for index: Int) -> Double {
        guard isVisible else { return 0 }
        return index.isMultiple(of: 3) ? 0.95 : 0.82
    }

    private var centerOpacity: Double {
        isVisible ? 0.64 : 0
    }

    private var centerRingSize: CGFloat {
        16 + phase * 36
    }

    private func sleep(for seconds: Double) async {
        guard seconds > 0 else { return }
        try? await Task.sleep(nanoseconds: UInt64(seconds * 1_000_000_000))
    }
}

private struct FireworkBurstSpec: Identifiable {
    let id: Int
    let xRatio: CGFloat
    let yRatio: CGFloat
    let radius: CGFloat
    let delay: Double
    let colors: [Color]
}

#Preview("Child Routine · iPhone", traits: .fixedLayout(width: 393, height: 852)) {
    let repository = try! RoutinePreviewStore.makeLightweightSampleRepository()
    ChildRoutineView(viewModel: ChildRoutineViewModel.preview(repository: repository))
}

#Preview("Child Routine · iPad Landscape", traits: .fixedLayout(width: 1194, height: 834)) {
    let repository = try! RoutinePreviewStore.makeLightweightSampleRepository()
    ChildRoutineView(viewModel: ChildRoutineViewModel.preview(repository: repository))
}

#Preview("Child Routine · Accessibility Text", traits: .fixedLayout(width: 393, height: 852)) {
    let repository = try! RoutinePreviewStore.makeLightweightSampleRepository()
    ChildRoutineView(viewModel: ChildRoutineViewModel.preview(repository: repository))
        .environment(\.dynamicTypeSize, .accessibility3)
}
