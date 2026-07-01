import SwiftUI

struct BackupRestoreView: View {
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    let viewModel: GuardianModeViewModel
    let onDone: () -> Void
    let onInteraction: () -> Void

    @State private var isFileExporterPresented = false
    @State private var isFileImporterPresented = false
    @State private var exportDocument = BackupFileDocument()
    @State private var exportFileName = "steppie-backup.zip"

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: SteppieSpacing.medium) {
                header(title: "백업/복원", subtitle: "로컬 파일로 데이터를 내보내고 Replace 복원을 실행합니다")
                backupSection
                restoreSection
                if let message = viewModel.backupStatusMessage {
                    statusNote(message)
                }
                if let errorMessage = viewModel.errorMessage {
                    warningNote(errorMessage)
                }
                SteppieButton("완료", role: .secondary, action: onDone)
                    .padding(.top, SteppieSpacing.large)
            }
            .frame(maxWidth: SteppieLayout.focusCardTabletMaximumWidth)
            .frame(maxWidth: .infinity)
            .padding(SteppieLayout.guardianScreenPadding)
        }
        .background(Color.steppieBackgroundSecondary)
        .fileExporter(
            isPresented: $isFileExporterPresented,
            document: exportDocument,
            contentType: BackupFileDocument.backupContentType,
            defaultFilename: exportFileName
        ) { result in
            if case .failure = result {
                viewModel.reportBackupFileError("백업 파일을 저장하지 못했어요.")
            }
            onInteraction()
        }
        .fileImporter(
            isPresented: $isFileImporterPresented,
            allowedContentTypes: [BackupFileDocument.backupContentType],
            allowsMultipleSelection: false
        ) { result in
            handleImport(result)
        }
        .onChange(of: viewModel.backupStatusMessage) { _, message in
            postAccessibilityAnnouncement(message)
        }
        .onChange(of: viewModel.errorMessage) { _, message in
            postAccessibilityAnnouncement(message)
        }
    }

    private var backupSection: some View {
        labeledCard("백업 파일 만들기") {
            Text("루틴 세트, 활동, 진행 기록, 설정, PIN/복구 코드 해시를 Steppie 백업 파일로 저장합니다. 원본 PIN과 원본 복구 코드는 포함하지 않습니다.")
                .steppieTextStyle(.guardianCaption)
                .foregroundStyle(Color.steppieTextPrimary)
                .fixedSize(horizontal: false, vertical: true)
            SteppieButton("백업 내보내기") {
                viewModel.createBackupPackage()
                if let package = viewModel.backupPackage {
                    exportDocument = BackupFileDocument(data: package.archiveData)
                    exportFileName = package.fileName
                    isFileExporterPresented = true
                }
                onInteraction()
            }
        }
    }

    private var restoreSection: some View {
        labeledCard("백업 파일 복원") {
            Text("복원은 현재 앱 데이터를 백업 파일 내용으로 교체합니다. 실패하면 기존 데이터는 유지됩니다.")
                .steppieTextStyle(.guardianCaption)
                .foregroundStyle(Color.steppieTextPrimary)
                .fixedSize(horizontal: false, vertical: true)
            SteppieButton("백업 파일 선택", role: .secondary) {
                isFileImporterPresented = true
                onInteraction()
            }

            if viewModel.validatedRestoreSnapshot != nil {
                Divider()
                Text("Replace 복원 확인")
                    .steppieTextStyle(.button)
                    .foregroundStyle(Color.steppieTextSecondary)
                SecureField("보호자 PIN", text: restorePINBinding)
                    .keyboardType(.numberPad)
                    .textContentType(.oneTimeCode)
                    .steppieTextStyle(.guardianBody)
                    .padding(12)
                    .background(Color.steppieBackgroundSecondary)
                    .clipShape(.rect(cornerRadius: SteppieCornerRadius.control))
                    .accessibilityLabel(Text("보호자 PIN"))
                adaptiveActionStack {
                    SteppieButton("취소", role: .secondary) {
                        viewModel.cancelRestore()
                        onInteraction()
                    }
                    SteppieButton(
                        "복원 실행",
                        role: .danger,
                        state: viewModel.canConfirmRestore ? .enabled : .disabled
                    ) {
                        viewModel.confirmRestore()
                        onInteraction()
                    }
                    .accessibilityHint(Text("a11y.backup.restore.replaceHint"))
                }
            }
        }
    }

    private var restorePINBinding: Binding<String> {
        Binding(
            get: { viewModel.restorePIN },
            set: {
                viewModel.restorePIN = String($0.filter(\.isNumber).prefix(4))
                onInteraction()
            }
        )
    }

    private func handleImport(_ result: Result<[URL], Error>) {
        defer { onInteraction() }
        guard case let .success(urls) = result, let url = urls.first else {
            viewModel.reportBackupFileError("백업 파일을 선택하지 못했어요.")
            return
        }
        let didStartAccessing = url.startAccessingSecurityScopedResource()
        defer {
            if didStartAccessing {
                url.stopAccessingSecurityScopedResource()
            }
        }
        do {
            viewModel.validateRestorePackage(try Data(contentsOf: url))
        } catch {
            viewModel.reportBackupFileError("백업 파일을 읽지 못했어요.")
        }
    }

    private func header(title: String, subtitle: String) -> some View {
        VStack(alignment: .leading, spacing: SteppieSpacing.twoExtraSmall) {
            Text(title)
                .steppieTextStyle(.guardianTitle)
                .foregroundStyle(Color.steppieTextSecondary)
                .fixedSize(horizontal: false, vertical: true)
                .accessibilityAddTraits(.isHeader)
            Text(subtitle)
                .steppieTextStyle(.guardianCaption)
                .foregroundStyle(Color.steppieTextPrimary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func labeledCard<Content: View>(
        _ title: String,
        @ViewBuilder content: () -> Content
    ) -> some View {
        VStack(alignment: .leading, spacing: SteppieSpacing.medium) {
            Text(title)
                .steppieTextStyle(.button)
                .foregroundStyle(Color.steppieTextSecondary)
                .fixedSize(horizontal: false, vertical: true)
            content()
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.steppieBackgroundPrimary)
        .overlay {
            RoundedRectangle(cornerRadius: SteppieCornerRadius.card)
                .stroke(Color.steppieBorderSubtle, lineWidth: SteppieStroke.divider)
        }
        .clipShape(.rect(cornerRadius: SteppieCornerRadius.card))
    }

    private func statusNote(_ text: String) -> some View {
        Text(text)
            .steppieTextStyle(.guardianCaption)
            .foregroundStyle(Color.steppieTextPrimary)
            .padding(SteppieSpacing.small)
            .frame(maxWidth: .infinity, minHeight: 62, alignment: .leading)
            .background(Color.steppieCardMint)
            .clipShape(.rect(cornerRadius: SteppieCornerRadius.control))
            .accessibilityElement(children: .combine)
            .accessibilityLabel(Text(text))
    }

    private func warningNote(_ text: String) -> some View {
        HStack(spacing: SteppieSpacing.small) {
            Image(systemName: "exclamationmark.triangle.fill")
                .foregroundStyle(Color.steppieWarning)
                .font(.title2)
                .accessibilityHidden(true)
            Text(text)
                .steppieTextStyle(.guardianCaption)
                .foregroundStyle(Color.steppieTextPrimary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(SteppieSpacing.small)
        .frame(maxWidth: .infinity, minHeight: 62, alignment: .leading)
        .background(Color.steppieCardLemon)
        .overlay {
            RoundedRectangle(cornerRadius: SteppieCornerRadius.control)
                .stroke(Color.steppieWarning, lineWidth: SteppieStroke.divider)
        }
        .clipShape(.rect(cornerRadius: SteppieCornerRadius.control))
        .accessibilityElement(children: .combine)
        .accessibilityLabel(Text(text))
    }

    private func postAccessibilityAnnouncement(_ message: String?) {
        guard let message, !message.isEmpty else { return }
        UIAccessibility.post(notification: .announcement, argument: message)
    }

    @ViewBuilder
    private func adaptiveActionStack<Content: View>(
        @ViewBuilder content: () -> Content
    ) -> some View {
        if dynamicTypeSize.isAccessibilitySize {
            VStack(spacing: SteppieSpacing.medium, content: content)
        } else {
            HStack(spacing: SteppieSpacing.medium, content: content)
        }
    }
}
