import SwiftUI

struct GuardianModeView: View {
    @Environment(\.locale) private var locale
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @State private var phoneNavigationPath: [GuardianDestination] = []
    @State private var isTodayRoutineSelectionDismissed = false
    @State private var isWideLayout = false
    let viewModel: GuardianModeViewModel
    let tutorialCoordinator: TutorialCoordinator
    let onDone: () -> Void
    let onSecurityAction: (GuardianSecurityAction) -> Void
    let onInteraction: () -> Void

    var body: some View {
        GeometryReader { proxy in
            let isWide = proxy.size.width >= SteppieLayout.splitMinimumWidth
            content(isWide: isWide)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(Color.steppieBackgroundSecondary)
                .onTapGesture(perform: onInteraction)
                .sheet(item: draftBinding(isWide: isWide)) { _ in
                    NavigationStack {
                        GuardianRoutineCardEditorView(
                            viewModel: viewModel,
                            onInteraction: onInteraction
                        )
                    }
                    .presentationDetents([.large])
                }
                .onAppear {
                    isWideLayout = isWide
                }
                .onChange(of: isWide) { _, newValue in
                    isWideLayout = newValue
                }
        }
        .overlay {
            if todayRoutineSelectionBinding.wrappedValue {
                Color.black
                    .opacity(0.32)
                    .ignoresSafeArea()
                    .allowsHitTesting(false)
            }
        }
        .task { viewModel.loadIfNeeded() }
        .alert("삭제할까요?", isPresented: deleteBinding) {
            Button("취소", role: .cancel) {}
            Button("삭제", role: .destructive) {
                viewModel.confirmDelete()
                onInteraction()
            }
        } message: {
            Text("이 활동은 아이 모드에서 보이지 않게 됩니다.")
        }
        .alert("루틴 세트를 삭제할까요?", isPresented: routineSetDeleteBinding) {
            Button("취소", role: .cancel) {}
            Button("삭제", role: .destructive) {
                viewModel.confirmDeleteRoutineSet()
                onInteraction()
            }
        } message: {
            Text("이 루틴 세트와 포함된 활동은 보호자 모드 목록에서 보이지 않게 됩니다.")
        }
        .alert("문제가 생겼어요", isPresented: errorMessageBinding) {
            Button("확인", role: .cancel) {
                viewModel.clearErrorMessage()
            }
        } message: {
            Text(viewModel.errorMessage ?? "")
        }
        .sheet(item: routineSetNameDraftBinding) { _ in
            NavigationStack {
                routineSetNameEditor
            }
            .presentationDetents([.medium])
        }
        .sheet(item: routineSetScheduleDraftBinding) { _ in
            NavigationStack {
                routineSetScheduleEditor
            }
            .presentationDetents([.medium])
        }
        .sheet(isPresented: todayRoutineSelectionBinding) {
            NavigationStack {
                todayRoutineSelectionSheet
            }
            .presentationDetents([.medium, .large])
        }
        .tutorialOverlay(coordinator: tutorialCoordinator, screen: tutorialScreen)
    }

    @ViewBuilder
    private func content(isWide: Bool) -> some View {
        switch viewModel.loadState {
        case .idle:
            ProgressView()
        case .failed:
            messageState(title: "불러오지 못했어요", message: "잠시 후 다시 시도해 주세요.")
        case .empty, .loaded:
            if isWide, viewModel.selectedDestination != nil {
                HStack(spacing: 0) {
                    homeView(isWide: true)
                        .frame(width: SteppieLayout.splitListWidth)
                    Rectangle()
                        .fill(Color.steppieBorderSubtle)
                        .frame(width: SteppieStroke.divider)
                    if let destination = viewModel.selectedDestination {
                        destinationDetail(destination, isWide: true)
                    }
                }
            } else if isWide {
                homeView(isWide: true)
                    .frame(maxWidth: 393)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                NavigationStack(path: $phoneNavigationPath) {
                    homeView(isWide: false)
                        .navigationBarTitleDisplayMode(.inline)
                        .toolbar(.hidden, for: .navigationBar)
                        .navigationDestination(for: GuardianDestination.self) { destination in
                            destinationDetail(destination, isWide: false)
                        }
                }
                .onChange(of: viewModel.selectedDestination) { _, destination in
                    syncPhoneNavigation(with: destination)
                }
                .onChange(of: phoneNavigationPath) { _, path in
                    viewModel.selectedDestination = path.last
                }
            }
        }
    }

    private func homeView(isWide: Bool) -> some View {
        GuardianHomeView(
            hasRoutineSets: viewModel.hasRoutineSets,
            onDestinationSelected: { destination in
                if destination == .routineTemplates {
                    viewModel.beginTemplateSelection()
                } else {
                    openDestination(destination, isWide: isWide)
                }
            },
            onDone: onDone,
            onInteraction: onInteraction
        )
    }

    private var tutorialScreen: TutorialScreen {
        switch viewModel.selectedDestination {
        case nil: .guardianHome
        case .routineSetCreator: .routineSetCreator
        case .routineTemplates: .routineTemplates
        case .routineEditor: .routineEditor
        case .feedbackSettings: .feedbackSettings
        case .records: .records
        case .recordCalendar: .recordCalendar
        case .security: .security
        case .backupRestore: .backupRestore
        }
    }

    private func destinationDetail(_ destination: GuardianDestination, isWide: Bool) -> AnyView {
        switch destination {
        case .routineSetCreator:
            AnyView(GuardianRoutineSetCreatorView(
                isWide: isWide,
                viewModel: viewModel,
                onDone: onDone,
                onInteraction: onInteraction
            ))
        case .routineTemplates:
            AnyView(GuardianRoutineTemplateSelectionView(
                isWide: isWide,
                templates: viewModel.routineTemplates,
                selectedTemplate: viewModel.selectedTemplate,
                prepareSelection: {
                    if viewModel.selectedTemplateID == nil {
                        viewModel.beginTemplateSelection(
                            returnDestination: viewModel.templateReturnDestination
                        )
                    }
                },
                onTemplateSelected: viewModel.selectTemplate,
                onClose: viewModel.closeTemplateSelection,
                onSave: viewModel.saveSelectedTemplate,
                onDone: onDone,
                onInteraction: onInteraction
            ))
        case .routineEditor:
            AnyView(routineEditor(isWide: isWide))
        case .feedbackSettings:
            AnyView(GuardianFeedbackSettingsView(
                settings: viewModel.settings ?? (try! AppSettings()),
                updateSettings: viewModel.updateFeedbackSettings,
                onDone: onDone,
                onInteraction: onInteraction
            ))
        case .records:
            AnyView(GuardianRecordsView(
                summaries: viewModel.recordSummaries,
                selectedDate: viewModel.selectedRecordDate,
                selectedDetail: viewModel.selectedRecordDetail,
                onDateSelected: {
                    viewModel.selectRecordDate($0)
                    onInteraction()
                },
                onCalendarRequested: {
                    viewModel.prepareRecordCalendar()
                    viewModel.selectedDestination = .recordCalendar
                    onInteraction()
                }
            ))
        case .recordCalendar:
            AnyView(GuardianRecordCalendarView(
                isWide: isWide,
                recordDates: viewModel.recordCalendarDates,
                selectedDate: viewModel.selectedCalendarRecordDate,
                selectedDetail: viewModel.selectedCalendarRecordDetail,
                prepareCalendar: {
                    viewModel.prepareRecordCalendar()
                    return GuardianRecordCalendarPreparation(
                        recordDates: viewModel.recordCalendarDates,
                        selectedDate: viewModel.selectedCalendarRecordDate
                    )
                },
                onDateSelected: viewModel.selectCalendarRecordDate,
                onClose: {
                    viewModel.selectedDestination = .records
                },
                onInteraction: onInteraction
            ))
        case .security:
            AnyView(GuardianSecurityView(
                tutorialCoordinator: tutorialCoordinator,
                onSecurityAction: onSecurityAction,
                onBackupRestoreRequested: {
                    openDestination(.backupRestore, isWide: isWide)
                },
                onDone: onDone,
                onInteraction: onInteraction
            ))
        case .backupRestore:
            AnyView(BackupRestoreView(
                viewModel: viewModel,
                onDone: onDone,
                onInteraction: onInteraction
            ))
        }
    }

    private func openDestination(_ destination: GuardianDestination, isWide: Bool) {
        if destination == .routineEditor {
            viewModel.selectTodayAssignedRoutineSet()
        }
        if isWide {
            viewModel.selectedDestination = destination
        } else if phoneNavigationPath.last != destination {
            phoneNavigationPath.append(destination)
        }
        viewModel.selectedDestination = destination
    }

    private func syncPhoneNavigation(with destination: GuardianDestination?) {
        guard let destination else {
            phoneNavigationPath.removeAll()
            return
        }

        if let index = phoneNavigationPath.firstIndex(of: destination) {
            phoneNavigationPath.removeSubrange(phoneNavigationPath.index(after: index)..<phoneNavigationPath.endIndex)
        } else if phoneNavigationPath.last == .records, destination == .recordCalendar {
            phoneNavigationPath.append(destination)
        } else if phoneNavigationPath.last == .security, destination == .backupRestore {
            phoneNavigationPath.append(destination)
        } else {
            phoneNavigationPath = [destination]
        }
    }

    private func routineEditor(isWide: Bool) -> some View {
        HStack(spacing: 0) {
            GuardianRoutineManagementListView(
                isWide: isWide,
                viewModel: viewModel,
                onDone: onDone,
                onInteraction: onInteraction
            )
            if isWide {
                Rectangle()
                    .fill(Color.steppieBorderSubtle)
                    .frame(width: SteppieStroke.divider)
                GuardianRoutineCardEditorView(
                    viewModel: viewModel,
                    onInteraction: onInteraction
                )
                    .frame(maxWidth: .infinity)
            }
        }
    }

    private var routineSetNameEditor: some View {
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

    private var routineSetScheduleEditor: some View {
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

    private var headerListRowInsets: EdgeInsets {
        EdgeInsets(top: 0, leading: 0, bottom: SteppieSpacing.extraSmall, trailing: 0)
    }

    private var routineListRowInsets: EdgeInsets {
        EdgeInsets(
            top: SteppieSpacing.extraSmall,
            leading: SteppieLayout.guardianScreenPadding,
            bottom: SteppieSpacing.extraSmall,
            trailing: SteppieLayout.guardianScreenPadding
        )
    }
    private var todayRoutineSelectionSheet: some View {
        List {
            Section {
                header(
                    title: "오늘의 루틴 선택",
                    subtitle: "오늘 사용할 루틴 세트를 선택해 주세요"
                )
                .padding(.top, SteppieSpacing.medium)
                .padding(.horizontal, SteppieLayout.guardianScreenPadding)
                .listRowInsets(headerListRowInsets)
                .listRowSeparator(.hidden)
                .listRowBackground(Color.clear)

                ForEach(viewModel.routineSets) { routineSet in
                    Button {
                        viewModel.assignRoutineSetForToday(routineSet)
                        onInteraction()
                    } label: {
                        adaptiveCardStack(spacing: SteppieSpacing.small) {
                            Image(systemName: viewModel.isRoutineSetAssignedToday(routineSet) ? "checkmark.circle.fill" : "circle")
                                .font(.title3.weight(.semibold))
                                .foregroundStyle(viewModel.isRoutineSetAssignedToday(routineSet) ? Color.steppieFocusRing : Color.steppieTextSecondary)
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
                                Text(viewModel.isRoutineSetAssignedToday(routineSet) ? "현재 사용 중" : "오늘 루틴으로 설정")
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
                    .accessibilityValue(Text(viewModel.isRoutineSetAssignedToday(routineSet) ? "현재 사용 중" : "오늘 루틴으로 설정 가능"))
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

    private func unavailableScreen(title: String, subtitle: String) -> some View {
        VStack(spacing: SteppieSpacing.large) {
            header(title: title, subtitle: subtitle)
            VStack(spacing: SteppieSpacing.small) {
                Image(systemName: "hammer")
                    .font(.title)
                    .foregroundStyle(Color.steppieTextSecondary)
                    .accessibilityHidden(true)
                Text("이번 Sprint 7 범위에서는 구현하지 않습니다.")
                    .steppieTextStyle(.guardianBody)
                    .foregroundStyle(Color.steppieTextPrimary)
                    .multilineTextAlignment(.center)
            }
            .padding(SteppieSpacing.large)
            SteppieButton("뒤로", role: .secondary) {
                viewModel.selectedDestination = nil
            }
        }
        .padding(SteppieLayout.guardianScreenPadding)
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

    private func messageState(title: String, message: String) -> some View {
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

    private func routineSetTitle(_ routineSet: RoutineSet) -> String {
        routineSet.name.resolved(
            appLocale: locale.identifier,
            systemLanguages: [locale.identifier]
        )
    }

    private var localeIdentifier: String {
        locale.language.languageCode?.identifier ?? "ko"
    }

    private var deleteBinding: Binding<Bool> {
        Binding(
            get: { viewModel.pendingDeleteRoutine != nil },
            set: { if !$0 { viewModel.pendingDeleteRoutine = nil } }
        )
    }

    private var routineSetDeleteBinding: Binding<Bool> {
        Binding(
            get: { viewModel.pendingDeleteRoutineSet != nil },
            set: { if !$0 { viewModel.pendingDeleteRoutineSet = nil } }
        )
    }

    private var todayRoutineSelectionBinding: Binding<Bool> {
        Binding(
            get: {
                viewModel.requiresTodayRoutineSelection
                    && !isTodayRoutineSelectionDismissed
            },
            set: { isPresented in
                if !isPresented {
                    isTodayRoutineSelectionDismissed = true
                }
            }
        )
    }

    private var errorMessageBinding: Binding<Bool> {
        Binding(
            get: { viewModel.errorMessage != nil },
            set: { if !$0 { viewModel.clearErrorMessage() } }
        )
    }

    private var routineSetNameDraftBinding: Binding<RoutineSetNameDraft?> {
        Binding(
            get: { viewModel.routineSetNameDraft },
            set: { if $0 == nil { viewModel.routineSetNameDraft = nil } }
        )
    }

    private var routineSetScheduleDraftBinding: Binding<RoutineSetScheduleDraft?> {
        Binding(
            get: { viewModel.routineSetScheduleDraft },
            set: { if $0 == nil { viewModel.cancelRoutineSetSchedule() } }
        )
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

    private var routineSetNameDraftNameBinding: Binding<String> {
        Binding(
            get: { viewModel.routineSetNameDraft?.name ?? "" },
            set: {
                viewModel.routineSetNameDraft?.name = $0
                onInteraction()
            }
        )
    }

    private func draftBinding(isWide: Bool) -> Binding<RoutineDraft?> {
        Binding(
            get: {
                !isWide && viewModel.selectedDestination == .routineEditor
                    ? viewModel.draft
                    : nil
            },
            set: {
                if $0 == nil,
                   !isWideLayout,
                   viewModel.selectedDestination == .routineEditor {
                    viewModel.cancelDraft()
                }
            }
        )
    }

}

extension SteppieCardColor {
    var token: String {
        switch self {
        case .sky: "color.card.sky"
        case .mint: "color.card.mint"
        case .lemon: "color.card.lemon"
        case .peach: "color.card.peach"
        case .lavender: "color.card.lavender"
        case .rose: "color.card.rose"
        }
    }

    var accessibilityName: String {
        switch self {
        case .sky: "하늘색"
        case .mint: "민트색"
        case .lemon: "노란색"
        case .peach: "복숭아색"
        case .lavender: "라벤더색"
        case .rose: "장미색"
        }
    }
}

extension RoutineIconName {
    func displayName(for locale: Locale) -> String {
        let isKorean = locale.language.languageCode?.identifier == "ko"
        switch self {
        case .wakeUp: return isKorean ? "일어나기" : "Wake up"
        case .washFace: return isKorean ? "세수하기" : "Wash face"
        case .brushTeeth: return isKorean ? "양치하기" : "Brush teeth"
        case .getDressed: return isKorean ? "옷 입기" : "Get dressed"
        case .breakfast: return isKorean ? "아침 먹기" : "Eat breakfast"
        case .packBag: return isKorean ? "가방 챙기기" : "Pack bag"
        case .school: return isKorean ? "학교" : "School"
        case .book: return isKorean ? "책" : "Book"
        case .pencil: return isKorean ? "연필" : "Pencil"
        case .lunch: return isKorean ? "점심 먹기" : "Eat lunch"
        case .playground: return isKorean ? "놀이터" : "Playground"
        case .bus: return isKorean ? "버스" : "Bus"
        case .bath: return isKorean ? "목욕하기" : "Take a bath"
        case .pajamas: return isKorean ? "잠옷 입기" : "Put on pajamas"
        case .storyBook: return isKorean ? "이야기책" : "Story book"
        case .toilet: return isKorean ? "화장실" : "Toilet"
        case .sleep: return isKorean ? "잠자기" : "Sleep"
        case .star: return isKorean ? "별" : "Star"
        case .home: return isKorean ? "집" : "Home"
        case .meal: return isKorean ? "식사" : "Meal"
        case .snack: return isKorean ? "간식" : "Snack"
        case .medicine: return isKorean ? "약" : "Medicine"
        case .walk: return isKorean ? "산책" : "Walk"
        case .therapy: return isKorean ? "치료" : "Therapy"
        case .music: return isKorean ? "음악" : "Music"
        case .art: return isKorean ? "미술" : "Art"
        case .cleanUp: return isKorean ? "정리하기" : "Clean up"
        case .timer: return isKorean ? "타이머" : "Timer"
        }
    }
}
