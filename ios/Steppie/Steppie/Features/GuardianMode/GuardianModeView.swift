import SwiftUI

struct GuardianModeView: View {
    @Environment(\.locale) private var locale
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass
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
                    isEnabled: false
                )
                menuCard(
                    title: "진행 기록",
                    subtitle: "날짜별 완료 현황",
                    assetName: "guardian-menu-records",
                    destination: .records,
                    isEnabled: false
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
            case .routineEditor:
                routineEditor(isWide: isWide)
            case .feedbackSettings:
                unavailableScreen(title: "환경 설정", subtitle: "Sprint 7 이후 상세 구현 예정입니다")
            case .records:
                unavailableScreen(title: "진행 기록", subtitle: "Sprint 7 이후 상세 구현 예정입니다")
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
            viewModel.selectedDestination = destination
            onInteraction()
        } label: {
            HStack(spacing: 14) {
                Image(assetName)
                    .resizable()
                    .scaledToFit()
                    .frame(width: 52, height: 52)
                    .accessibilityHidden(true)
                VStack(alignment: .leading, spacing: SteppieSpacing.twoExtraSmall) {
                    Text(title)
                        .steppieTextStyle(.guardianSection)
                        .foregroundStyle(Color.steppieTextSecondary)
                    Text(subtitle)
                        .steppieTextStyle(.guardianCaption)
                        .foregroundStyle(Color.steppieTextPrimary)
                }
                Spacer()
                if !isEnabled {
                    Text("준비 중")
                        .steppieTextStyle(.guardianCaption)
                        .foregroundStyle(Color.steppieTextSecondary)
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
        HStack(spacing: SteppieSpacing.small) {
            RoundedRectangle(cornerRadius: 10)
                .fill(SteppieCardColor(colorToken: step.colorToken).color)
                .frame(width: 20, height: 68)
            SteppieRoutineIcon(step.iconName, size: .list)
                .frame(width: 52, height: 52)
            VStack(alignment: .leading, spacing: SteppieSpacing.twoExtraSmall) {
                Text(step.title)
                    .steppieTextStyle(.button)
                    .foregroundStyle(Color.steppieTextPrimary)
                Text(step.scheduledTime?.description ?? "시간 없음")
                    .steppieTextStyle(.guardianCaption)
                    .foregroundStyle(Color.steppieTextSecondary)
            }
            .contentShape(.rect)
            .onTapGesture {
                viewModel.beginEditRoutineSetStep(step)
                onInteraction()
            }
            Spacer()
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
                        }
                    }
                }
                labeledCard("예정 시각") {
                    HStack {
                        DatePicker(
                            "예정 시각",
                            selection: routineSetStepScheduledDateBinding,
                            displayedComponents: .hourAndMinute
                        )
                        .labelsHidden()
                        Spacer()
                        Button("시간 없음") {
                            viewModel.routineSetStepDraft?.scheduledTime = nil
                            onInteraction()
                        }
                        .buttonStyle(.borderless)
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
                SteppieButton("템플릿에서 시작하기", role: .secondary, state: .disabled) {}
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
        HStack(spacing: SteppieSpacing.small) {
            Button {
                viewModel.beginEditRoutine(routine)
                onInteraction()
            } label: {
                HStack(spacing: SteppieSpacing.small) {
                    RoundedRectangle(cornerRadius: 10)
                        .fill(SteppieCardColor(colorToken: routine.colorToken).color)
                        .frame(width: 20, height: 68)
                    RoutineVisualView(icon: routine.icon, size: .list)
                        .frame(width: 52, height: 52)
                    VStack(alignment: .leading, spacing: SteppieSpacing.twoExtraSmall) {
                        Text(viewModel.localizedTitle(for: routine))
                            .steppieTextStyle(.button)
                            .foregroundStyle(Color.steppieTextPrimary)
                        Text(routine.scheduledTime?.description ?? "시간 없음")
                            .steppieTextStyle(.guardianCaption)
                            .foregroundStyle(Color.steppieTextSecondary)
                    }
                    Spacer()
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
                .contentShape(.rect)
            }
            .buttonStyle(.plain)
            .accessibilityLabel(Text(viewModel.localizedTitle(for: routine)))
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
                        }
                    }
                }
                labeledCard("예정 시각") {
                    HStack {
                        DatePicker(
                            "예정 시각",
                            selection: scheduledDateBinding,
                            displayedComponents: .hourAndMinute
                        )
                        .labelsHidden()
                        Spacer()
                        Button("시간 없음") {
                            viewModel.draft?.scheduledTime = nil
                            onInteraction()
                        }
                        .buttonStyle(.borderless)
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

    private func securityScreen(isWide: Bool) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: SteppieSpacing.medium) {
                header(title: "보안", subtitle: "PIN, 복구 코드, 개인정보 설정")
                securityCard(title: "PIN 변경", subtitle: "4자리 보호자 PIN 재설정", assetName: "guardian-security-warning", iconSize: 55) {
                    NotificationCenter.default.post(name: .guardianPINChangeRequested, object: nil)
                    onInteraction()
                }
                securityCard(title: "복구 코드 확인", subtitle: "6자리 복구 코드. 원본은 저장하지 않음", assetName: "guardian-security-warning", iconSize: 55) {}
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
            HStack(spacing: SteppieSpacing.small) {
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
                    Text(subtitle)
                        .steppieTextStyle(.guardianCaption)
                        .foregroundStyle(Color.steppieTextPrimary)
                }
                Spacer()
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

    private func warningNote(_ text: String) -> some View {
        HStack(spacing: SteppieSpacing.small) {
            Image(systemName: "exclamationmark.triangle.fill")
                .foregroundStyle(Color.steppieWarning)
                .font(.title2)
            Text(text)
                .steppieTextStyle(.guardianCaption)
                .foregroundStyle(Color.steppieTextPrimary)
        }
        .padding(SteppieSpacing.small)
        .frame(maxWidth: .infinity, minHeight: 62, alignment: .leading)
        .background(Color.steppieCardLemon)
        .overlay {
            RoundedRectangle(cornerRadius: SteppieCornerRadius.control)
                .stroke(Color.steppieWarning, lineWidth: SteppieStroke.divider)
        }
        .clipShape(.rect(cornerRadius: SteppieCornerRadius.control))
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
}
