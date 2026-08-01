import SwiftUI

struct ChildRoutineWaitingView: View {
    @Environment(\.locale) private var locale
    let viewModel: ChildRoutineViewModel

    var body: some View {
        ChildRoutineStateScreenContainer {
            VStack(spacing: SteppieSpacing.large) {
                Image(systemName: "clock.fill")
                    .font(.system(size: 96, weight: .semibold))
                    .foregroundStyle(Color.steppieFocusRing)
                    .accessibilityHidden(true)

                Text("다음 루틴을 기다려요")
                    .steppieTextStyle(.childScreenTitle)
                    .foregroundStyle(Color.steppieTextSecondary)
                    .multilineTextAlignment(.center)

                if let routineSet = viewModel.nextScheduledRoutineSet {
                    Text(verbatim: localizedTitle(for: routineSet))
                        .steppieTextStyle(.childCardTitle)
                        .foregroundStyle(Color.steppieTextPrimary)
                        .multilineTextAlignment(.center)
                        .fixedSize(horizontal: false, vertical: true)

                    if let startTime = routineSet.dailyStartTime {
                        Text(verbatim: "\(startTime.description)에 시작해요")
                            .steppieTextStyle(.childProgress)
                            .foregroundStyle(Color.steppieFocusRing)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                }
            }
            .padding(SteppieSpacing.extraLarge)
            .frame(maxWidth: SteppieLayout.focusCardPhoneMaximumWidth)
            .frame(maxWidth: .infinity)
            .frame(minHeight: 520)
            .background(Color.steppieCardSky)
            .clipShape(.rect(cornerRadius: SteppieCornerRadius.sheet))
            .accessibilityElement(children: .combine)
        }
    }

    private func localizedTitle(for routineSet: RoutineSet) -> String {
        routineSet.name.resolved(
            appLocale: locale.identifier,
            systemLanguages: [locale.identifier]
        )
    }
}
