import SwiftUI

struct GuardianRoutineSetNameEditorView: View {
    @Environment(\.locale) private var locale

    let viewModel: GuardianModeViewModel
    let onInteraction: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: SteppieSpacing.medium) {
                header(title: "루틴 세트 이름 변경", subtitle: "보호자 모드에 표시되는 이름입니다")
                labeledCard("루틴 세트 이름") {
                    TextField("루틴 세트 이름", text: routineSetNameDraftNameBinding)
                        .steppieTextStyle(.guardianBody)
                        .textFieldStyle(.plain)
                }
                HStack(spacing: SteppieSpacing.medium) {
                    SteppieButton("취소", role: .secondary) {
                        viewModel.cancelRoutineSetRename()
                        onInteraction()
                    }
                    SteppieButton(
                        "저장",
                        state: viewModel.routineSetNameDraft?.isValid == true ? .enabled : .disabled
                    ) {
                        viewModel.saveRoutineSetName(localeIdentifier: localeIdentifier)
                        onInteraction()
                    }
                }
            }
            .padding(SteppieLayout.guardianScreenPadding)
        }
        .background(Color.steppieBackgroundSecondary)
    }

    private var localeIdentifier: String {
        locale.language.languageCode?.identifier ?? "ko"
    }

    private var routineSetNameDraftNameBinding: Binding<String> {
        Binding(
            get: { viewModel.routineSetNameDraft?.name ?? "" },
            set: {
                viewModel.routineSetNameDraft?.name = $0
                onInteraction()
            }
        )
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
}

struct GuardianRoutineSetScheduleEditorView: View {
    let viewModel: GuardianModeViewModel
    let onInteraction: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: SteppieSpacing.medium) {
                header(
                    title: "매일 루틴 시간",
                    subtitle: "현재 세트를 마친 뒤 이 시간이 되면 자동으로 시작합니다"
                )
                labeledCard("시작 시각") {
                    DatePicker(
                        "시작 시각",
                        selection: routineSetScheduleDateBinding,
                        displayedComponents: .hourAndMinute
                    )
                    .datePickerStyle(.wheel)
                    .labelsHidden()
                    .frame(maxWidth: .infinity)
                    .accessibilityLabel(Text("루틴 세트 시작 시각"))
                }
                HStack(spacing: SteppieSpacing.medium) {
                    SteppieButton("취소", role: .secondary) {
                        viewModel.cancelRoutineSetSchedule()
                        onInteraction()
                    }
                    SteppieButton("저장") {
                        viewModel.saveRoutineSetSchedule()
                        onInteraction()
                    }
                }
            }
            .padding(SteppieLayout.guardianScreenPadding)
        }
        .background(Color.steppieBackgroundSecondary)
    }

    private var routineSetScheduleDateBinding: Binding<Date> {
        Binding(
            get: {
                guard let time = viewModel.routineSetScheduleDraft?.startTime else {
                    return Date()
                }
                return Calendar.current.date(
                    bySettingHour: time.hour,
                    minute: time.minute,
                    second: 0,
                    of: Date()
                ) ?? Date()
            },
            set: { date in
                let components = Calendar.current.dateComponents([.hour, .minute], from: date)
                guard let hour = components.hour,
                      let minute = components.minute,
                      let time = try? LocalTime(hour: hour, minute: minute) else { return }
                viewModel.routineSetScheduleDraft?.startTime = time
                onInteraction()
            }
        )
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
}

struct GuardianTodayRoutineSelectionView: View {
    @Environment(\.locale) private var locale
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    let viewModel: GuardianModeViewModel
    let onInteraction: () -> Void

    var body: some View {
        List {
            Section {
                header
                    .padding(.top, SteppieSpacing.medium)
                    .padding(.horizontal, SteppieLayout.guardianScreenPadding)
                    .listRowInsets(headerListRowInsets)
                    .listRowSeparator(.hidden)
                    .listRowBackground(Color.clear)

                ForEach(viewModel.routineSets) { routineSet in
                    routineSetButton(routineSet)
                        .listRowInsets(routineListRowInsets)
                        .listRowSeparator(.hidden)
                        .listRowBackground(Color.clear)
                }
            }
        }
        .listStyle(.plain)
        .scrollContentBackground(.hidden)
        .background(Color.steppieBackgroundSecondary)
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: SteppieSpacing.twoExtraSmall) {
            Text("오늘의 루틴 선택")
                .steppieTextStyle(.guardianTitle)
                .foregroundStyle(Color.steppieTextSecondary)
                .fixedSize(horizontal: false, vertical: true)
                .accessibilityAddTraits(.isHeader)
            Text("오늘 사용할 루틴 세트를 선택해 주세요")
                .steppieTextStyle(.guardianCaption)
                .foregroundStyle(Color.steppieTextPrimary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.bottom, SteppieSpacing.small)
    }

    private func routineSetButton(_ routineSet: RoutineSet) -> some View {
        let isAssigned = viewModel.isRoutineSetAssignedToday(routineSet)
        return Button {
            viewModel.assignRoutineSetForToday(routineSet)
            onInteraction()
        } label: {
            adaptiveCardStack(spacing: SteppieSpacing.small) {
                Image(systemName: isAssigned ? "checkmark.circle.fill" : "circle")
                    .font(.title3.weight(.semibold))
                    .foregroundStyle(isAssigned ? Color.steppieFocusRing : Color.steppieTextSecondary)
                    .frame(
                        width: SteppieLayout.guardianMinimumTouchTarget,
                        height: SteppieLayout.guardianMinimumTouchTarget
                    )
                    .accessibilityHidden(true)
                VStack(alignment: .leading, spacing: SteppieSpacing.twoExtraSmall) {
                    Text(routineSetTitle(routineSet))
                        .steppieTextStyle(.button)
                        .foregroundStyle(Color.steppieTextPrimary)
                        .fixedSize(horizontal: false, vertical: true)
                    Text(isAssigned ? "현재 사용 중" : "오늘 루틴으로 설정")
                        .steppieTextStyle(.guardianCaption)
                        .foregroundStyle(Color.steppieTextSecondary)
                        .fixedSize(horizontal: false, vertical: true)
                }
                if !dynamicTypeSize.isAccessibilitySize {
                    Spacer()
                }
            }
            .padding(SteppieSpacing.small)
            .frame(maxWidth: .infinity, minHeight: 80, alignment: .leading)
            .background(Color.steppieBackgroundPrimary)
            .overlay {
                RoundedRectangle(cornerRadius: SteppieCornerRadius.card)
                    .stroke(Color.steppieBorderSubtle, lineWidth: SteppieStroke.divider)
            }
            .clipShape(.rect(cornerRadius: SteppieCornerRadius.card))
        }
        .buttonStyle(.plain)
        .accessibilityLabel(Text(routineSetTitle(routineSet)))
        .accessibilityValue(Text(isAssigned ? "현재 사용 중" : "오늘 루틴으로 설정 가능"))
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

    private func routineSetTitle(_ routineSet: RoutineSet) -> String {
        routineSet.name.resolved(
            appLocale: locale.identifier,
            systemLanguages: [locale.identifier]
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

    private var headerListRowInsets: EdgeInsets {
        EdgeInsets(top: 0, leading: 0, bottom: SteppieSpacing.extraSmall, trailing: 0)
    }
}
