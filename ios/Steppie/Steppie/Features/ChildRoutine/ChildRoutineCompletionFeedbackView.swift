import SwiftUI

enum ChildRoutineFeedbackPresentationPolicy {
    static func effectiveIntensity(
        settings: AppSettings?,
        reduceMotion: Bool
    ) -> FeedbackIntensity {
        let intensity = settings?.feedbackIntensity ?? .normal
        if reduceMotion && (intensity == .strong || intensity == .normal) {
            return .quiet
        }
        return intensity
    }
}

struct ChildRoutineCompletedCard: View {
    @Environment(\.locale) private var locale
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    let routine: Routine
    let minimumHeight: CGFloat
    let settings: AppSettings?

    var body: some View {
        VStack(spacing: SteppieSpacing.large) {
            PraiseFeedbackMark(
                imageName: "feedback-check",
                size: CGSize(width: 150, height: 150),
                intensity: effectiveFeedbackIntensity,
                reduceMotion: reduceMotion,
                showsParticles: true
            )

            Text(verbatim: localizedTitle + localizedCompleteSuffix)
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
        .accessibilityLabel(Text(verbatim: localizedTitle + localizedCompleteSuffix))
        .accessibilityValue(Text("a11y.status.completed"))
    }

    private var effectiveFeedbackIntensity: FeedbackIntensity {
        ChildRoutineFeedbackPresentationPolicy.effectiveIntensity(
            settings: settings,
            reduceMotion: reduceMotion
        )
    }

    private var localizedTitle: String {
        routine.title.resolved(
            appLocale: locale.identifier,
            systemLanguages: [locale.identifier]
        )
    }

    private var localizedCompleteSuffix: String {
        locale.language.languageCode?.identifier == "en" ? " complete!" : " 완료!"
    }
}

struct ChildRoutineFeedbackUndoButton: View {
    @Environment(\.colorScheme) private var colorScheme
    let viewModel: ChildRoutineViewModel

    var body: some View {
        Button(action: viewModel.undoLastCompletion) {
            HStack(spacing: SteppieSpacing.extraSmall) {
                Image(colorScheme == .dark ? "feedback-undo-arrow-dark" : "feedback-undo-arrow")
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
}

struct ChildRoutineNextPreview: View {
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.locale) private var locale
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    let viewModel: ChildRoutineViewModel

    @ViewBuilder
    var body: some View {
        if let nextRoutine = viewModel.nextRoutineAfterFeedback {
            Button(action: viewModel.proceedAfterCompletionFeedback) {
                adaptivePreviewStack {
                    RoutineVisualView(icon: nextRoutine.icon, size: .list)
                        .frame(
                            width: ChildRoutinePresentationMetrics.previewVisualSize,
                            height: ChildRoutinePresentationMetrics.previewVisualSize
                        )
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
        } else if let next = viewModel.nextRoutineSetAfterFeedback {
            if viewModel.canStartNextRoutineSetAfterFeedback {
                Button(action: viewModel.proceedAfterCompletionFeedback) {
                    nextRoutineSetPreview(
                        routineSet: next.routineSet,
                        firstRoutine: next.firstRoutine,
                        isLocked: false
                    )
                }
                .buttonStyle(.plain)
                .accessibilityLabel(
                    Text("screen.feedback.nextRoutineSet.label \(localizedTitle(for: next.routineSet))")
                )
                .accessibilityValue(Text("screen.feedback.nextRoutineSet.available"))
            } else {
                nextRoutineSetPreview(
                    routineSet: next.routineSet,
                    firstRoutine: next.firstRoutine,
                    isLocked: true
                )
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(
                    Text("screen.feedback.nextRoutineSet.label \(localizedTitle(for: next.routineSet))")
                )
                .accessibilityValue(unavailableAccessibilityValue(for: next.routineSet))
            }
        } else if viewModel.isAllCompleted {
            Button(action: viewModel.proceedAfterCompletionFeedback) {
                adaptivePreviewStack {
                    Image(allDoneStampImageName)
                        .resizable()
                        .scaledToFit()
                        .frame(
                            width: ChildRoutinePresentationMetrics.previewVisualSize,
                            height: ChildRoutinePresentationMetrics.previewVisualSize
                        )
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

    private func nextRoutineSetPreview(
        routineSet: RoutineSet,
        firstRoutine: Routine,
        isLocked: Bool
    ) -> some View {
        adaptivePreviewStack {
            RoutineVisualView(icon: firstRoutine.icon, size: .list)
                .frame(
                    width: ChildRoutinePresentationMetrics.previewVisualSize,
                    height: ChildRoutinePresentationMetrics.previewVisualSize
                )
                .accessibilityHidden(true)

            VStack(alignment: .leading, spacing: SteppieSpacing.twoExtraSmall) {
                Text(verbatim: localizedTitle(for: routineSet))
                if let startTime = routineSet.dailyStartTime {
                    routineSetStartTimeText(startTime, isLocked: isLocked)
                        .steppieTextStyle(.childCaption)
                        .foregroundStyle(Color.steppieTextSecondary)
                }
            }

            if isLocked {
                Image(systemName: "lock.fill")
                    .font(.title2.weight(.semibold))
                    .foregroundStyle(Color.steppieTextSecondary)
                    .accessibilityHidden(true)
            }
        }
        .steppieTextStyle(.childListTitle)
        .foregroundStyle(Color.steppieTextPrimary)
        .frame(
            maxWidth: .infinity,
            minHeight: SteppieLayout.childMinimumTouchTarget,
            alignment: .leading
        )
        .padding(.horizontal, SteppieSpacing.medium)
        .background(isLocked ? Color.steppieBackgroundPrimary : Color.steppieCardPeach)
        .overlay {
            RoundedRectangle(cornerRadius: SteppieCornerRadius.card)
                .stroke(
                    isLocked ? Color.steppieBorderSubtle : Color.clear,
                    lineWidth: SteppieStroke.divider
                )
        }
        .clipShape(.rect(cornerRadius: SteppieCornerRadius.card))
    }

    private func unavailableAccessibilityValue(for routineSet: RoutineSet) -> Text {
        if let startTime = routineSet.dailyStartTime {
            Text("screen.feedback.nextRoutineSet.lockedUntil \(startTime.description)")
        } else {
            Text("screen.feedback.nextRoutineSet.notAvailable")
        }
    }

    private func routineSetStartTimeText(_ startTime: LocalTime, isLocked: Bool) -> Text {
        if isLocked {
            Text("screen.routineSet.cannotStartUntil \(startTime.description)")
        } else {
            Text("screen.routineSet.startsAt \(startTime.description)")
        }
    }

    private func localizedTitle(for routine: Routine) -> String {
        routine.title.resolved(
            appLocale: locale.identifier,
            systemLanguages: [locale.identifier]
        )
    }

    private func localizedTitle(for routineSet: RoutineSet) -> String {
        routineSet.name.resolved(
            appLocale: locale.identifier,
            systemLanguages: [locale.identifier]
        )
    }

    private var allDoneStampImageName: String {
        guard locale.language.languageCode?.identifier == "ko" else {
            return "routine-all-done-great-job-stmap"
        }

        return colorScheme == .dark
            ? "routine-all-done-stamp-dark"
            : "routine-all-done-stamp"
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
