import SwiftUI

struct GuardianModeView: View {
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
                GuardianRoutineSetNameEditorView(
                    viewModel: viewModel,
                    onInteraction: onInteraction
                )
            }
            .presentationDetents([.medium])
        }
        .sheet(item: routineSetScheduleDraftBinding) { _ in
            NavigationStack {
                GuardianRoutineSetScheduleEditorView(
                    viewModel: viewModel,
                    onInteraction: onInteraction
                )
            }
            .presentationDetents([.medium])
        }
        .sheet(isPresented: todayRoutineSelectionBinding) {
            NavigationStack {
                GuardianTodayRoutineSelectionView(
                    viewModel: viewModel,
                    onInteraction: onInteraction
                )
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
        case .sky: String(localized: "하늘색")
        case .mint: String(localized: "민트색")
        case .lemon: String(localized: "노란색")
        case .peach: String(localized: "복숭아색")
        case .lavender: String(localized: "라벤더색")
        case .rose: String(localized: "장미색")
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
