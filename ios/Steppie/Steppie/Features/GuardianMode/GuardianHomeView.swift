import SwiftUI

struct GuardianHomeView: View {
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    let hasRoutineSets: Bool
    let onDestinationSelected: (GuardianDestination) -> Void
    let onDone: () -> Void
    let onInteraction: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: SteppieSpacing.medium) {
                header
                menuCard(
                    title: "루틴 세트 생성",
                    subtitle: "새 루틴 제목과 단계 목록 만들기",
                    assetName: "guardian-menu-routine-set",
                    destination: .routineSetCreator,
                    isEnabled: true
                )
                .tutorialTarget(.primary)
                menuCard(
                    title: "템플릿에서 시작하기",
                    subtitle: "아침, 학교, 취침 루틴으로 빠르게 만들기",
                    assetName: "guardian-menu-template",
                    destination: .routineTemplates,
                    isEnabled: true
                )
                .tutorialTarget(.secondary)
                menuCard(
                    title: "루틴 관리",
                    subtitle: hasRoutineSets ? "루틴 세트 목록, 활동 추가, 순서 변경" : "먼저 루틴 세트를 만들어 주세요",
                    assetName: "guardian-menu-routine",
                    destination: .routineEditor,
                    isEnabled: hasRoutineSets
                )
                .tutorialTarget(.tertiary)
                menuCard(
                    title: "환경 설정",
                    subtitle: "음성, 효과음, 햅틱, 알림",
                    assetName: "guardian-menu-settings",
                    destination: .feedbackSettings,
                    isEnabled: true
                )
                menuCard(
                    title: "진행 기록",
                    subtitle: "날짜별 완료 현황",
                    assetName: "guardian-menu-records",
                    destination: .records,
                    isEnabled: true
                )
                menuCard(
                    title: "보안",
                    subtitle: "PIN, 복구 코드, 백업/복원",
                    assetName: "guardian-menu-security",
                    destination: .security,
                    isEnabled: true
                )
                SteppieButton("완료", action: onDone)
                    .padding(.top, SteppieSpacing.medium)
                    .tutorialTarget(.done)
            }
            .padding(SteppieLayout.guardianScreenPadding)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.steppieBackgroundSecondary)
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: SteppieSpacing.twoExtraSmall) {
            Text("보호자 모드")
                .steppieTextStyle(.guardianTitle)
                .foregroundStyle(Color.steppieTextSecondary)
                .fixedSize(horizontal: false, vertical: true)
                .accessibilityAddTraits(.isHeader)
            Text("완료 또는 3분 미조작 시 아이 모드로 돌아갑니다")
                .steppieTextStyle(.guardianCaption)
                .foregroundStyle(Color.steppieTextPrimary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.bottom, SteppieSpacing.small)
    }

    private func menuCard(
        title: LocalizedStringKey,
        subtitle: LocalizedStringKey,
        assetName: String,
        destination: GuardianDestination,
        isEnabled: Bool
    ) -> some View {
        Button {
            guard isEnabled else { return }
            onDestinationSelected(destination)
            onInteraction()
        } label: {
            adaptiveCardStack(spacing: 14) {
                Image(assetName)
                    .resizable()
                    .scaledToFit()
                    .frame(width: 52, height: 52)
                    .accessibilityHidden(true)
                VStack(alignment: .leading, spacing: SteppieSpacing.twoExtraSmall) {
                    Text(title)
                        .steppieTextStyle(.guardianSection)
                        .foregroundStyle(Color.steppieTextSecondary)
                        .fixedSize(horizontal: false, vertical: true)
                    Text(subtitle)
                        .steppieTextStyle(.guardianCaption)
                        .foregroundStyle(Color.steppieTextPrimary)
                        .fixedSize(horizontal: false, vertical: true)
                }
                if !dynamicTypeSize.isAccessibilitySize {
                    Spacer()
                }
            }
            .padding(SteppieSpacing.medium)
            .frame(maxWidth: .infinity, minHeight: 112, alignment: .leading)
            .background(Color.steppieBackgroundPrimary)
            .overlay {
                RoundedRectangle(cornerRadius: SteppieCornerRadius.card)
                    .stroke(Color.steppieBorderSubtle, lineWidth: SteppieStroke.divider)
            }
            .clipShape(.rect(cornerRadius: SteppieCornerRadius.card))
            .opacity(isEnabled ? 1 : 0.62)
        }
        .buttonStyle(.plain)
        .disabled(!isEnabled)
        .accessibilityElement(children: .combine)
        .accessibilityLabel(Text(title))
        .accessibilityValue(Text(subtitle))
        .accessibilityHint(isEnabled ? Text("열기") : Text("먼저 루틴 세트를 만들어 주세요"))
    }

    @ViewBuilder
    private func adaptiveCardStack<Content: View>(
        spacing: CGFloat,
        @ViewBuilder content: () -> Content
    ) -> some View {
        if dynamicTypeSize.isAccessibilitySize {
            VStack(alignment: .leading, spacing: spacing, content: content)
        } else {
            HStack(spacing: spacing, content: content)
        }
    }
}
