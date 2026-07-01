import SwiftUI

struct GuardianModeView: View {
    @Environment(\.locale) private var locale
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @State private var draggedRoutineID: UUID?
    @State private var dragStartIndex: Int?
    @State private var lastDragStep = 0
    let viewModel: GuardianModeViewModel
    let onDone: () -> Void
    let onInteraction: () -> Void

    var body: some View {
        GeometryReader { proxy in
            content(isWide: proxy.size.width >= SteppieLayout.splitMinimumWidth)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(Color.steppieBackgroundSecondary)
                .onTapGesture(perform: onInteraction)
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
        .sheet(item: routineSetNameDraftBinding) { _ in
            NavigationStack {
                routineSetNameEditor
            }
            .presentationDetents([.medium])
        }
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
                    homeList
                        .frame(width: SteppieLayout.splitListWidth)
                    Rectangle()
                        .fill(Color.steppieBorderSubtle)
                        .frame(width: SteppieStroke.divider)
                    destinationDetail(isWide: true)
                }
            } else if isWide {
                homeList
                    .frame(maxWidth: 393)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                NavigationStack {
                    destinationDetail(isWide: false)
                }
            }
        }
    }

    private var homeList: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: SteppieSpacing.medium) {
                header(title: "보호자 모드", subtitle: "완료 또는 3분 미조작 시 아이 모드로 돌아갑니다")
                menuCard(
                    title: "루틴 세트 생성",
                    subtitle: "새 루틴 제목과 단계 목록 만들기",
                    assetName: "guardian-menu-routine",
                    destination: .routineSetCreator,
                    isEnabled: true
                )
                menuCard(
                    title: "템플릿에서 시작하기",
                    subtitle: "아침, 학교, 취침 루틴으로 빠르게 만들기",
                    assetName: "guardian-menu-routine",
                    destination: .routineTemplates,
                    isEnabled: true
                )
                menuCard(
                    title: "루틴 관리",
                    subtitle: viewModel.hasRoutineSets ? "루틴 세트 목록, 활동 추가, 순서 변경" : "먼저 루틴 세트를 만들어 주세요",
                    assetName: "guardian-menu-routine",
                    destination: .routineEditor,
                    isEnabled: viewModel.hasRoutineSets
                )
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
                SteppieButton("완료", role: .secondary, action: onDone)
                    .padding(.top, SteppieSpacing.medium)
            }
            .padding(SteppieLayout.guardianScreenPadding)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.steppieBackgroundSecondary)
    }

    @ViewBuilder
    private func destinationDetail(isWide: Bool) -> some View {
        if !isWide, viewModel.selectedDestination == nil {
            phoneHome
        } else {
            switch viewModel.selectedDestination ?? .security {
            case .routineSetCreator:
                routineSetCreator(isWide: isWide)
            case .routineTemplates:
                routineTemplateSelector(isWide: isWide)
            case .routineEditor:
                routineEditor(isWide: isWide)
            case .feedbackSettings:
                feedbackSettingsScreen(isWide: isWide)
            case .records:
                recordsScreen(isWide: isWide)
            case .security:
                securityScreen(isWide: isWide)
            case .backupRestore:
                BackupRestoreView(
                    viewModel: viewModel,
                    onDone: onDone,
                    onInteraction: onInteraction
                )
                .toolbar {
                    if !isWide {
                        ToolbarItem(placement: .topBarLeading) {
                            Button("뒤로") { viewModel.selectedDestination = .security }
                        }
                    }
                }
            }
        }
    }

    private var phoneHome: some View {
        homeList
            .navigationBarTitleDisplayMode(.inline)
            .toolbar(.hidden, for: .navigationBar)
    }

    private func menuCard(
        title: String,
        subtitle: String,
        assetName: String,
        destination: GuardianDestination,
        isEnabled: Bool
    ) -> some View {
        Button {
            guard isEnabled else { return }
            if destination == .routineTemplates {
                viewModel.beginTemplateSelection()
            } else {
                viewModel.selectedDestination = destination
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

    private func routineEditor(isWide: Bool) -> some View {
        HStack(spacing: 0) {
            routineListPane(isWide: isWide)
            if isWide {
                Rectangle()
                    .fill(Color.steppieBorderSubtle)
                    .frame(width: SteppieStroke.divider)
                cardEditorPane
                    .frame(maxWidth: .infinity)
            }
        }
    }

    private func routineSetCreator(isWide: Bool) -> some View {
        HStack(spacing: 0) {
            routineSetStepListPane(isWide: isWide)
            if isWide {
                Rectangle()
                    .fill(Color.steppieBorderSubtle)
                    .frame(width: SteppieStroke.divider)
                routineSetStepEditorPane
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
                ToolbarItem(placement: .topBarLeading) {
                    Button("뒤로") {
                        viewModel.cancelRoutineSetDraft()
                        viewModel.selectedDestination = nil
                    }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button("완료", action: onDone)
                }
            }
        }
        .sheet(item: routineSetStepDraftBinding) { _ in
            NavigationStack {
                routineSetStepEditorPane
            }
            .presentationDetents([.large])
        }
    }

    private func routineSetStepRow(_ step: RoutineSetStepDraft) -> some View {
        adaptiveCardStack(spacing: SteppieSpacing.small) {
            RoundedRectangle(cornerRadius: 10)
                .fill(SteppieCardColor(colorToken: step.colorToken).color)
                .frame(width: dynamicTypeSize.isAccessibilitySize ? 68 : 20, height: dynamicTypeSize.isAccessibilitySize ? 20 : 68)
                .accessibilityHidden(true)
            SteppieRoutineIcon(step.iconName, size: .list)
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
                    Picker("아이콘", selection: routineSetStepIconBinding) {
                        ForEach(RoutineIconName.allCases) { icon in
                            Text(icon.rawValue).tag(icon)
                        }
                    }
                    .pickerStyle(.menu)
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
                    adaptiveControlStack {
                        DatePicker(
                            "예정 시각",
                            selection: routineSetStepScheduledDateBinding,
                            displayedComponents: .hourAndMinute
                        )
                        .accessibilityHint(Text("a11y.guardian.scheduledTime.selectHint"))
                        if !dynamicTypeSize.isAccessibilitySize {
                            Spacer()
                        }
                        Button("시간 없음") {
                            viewModel.routineSetStepDraft?.scheduledTime = nil
                            onInteraction()
                        }
                        .buttonStyle(.borderless)
                        .frame(minHeight: SteppieLayout.guardianMinimumTouchTarget)
                        .accessibilityHint(Text("a11y.guardian.scheduledTime.clearHint"))
                    }
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
                ForEach(viewModel.routineSets) { routineSet in
                    routineSetRow(routineSet)
                        .listRowInsets(routineListRowInsets)
                        .listRowSeparator(.hidden)
                        .listRowBackground(Color.clear)
                }
            } header: {
                routineSetListHeader
            }

            Section {
                if viewModel.routines.isEmpty {
                    messageState(title: "활동이 없어요", message: "활동 추가로 첫 루틴을 만들어 주세요.")
                        .listRowInsets(routineListRowInsets)
                        .listRowSeparator(.hidden)
                        .listRowBackground(Color.clear)
                } else {
                    ForEach(viewModel.routines) { routine in
                        editableRoutineRow(routine, isWide: isWide)
                            .listRowInsets(routineListRowInsets)
                            .listRowSeparator(.hidden)
                            .listRowBackground(Color.clear)
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
            } header: {
                header(
                    title: routineEditorTitle,
                    subtitle: "각 루틴을 선택하면 해당 루틴을 수정할 수 있습니다."
                )
                .padding(.horizontal, SteppieLayout.guardianScreenPadding)
                .textCase(nil)
            }
            Section {
                SteppieButton("템플릿에서 시작하기", role: .secondary) {
                    viewModel.beginTemplateSelection(returnDestination: .routineEditor)
                    onInteraction()
                }
                    .padding(.top, SteppieSpacing.medium)
                    .listRowInsets(routineListRowInsets)
                    .listRowSeparator(.hidden)
                    .listRowBackground(Color.clear)
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
                ToolbarItem(placement: .topBarLeading) {
                    Button("뒤로") { viewModel.selectedDestination = nil }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button("완료", action: onDone)
                }
            }
        }
        .sheet(item: draftBinding) { _ in
            NavigationStack {
                cardEditorPane
            }
            .presentationDetents([.large])
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
                ForEach(viewModel.routineTemplates) { template in
                    routineTemplateRow(template)
                        .listRowInsets(routineListRowInsets)
                        .listRowSeparator(.hidden)
                        .listRowBackground(Color.clear)
                }
            } header: {
                header(
                    title: "템플릿에서 시작하기",
                    subtitle: "템플릿을 고른 뒤 새 루틴 세트로 저장합니다"
                )
                .padding(.top, SteppieSpacing.medium)
                .padding(.horizontal, SteppieLayout.guardianScreenPadding)
                .textCase(nil)
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
                ToolbarItem(placement: .topBarLeading) {
                    Button("뒤로") {
                        viewModel.closeTemplateSelection()
                        onInteraction()
                    }
                }
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
                    VStack(alignment: .leading, spacing: SteppieSpacing.twoExtraSmall) {
                        Text(routineSetTitle(routineSet))
                            .steppieTextStyle(.button)
                            .foregroundStyle(Color.steppieTextPrimary)
                        Text(routineSet.isActive ? "아이 모드에서 사용 중" : "보관된 루틴 세트")
                            .steppieTextStyle(.guardianCaption)
                            .foregroundStyle(Color.steppieTextSecondary)
                    }
                    Spacer()
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
                .contentShape(.rect)
            }
            .buttonStyle(.plain)
            .accessibilityLabel(Text(routineSetTitle(routineSet)))
            .accessibilityValue(Text(routineSet.isActive ? "아이 모드에서 사용 중" : "보관된 루틴 세트"))
            .accessibilityHint(Text("이 루틴 세트를 편집합니다"))

            if viewModel.isEditingRoutineSets {
                routineSetEditActions(for: routineSet)
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

    private var routineSetListHeader: some View {
        HStack(alignment: .top, spacing: SteppieSpacing.medium) {
            header(
                title: "루틴 관리",
                subtitle: "편집할 루틴 세트를 선택합니다"
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

    private var routineListRowInsets: EdgeInsets {
        EdgeInsets(
            top: SteppieSpacing.extraSmall,
            leading: SteppieLayout.guardianScreenPadding,
            bottom: SteppieSpacing.extraSmall,
            trailing: SteppieLayout.guardianScreenPadding
        )
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
        }
        .padding(SteppieSpacing.small)
        .frame(maxWidth: .infinity, minHeight: 92, alignment: .leading)
        .background(Color.steppieBackgroundPrimary)
        .overlay {
            RoundedRectangle(cornerRadius: SteppieCornerRadius.card)
                .stroke(Color.steppieBorderSubtle, lineWidth: SteppieStroke.divider)
        }
        .clipShape(.rect(cornerRadius: SteppieCornerRadius.card))
        .contextMenu {
            Button("수정") { viewModel.beginEditRoutine(routine) }
            Button("삭제", role: .destructive) { viewModel.requestDelete(routine) }
        }
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
                    Picker("아이콘", selection: draftIconBinding) {
                        ForEach(RoutineIconName.allCases) { icon in
                            Text(icon.rawValue).tag(icon)
                        }
                    }
                    .pickerStyle(.menu)
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
                    adaptiveControlStack {
                        DatePicker(
                            "예정 시각",
                            selection: scheduledDateBinding,
                            displayedComponents: .hourAndMinute
                        )
                        .accessibilityHint(Text("a11y.guardian.scheduledTime.selectHint"))
                        if !dynamicTypeSize.isAccessibilitySize {
                            Spacer()
                        }
                        Button("시간 없음") {
                            viewModel.draft?.scheduledTime = nil
                            onInteraction()
                        }
                        .buttonStyle(.borderless)
                        .frame(minHeight: SteppieLayout.guardianMinimumTouchTarget)
                        .accessibilityHint(Text("a11y.guardian.scheduledTime.clearHint"))
                    }
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

                SteppieButton("완료", role: .secondary, action: onDone)
                    .padding(.top, SteppieSpacing.large)
            }
            .frame(maxWidth: SteppieLayout.focusCardTabletMaximumWidth)
            .frame(maxWidth: .infinity)
            .padding(SteppieLayout.guardianScreenPadding)
        }
        .background(Color.steppieBackgroundSecondary)
        .toolbar {
            if !isWide {
                ToolbarItem(placement: .topBarLeading) {
                    Button("뒤로") { viewModel.selectedDestination = nil }
                }
            }
        }
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
        .toolbar {
            if !isWide {
                ToolbarItem(placement: .topBarLeading) {
                    Button("뒤로") { viewModel.selectedDestination = nil }
                }
            }
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
            header(title: "진행 기록", subtitle: "날짜별 완료 수와 루틴 상태를 함께 봅니다")
            VStack(spacing: SteppieSpacing.small) {
                ForEach(viewModel.recordSummaries) { summary in
                    recordSummaryRow(summary)
                }
            }
        }
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

    private func securityScreen(isWide: Bool) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: SteppieSpacing.medium) {
                header(title: "보안", subtitle: "PIN, 복구 코드, 개인정보 설정")
                securityCard(title: "PIN 변경", subtitle: "4자리 보호자 PIN 재설정", assetName: "guardian-security-warning", iconSize: 55) {
                    NotificationCenter.default.post(name: .guardianPINChangeRequested, object: nil)
                    onInteraction()
                }
                securityCard(title: "복구 코드 확인", subtitle: "PIN 확인 후 새 6자리 복구 코드를 한 번만 표시", assetName: "guardian-security-warning", iconSize: 55) {
                    NotificationCenter.default.post(name: .guardianRecoveryCodeRegenerationRequested, object: nil)
                    onInteraction()
                }
                securityCard(title: "백업/복원", subtitle: "로컬 파일 백업과 Replace 복원", assetName: "guardian-security-backup", iconSize: 50) {
                    viewModel.selectedDestination = .backupRestore
                    onInteraction()
                }
                privacyPolicyNote
                SteppieButton("완료", role: .secondary, action: onDone)
                    .padding(.top, SteppieSpacing.large)
            }
            .frame(maxWidth: SteppieLayout.focusCardTabletMaximumWidth)
            .frame(maxWidth: .infinity)
            .padding(SteppieLayout.guardianScreenPadding)
        }
        .background(Color.steppieBackgroundSecondary)
        .toolbar {
            if !isWide {
                ToolbarItem(placement: .topBarLeading) {
                    Button("뒤로") { viewModel.selectedDestination = nil }
                }
            }
        }
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

    private var routineSetNameDraftBinding: Binding<RoutineSetNameDraft?> {
        Binding(
            get: { viewModel.routineSetNameDraft },
            set: { if $0 == nil { viewModel.routineSetNameDraft = nil } }
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

    private var draftBinding: Binding<RoutineDraft?> {
        Binding(
            get: { horizontalSizeClass == .compact ? viewModel.draft : nil },
            set: { if $0 == nil { viewModel.draft = nil } }
        )
    }

    private var routineSetStepDraftBinding: Binding<RoutineSetStepDraft?> {
        Binding(
            get: { horizontalSizeClass == .compact ? viewModel.routineSetStepDraft : nil },
            set: { if $0 == nil { viewModel.routineSetStepDraft = nil } }
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
                onInteraction()
            }
        )
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
                    dragStartIndex = viewModel.routines.firstIndex(where: { $0.id == routine.id })
                    lastDragStep = 0
                case .second(true, let drag?):
                    guard let startIndex = dragStartIndex else { return }
                    let step = Int((drag.translation.height / 104).rounded())
                    guard step != lastDragStep else { return }
                    let destination = min(max(startIndex + step, 0), viewModel.routines.count - 1)
                    viewModel.moveRoutine(routine, to: destination)
                    lastDragStep = step
                    onInteraction()
                default:
                    break
                }
            }
            .onEnded { _ in
                draggedRoutineID = nil
                dragStartIndex = nil
                lastDragStep = 0
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

extension Notification.Name {
    static let guardianPINChangeRequested = Notification.Name("guardianPINChangeRequested")
    static let guardianRecoveryCodeRegenerationRequested = Notification.Name("guardianRecoveryCodeRegenerationRequested")
}
