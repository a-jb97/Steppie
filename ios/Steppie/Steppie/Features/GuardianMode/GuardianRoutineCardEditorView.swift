import PhotosUI
import SwiftUI
import UIKit

struct GuardianRoutineCardEditorView: View {
    @Environment(\.locale) private var locale
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    @State private var selectedPhotoItem: PhotosPickerItem?

    let viewModel: GuardianModeViewModel
    let onInteraction: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: SteppieSpacing.medium) {
                header(title: "카드 편집", subtitle: "이름, 아이콘, 색상, 시간을 설정합니다")
                if viewModel.draft == nil, let routine = viewModel.selectedRoutine {
                    SteppieButton("선택한 카드 편집") {
                        viewModel.beginEditRoutine(routine)
                        onInteraction()
                    }
                    SteppieButton("선택한 카드 삭제", role: .danger) {
                        viewModel.requestDelete(routine)
                        onInteraction()
                    }
                } else {
                    draftForm
                }
            }
            .frame(maxWidth: SteppieLayout.focusCardTabletMaximumWidth)
            .frame(maxWidth: .infinity, alignment: .center)
            .padding(SteppieLayout.guardianScreenPadding)
        }
        .background(Color.steppieBackgroundSecondary)
    }

    @ViewBuilder
    private var draftForm: some View {
        if let draft = viewModel.draft {
            VStack(alignment: .leading, spacing: SteppieSpacing.medium) {
                labeledCard("활동 이름") {
                    TextField("활동 이름", text: draftTitleBinding)
                        .steppieTextStyle(.guardianBody)
                        .textFieldStyle(.plain)
                }
                labeledCard("아이콘 또는 사진") {
                    draftVisualPicker(for: draft)
                }
                labeledCard("카드 색상") {
                    HStack(spacing: 10) {
                        ForEach(SteppieCardColor.allCases, id: \.self) { color in
                            Button {
                                viewModel.draft?.colorToken = color.token
                                onInteraction()
                            } label: {
                                Circle()
                                    .fill(color.color)
                                    .frame(width: 44, height: 44)
                                    .overlay {
                                        if color.token == draft.colorToken {
                                            Circle().stroke(Color.steppieFocusRing, lineWidth: 3)
                                        }
                                    }
                            }
                            .buttonStyle(.plain)
                            .accessibilityLabel(Text(color.accessibilityName))
                            .accessibilityValue(
                                Text(
                                    color.token == draft.colorToken
                                        ? "a11y.selection.selected"
                                        : "a11y.selection.notSelected"
                                )
                            )
                            .accessibilityHint(Text("a11y.guardian.cardColor.selectHint"))
                        }
                    }
                }
                labeledCard("예정 시각") {
                    scheduledTimeEditor(
                        scheduledTime: draft.scheduledTime,
                        date: scheduledDateBinding,
                        setDefaultTime: {
                            viewModel.draft?.scheduledTime = defaultScheduledTime
                        },
                        clearTime: {
                            viewModel.draft?.scheduledTime = nil
                        }
                    )
                }
                if viewModel.hasUnsavedDraft {
                    warningNote("저장하지 않고 나가면 확인이 필요합니다.")
                }
                if !draft.isNew, let routine = viewModel.selectedRoutine {
                    SteppieButton("삭제", role: .danger) {
                        viewModel.requestDelete(routine)
                        onInteraction()
                    }
                    .padding(.top, SteppieSpacing.extraSmall)
                }
                HStack(spacing: SteppieSpacing.medium) {
                    SteppieButton("취소", role: .secondary) {
                        viewModel.cancelDraft()
                        onInteraction()
                    }
                    SteppieButton(
                        "저장",
                        state: draft.isValid ? .enabled : .disabled
                    ) {
                        viewModel.saveDraft(localeIdentifier: localeIdentifier)
                        onInteraction()
                    }
                }
            }
        }
    }

    private func draftVisualPicker(for draft: RoutineDraft) -> some View {
        VStack(alignment: .leading, spacing: SteppieSpacing.small) {
            adaptiveCardStack(spacing: SteppieSpacing.medium) {
                RoutineVisualView(icon: draft.icon, size: .list)
                    .frame(width: 64, height: 64)
                Picker("기본 아이콘", selection: draftIconBinding) {
                    ForEach(RoutineIconName.allCases) { icon in
                        Text(icon.displayName(for: locale)).tag(icon)
                    }
                }
                .pickerStyle(.menu)
                .frame(minHeight: SteppieLayout.guardianMinimumTouchTarget)
            }

            adaptiveControlStack {
                PhotosPicker(
                    selection: $selectedPhotoItem,
                    matching: .images,
                    photoLibrary: .shared()
                ) {
                    secondaryPickerLabel("사진 선택")
                }
                .buttonStyle(.plain)
                .accessibilityHint(Text("사진 앱에서 활동 사진을 선택합니다"))

                RoutineCameraCaptureButton(
                    onImagePicked: updateDraftPhoto(image:),
                    onInteraction: onInteraction
                )

                if draft.icon.type == .photo {
                    Button(role: .destructive) {
                        viewModel.resetDraftIconToDefault()
                        selectedPhotoItem = nil
                        onInteraction()
                    } label: {
                        secondaryPickerLabel("사진 삭제", foregroundColor: Color.steppieDanger)
                    }
                    .buttonStyle(.plain)
                    .accessibilityHint(Text("기본 아이콘으로 되돌립니다"))
                }
            }
        }
        .onChange(of: selectedPhotoItem) { _, item in
            guard let item else { return }
            loadSelectedPhoto(item)
        }
    }

    private func scheduledTimeEditor(
        scheduledTime: LocalTime?,
        date: Binding<Date>,
        setDefaultTime: @escaping () -> Void,
        clearTime: @escaping () -> Void
    ) -> some View {
        let hasTime = scheduledTime != nil
        return VStack(alignment: .leading, spacing: SteppieSpacing.small) {
            adaptiveSegmentRow {
                settingSegment(
                    title: "시간 설정",
                    isSelected: hasTime,
                    accessibilityValue: hasTime ? "선택됨" : "선택 안 됨"
                ) {
                    if !hasTime {
                        setDefaultTime()
                    }
                }
                settingSegment(
                    title: "시간 없음",
                    isSelected: !hasTime,
                    accessibilityValue: hasTime ? "선택 안 됨" : "선택됨"
                ) {
                    clearTime()
                }
            }

            Text(hasTime ? "현재 선택: \(scheduledTime?.description ?? "")" : "현재 선택: 시간 없음")
                .steppieTextStyle(.guardianCaption)
                .foregroundStyle(Color.steppieTextSecondary)
                .fixedSize(horizontal: false, vertical: true)

            if hasTime {
                DatePicker(
                    "예정 시각",
                    selection: date,
                    displayedComponents: .hourAndMinute
                )
                .frame(minHeight: SteppieLayout.guardianMinimumTouchTarget)
                .accessibilityHint(Text("a11y.guardian.scheduledTime.selectHint"))
            }
        }
    }

    private func settingSegment(
        title: String,
        isSelected: Bool,
        accessibilityValue: String,
        action: @escaping () -> Void
    ) -> some View {
        Button {
            action()
            onInteraction()
        } label: {
            Text(title)
                .steppieTextStyle(.guardianCaption)
                .foregroundStyle(isSelected ? Color.steppieBackgroundPrimary : Color.steppieTextPrimary)
                .multilineTextAlignment(.center)
                .lineLimit(dynamicTypeSize.isAccessibilitySize ? nil : 1)
                .fixedSize(horizontal: false, vertical: true)
                .frame(maxWidth: .infinity)
                .frame(minHeight: 32)
                .padding(.horizontal, SteppieSpacing.extraSmall)
                .background(isSelected ? Color.steppieFocusRing : Color.steppieBackgroundSecondary)
                .clipShape(.rect(cornerRadius: 10))
        }
        .buttonStyle(.plain)
        .frame(minHeight: SteppieLayout.guardianMinimumTouchTarget)
        .accessibilityLabel(Text(title))
        .accessibilityValue(Text(accessibilityValue))
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
        .padding(.bottom, SteppieSpacing.small)
    }

    private func labeledCard<Content: View>(
        _ title: String,
        @ViewBuilder content: () -> Content
    ) -> some View {
        VStack(alignment: .leading, spacing: SteppieSpacing.extraSmall) {
            Text(title)
                .steppieTextStyle(.button)
                .foregroundStyle(Color.steppieTextSecondary)
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
    }

    private func secondaryPickerLabel(
        _ title: LocalizedStringKey,
        foregroundColor: Color = Color.steppieTextPrimary
    ) -> some View {
        Text(title)
            .steppieTextStyle(.button)
            .multilineTextAlignment(.center)
            .fixedSize(horizontal: false, vertical: true)
            .frame(maxWidth: .infinity)
            .frame(minHeight: 54)
            .padding(.horizontal, SteppieSpacing.large)
            .foregroundStyle(foregroundColor)
            .background(Color.steppieBackgroundPrimary)
            .overlay {
                RoundedRectangle(cornerRadius: SteppieCornerRadius.control)
                    .stroke(Color.steppieBorderSubtle, lineWidth: SteppieStroke.divider)
            }
            .clipShape(.rect(cornerRadius: SteppieCornerRadius.control))
            .contentShape(.rect)
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

    @ViewBuilder
    private func adaptiveControlStack<Content: View>(
        @ViewBuilder content: () -> Content
    ) -> some View {
        if dynamicTypeSize.isAccessibilitySize {
            VStack(alignment: .leading, spacing: SteppieSpacing.small, content: content)
        } else {
            HStack(content: content)
        }
    }

    @ViewBuilder
    private func adaptiveSegmentRow<Content: View>(
        @ViewBuilder content: () -> Content
    ) -> some View {
        if dynamicTypeSize.isAccessibilitySize {
            VStack(spacing: SteppieSpacing.extraSmall, content: content)
        } else {
            HStack(spacing: 6, content: content)
        }
    }

    private func loadSelectedPhoto(_ item: PhotosPickerItem) {
        Task {
            guard let data = try? await item.loadTransferable(type: Data.self) else {
                selectedPhotoItem = nil
                return
            }
            viewModel.updateDraftPhoto(data: data)
            selectedPhotoItem = nil
            onInteraction()
        }
    }

    private func updateDraftPhoto(image: UIImage) {
        guard let data = image.jpegData(compressionQuality: 0.9) else { return }
        viewModel.updateDraftPhoto(data: data)
        selectedPhotoItem = nil
        onInteraction()
    }

    private var draftTitleBinding: Binding<String> {
        Binding(
            get: { viewModel.draft?.title ?? "" },
            set: {
                viewModel.draft?.title = $0
                onInteraction()
            }
        )
    }

    private var draftIconBinding: Binding<RoutineIconName> {
        Binding(
            get: { viewModel.draft?.iconName ?? .star },
            set: {
                viewModel.draft?.iconName = $0
                selectedPhotoItem = nil
                onInteraction()
            }
        )
    }

    private var defaultScheduledTime: LocalTime {
        try! LocalTime(hour: 7, minute: 30)
    }

    private var scheduledDateBinding: Binding<Date> {
        Binding(
            get: {
                let time = viewModel.draft?.scheduledTime
                return Calendar.current.date(
                    from: DateComponents(hour: time?.hour ?? 7, minute: time?.minute ?? 30)
                ) ?? .now
            },
            set: {
                let components = Calendar.current.dateComponents([.hour, .minute], from: $0)
                if let hour = components.hour, let minute = components.minute {
                    viewModel.draft?.scheduledTime = try? LocalTime(hour: hour, minute: minute)
                }
                onInteraction()
            }
        )
    }

    private var localeIdentifier: String {
        locale.language.languageCode?.identifier ?? "ko"
    }
}
