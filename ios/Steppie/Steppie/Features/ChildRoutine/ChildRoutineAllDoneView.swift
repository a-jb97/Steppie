import SwiftUI

struct ChildRoutineAllDoneView: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    let viewModel: ChildRoutineViewModel

    var body: some View {
        ChildRoutineStateScreenContainer {
            VStack(spacing: SteppieSpacing.large) {
                PraiseFeedbackMark(
                    imageName: "routine-all-done-great-job-stmap",
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
        }
    }

    private var effectiveFeedbackIntensity: FeedbackIntensity {
        ChildRoutineFeedbackPresentationPolicy.effectiveIntensity(
            settings: viewModel.settings,
            reduceMotion: reduceMotion
        )
    }
}
