import PhotosUI
import SwiftUI
import UIKit

struct GuardianModeView: View {
    @Environment(\.locale) private var locale
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @State private var selectedRoutinePhotoItem: PhotosPickerItem?
    @State private var selectedRoutineSetStepPhotoItem: PhotosPickerItem?
    @State private var draggedRoutineID: UUID?
    @State private var routineDragOrder: [UUID]?
    @State private var dragStartIndex: Int?
    @State private var phoneNavigationPath: [GuardianDestination] = []
    @State private var recordCalendarVisibleMonth = Date()
    @State private var recordCalendarPickerYear = Calendar.current.component(.year, from: Date())
    @State private var recordCalendarPickerMonth = Calendar.current.component(.month, from: Date())
    @State private var isRecordCalendarDatePickerPresented = false
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
                .sheet(item: routineSetStepDraftBinding(isWide: isWide)) { _ in
                    NavigationStack {
                        AnyView(routineSetStepEditorPane)
                    }
                    .presentationDetents([.large])
                }
                .sheet(item: draftBinding(isWide: isWide)) { _ in
                    NavigationStack {
                        AnyView(cardEditorPane)
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
        .sheet(isPresented: $isRecordCalendarDatePickerPresented) {
            recordCalendarDatePickerSheet
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
                    homeList(isWide: true)
                        .frame(width: SteppieLayout.splitListWidth)
                    Rectangle()
                        .fill(Color.steppieBorderSubtle)
                        .frame(width: SteppieStroke.divider)
                    if let destination = viewModel.selectedDestination {
                        destinationDetail(destination, isWide: true)
                    }
                }
            } else if isWide {
                homeList(isWide: true)
                    .frame(maxWidth: 393)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                NavigationStack(path: $phoneNavigationPath) {
                    homeList(isWide: false)
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

    private func homeList(isWide: Bool) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: SteppieSpacing.medium) {
                header(title: "보호자 모드", subtitle: "완료 또는 3분 미조작 시 아이 모드로 돌아갑니다")
                menuCard(
                    title: "루틴 세트 생성",
                    subtitle: "새 루틴 제목과 단계 목록 만들기",
                    assetName: "guardian-menu-routine-set",
                    destination: .routineSetCreator,
                    isWide: isWide,
                    isEnabled: true
                )
                .tutorialTarget(.primary)
                menuCard(
                    title: "템플릿에서 시작하기",
                    subtitle: "아침, 학교, 취침 루틴으로 빠르게 만들기",
                    assetName: "guardian-menu-template",
                    destination: .routineTemplates,
                    isWide: isWide,
                    isEnabled: true
                )
                .tutorialTarget(.secondary)
                menuCard(
                    title: "루틴 관리",
                    subtitle: viewModel.hasRoutineSets ? "루틴 세트 목록, 활동 추가, 순서 변경" : "먼저 루틴 세트를 만들어 주세요",
                    assetName: "guardian-menu-routine",
                    destination: .routineEditor,
                    isWide: isWide,
                    isEnabled: viewModel.hasRoutineSets
                )
                .tutorialTarget(.tertiary)
                menuCard(
                    title: "환경 설정",
                    subtitle: "음성, 효과음, 햅틱, 알림",
                    assetName: "guardian-menu-settings",
                    destination: .feedbackSettings,
                    isWide: isWide,
                    isEnabled: true
                )
                menuCard(
                    title: "진행 기록",
                    subtitle: "날짜별 완료 현황",
                    assetName: "guardian-menu-records",
                    destination: .records,
                    isWide: isWide,
                    isEnabled: true
                )
                menuCard(
                    title: "보안",
                    subtitle: "PIN, 복구 코드, 백업/복원",
                    assetName: "guardian-menu-security",
                    destination: .security,
                    isWide: isWide,
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
            AnyView(routineSetCreator(isWide: isWide))
        case .routineTemplates:
            AnyView(routineTemplateSelector(isWide: isWide))
        case .routineEditor:
            AnyView(routineEditor(isWide: isWide))
        case .feedbackSettings:
            AnyView(feedbackSettingsScreen(isWide: isWide))
        case .records:
            AnyView(recordsScreen(isWide: isWide))
        case .recordCalendar:
            AnyView(recordCalendarScreen(isWide: isWide))
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

    private func menuCard(
        title: String,
        subtitle: String,
        assetName: String,
        destination: GuardianDestination,
        isWide: Bool,
        isEnabled: Bool
    ) -> some View {
        Button {
            guard isEnabled else { return }
            if destination == .routineTemplates {
                viewModel.beginTemplateSelection()
            } else {
                openDestination(destination, isWide: isWide)
            }
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
                if !isEnabled {
                    Text("준비 중")
                        .steppieTextStyle(.guardianCaption)
                        .foregroundStyle(Color.steppieTextSecondary)
                        .fixedSize(horizontal: false, vertical: true)
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
        .accessibilityHint(isEnabled ? Text("열기") : Text("아직 구현되지 않았습니다"))
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
            AnyView(routineListPane(isWide: isWide))
            if isWide {
                Rectangle()
                    .fill(Color.steppieBorderSubtle)
                    .frame(width: SteppieStroke.divider)
                AnyView(cardEditorPane)
                    .frame(maxWidth: .infinity)
            }
        }
    }

    private func routineSetCreator(isWide: Bool) -> some View {
        HStack(spacing: 0) {
            AnyView(routineSetStepListPane(isWide: isWide))
            if isWide {
                Rectangle()
                    .fill(Color.steppieBorderSubtle)
                    .frame(width: SteppieStroke.divider)
                AnyView(routineSetStepEditorPane)
                    .frame(maxWidth: .infinity)
            }
        }
        .onAppear {
            if viewModel.routineSetDraft == nil {
                viewModel.beginCreateRoutineSet()
            }
        }
    }

    private func routineSetStepListPane(isWide: Bool) -> some View {
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
                        routineSetStepRow(step)
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

    private func routineSetStepRow(_ step: RoutineSetStepDraft) -> some View {
        adaptiveCardStack(spacing: SteppieSpacing.small) {
            RoundedRectangle(cornerRadius: 10)
                .fill(SteppieCardColor(colorToken: step.colorToken).color)
                .frame(width: dynamicTypeSize.isAccessibilitySize ? 68 : 20, height: dynamicTypeSize.isAccessibilitySize ? 20 : 68)
                .accessibilityHidden(true)
            RoutineVisualView(icon: step.icon, size: .list)
                .frame(width: 52, height: 52)
            VStack(alignment: .leading, spacing: SteppieSpacing.twoExtraSmall) {
                Text(step.title)
                    .steppieTextStyle(.button)
                    .foregroundStyle(Color.steppieTextPrimary)
                    .fixedSize(horizontal: false, vertical: true)
                Text(step.scheduledTime?.description ?? "시간 없음")
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
        .accessibilityValue(Text(step.scheduledTime?.description ?? "시간 없음"))
        .accessibilityHint(Text("a11y.guardian.step.editOrReorderHint"))
        .contextMenu {
            Button("수정") { viewModel.beginEditRoutineSetStep(step) }
            Button("삭제", role: .destructive) { viewModel.deleteRoutineSetStep(step) }
        }
    }

    private var routineSetStepEditorPane: some View {
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
                    routineSetStepForm
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
    private var routineSetStepForm: some View {
        if let stepDraft = viewModel.routineSetStepDraft {
            VStack(alignment: .leading, spacing: SteppieSpacing.medium) {
                labeledCard("활동 이름") {
                    TextField("활동 이름", text: routineSetStepTitleBinding)
                        .steppieTextStyle(.guardianBody)
                        .textFieldStyle(.plain)
                }
                labeledCard("아이콘 또는 사진") {
                    routineSetStepDraftVisualPicker(for: stepDraft)
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
                            .accessibilityValue(Text(color.token == stepDraft.colorToken ? "a11y.selection.selected" : "a11y.selection.notSelected"))
                            .accessibilityHint(Text("a11y.guardian.cardColor.selectHint"))
                        }
                    }
                }
                labeledCard("예정 시각") {
                    scheduledTimeEditor(
                        scheduledTime: stepDraft.scheduledTime,
                        date: routineSetStepScheduledDateBinding,
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

    private func routineListPane(isWide: Bool) -> some View {
        List {
            Section {
                routineSetListHeader
                    .listRowInsets(headerListRowInsets)
                    .listRowSeparator(.hidden)
                    .listRowBackground(Color.clear)

                ForEach(viewModel.routineSets) { routineSet in
                    routineSetRow(routineSet)
                        .listRowInsets(routineListRowInsets)
                        .listRowSeparator(.hidden)
                        .listRowBackground(Color.clear)
                }
            }

            Section {
                header(
                    title: routineEditorTitle,
                    subtitle: "각 루틴을 선택하면 해당 루틴을 수정할 수 있습니다."
                )
                .padding(.top, SteppieSpacing.large)
                .padding(.horizontal, SteppieLayout.guardianScreenPadding)
                .listRowInsets(headerListRowInsets)
                .listRowSeparator(.hidden)
                .listRowBackground(Color.clear)

                if viewModel.routines.isEmpty {
                    messageState(title: "활동이 없어요", message: "활동 추가로 첫 루틴을 만들어 주세요.")
                        .listRowInsets(routineListRowInsets)
                        .listRowSeparator(.hidden)
                        .listRowBackground(Color.clear)
                } else {
                    ForEach(displayedRoutines) { routine in
                        editableRoutineRow(routine, isWide: isWide)
                            .listRowInsets(routineListRowInsets)
                            .listRowSeparator(.hidden)
                            .listRowBackground(Color.clear)
                            .transaction { transaction in
                                transaction.animation = nil
                            }
                            .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                                Button(role: .destructive) {
                                    viewModel.requestDelete(routine)
                                    onInteraction()
                                } label: {
                                    Label("삭제", systemImage: "trash")
                                }
                            }
                    }
                }
            }
            Section {
                SteppieButton("+ 활동 추가") {
                    viewModel.beginAddRoutine()
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
    }

    private func routineTemplateSelector(isWide: Bool) -> some View {
        Group {
            if isWide {
                HStack(spacing: 0) {
                    routineTemplateListPane(isWide: true)
                        .frame(width: SteppieLayout.splitListWidth)
                    Rectangle()
                        .fill(Color.steppieBorderSubtle)
                        .frame(width: SteppieStroke.divider)
                    routineTemplatePreviewPane
                        .frame(maxWidth: .infinity)
                }
            } else {
                routineTemplateListPane(isWide: false)
            }
        }
        .onAppear {
            if viewModel.selectedTemplateID == nil {
                viewModel.beginTemplateSelection(returnDestination: viewModel.templateReturnDestination)
            }
        }
    }

    private func routineTemplateListPane(isWide: Bool) -> some View {
        List {
            Section {
                header(
                    title: "템플릿에서 시작하기",
                    subtitle: "템플릿을 고른 뒤 새 루틴 세트로 저장합니다"
                )
                .padding(.top, SteppieSpacing.medium)
                .padding(.horizontal, SteppieLayout.guardianScreenPadding)
                .listRowInsets(headerListRowInsets)
                .listRowSeparator(.hidden)
                .listRowBackground(Color.clear)

                ForEach(viewModel.routineTemplates) { template in
                    routineTemplateRow(template)
                        .listRowInsets(routineListRowInsets)
                        .listRowSeparator(.hidden)
                        .listRowBackground(Color.clear)
                }
            }

            if !isWide {
                Section {
                    routineTemplatePreviewCard
                        .listRowInsets(routineListRowInsets)
                        .listRowSeparator(.hidden)
                        .listRowBackground(Color.clear)
                }
            }
        }
        .listStyle(.plain)
        .scrollContentBackground(.hidden)
        .background(Color.steppieBackgroundSecondary)
        .navigationTitle("")
        .toolbar {
            if !isWide {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("완료", action: onDone)
                }
            }
        }
    }

    private func routineTemplateRow(_ template: BuiltInRoutineTemplate) -> some View {
        let isSelected = template.id == viewModel.selectedTemplate?.id
        return Button {
            viewModel.selectTemplate(template)
            onInteraction()
        } label: {
            adaptiveCardStack(spacing: SteppieSpacing.small) {
                Image(systemName: isSelected ? "checkmark.circle.fill" : "circle")
                    .font(.title3.weight(.semibold))
                    .foregroundStyle(isSelected ? Color.steppieFocusRing : Color.steppieTextSecondary)
                    .frame(
                        width: SteppieLayout.guardianMinimumTouchTarget,
                        height: SteppieLayout.guardianMinimumTouchTarget
                    )
                    .accessibilityHidden(true)
                VStack(alignment: .leading, spacing: SteppieSpacing.twoExtraSmall) {
                    Text(localizedText(template.name))
                        .steppieTextStyle(.button)
                        .foregroundStyle(Color.steppieTextPrimary)
                        .fixedSize(horizontal: false, vertical: true)
                    Text(template.routineCountText)
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
                    .stroke(
                        isSelected ? Color.steppieFocusRing : Color.steppieBorderSubtle,
                        lineWidth: isSelected ? SteppieStroke.focus : SteppieStroke.divider
                    )
            }
            .clipShape(.rect(cornerRadius: SteppieCornerRadius.card))
        }
        .buttonStyle(.plain)
        .accessibilityLabel(Text(localizedText(template.name)))
        .accessibilityValue(Text("\(template.routineCountText), \(isSelected ? "선택됨" : "선택 안 됨")"))
        .accessibilityHint(Text("템플릿 미리보기를 표시합니다"))
    }

    private var routineTemplatePreviewPane: some View {
        ScrollView {
            routineTemplatePreviewCard
                .frame(maxWidth: SteppieLayout.focusCardTabletMaximumWidth)
                .frame(maxWidth: .infinity, alignment: .center)
                .padding(SteppieLayout.guardianScreenPadding)
        }
        .background(Color.steppieBackgroundSecondary)
    }

    @ViewBuilder
    private var routineTemplatePreviewCard: some View {
        if let template = viewModel.selectedTemplate {
            VStack(alignment: .leading, spacing: SteppieSpacing.medium) {
                header(
                    title: localizedText(template.name),
                    subtitle: "\(template.steps.count)개 활동이 새 루틴 세트로 저장됩니다"
                )

                VStack(spacing: SteppieSpacing.small) {
                    ForEach(Array(template.steps.enumerated()), id: \.element.id) { order, step in
                        routineTemplateStepRow(step, order: order)
                    }
                }

                HStack(spacing: SteppieSpacing.medium) {
                    SteppieButton("뒤로", role: .secondary) {
                        viewModel.closeTemplateSelection()
                        onInteraction()
                    }
                    SteppieButton("새 루틴 세트로 저장") {
                        viewModel.saveSelectedTemplate()
                        onInteraction()
                    }
                }
                .padding(.top, SteppieSpacing.extraSmall)
            }
        } else {
            messageState(title: "템플릿이 없어요", message: "저장할 템플릿을 찾지 못했어요.")
        }
    }

    private func routineTemplateStepRow(_ step: BuiltInRoutineTemplateStep, order: Int) -> some View {
        adaptiveCardStack(spacing: SteppieSpacing.small) {
            Text("\(order + 1)")
                .steppieTextStyle(.guardianCaption)
                .foregroundStyle(Color.steppieTextSecondary)
                .frame(
                    width: SteppieLayout.guardianMinimumTouchTarget,
                    height: SteppieLayout.guardianMinimumTouchTarget
                )
                .background(Color.steppieBackgroundSecondary)
                .clipShape(Circle())
            RoutineVisualView(icon: step.icon, size: .list)
                .frame(width: 52, height: 52)
            VStack(alignment: .leading, spacing: SteppieSpacing.twoExtraSmall) {
                Text(localizedText(step.title))
                    .steppieTextStyle(.button)
                    .foregroundStyle(Color.steppieTextPrimary)
                    .fixedSize(horizontal: false, vertical: true)
                Text(step.scheduledTime?.description ?? "시간 없음")
                    .steppieTextStyle(.guardianCaption)
                    .foregroundStyle(Color.steppieTextSecondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
            if !dynamicTypeSize.isAccessibilitySize {
                Spacer()
            }
        }
        .padding(SteppieSpacing.small)
        .frame(maxWidth: .infinity, minHeight: 92, alignment: .leading)
        .background(Color.steppieBackgroundPrimary)
        .overlay {
            RoundedRectangle(cornerRadius: SteppieCornerRadius.card)
                .stroke(Color.steppieBorderSubtle, lineWidth: SteppieStroke.divider)
        }
        .clipShape(.rect(cornerRadius: SteppieCornerRadius.card))
        .accessibilityElement(children: .combine)
        .accessibilityLabel(Text("\(order + 1)번째, \(localizedText(step.title))"))
        .accessibilityValue(Text(step.scheduledTime?.description ?? "시간 없음"))
    }

    private func routineSetRow(_ routineSet: RoutineSet) -> some View {
        let isSelected = routineSet.id == viewModel.selectedRoutineSet?.id
        return HStack(spacing: SteppieSpacing.small) {
            Button {
                viewModel.selectRoutineSet(routineSet)
                onInteraction()
            } label: {
                HStack(spacing: SteppieSpacing.small) {
                    Image(systemName: isSelected ? "checkmark.circle.fill" : "circle")
                        .font(.title3.weight(.semibold))
                        .foregroundStyle(isSelected ? Color.steppieFocusRing : Color.steppieTextSecondary)
                        .frame(
                            width: SteppieLayout.guardianMinimumTouchTarget,
                            height: SteppieLayout.guardianMinimumTouchTarget
                        )
                        .accessibilityHidden(true)
                    Text(routineSetTitle(routineSet))
                        .steppieTextStyle(.button)
                        .foregroundStyle(Color.steppieTextPrimary)
                    Spacer()
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
                .contentShape(.rect)
            }
            .buttonStyle(.plain)
            .accessibilityLabel(Text(routineSetTitle(routineSet)))
            .accessibilityValue(Text(isSelected ? "선택됨" : "선택 안 됨"))
            .accessibilityHint(Text("이 루틴 세트를 편집합니다"))

            if viewModel.isEditingRoutineSets {
                routineSetEditActions(for: routineSet)
            } else {
                dailyRoutineSetAction(for: routineSet)
            }
        }
        .padding(SteppieSpacing.small)
        .frame(maxWidth: .infinity, minHeight: 80, alignment: .leading)
        .background(Color.steppieBackgroundPrimary)
        .overlay {
            RoundedRectangle(cornerRadius: SteppieCornerRadius.card)
                .stroke(
                    isSelected ? Color.steppieFocusRing : Color.steppieBorderSubtle,
                    lineWidth: isSelected ? SteppieStroke.focus : SteppieStroke.divider
                )
        }
        .clipShape(.rect(cornerRadius: SteppieCornerRadius.card))
    }

    @ViewBuilder
    private func dailyRoutineSetAction(for routineSet: RoutineSet) -> some View {
        if let startTime = routineSet.dailyStartTime {
            VStack(spacing: SteppieSpacing.twoExtraSmall) {
                Button {
                    viewModel.beginScheduleRoutineSet(routineSet)
                    onInteraction()
                } label: {
                    Text(startTime.description)
                        .steppieTextStyle(.button)
                        .foregroundStyle(Color.steppieFocusRing)
                        .frame(
                            minWidth: SteppieLayout.guardianMinimumTouchTarget,
                            minHeight: SteppieLayout.guardianMinimumTouchTarget
                        )
                }
                .buttonStyle(.plain)
                .accessibilityLabel(Text("\(routineSetTitle(routineSet)) 매일 \(startTime.description) 시작, 시간 변경"))

                Button {
                    viewModel.removeRoutineSetFromDailySchedule(routineSet)
                    onInteraction()
                } label: {
                    Text("제외")
                        .steppieTextStyle(.guardianCaption)
                        .foregroundStyle(Color.steppieDanger)
                        .frame(
                            minWidth: SteppieLayout.guardianMinimumTouchTarget,
                            minHeight: SteppieLayout.guardianMinimumTouchTarget
                        )
                }
                .buttonStyle(.plain)
                .accessibilityLabel(Text("\(routineSetTitle(routineSet)) 매일 루틴에서 제외"))
            }
        } else {
            Button {
                viewModel.beginScheduleRoutineSet(routineSet)
                onInteraction()
            } label: {
                Text("매일 사용")
                    .steppieTextStyle(.button)
                    .foregroundStyle(Color.steppieBackgroundPrimary)
                    .fixedSize(horizontal: false, vertical: true)
                    .padding(.horizontal, SteppieSpacing.small)
                    .frame(minWidth: SteppieLayout.guardianMinimumTouchTarget, minHeight: SteppieLayout.guardianMinimumTouchTarget)
                    .background(Color.steppieFocusRing)
                    .clipShape(.rect(cornerRadius: SteppieCornerRadius.control))
            }
            .buttonStyle(.plain)
            .accessibilityLabel(Text("\(routineSetTitle(routineSet)) 매일 루틴에 추가"))
        }
    }

    private var routineSetListHeader: some View {
        HStack(alignment: .top, spacing: SteppieSpacing.medium) {
            header(
                title: "루틴 관리",
                subtitle: "여러 세트를 매일 사용할 시간과 함께 선택합니다"
            )
            Button(viewModel.isEditingRoutineSets ? "완료" : "편집") {
                viewModel.toggleRoutineSetEditing()
                onInteraction()
            }
            .buttonStyle(.borderless)
            .steppieTextStyle(.button)
            .foregroundStyle(Color.steppieFocusRing)
            .frame(minWidth: SteppieLayout.guardianMinimumTouchTarget, minHeight: SteppieLayout.guardianMinimumTouchTarget)
            .accessibilityLabel(Text(viewModel.isEditingRoutineSets ? "루틴 세트 편집 완료" : "루틴 세트 편집"))
        }
        .padding(.top, SteppieSpacing.medium)
        .padding(.horizontal, SteppieLayout.guardianScreenPadding)
        .textCase(nil)
    }

    private func routineSetEditActions(for routineSet: RoutineSet) -> some View {
        HStack(spacing: SteppieSpacing.extraSmall) {
            Button {
                viewModel.beginRenameRoutineSet(routineSet)
                onInteraction()
            } label: {
                Image(systemName: "pencil")
                    .font(.body.weight(.semibold))
                    .foregroundStyle(Color.steppieFocusRing)
                    .frame(
                        width: SteppieLayout.guardianMinimumTouchTarget,
                        height: SteppieLayout.guardianMinimumTouchTarget
                    )
            }
            .buttonStyle(.plain)
            .accessibilityLabel(Text("\(routineSetTitle(routineSet)) 이름 변경"))

            Button {
                viewModel.requestDeleteRoutineSet(routineSet)
                onInteraction()
            } label: {
                Image(systemName: "minus")
                    .font(.body.weight(.bold))
                    .foregroundStyle(Color.steppieBackgroundPrimary)
                    .frame(
                        width: SteppieLayout.guardianMinimumTouchTarget,
                        height: SteppieLayout.guardianMinimumTouchTarget
                    )
                    .background(Color.steppieDanger)
                    .clipShape(Circle())
            }
            .buttonStyle(.plain)
            .disabled(viewModel.routineSets.count <= 1)
            .opacity(viewModel.routineSets.count <= 1 ? 0.4 : 1)
            .accessibilityLabel(Text("\(routineSetTitle(routineSet)) 삭제"))
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

    private var routineListRowInsets: EdgeInsets {
        EdgeInsets(
            top: SteppieSpacing.extraSmall,
            leading: SteppieLayout.guardianScreenPadding,
            bottom: SteppieSpacing.extraSmall,
            trailing: SteppieLayout.guardianScreenPadding
        )
    }

    private var displayedRoutines: [Routine] {
        guard let routineDragOrder else { return viewModel.routines }
        let routinesByID = Dictionary(uniqueKeysWithValues: viewModel.routines.map { ($0.id, $0) })
        let orderedRoutines = routineDragOrder.compactMap { routinesByID[$0] }
        return orderedRoutines.count == viewModel.routines.count ? orderedRoutines : viewModel.routines
    }

    private var headerListRowInsets: EdgeInsets {
        EdgeInsets(top: 0, leading: 0, bottom: SteppieSpacing.extraSmall, trailing: 0)
    }

    private func editableRoutineRow(_ routine: Routine, isWide: Bool) -> some View {
        adaptiveCardStack(spacing: SteppieSpacing.small) {
            Button {
                viewModel.beginEditRoutine(routine)
                onInteraction()
            } label: {
                adaptiveCardStack(spacing: SteppieSpacing.small) {
                    RoundedRectangle(cornerRadius: 10)
                        .fill(SteppieCardColor(colorToken: routine.colorToken).color)
                        .frame(width: dynamicTypeSize.isAccessibilitySize ? 68 : 20, height: dynamicTypeSize.isAccessibilitySize ? 20 : 68)
                        .accessibilityHidden(true)
                    RoutineVisualView(icon: routine.icon, size: .list)
                        .frame(width: 52, height: 52)
                    VStack(alignment: .leading, spacing: SteppieSpacing.twoExtraSmall) {
                        Text(viewModel.localizedTitle(for: routine))
                            .steppieTextStyle(.button)
                            .foregroundStyle(Color.steppieTextPrimary)
                            .fixedSize(horizontal: false, vertical: true)
                        Text(routine.scheduledTime?.description ?? "시간 없음")
                            .steppieTextStyle(.guardianCaption)
                            .foregroundStyle(Color.steppieTextSecondary)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                    if !dynamicTypeSize.isAccessibilitySize {
                        Spacer()
                    }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
                .contentShape(.rect)
            }
            .buttonStyle(.plain)
            .accessibilityLabel(Text(viewModel.localizedTitle(for: routine)))
            .accessibilityValue(Text(routine.scheduledTime?.description ?? "시간 없음"))
            .accessibilityHint(Text("이 루틴을 수정합니다"))
            Image(systemName: "line.3.horizontal")
                .font(.title3.weight(.semibold))
                .foregroundStyle(Color.steppieTextSecondary)
                .frame(width: 52, height: 68)
                .contentShape(.rect)
                .gesture(reorderGesture(for: routine))
                .accessibilityLabel(Text("\(viewModel.localizedTitle(for: routine)) 순서 변경 핸들"))
                .accessibilityHint(Text("누른 상태로 위아래로 움직여 순서를 바꿉니다"))
                .accessibilityAction(named: Text("\(viewModel.localizedTitle(for: routine)) 위로 이동")) {
                    viewModel.moveRoutine(routine, direction: -1)
                    onInteraction()
                }
                .accessibilityAction(named: Text("\(viewModel.localizedTitle(for: routine)) 아래로 이동")) {
                    viewModel.moveRoutine(routine, direction: 1)
                    onInteraction()
                }
        }
        .padding(SteppieSpacing.small)
        .frame(maxWidth: .infinity, minHeight: 92, alignment: .leading)
        .background(Color.steppieBackgroundPrimary)
        .overlay {
            RoundedRectangle(cornerRadius: SteppieCornerRadius.card)
                .stroke(
                    draggedRoutineID == routine.id ? Color.steppieFocusRing : Color.steppieBorderSubtle,
                    lineWidth: draggedRoutineID == routine.id ? SteppieStroke.focus : SteppieStroke.divider
                )
        }
        .clipShape(.rect(cornerRadius: SteppieCornerRadius.card))
        .contextMenu {
            Button("수정") { viewModel.beginEditRoutine(routine) }
            Button("삭제", role: .destructive) { viewModel.requestDelete(routine) }
        }
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

    private var cardEditorPane: some View {
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
                            .accessibilityValue(Text(color.token == draft.colorToken ? "a11y.selection.selected" : "a11y.selection.notSelected"))
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

    private func feedbackSettingsScreen(isWide: Bool) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: SteppieSpacing.medium) {
                header(title: "환경 설정", subtitle: "자극 강도를 아이에게 맞춥니다")

                settingsSegmentBlock(
                    title: "칭찬 애니메이션",
                    options: feedbackIntensityOptions,
                    selected: currentSettings.feedbackIntensity,
                    label: feedbackIntensityLabel
                ) { intensity in
                    updateSettings { try $0.replacing(feedbackIntensity: intensity) }
                }

                settingsSegmentBlock(
                    title: "음성 안내",
                    options: [true, false],
                    selected: currentSettings.ttsEnabled,
                    label: enabledLabel
                ) { isEnabled in
                    updateSettings { try $0.replacing(ttsEnabled: isEnabled) }
                }

                settingsSliderBlock(
                    title: "음성 속도",
                    valueText: String(format: "%.1fx", currentSettings.ttsRate),
                    value: ttsRateBinding,
                    range: 0.5...1.5
                )

                settingsSliderBlock(
                    title: "음성 볼륨",
                    valueText: "\(Int((currentSettings.ttsVolume * 100).rounded()))%",
                    value: ttsVolumeBinding,
                    range: 0.0...1.0
                )

                settingsSegmentBlock(
                    title: "효과음",
                    options: [true, false],
                    selected: currentSettings.soundEnabled,
                    label: enabledLabel
                ) { isEnabled in
                    updateSettings { try $0.replacing(soundEnabled: isEnabled) }
                }

                settingsSegmentBlock(
                    title: "햅틱/진동",
                    options: [true, false],
                    selected: currentSettings.hapticEnabled,
                    label: enabledLabel
                ) { isEnabled in
                    updateSettings { try $0.replacing(hapticEnabled: isEnabled) }
                }

                notificationLeadTimeBlock
                quietHoursBlock
                quietHoursNote

                SteppieButton("완료", action: onDone)
                    .padding(.top, SteppieSpacing.large)
            }
            .frame(maxWidth: SteppieLayout.focusCardTabletMaximumWidth)
            .frame(maxWidth: .infinity)
            .padding(SteppieLayout.guardianScreenPadding)
        }
        .background(Color.steppieBackgroundSecondary)
    }

    private var notificationLeadTimeBlock: some View {
        let leadTimes = currentSettings.notificationLeadTimes
        return settingsBlock(title: "예고 알림") {
            adaptiveSegmentRow {
                settingSegment(
                    title: "10분 전",
                    isSelected: leadTimes.contains(10),
                    accessibilityValue: leadTimes.contains(10) ? "선택됨" : "선택 안 됨"
                ) {
                    toggleLeadTime(10)
                }
                settingSegment(
                    title: "5분 전",
                    isSelected: leadTimes.contains(5),
                    accessibilityValue: leadTimes.contains(5) ? "선택됨" : "선택 안 됨"
                ) {
                    toggleLeadTime(5)
                }
                settingSegment(
                    title: "없음",
                    isSelected: leadTimes.isEmpty,
                    accessibilityValue: leadTimes.isEmpty ? "선택됨" : "선택 안 됨"
                ) {
                    updateSettings { try $0.replacing(notificationLeadTimes: []) }
                }
            }
        }
    }

    private var quietHoursBlock: some View {
        settingsBlock(title: "방해 금지 시간") {
            VStack(alignment: .leading, spacing: SteppieSpacing.small) {
                adaptiveSegmentRow {
                    settingSegment(
                        title: "켜기",
                        isSelected: quietHoursEnabled,
                        accessibilityValue: quietHoursEnabled ? "선택됨" : "선택 안 됨"
                    ) {
                        updateSettings {
                            try $0.replacing(
                                quietHours: (
                                    currentSettings.quietHoursStart ?? (try LocalTime(hour: 21, minute: 0)),
                                    currentSettings.quietHoursEnd ?? (try LocalTime(hour: 7, minute: 0))
                                )
                            )
                        }
                    }
                    settingSegment(
                        title: "끄기",
                        isSelected: !quietHoursEnabled,
                        accessibilityValue: !quietHoursEnabled ? "선택됨" : "선택 안 됨"
                    ) {
                        updateSettings { try $0.replacing(quietHours: (nil, nil)) }
                    }
                }

                if quietHoursEnabled {
                    adaptiveControlStack {
                        DatePicker(
                            "시작",
                            selection: quietHoursStartBinding,
                            displayedComponents: .hourAndMinute
                        )
                        DatePicker(
                            "종료",
                            selection: quietHoursEndBinding,
                            displayedComponents: .hourAndMinute
                        )
                    }
                    .steppieTextStyle(.guardianCaption)
                }
            }
        }
    }

    private var quietHoursNote: some View {
        VStack(alignment: .leading, spacing: SteppieSpacing.twoExtraSmall) {
            Text("방해 금지 시간 \(quietHoursSummary)")
                .steppieTextStyle(.button)
                .foregroundStyle(Color.steppieWarning)
                .fixedSize(horizontal: false, vertical: true)
            Text("권한이 없어도 루틴 기능은 계속 동작합니다.")
                .steppieTextStyle(.guardianCaption)
                .foregroundStyle(Color.steppieTextPrimary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(.horizontal, SteppieSpacing.small)
        .padding(.vertical, SteppieSpacing.extraSmall)
        .frame(maxWidth: .infinity, minHeight: 58, alignment: .leading)
        .background(Color.steppieCardLemon)
        .overlay {
            RoundedRectangle(cornerRadius: SteppieCornerRadius.control)
                .stroke(Color.steppieWarning, lineWidth: SteppieStroke.divider)
        }
        .clipShape(.rect(cornerRadius: SteppieCornerRadius.control))
        .accessibilityElement(children: .combine)
    }

    private func recordsScreen(isWide: Bool) -> some View {
        GeometryReader { proxy in
            let useSplit = proxy.size.width >= 720 && !dynamicTypeSize.isAccessibilitySize
            Group {
                if useSplit {
                    HStack(spacing: 0) {
                        recordSummaryPane
                            .frame(width: 330)
                        Rectangle()
                            .fill(Color.steppieBorderSubtle)
                            .frame(width: SteppieStroke.divider)
                        recordDetailPane
                            .frame(maxWidth: .infinity)
                    }
                } else {
                    ScrollView {
                        VStack(alignment: .leading, spacing: SteppieSpacing.medium) {
                            recordSummaryContent
                            recordDetailContent
                        }
                        .frame(maxWidth: SteppieLayout.focusCardTabletMaximumWidth)
                        .frame(maxWidth: .infinity)
                        .padding(SteppieLayout.guardianScreenPadding)
                    }
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Color.steppieBackgroundSecondary)
        }
    }

    private var recordSummaryPane: some View {
        ScrollView {
            recordSummaryContent
                .padding(SteppieLayout.guardianScreenPadding)
        }
        .background(Color.steppieBackgroundSecondary)
    }

    private var recordDetailPane: some View {
        ScrollView {
            recordDetailContent
                .frame(maxWidth: SteppieLayout.focusCardTabletMaximumWidth)
                .frame(maxWidth: .infinity)
                .padding(SteppieLayout.guardianScreenPadding)
        }
        .background(Color.steppieBackgroundSecondary)
    }

    private var recordSummaryContent: some View {
        VStack(alignment: .leading, spacing: SteppieSpacing.medium) {
            recordHeader
            VStack(spacing: SteppieSpacing.small) {
                ForEach(viewModel.recordSummaries) { summary in
                    recordSummaryRow(summary)
                }
            }
        }
    }

    private var recordHeader: some View {
        HStack(alignment: .top, spacing: SteppieSpacing.small) {
            header(title: "진행 기록", subtitle: "날짜별 완료 수와 루틴 상태를 함께 봅니다")
                .padding(.bottom, 0)
            Button {
                viewModel.prepareRecordCalendar()
                syncRecordCalendarVisibleMonth()
                viewModel.selectedDestination = .recordCalendar
                onInteraction()
            } label: {
                Image(systemName: "calendar")
                    .font(.title3.weight(.semibold))
                    .foregroundStyle(Color.steppieTextSecondary)
                    .frame(
                        width: SteppieLayout.guardianMinimumTouchTarget,
                        height: SteppieLayout.guardianMinimumTouchTarget
                    )
            }
            .buttonStyle(.plain)
            .accessibilityLabel(Text("전체 기록"))
            .accessibilityHint(Text("저장된 모든 진행 기록 날짜를 봅니다"))
        }
        .padding(.bottom, SteppieSpacing.small)
    }

    @ViewBuilder
    private var recordDetailContent: some View {
        if let detail = viewModel.selectedRecordDetail {
            VStack(alignment: .leading, spacing: SteppieSpacing.medium) {
                recordStats(detail)
                if detail.isEmpty {
                    recordEmptyState(date: detail.date)
                } else {
                    VStack(alignment: .leading, spacing: SteppieSpacing.small) {
                        Text("루틴별 상태")
                            .steppieTextStyle(.guardianSection)
                            .foregroundStyle(Color.steppieTextSecondary)
                            .fixedSize(horizontal: false, vertical: true)
                        ForEach(detail.rows) { row in
                            recordRoutineRow(row)
                        }
                    }
                }
            }
        } else {
            messageState(title: "기록을 불러오지 못했어요", message: "잠시 후 다시 시도해 주세요.")
        }
    }

    private func recordCalendarScreen(isWide: Bool) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                HStack(alignment: .top, spacing: SteppieSpacing.small) {
                    header(title: "전체 기록", subtitle: "기록이 있는 날짜만 선택할 수 있습니다")
                        .padding(.bottom, 0)

                    if isWide {
                        Button {
                            viewModel.selectedDestination = .records
                            onInteraction()
                        } label: {
                            Image(systemName: "xmark")
                                .font(.body.weight(.semibold))
                                .foregroundStyle(Color.steppieTextSecondary)
                                .frame(
                                    width: SteppieLayout.guardianMinimumTouchTarget,
                                    height: SteppieLayout.guardianMinimumTouchTarget
                                )
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel(Text("전체 기록 달력 닫기"))
                    }
                }
                .padding(.bottom, SteppieSpacing.small)

                recordCalendar
                    .padding(.bottom, SteppieSpacing.small * 3)

                if let detail = viewModel.selectedCalendarRecordDetail {
                    recordCalendarDetail(detail)
                } else if let selectedDate = viewModel.selectedCalendarRecordDate {
                    recordCalendarEmptySelectedDateState(selectedDate)
                } else {
                    recordCalendarNoRecordsState
                }
            }
            .frame(maxWidth: SteppieLayout.focusCardTabletMaximumWidth)
            .frame(maxWidth: .infinity)
            .padding(SteppieLayout.guardianScreenPadding)
        }
        .background(Color.steppieBackgroundSecondary)
        .onAppear {
            viewModel.prepareRecordCalendar()
            syncRecordCalendarVisibleMonth()
        }
    }

    private var recordCalendar: some View {
        VStack(alignment: .leading, spacing: SteppieSpacing.small) {
            HStack(spacing: SteppieSpacing.small) {
                Button {
                    moveRecordCalendarMonth(by: -1)
                    onInteraction()
                } label: {
                    Image(systemName: "chevron.left")
                        .font(.body.weight(.semibold))
                        .frame(
                            width: SteppieLayout.guardianMinimumTouchTarget,
                            height: SteppieLayout.guardianMinimumTouchTarget
                        )
                }
                .buttonStyle(.plain)
                .accessibilityLabel(Text("이전 달"))

                Button {
                    prepareRecordCalendarMonthPicker()
                    isRecordCalendarDatePickerPresented = true
                    onInteraction()
                } label: {
                    Text(recordCalendarMonthTitle)
                        .steppieTextStyle(.guardianSection)
                        .foregroundStyle(Color.steppieTextSecondary)
                        .frame(maxWidth: .infinity, minHeight: SteppieLayout.guardianMinimumTouchTarget)
                }
                .buttonStyle(.plain)
                .accessibilityLabel(Text("\(recordCalendarMonthTitle) 연월 선택"))
                .accessibilityHint(Text("연월 선택기를 엽니다"))
                .accessibilityAddTraits(.isHeader)

                Button {
                    moveRecordCalendarMonth(by: 1)
                    onInteraction()
                } label: {
                    Image(systemName: "chevron.right")
                        .font(.body.weight(.semibold))
                        .frame(
                            width: SteppieLayout.guardianMinimumTouchTarget,
                            height: SteppieLayout.guardianMinimumTouchTarget
                        )
                }
                .buttonStyle(.plain)
                .accessibilityLabel(Text("다음 달"))
            }

            LazyVGrid(columns: recordCalendarColumns, spacing: SteppieSpacing.extraSmall) {
                ForEach(recordCalendarWeekdaySymbols, id: \.self) { symbol in
                    Text(symbol)
                        .steppieTextStyle(.guardianCaption)
                        .foregroundStyle(Color.steppieTextPrimary)
                        .frame(maxWidth: .infinity, minHeight: 28)
                }
                ForEach(Array(recordCalendarDays.enumerated()), id: \.offset) { _, date in
                    recordCalendarDayCell(date)
                }
            }
        }
        .padding(SteppieSpacing.medium)
        .background(Color.steppieBackgroundPrimary)
        .overlay {
            RoundedRectangle(cornerRadius: SteppieCornerRadius.card)
                .stroke(Color.steppieBorderSubtle, lineWidth: SteppieStroke.divider)
        }
        .clipShape(.rect(cornerRadius: SteppieCornerRadius.card))
    }

    private var recordCalendarDatePickerSheet: some View {
        NavigationStack {
            HStack(spacing: SteppieSpacing.small) {
                Picker("연도", selection: $recordCalendarPickerYear) {
                    ForEach(recordCalendarYearRange, id: \.self) { year in
                        Text(String(year) + "년").tag(year)
                    }
                }
                .pickerStyle(.wheel)
                .frame(maxWidth: .infinity)
                .clipped()

                Picker("월", selection: $recordCalendarPickerMonth) {
                    ForEach(1...12, id: \.self) { month in
                        Text("\(month)월").tag(month)
                    }
                }
                .pickerStyle(.wheel)
                .frame(maxWidth: .infinity)
                .clipped()
            }
            .padding(SteppieLayout.guardianScreenPadding)
            .navigationTitle("연월 선택")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("취소") {
                        isRecordCalendarDatePickerPresented = false
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("완료") {
                        applyRecordCalendarPickerDate()
                    }
                }
            }
        }
    }

    private func recordCalendarDayCell(_ date: Date?) -> some View {
        Group {
            if let date {
                let localDate = DailyLog.localDateString(for: date, calendar: Calendar.current)
                let hasRecord = viewModel.recordCalendarDates.contains(localDate)
                if hasRecord {
                    Button {
                        viewModel.selectCalendarRecordDate(localDate)
                        onInteraction()
                    } label: {
                        recordCalendarDayContent(date: date, localDate: localDate, hasRecord: true)
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(Text("\(fullDateText(localDate)) 기록"))
                    .accessibilityValue(Text("기록 있음"))
                    .accessibilityHint(Text("날짜 기록 보기"))
                } else {
                    recordCalendarDayContent(date: date, localDate: localDate, hasRecord: false)
                        .accessibilityElement(children: .ignore)
                        .accessibilityLabel(Text(fullDateText(localDate)))
                        .accessibilityValue(Text("기록 없음"))
                }
            } else {
                Color.clear
                    .frame(minHeight: 58)
                    .accessibilityHidden(true)
            }
        }
    }

    private func recordCalendarDayContent(date: Date, localDate: String, hasRecord: Bool) -> some View {
        let isSelected = viewModel.selectedCalendarRecordDate == localDate
        return VStack(spacing: SteppieSpacing.twoExtraSmall) {
            Text("\(Calendar.current.component(.day, from: date))")
                .steppieTextStyle(.guardianBody)
                .foregroundStyle(hasRecord ? Color.steppieTextSecondary : Color.steppieTextPrimary.opacity(0.42))
                .frame(maxWidth: .infinity)
            Circle()
                .fill(hasRecord ? Color.steppieWarning : Color.clear)
                .frame(width: 6, height: 6)
                .accessibilityHidden(true)
        }
        .padding(.vertical, SteppieSpacing.extraSmall)
        .frame(maxWidth: .infinity, minHeight: 58)
        .background(isSelected ? Color.steppieCardLemon.opacity(0.45) : Color.clear)
        .overlay {
            RoundedRectangle(cornerRadius: SteppieCornerRadius.control)
                .stroke(isSelected ? Color.steppieFocusRing : Color.clear, lineWidth: SteppieStroke.focus)
        }
        .clipShape(.rect(cornerRadius: SteppieCornerRadius.control))
    }

    private func recordCalendarDetail(_ detail: GuardianRecordDetail) -> some View {
        VStack(alignment: .leading, spacing: SteppieSpacing.medium) {
            recordStats(detail)
            VStack(alignment: .leading, spacing: SteppieSpacing.small) {
                Text("루틴별 상태")
                    .steppieTextStyle(.guardianSection)
                    .foregroundStyle(Color.steppieTextSecondary)
                    .fixedSize(horizontal: false, vertical: true)
                ForEach(detail.rows) { row in
                    recordRoutineRow(row)
                }
            }
        }
    }

    private func recordCalendarEmptySelectedDateState(_ date: String) -> some View {
        let isToday = date == DailyLog.localDateString(for: Date(), calendar: Calendar.current)
        return VStack(alignment: .leading, spacing: SteppieSpacing.small) {
            Text(fullDateText(date))
                .steppieTextStyle(.guardianTitle)
                .foregroundStyle(Color.steppieTextSecondary)
                .fixedSize(horizontal: false, vertical: true)
                .accessibilityAddTraits(.isHeader)
            Text(isToday ? "오늘은 아직 루틴이 없어요." : "\(fullDateText(date))에는 완료 기록이 없습니다.")
                .steppieTextStyle(.guardianBody)
                .foregroundStyle(Color.steppieTextPrimary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .accessibilityElement(children: .combine)
    }

    private var recordCalendarNoRecordsState: some View {
        VStack(alignment: .leading, spacing: SteppieSpacing.small) {
            Image(systemName: "calendar.badge.exclamationmark")
                .font(.title2.weight(.semibold))
                .foregroundStyle(Color.steppieTextSecondary)
                .accessibilityHidden(true)
            Text("기록이 없어요")
                .steppieTextStyle(.guardianSection)
                .foregroundStyle(Color.steppieTextSecondary)
            Text("저장된 진행 기록이 없습니다.")
                .steppieTextStyle(.guardianBody)
                .foregroundStyle(Color.steppieTextPrimary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(SteppieSpacing.medium)
        .frame(maxWidth: .infinity, minHeight: 148, alignment: .leading)
        .background(Color.steppieBackgroundPrimary)
        .overlay {
            RoundedRectangle(cornerRadius: SteppieCornerRadius.card)
                .stroke(Color.steppieBorderSubtle, lineWidth: SteppieStroke.divider)
        }
        .clipShape(.rect(cornerRadius: SteppieCornerRadius.card))
        .accessibilityElement(children: .combine)
    }

    private func recordSummaryRow(_ summary: GuardianRecordSummary) -> some View {
        let isSelected = summary.date == viewModel.selectedRecordDate
        return Button {
            viewModel.selectRecordDate(summary.date)
            onInteraction()
        } label: {
            adaptiveCardStack(spacing: SteppieSpacing.small) {
                VStack(alignment: .leading, spacing: SteppieSpacing.twoExtraSmall) {
                    Text(summary.weekdaySymbol)
                        .steppieTextStyle(.guardianSection)
                        .foregroundStyle(Color.steppieTextSecondary)
                    Text(shortDateText(summary.date))
                        .steppieTextStyle(.guardianCaption)
                        .foregroundStyle(Color.steppieTextPrimary)
                }
                .frame(minWidth: dynamicTypeSize.isAccessibilitySize ? 0 : 48, alignment: .leading)

                recordProgressSegments(completedCount: summary.completedCount, totalCount: summary.totalCount)
                    .frame(maxWidth: dynamicTypeSize.isAccessibilitySize ? .infinity : 168, alignment: .leading)

                Text(summary.statusText)
                    .steppieTextStyle(.guardianCaption)
                    .foregroundStyle(Color.steppieTextPrimary)
                    .fixedSize(horizontal: false, vertical: true)

                if !dynamicTypeSize.isAccessibilitySize {
                    Spacer()
                }
            }
            .padding(.horizontal, SteppieSpacing.medium)
            .padding(.vertical, SteppieSpacing.small)
            .frame(maxWidth: .infinity, minHeight: 74, alignment: .leading)
            .background(Color.steppieBackgroundPrimary)
            .overlay {
                RoundedRectangle(cornerRadius: 14)
                    .stroke(
                        isSelected ? Color.steppieFocusRing : Color.steppieBorderSubtle,
                        lineWidth: isSelected ? SteppieStroke.focus : SteppieStroke.divider
                    )
            }
            .clipShape(.rect(cornerRadius: 14))
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Text("\(summary.date) 진행 기록"))
        .accessibilityValue(Text("\(summary.completedCount)개 완료, 전체 \(summary.totalCount)개, \(summary.percentage)퍼센트, \(summary.statusText)"))
        .accessibilityHint(Text("날짜 기록 보기"))
    }

    private func recordStats(_ detail: GuardianRecordDetail) -> some View {
        VStack(alignment: .leading, spacing: SteppieSpacing.small) {
            header(title: fullDateText(detail.date), subtitle: "\(detail.completedCount)/\(detail.totalCount) 완료, 완료율 \(detail.percentage)%")
            adaptiveCardStack(spacing: SteppieSpacing.small) {
                recordStatTile(title: "완료 수", value: "\(detail.completedCount)")
                recordStatTile(title: "전체 수", value: "\(detail.totalCount)")
                recordStatTile(title: "완료율", value: "\(detail.percentage)%")
            }
            recordProgressSegments(completedCount: detail.completedCount, totalCount: detail.totalCount)
                .padding(.top, SteppieSpacing.twoExtraSmall)
                .accessibilityHidden(true)
        }
    }

    private func recordStatTile(title: String, value: String) -> some View {
        VStack(alignment: .leading, spacing: SteppieSpacing.twoExtraSmall) {
            Text(title)
                .steppieTextStyle(.guardianCaption)
                .foregroundStyle(Color.steppieTextPrimary)
            Text(value)
                .steppieTextStyle(.guardianSection)
                .foregroundStyle(Color.steppieTextSecondary)
        }
        .padding(SteppieSpacing.small)
        .frame(maxWidth: .infinity, minHeight: 74, alignment: .leading)
        .background(Color.steppieBackgroundPrimary)
        .overlay {
            RoundedRectangle(cornerRadius: SteppieCornerRadius.control)
                .stroke(Color.steppieBorderSubtle, lineWidth: SteppieStroke.divider)
        }
        .clipShape(.rect(cornerRadius: SteppieCornerRadius.control))
        .accessibilityElement(children: .combine)
    }

    private func recordRoutineRow(_ row: GuardianRecordRoutineRow) -> some View {
        adaptiveCardStack(spacing: SteppieSpacing.small) {
            Image(systemName: row.isCompleted ? "checkmark.circle.fill" : "circle")
                .font(.title3.weight(.semibold))
                .foregroundStyle(row.isCompleted ? Color.steppieSuccess : Color.steppieTextSecondary)
                .frame(
                    width: SteppieLayout.guardianMinimumTouchTarget,
                    height: SteppieLayout.guardianMinimumTouchTarget
                )
                .accessibilityHidden(true)

            VStack(alignment: .leading, spacing: SteppieSpacing.twoExtraSmall) {
                Text(row.title)
                    .steppieTextStyle(.button)
                    .foregroundStyle(Color.steppieTextSecondary)
                    .fixedSize(horizontal: false, vertical: true)
                Text(recordRoutineMeta(row))
                    .steppieTextStyle(.guardianCaption)
                    .foregroundStyle(Color.steppieTextPrimary)
                    .fixedSize(horizontal: false, vertical: true)
            }
            if !dynamicTypeSize.isAccessibilitySize {
                Spacer()
            }
            Text(row.statusText)
                .steppieTextStyle(.guardianCaption)
                .foregroundStyle(row.isCompleted ? Color.steppieSuccess : Color.steppieTextPrimary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(SteppieSpacing.small)
        .frame(maxWidth: .infinity, minHeight: 80, alignment: .leading)
        .background(Color.steppieBackgroundPrimary)
        .overlay {
            RoundedRectangle(cornerRadius: SteppieCornerRadius.card)
                .stroke(Color.steppieBorderSubtle, lineWidth: SteppieStroke.divider)
        }
        .clipShape(.rect(cornerRadius: SteppieCornerRadius.card))
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Text(row.title))
        .accessibilityValue(Text(recordRoutineAccessibilityValue(row)))
    }

    @ViewBuilder
    private func recordProgressSegments(completedCount: Int, totalCount: Int) -> some View {
        let visibleCompletedCount = min(max(completedCount, 0), max(totalCount, 0))
        if totalCount <= 0 {
            Color.clear
                .frame(height: 20)
                .frame(maxWidth: .infinity)
        } else if totalCount <= 8 {
            HStack(spacing: SteppieSpacing.twoExtraSmall) {
                ForEach(0..<totalCount, id: \.self) { index in
                    RoundedRectangle(cornerRadius: 5)
                        .fill(index < visibleCompletedCount ? Color.steppieFocusRing : Color.steppieBorderSubtle)
                        .frame(height: 20)
                }
            }
            .frame(maxWidth: .infinity)
        } else {
            GeometryReader { proxy in
                let ratio = Double(visibleCompletedCount) / Double(totalCount)
                ZStack(alignment: .leading) {
                    RoundedRectangle(cornerRadius: 5)
                        .fill(Color.steppieBorderSubtle)
                    RoundedRectangle(cornerRadius: 5)
                        .fill(Color.steppieFocusRing)
                        .frame(width: proxy.size.width * ratio)
                }
            }
            .frame(height: 20)
            .frame(maxWidth: .infinity)
        }
    }

    private func recordEmptyState(date: String) -> some View {
        VStack(alignment: .leading, spacing: SteppieSpacing.small) {
            Image(systemName: "calendar.badge.exclamationmark")
                .font(.title2.weight(.semibold))
                .foregroundStyle(Color.steppieTextSecondary)
                .accessibilityHidden(true)
            Text("기록이 없어요")
                .steppieTextStyle(.guardianSection)
                .foregroundStyle(Color.steppieTextSecondary)
            Text("\(fullDateText(date))에는 완료 기록이 없습니다.")
                .steppieTextStyle(.guardianBody)
                .foregroundStyle(Color.steppieTextPrimary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(SteppieSpacing.medium)
        .frame(maxWidth: .infinity, minHeight: 148, alignment: .leading)
        .background(Color.steppieBackgroundPrimary)
        .overlay {
            RoundedRectangle(cornerRadius: SteppieCornerRadius.card)
                .stroke(Color.steppieBorderSubtle, lineWidth: SteppieStroke.divider)
        }
        .clipShape(.rect(cornerRadius: SteppieCornerRadius.card))
        .accessibilityElement(children: .combine)
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

    private func settingsBlock<Content: View>(
        title: String,
        @ViewBuilder content: () -> Content
    ) -> some View {
        VStack(alignment: .leading, spacing: SteppieSpacing.extraSmall) {
            Text(title)
                .steppieTextStyle(.button)
                .foregroundStyle(Color.steppieTextSecondary)
                .fixedSize(horizontal: false, vertical: true)
            content()
        }
        .padding(.horizontal, 14)
        .padding(.vertical, SteppieSpacing.extraSmall)
        .frame(maxWidth: .infinity, minHeight: 86, alignment: .leading)
        .background(Color.steppieBackgroundPrimary)
        .overlay {
            RoundedRectangle(cornerRadius: SteppieCornerRadius.card)
                .stroke(Color.steppieBorderSubtle, lineWidth: SteppieStroke.divider)
        }
        .clipShape(.rect(cornerRadius: SteppieCornerRadius.card))
    }

    private func settingsSegmentBlock<Value: Hashable>(
        title: String,
        options: [Value],
        selected: Value,
        label: @escaping (Value) -> String,
        onSelect: @escaping (Value) -> Void
    ) -> some View {
        settingsBlock(title: title) {
            adaptiveSegmentRow {
                ForEach(options, id: \.self) { option in
                    let isSelected = option == selected
                    settingSegment(
                        title: label(option),
                        isSelected: isSelected,
                        accessibilityValue: isSelected ? "선택됨" : "선택 안 됨"
                    ) {
                        onSelect(option)
                    }
                }
            }
        }
        .tutorialTarget(.primary)
    }

    private func settingsSliderBlock(
        title: String,
        valueText: String,
        value: Binding<Double>,
        range: ClosedRange<Double>
    ) -> some View {
        settingsBlock(title: title) {
            adaptiveControlStack {
                Slider(value: value, in: range, step: 0.1) {
                    Text(title)
                } minimumValueLabel: {
                    Text(String(format: "%.1f", range.lowerBound))
                        .steppieTextStyle(.guardianCaption)
                        .foregroundStyle(Color.steppieTextSecondary)
                } maximumValueLabel: {
                    Text(String(format: "%.1f", range.upperBound))
                        .steppieTextStyle(.guardianCaption)
                        .foregroundStyle(Color.steppieTextSecondary)
                }
                Text(valueText)
                    .steppieTextStyle(.guardianCaption)
                    .foregroundStyle(Color.steppieTextPrimary)
                    .frame(minWidth: 54, alignment: .trailing)
                    .accessibilityHidden(true)
            }
            .frame(minHeight: SteppieLayout.guardianMinimumTouchTarget)
            .accessibilityValue(Text(valueText))
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
                    selection: $selectedRoutinePhotoItem,
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
                        selectedRoutinePhotoItem = nil
                        onInteraction()
                    } label: {
                        secondaryPickerLabel("사진 삭제", foregroundColor: Color.steppieDanger)
                    }
                    .buttonStyle(.plain)
                    .accessibilityHint(Text("기본 아이콘으로 되돌립니다"))
                }
            }
        }
        .onChange(of: selectedRoutinePhotoItem) { _, item in
            guard let item else { return }
            loadSelectedRoutinePhoto(item)
        }
    }

    private func routineSetStepDraftVisualPicker(for draft: RoutineSetStepDraft) -> some View {
        VStack(alignment: .leading, spacing: SteppieSpacing.small) {
            adaptiveCardStack(spacing: SteppieSpacing.medium) {
                RoutineVisualView(icon: draft.icon, size: .list)
                    .frame(width: 64, height: 64)
                Picker("기본 아이콘", selection: routineSetStepIconBinding) {
                    ForEach(RoutineIconName.allCases) { icon in
                        Text(icon.displayName(for: locale)).tag(icon)
                    }
                }
                .pickerStyle(.menu)
                .frame(minHeight: SteppieLayout.guardianMinimumTouchTarget)
            }

            adaptiveControlStack {
                PhotosPicker(
                    selection: $selectedRoutineSetStepPhotoItem,
                    matching: .images,
                    photoLibrary: .shared()
                ) {
                    secondaryPickerLabel("사진 선택")
                }
                .buttonStyle(.plain)
                .accessibilityHint(Text("사진 앱에서 활동 사진을 선택합니다"))

                RoutineCameraCaptureButton(
                    onImagePicked: updateRoutineSetStepDraftPhoto(image:),
                    onInteraction: onInteraction
                )

                if draft.icon.type == .photo {
                    Button(role: .destructive) {
                        viewModel.resetRoutineSetStepDraftIconToDefault()
                        selectedRoutineSetStepPhotoItem = nil
                        onInteraction()
                    } label: {
                        secondaryPickerLabel("사진 삭제", foregroundColor: Color.steppieDanger)
                    }
                    .buttonStyle(.plain)
                    .accessibilityHint(Text("기본 아이콘으로 되돌립니다"))
                }
            }
        }
        .onChange(of: selectedRoutineSetStepPhotoItem) { _, item in
            guard let item else { return }
            loadSelectedRoutineSetStepPhoto(item)
        }
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

    private func loadSelectedRoutinePhoto(_ item: PhotosPickerItem) {
        Task {
            guard let data = try? await item.loadTransferable(type: Data.self) else {
                selectedRoutinePhotoItem = nil
                return
            }
            viewModel.updateDraftPhoto(data: data)
            selectedRoutinePhotoItem = nil
            onInteraction()
        }
    }

    private func loadSelectedRoutineSetStepPhoto(_ item: PhotosPickerItem) {
        Task {
            guard let data = try? await item.loadTransferable(type: Data.self) else {
                selectedRoutineSetStepPhotoItem = nil
                return
            }
            viewModel.updateRoutineSetStepDraftPhoto(data: data)
            selectedRoutineSetStepPhotoItem = nil
            onInteraction()
        }
    }

    private func updateDraftPhoto(image: UIImage) {
        guard let data = image.jpegData(compressionQuality: 0.9) else { return }
        viewModel.updateDraftPhoto(data: data)
        selectedRoutinePhotoItem = nil
        onInteraction()
    }

    private func updateRoutineSetStepDraftPhoto(image: UIImage) {
        guard let data = image.jpegData(compressionQuality: 0.9) else { return }
        viewModel.updateRoutineSetStepDraftPhoto(data: data)
        selectedRoutineSetStepPhotoItem = nil
        onInteraction()
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

    private var routineEditorTitle: String {
        guard let routineSet = viewModel.selectedRoutineSet else {
            return "루틴"
        }
        return routineSetTitle(routineSet)
    }

    private func routineSetTitle(_ routineSet: RoutineSet) -> String {
        routineSet.name.resolved(
            appLocale: locale.identifier,
            systemLanguages: [locale.identifier]
        )
    }

    private func localizedText(_ text: LocalizedText) -> String {
        text.resolved(
            appLocale: locale.identifier,
            systemLanguages: [locale.identifier]
        )
    }

    private var localeIdentifier: String {
        locale.language.languageCode?.identifier ?? "ko"
    }

    private var currentSettings: AppSettings {
        viewModel.settings ?? (try! AppSettings())
    }

    private var feedbackIntensityOptions: [FeedbackIntensity] {
        [.strong, .normal, .quiet, .off]
    }

    private func feedbackIntensityLabel(_ intensity: FeedbackIntensity) -> String {
        switch intensity {
        case .strong: "강함"
        case .normal: "보통"
        case .quiet: "조용함"
        case .off: "없음"
        }
    }

    private func enabledLabel(_ value: Bool) -> String {
        value ? "켜기" : "끄기"
    }

    private var quietHoursEnabled: Bool {
        currentSettings.quietHoursStart != nil && currentSettings.quietHoursEnd != nil
    }

    private var quietHoursSummary: String {
        guard let start = currentSettings.quietHoursStart,
              let end = currentSettings.quietHoursEnd else {
            return "꺼짐"
        }
        return "\(start)-\(end)"
    }

    private var recordCalendarColumns: [GridItem] {
        Array(
            repeating: GridItem(.flexible(minimum: 36), spacing: SteppieSpacing.twoExtraSmall),
            count: 7
        )
    }

    private var recordCalendarWeekdaySymbols: [String] {
        ["일", "월", "화", "수", "목", "금", "토"]
    }

    private var recordCalendarMonthTitle: String {
        let formatter = DateFormatter()
        formatter.locale = locale
        formatter.calendar = Calendar(identifier: .gregorian)
        formatter.dateFormat = "yyyy년 M월"
        return formatter.string(from: recordCalendarVisibleMonth)
    }

    private var recordCalendarDays: [Date?] {
        let calendar = Calendar.current
        guard let monthStart = startOfRecordCalendarMonth(for: recordCalendarVisibleMonth),
              let dayRange = calendar.range(of: .day, in: .month, for: monthStart) else {
            return []
        }

        let leadingBlankCount = max(calendar.component(.weekday, from: monthStart) - 1, 0)
        let dates = dayRange.compactMap { day -> Date? in
            calendar.date(byAdding: .day, value: day - 1, to: monthStart)
        }
        let cells: [Date?] = Array(repeating: nil, count: leadingBlankCount) + dates.map(Optional.some)
        let trailingBlankCount = (7 - (cells.count % 7)) % 7
        return cells + Array(repeating: nil, count: trailingBlankCount)
    }

    private func syncRecordCalendarVisibleMonth() {
        let selectedDate = viewModel.selectedCalendarRecordDate.flatMap(Self.localDateFormatter.date(from:))
        let latestDate = viewModel.recordCalendarDates.sorted(by: >).first.flatMap(Self.localDateFormatter.date(from:))
        if let month = startOfRecordCalendarMonth(for: selectedDate ?? latestDate ?? Date()) {
            recordCalendarVisibleMonth = month
        }
    }

    private func moveRecordCalendarMonth(by value: Int) {
        if let moved = Calendar.current.date(byAdding: .month, value: value, to: recordCalendarVisibleMonth),
           let month = startOfRecordCalendarMonth(for: moved) {
            recordCalendarVisibleMonth = month
        }
    }

    private var recordCalendarYearRange: ClosedRange<Int> {
        let currentYear = Calendar.current.component(.year, from: Date())
        let recordYears = viewModel.recordCalendarDates.compactMap { localDate -> Int? in
            Self.localDateFormatter.date(from: localDate).map { Calendar.current.component(.year, from: $0) }
        }
        let minimumYear = min(recordYears.min() ?? currentYear, currentYear) - 1
        let maximumYear = max(recordYears.max() ?? currentYear, currentYear) + 1
        return minimumYear...maximumYear
    }

    private func prepareRecordCalendarMonthPicker() {
        let calendar = Calendar.current
        recordCalendarPickerYear = calendar.component(.year, from: recordCalendarVisibleMonth)
        recordCalendarPickerMonth = calendar.component(.month, from: recordCalendarVisibleMonth)
    }

    private func applyRecordCalendarPickerDate() {
        var components = DateComponents()
        components.year = recordCalendarPickerYear
        components.month = recordCalendarPickerMonth
        components.day = 1

        if let date = Calendar.current.date(from: components),
           let month = startOfRecordCalendarMonth(for: date) {
            recordCalendarVisibleMonth = month
        }

        isRecordCalendarDatePickerPresented = false
        onInteraction()
    }

    private func startOfRecordCalendarMonth(for date: Date) -> Date? {
        let calendar = Calendar.current
        let components = calendar.dateComponents([.year, .month], from: date)
        return calendar.date(from: components)
    }

    private func shortDateText(_ localDate: String) -> String {
        guard let date = Self.localDateFormatter.date(from: localDate) else {
            return localDate
        }
        let formatter = DateFormatter()
        formatter.locale = locale
        formatter.calendar = Calendar(identifier: .gregorian)
        formatter.dateFormat = "M/d"
        return formatter.string(from: date)
    }

    private func fullDateText(_ localDate: String) -> String {
        guard let date = Self.localDateFormatter.date(from: localDate) else {
            return localDate
        }
        let formatter = DateFormatter()
        formatter.locale = locale
        formatter.calendar = Calendar(identifier: .gregorian)
        formatter.dateFormat = "yyyy년 M월 d일"
        return formatter.string(from: date)
    }

    private func recordRoutineMeta(_ row: GuardianRecordRoutineRow) -> String {
        var parts = [row.statusText]
        if let completedAt = row.completedAt {
            parts.append("\(timeText(completedAt)) 완료")
        }
        if let availabilityText = row.availabilityText {
            parts.append(availabilityText)
        }
        return parts.joined(separator: " · ")
    }

    private func recordRoutineAccessibilityValue(_ row: GuardianRecordRoutineRow) -> String {
        var parts = [row.statusText]
        if let availabilityText = row.availabilityText {
            parts.append(availabilityText)
        }
        if let completedAt = row.completedAt {
            parts.append("\(timeText(completedAt))에 완료")
        }
        return parts.joined(separator: ", ")
    }

    private func timeText(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.locale = locale
        formatter.timeStyle = .short
        formatter.dateStyle = .none
        return formatter.string(from: date)
    }

    private static let localDateFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.calendar = Calendar(identifier: .gregorian)
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = TimeZone(secondsFromGMT: 0)
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter
    }()

    private func updateSettings(_ transform: @escaping (AppSettings) throws -> AppSettings) {
        viewModel.updateFeedbackSettings(transform)
    }

    private func toggleLeadTime(_ leadTime: Int) {
        var leadTimes = Set(currentSettings.notificationLeadTimes)
        if leadTimes.contains(leadTime) {
            leadTimes.remove(leadTime)
        } else {
            leadTimes.insert(leadTime)
        }
        let orderedLeadTimes = [10, 5].filter(leadTimes.contains)
        updateSettings { try $0.replacing(notificationLeadTimes: orderedLeadTimes) }
    }

    private var ttsRateBinding: Binding<Double> {
        Binding(
            get: { currentSettings.ttsRate },
            set: { newValue in
                updateSettings { try $0.replacing(ttsRate: newValue) }
                onInteraction()
            }
        )
    }

    private var ttsVolumeBinding: Binding<Double> {
        Binding(
            get: { currentSettings.ttsVolume },
            set: { newValue in
                updateSettings { try $0.replacing(ttsVolume: newValue) }
                onInteraction()
            }
        )
    }

    private var quietHoursStartBinding: Binding<Date> {
        Binding(
            get: {
                date(for: currentSettings.quietHoursStart ?? (try? LocalTime(hour: 21, minute: 0)))
            },
            set: { newValue in
                let start = localTime(from: newValue) ?? currentSettings.quietHoursStart
                updateSettings {
                    try $0.replacing(
                        quietHours: (
                            start,
                            currentSettings.quietHoursEnd ?? (try LocalTime(hour: 7, minute: 0))
                        )
                    )
                }
                onInteraction()
            }
        )
    }

    private var quietHoursEndBinding: Binding<Date> {
        Binding(
            get: {
                date(for: currentSettings.quietHoursEnd ?? (try? LocalTime(hour: 7, minute: 0)))
            },
            set: { newValue in
                let end = localTime(from: newValue) ?? currentSettings.quietHoursEnd
                updateSettings {
                    try $0.replacing(
                        quietHours: (
                            currentSettings.quietHoursStart ?? (try LocalTime(hour: 21, minute: 0)),
                            end
                        )
                    )
                }
                onInteraction()
            }
        )
    }

    private func date(for time: LocalTime?) -> Date {
        Calendar.current.date(
            from: DateComponents(hour: time?.hour ?? 7, minute: time?.minute ?? 0)
        ) ?? .now
    }

    private func localTime(from date: Date) -> LocalTime? {
        let components = Calendar.current.dateComponents([.hour, .minute], from: date)
        guard let hour = components.hour, let minute = components.minute else { return nil }
        return try? LocalTime(hour: hour, minute: minute)
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

    private func routineSetStepDraftBinding(isWide: Bool) -> Binding<RoutineSetStepDraft?> {
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

    private var routineSetStepTitleBinding: Binding<String> {
        Binding(
            get: { viewModel.routineSetStepDraft?.title ?? "" },
            set: {
                viewModel.routineSetStepDraft?.title = $0
                onInteraction()
            }
        )
    }

    private var routineSetStepIconBinding: Binding<RoutineIconName> {
        Binding(
            get: { viewModel.routineSetStepDraft?.iconName ?? .star },
            set: {
                viewModel.routineSetStepDraft?.iconName = $0
                selectedRoutineSetStepPhotoItem = nil
                onInteraction()
            }
        )
    }

    private var defaultScheduledTime: LocalTime {
        try! LocalTime(hour: 7, minute: 30)
    }

    private var routineSetStepScheduledDateBinding: Binding<Date> {
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
                    viewModel.routineSetStepDraft?.scheduledTime = try? LocalTime(hour: hour, minute: minute)
                }
                onInteraction()
            }
        )
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
                selectedRoutinePhotoItem = nil
                onInteraction()
            }
        )
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

    private func reorderGesture(for routine: Routine) -> some Gesture {
        LongPressGesture(minimumDuration: 0.18)
            .sequenced(before: DragGesture(minimumDistance: 8))
            .onChanged { value in
                switch value {
                case .first(true):
                    draggedRoutineID = routine.id
                    let currentOrder = viewModel.routines.map(\.id)
                    routineDragOrder = currentOrder
                    dragStartIndex = currentOrder.firstIndex(of: routine.id)
                    onInteraction()
                case .second(true, let drag?):
                    guard let startIndex = dragStartIndex,
                          var order = routineDragOrder,
                          let currentIndex = order.firstIndex(of: routine.id)
                    else { return }
                    let step = Int((drag.translation.height / 104).rounded())
                    let destination = min(max(startIndex + step, 0), order.count - 1)
                    guard currentIndex != destination else { return }

                    let movedID = order.remove(at: currentIndex)
                    order.insert(movedID, at: destination)
                    routineDragOrder = order
                default:
                    break
                }
            }
            .onEnded { _ in
                if let routineDragOrder,
                   routineDragOrder != viewModel.routines.map(\.id) {
                    viewModel.saveRoutineOrder(orderedIDs: routineDragOrder)
                    onInteraction()
                }
                draggedRoutineID = nil
                routineDragOrder = nil
                dragStartIndex = nil
            }
    }
}

private struct RoutineCameraCaptureButton: View {
    @State private var isCameraPresented = false
    @State private var isCameraUnavailableAlertPresented = false
    let onImagePicked: (UIImage) -> Void
    let onInteraction: () -> Void

    var body: some View {
        SteppieButton("사진 촬영", role: .secondary) {
            if UIImagePickerController.isSourceTypeAvailable(.camera) {
                isCameraPresented = true
            } else {
                isCameraUnavailableAlertPresented = true
            }
            onInteraction()
        }
        .accessibilityHint(
            Text(
                UIImagePickerController.isSourceTypeAvailable(.camera)
                    ? "카메라로 활동 사진을 촬영합니다"
                    : "이 기기에서는 카메라를 사용할 수 없습니다"
            )
        )
        .fullScreenCover(isPresented: $isCameraPresented) {
            RoutineCameraPicker(
                onImagePicked: { image in
                    isCameraPresented = false
                    onImagePicked(image)
                },
                onCancel: {
                    isCameraPresented = false
                }
            )
            .ignoresSafeArea()
        }
        .alert("카메라를 사용할 수 없어요", isPresented: $isCameraUnavailableAlertPresented) {
            Button("확인", role: .cancel) {}
        } message: {
            Text("시뮬레이터 또는 카메라가 없는 기기에서는 사진 촬영을 사용할 수 없습니다.")
        }
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
