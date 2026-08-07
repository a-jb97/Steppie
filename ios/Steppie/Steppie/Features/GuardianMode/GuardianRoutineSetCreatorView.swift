import PhotosUI
import SwiftUI
import UIKit

struct GuardianRoutineSetCreatorView: View {
    @Environment(\.locale) private var locale
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    @State private var selectedStepPhotoItem: PhotosPickerItem?
    @State private var isWideLayout = false

    let isWide: Bool
    let viewModel: GuardianModeViewModel
    let onDone: () -> Void
    let onInteraction: () -> Void

    var body: some View {
        HStack(spacing: 0) {
            stepListPane
            if isWide {
                Rectangle()
                    .fill(Color.steppieBorderSubtle)
                    .frame(width: SteppieStroke.divider)
                stepEditorPane
                    .frame(maxWidth: .infinity)
            }
        }
        .sheet(item: stepDraftBinding) { _ in
            NavigationStack {
                stepEditorPane
            }
            .presentationDetents([.large])
        }
        .onAppear {
            isWideLayout = isWide
            if viewModel.routineSetDraft == nil {
                viewModel.beginCreateRoutineSet()
            }
        }
        .onChange(of: isWide) { _, newValue in
            isWideLayout = newValue
        }
    }

    private var stepListPane: some View {
        List {
            Section {
                labeledCard("루틴 세트 제목") {
                    TextField("예: 아침 루틴", text: routineSetNameBinding)
                        .steppieTextStyle(.guardianBody)
                        .textFieldStyle(.plain)
                }
                .listRowInsets(routineListRowInsets)
                .listRowSeparator(.hidden)
                .listRowBackground(Color.clear)
            } header: {
                header(
                    title: "루틴 세트 생성",
                    subtitle: "최소 1개 단계가 있어야 저장할 수 있습니다"
                )
                .padding(.top, SteppieSpacing.medium)
                .padding(.horizontal, SteppieLayout.guardianScreenPadding)
                .textCase(nil)
            }

            Section {
                if viewModel.routineSetDraft?.steps.isEmpty != false {
                    messageState(title: "단계가 없어요", message: "단계 추가로 첫 활동을 만들어 주세요.")
                        .listRowInsets(routineListRowInsets)
                        .listRowSeparator(.hidden)
                        .listRowBackground(Color.clear)
                } else {
                    ForEach(viewModel.routineSetDraft?.steps ?? []) { step in
                        stepRow(step)
                            .listRowInsets(routineListRowInsets)
                            .listRowSeparator(.hidden)
                            .listRowBackground(Color.clear)
                            .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                                Button(role: .destructive) {
                                    viewModel.deleteRoutineSetStep(step)
                                    onInteraction()
                                } label: {
                                    Label("삭제", systemImage: "trash")
                                }
                            }
                    }
                }
            } header: {
                Text("단계 목록")
                    .steppieTextStyle(.guardianSection)
                    .foregroundStyle(Color.steppieTextSecondary)
                    .padding(.horizontal, SteppieLayout.guardianScreenPadding)
                    .textCase(nil)
            }

            Section {
                SteppieButton("+ 단계 추가") {
                    viewModel.beginAddRoutineSetStep()
                    onInteraction()
                }
                .listRowInsets(routineListRowInsets)
                .listRowSeparator(.hidden)
                .listRowBackground(Color.clear)

                SteppieButton(
                    "루틴 세트 저장",
                    state: viewModel.canSaveRoutineSetDraft ? .enabled : .disabled
                ) {
                    viewModel.saveRoutineSetDraft(localeIdentifier: localeIdentifier)
                    onInteraction()
                }
                .listRowInsets(routineListRowInsets)
                .listRowSeparator(.hidden)
                .listRowBackground(Color.clear)

                SteppieButton("취소", role: .secondary) {
                    viewModel.cancelRoutineSetDraft()
                    viewModel.selectedDestination = nil
                    onInteraction()
                }
                .listRowInsets(routineListRowInsets)
                .listRowSeparator(.hidden)
                .listRowBackground(Color.clear)
            }
        }
        .listStyle(.plain)
        .scrollContentBackground(.hidden)
        .background(Color.steppieBackgroundSecondary)
        .frame(maxWidth: isWide ? SteppieLayout.splitListWidth : .infinity)
        .navigationTitle("")
        .toolbar {
            if !isWide {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("완료", action: onDone)
                }
            }
        }
        .onDisappear {
            if !isWide {
                Task { @MainActor in
                    if viewModel.selectedDestination != .routineSetCreator {
                        viewModel.cancelRoutineSetDraft()
                    }
                }
            }
        }
    }

    private func stepRow(_ step: RoutineSetStepDraft) -> some View {
        adaptiveCardStack(spacing: SteppieSpacing.small) {
            RoundedRectangle(cornerRadius: 10)
                .fill(SteppieCardColor(colorToken: step.colorToken).color)
                .frame(
                    width: dynamicTypeSize.isAccessibilitySize ? 68 : 20,
                    height: dynamicTypeSize.isAccessibilitySize ? 20 : 68
                )
                .accessibilityHidden(true)
            RoutineVisualView(icon: step.icon, size: .list)
                .frame(width: 52, height: 52)
            VStack(alignment: .leading, spacing: SteppieSpacing.twoExtraSmall) {
                Text(step.title)
                    .steppieTextStyle(.button)
                    .foregroundStyle(Color.steppieTextPrimary)
                    .fixedSize(horizontal: false, vertical: true)
                Text(step.scheduledTime?.description ?? String(localized: "시간 없음"))
                    .steppieTextStyle(.guardianCaption)
                    .foregroundStyle(Color.steppieTextSecondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .contentShape(.rect)
            .onTapGesture {
                viewModel.beginEditRoutineSetStep(step)
                onInteraction()
            }
            if !dynamicTypeSize.isAccessibilitySize {
                Spacer()
            }
            VStack(spacing: SteppieSpacing.twoExtraSmall) {
                Button {
                    viewModel.moveRoutineSetStep(step, direction: -1)
                    onInteraction()
                } label: {
                    Image(systemName: "chevron.up")
                        .frame(
                            width: SteppieLayout.guardianMinimumTouchTarget,
                            height: SteppieLayout.guardianMinimumTouchTarget
                        )
                }
                .buttonStyle(.plain)
                .accessibilityLabel(Text("\(step.title) 위로 이동"))

                Button {
                    viewModel.moveRoutineSetStep(step, direction: 1)
                    onInteraction()
                } label: {
                    Image(systemName: "chevron.down")
                        .frame(
                            width: SteppieLayout.guardianMinimumTouchTarget,
                            height: SteppieLayout.guardianMinimumTouchTarget
                        )
                }
                .buttonStyle(.plain)
                .accessibilityLabel(Text("\(step.title) 아래로 이동"))
            }
            .foregroundStyle(Color.steppieTextSecondary)
        }
        .padding(SteppieSpacing.small)
        .frame(maxWidth: .infinity, minHeight: 92, alignment: .leading)
        .background(Color.steppieBackgroundPrimary)
        .overlay {
            RoundedRectangle(cornerRadius: SteppieCornerRadius.card)
                .stroke(Color.steppieBorderSubtle, lineWidth: SteppieStroke.divider)
        }
        .clipShape(.rect(cornerRadius: SteppieCornerRadius.card))
        .accessibilityElement(children: .contain)
        .accessibilityLabel(Text(step.title))
        .accessibilityValue(Text(step.scheduledTime?.description ?? String(localized: "시간 없음")))
        .accessibilityHint(Text("a11y.guardian.step.editOrReorderHint"))
        .contextMenu {
            Button("수정") { viewModel.beginEditRoutineSetStep(step) }
            Button("삭제", role: .destructive) { viewModel.deleteRoutineSetStep(step) }
        }
    }

    private var stepEditorPane: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: SteppieSpacing.medium) {
                header(title: "단계 편집", subtitle: "이름, 아이콘, 색상, 시간을 설정합니다")
                if viewModel.routineSetStepDraft == nil, let step = viewModel.selectedRoutineSetStep {
                    SteppieButton("선택한 단계 편집") {
                        viewModel.beginEditRoutineSetStep(step)
                        onInteraction()
                    }
                    SteppieButton("선택한 단계 삭제", role: .danger) {
                        viewModel.deleteRoutineSetStep(step)
                        onInteraction()
                    }
                } else {
                    stepForm
                }
                if viewModel.hasUnsavedRoutineSetDraft {
                    warningNote("저장하지 않고 나가면 입력한 루틴 세트가 사라집니다.")
                }
            }
            .frame(maxWidth: SteppieLayout.focusCardTabletMaximumWidth)
            .frame(maxWidth: .infinity, alignment: .center)
            .padding(SteppieLayout.guardianScreenPadding)
        }
        .background(Color.steppieBackgroundSecondary)
    }

    @ViewBuilder
    private var stepForm: some View {
        if let stepDraft = viewModel.routineSetStepDraft {
            VStack(alignment: .leading, spacing: SteppieSpacing.medium) {
                labeledCard("활동 이름") {
                    TextField("활동 이름", text: stepTitleBinding)
                        .steppieTextStyle(.guardianBody)
                        .textFieldStyle(.plain)
                }
                labeledCard("아이콘 또는 사진") {
                    stepDraftVisualPicker(for: stepDraft)
                }
                labeledCard("카드 색상") {
                    HStack(spacing: 10) {
                        ForEach(SteppieCardColor.allCases, id: \.self) { color in
                            Button {
                                viewModel.routineSetStepDraft?.colorToken = color.token
                                onInteraction()
                            } label: {
                                Circle()
                                    .fill(color.color)
                                    .frame(width: 44, height: 44)
                                    .overlay {
                                        if color.token == stepDraft.colorToken {
                                            Circle().stroke(Color.steppieFocusRing, lineWidth: 3)
                                        }
                                    }
                            }
                            .buttonStyle(.plain)
                            .accessibilityLabel(Text(color.accessibilityName))
                            .accessibilityValue(
                                Text(
                                    color.token == stepDraft.colorToken
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
                        scheduledTime: stepDraft.scheduledTime,
                        date: stepScheduledDateBinding,
                        setDefaultTime: {
                            viewModel.routineSetStepDraft?.scheduledTime = defaultScheduledTime
                        },
                        clearTime: {
                            viewModel.routineSetStepDraft?.scheduledTime = nil
                        }
                    )
                }
                HStack(spacing: SteppieSpacing.medium) {
                    SteppieButton("취소", role: .secondary) {
                        viewModel.cancelRoutineSetStepDraft()
                        onInteraction()
                    }
                    SteppieButton(
                        "단계 저장",
                        state: stepDraft.isValid ? .enabled : .disabled
                    ) {
                        viewModel.saveRoutineSetStepDraft()
                        onInteraction()
                    }
                }
            }
        } else {
            messageState(title: "단계를 선택해 주세요", message: "단계 추가 또는 목록의 단계를 선택해 편집할 수 있습니다.")
        }
    }

    private func stepDraftVisualPicker(for draft: RoutineSetStepDraft) -> some View {
        VStack(alignment: .leading, spacing: SteppieSpacing.small) {
            adaptiveCardStack(spacing: SteppieSpacing.medium) {
                RoutineVisualView(icon: draft.icon, size: .list)
                    .frame(width: 64, height: 64)
                Picker("기본 아이콘", selection: stepIconBinding) {
                    ForEach(RoutineIconName.allCases) { icon in
                        Text(icon.displayName(for: locale)).tag(icon)
                    }
                }
                .pickerStyle(.menu)
                .frame(minHeight: SteppieLayout.guardianMinimumTouchTarget)
            }

            adaptiveControlStack {
                PhotosPicker(
                    selection: $selectedStepPhotoItem,
                    matching: .images,
                    photoLibrary: .shared()
                ) {
                    secondaryPickerLabel("사진 선택")
                }
                .buttonStyle(.plain)
                .accessibilityHint(Text("사진 앱에서 활동 사진을 선택합니다"))

                RoutineCameraCaptureButton(
                    onImagePicked: updateStepDraftPhoto(image:),
                    onInteraction: onInteraction
                )

                if draft.icon.type == .photo {
                    Button(role: .destructive) {
                        viewModel.resetRoutineSetStepDraftIconToDefault()
                        selectedStepPhotoItem = nil
                        onInteraction()
                    } label: {
                        secondaryPickerLabel("사진 삭제", foregroundColor: Color.steppieDanger)
                    }
                    .buttonStyle(.plain)
                    .accessibilityHint(Text("기본 아이콘으로 되돌립니다"))
                }
            }
        }
        .onChange(of: selectedStepPhotoItem) { _, item in
            guard let item else { return }
            loadSelectedStepPhoto(item)
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

            Group {
                if hasTime {
                    Text("현재 선택: \(scheduledTime?.description ?? "")")
                } else {
                    Text("현재 선택: 시간 없음")
                }
            }
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
        title: LocalizedStringKey,
        isSelected: Bool,
        accessibilityValue: LocalizedStringKey,
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

    private func header(title: LocalizedStringKey, subtitle: LocalizedStringKey) -> some View {
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
        _ title: LocalizedStringKey,
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

    private func warningNote(_ text: LocalizedStringKey) -> some View {
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

    private func messageState(title: LocalizedStringKey, message: LocalizedStringKey) -> some View {
        VStack(spacing: SteppieSpacing.medium) {
            Text(title)
                .steppieTextStyle(.guardianTitle)
                .foregroundStyle(Color.steppieTextSecondary)
            Text(message)
                .steppieTextStyle(.guardianBody)
                .foregroundStyle(Color.steppieTextPrimary)
        }
        .multilineTextAlignment(.center)
        .padding(SteppieLayout.guardianScreenPadding)
    }

    private func loadSelectedStepPhoto(_ item: PhotosPickerItem) {
        Task {
            guard let data = try? await item.loadTransferable(type: Data.self) else {
                selectedStepPhotoItem = nil
                return
            }
            viewModel.updateRoutineSetStepDraftPhoto(data: data)
            selectedStepPhotoItem = nil
            onInteraction()
        }
    }

    private func updateStepDraftPhoto(image: UIImage) {
        guard let data = image.jpegData(compressionQuality: 0.9) else { return }
        viewModel.updateRoutineSetStepDraftPhoto(data: data)
        selectedStepPhotoItem = nil
        onInteraction()
    }

    private var stepDraftBinding: Binding<RoutineSetStepDraft?> {
        Binding(
            get: {
                !isWide && viewModel.selectedDestination == .routineSetCreator
                    ? viewModel.routineSetStepDraft
                    : nil
            },
            set: {
                if $0 == nil,
                   !isWideLayout,
                   viewModel.selectedDestination == .routineSetCreator {
                    viewModel.cancelRoutineSetStepDraft()
                }
            }
        )
    }

    private var routineSetNameBinding: Binding<String> {
        Binding(
            get: { viewModel.routineSetDraft?.name ?? "" },
            set: {
                viewModel.routineSetDraft?.name = $0
                onInteraction()
            }
        )
    }

    private var stepTitleBinding: Binding<String> {
        Binding(
            get: { viewModel.routineSetStepDraft?.title ?? "" },
            set: {
                viewModel.routineSetStepDraft?.title = $0
                onInteraction()
            }
        )
    }

    private var stepIconBinding: Binding<RoutineIconName> {
        Binding(
            get: { viewModel.routineSetStepDraft?.iconName ?? .star },
            set: {
                viewModel.routineSetStepDraft?.iconName = $0
                selectedStepPhotoItem = nil
                onInteraction()
            }
        )
    }

    private var defaultScheduledTime: LocalTime {
        try! LocalTime(hour: 7, minute: 30)
    }

    private var stepScheduledDateBinding: Binding<Date> {
        Binding(
            get: {
                let time = viewModel.routineSetStepDraft?.scheduledTime
                return Calendar.current.date(
                    from: DateComponents(hour: time?.hour ?? 7, minute: time?.minute ?? 30)
                ) ?? .now
            },
            set: {
                let components = Calendar.current.dateComponents([.hour, .minute], from: $0)
                if let hour = components.hour, let minute = components.minute {
                    viewModel.routineSetStepDraft?.scheduledTime = try? LocalTime(
                        hour: hour,
                        minute: minute
                    )
                }
                onInteraction()
            }
        )
    }

    private var routineListRowInsets: EdgeInsets {
        EdgeInsets(
            top: SteppieSpacing.extraSmall,
            leading: SteppieLayout.guardianScreenPadding,
            bottom: SteppieSpacing.extraSmall,
            trailing: SteppieLayout.guardianScreenPadding
        )
    }

    private var localeIdentifier: String {
        locale.language.languageCode?.identifier ?? "ko"
    }
}
