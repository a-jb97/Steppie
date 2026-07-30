import SwiftUI

struct GuardianSecurityView: View {
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    let tutorialCoordinator: TutorialCoordinator
    let onSecurityAction: (GuardianSecurityAction) -> Void
    let onBackupRestoreRequested: () -> Void
    let onDone: () -> Void
    let onInteraction: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: SteppieSpacing.medium) {
                header
                securityCard(
                    title: "PIN 변경",
                    subtitle: "4자리 보호자 PIN 재설정",
                    assetName: "guardian-security-warning",
                    iconSize: 55
                ) {
                    onSecurityAction(.changePIN)
                    onInteraction()
                }
                securityCard(
                    title: "복구 코드 확인",
                    subtitle: "PIN 확인 후 새 6자리 복구 코드를 한 번만 표시",
                    assetName: "guardian-security-warning",
                    iconSize: 55
                ) {
                    onSecurityAction(.regenerateRecoveryCode)
                    onInteraction()
                }
                securityCard(
                    title: "백업/복원",
                    subtitle: "로컬 파일 백업과 Replace 복원",
                    assetName: "guardian-security-backup",
                    iconSize: 50
                ) {
                    onBackupRestoreRequested()
                    onInteraction()
                }
                SteppieButton("tutorial.replay", role: .secondary) {
                    tutorialCoordinator.resetAndPresent(.security)
                    onInteraction()
                }
                .tutorialTarget(.secondary)
                privacyPolicyNote
                SteppieButton("완료", action: onDone)
                    .padding(.top, SteppieSpacing.large)
            }
            .frame(maxWidth: SteppieLayout.focusCardTabletMaximumWidth)
            .frame(maxWidth: .infinity)
            .padding(SteppieLayout.guardianScreenPadding)
        }
        .background(Color.steppieBackgroundSecondary)
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: SteppieSpacing.twoExtraSmall) {
            Text("보안")
                .steppieTextStyle(.guardianTitle)
                .foregroundStyle(Color.steppieTextSecondary)
                .fixedSize(horizontal: false, vertical: true)
                .accessibilityAddTraits(.isHeader)
            Text("PIN, 복구 코드, 개인정보 설정")
                .steppieTextStyle(.guardianCaption)
                .foregroundStyle(Color.steppieTextPrimary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.bottom, SteppieSpacing.small)
    }

    private func securityCard(
        title: String,
        subtitle: String,
        assetName: String,
        iconSize: CGFloat,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            adaptiveCardStack(spacing: SteppieSpacing.small) {
                Image(assetName)
                    .resizable()
                    .scaledToFit()
                    .frame(width: iconSize, height: iconSize)
                    .frame(width: 55, height: 55)
                    .accessibilityHidden(true)
                VStack(alignment: .leading, spacing: SteppieSpacing.twoExtraSmall) {
                    Text(title)
                        .steppieTextStyle(.button)
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
            .padding(14)
            .frame(maxWidth: .infinity, minHeight: 96)
            .background(Color.steppieBackgroundPrimary)
            .overlay {
                RoundedRectangle(cornerRadius: SteppieCornerRadius.card)
                    .stroke(Color.steppieBorderSubtle, lineWidth: SteppieStroke.divider)
            }
            .clipShape(.rect(cornerRadius: SteppieCornerRadius.card))
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .combine)
        .accessibilityLabel(Text(title))
        .accessibilityValue(Text(subtitle))
        .accessibilityHint(Text("열기"))
    }

    private var privacyPolicyNote: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("v1.0 개인정보 원칙")
                .steppieTextStyle(.button)
                .foregroundStyle(Color.steppieTextSecondary)
                .frame(maxWidth: .infinity, alignment: .leading)
            Text("광고, 인앱결제, 서버 계정 시스템은 없습니다. 핵심 루틴 기능은 오프라인에서 100% 동작합니다.")
                .steppieTextStyle(.guardianCaption)
                .foregroundStyle(Color.steppieTextPrimary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(16)
        .frame(maxWidth: .infinity, minHeight: 148, alignment: .topLeading)
        .background(Color.steppieBackgroundPrimary)
        .overlay {
            RoundedRectangle(cornerRadius: SteppieCornerRadius.card)
                .stroke(Color.steppieBorderSubtle, lineWidth: SteppieStroke.divider)
        }
        .clipShape(.rect(cornerRadius: SteppieCornerRadius.card))
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
