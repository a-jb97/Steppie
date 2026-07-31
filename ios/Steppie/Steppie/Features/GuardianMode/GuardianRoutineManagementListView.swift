import SwiftUI

struct GuardianRoutineManagementListView: View {
    @Environment(\.locale) private var locale
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    @State private var draggedRoutineID: UUID?
    @State private var routineDragOrder: [UUID]?
    @State private var dragStartIndex: Int?

    let isWide: Bool
    let viewModel: GuardianModeViewModel
    let onDone: () -> Void
    let onInteraction: () -> Void

    var body: some View {
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
                        editableRoutineRow(routine)
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
                .accessibilityLabel(
                    Text("\(routineSetTitle(routineSet)) 매일 \(startTime.description) 시작, 시간 변경")
                )

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
                    .frame(
                        minWidth: SteppieLayout.guardianMinimumTouchTarget,
                        minHeight: SteppieLayout.guardianMinimumTouchTarget
                    )
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
            .frame(
                minWidth: SteppieLayout.guardianMinimumTouchTarget,
                minHeight: SteppieLayout.guardianMinimumTouchTarget
            )
            .accessibilityLabel(
                Text(viewModel.isEditingRoutineSets ? "루틴 세트 편집 완료" : "루틴 세트 편집")
            )
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

    private func editableRoutineRow(_ routine: Routine) -> some View {
        adaptiveCardStack(spacing: SteppieSpacing.small) {
            Button {
                viewModel.beginEditRoutine(routine)
                onInteraction()
            } label: {
                adaptiveCardStack(spacing: SteppieSpacing.small) {
                    RoundedRectangle(cornerRadius: 10)
                        .fill(SteppieCardColor(colorToken: routine.colorToken).color)
                        .frame(
                            width: dynamicTypeSize.isAccessibilitySize ? 68 : 20,
                            height: dynamicTypeSize.isAccessibilitySize ? 20 : 68
                        )
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
                    lineWidth: draggedRoutineID == routine.id
                        ? SteppieStroke.focus
                        : SteppieStroke.divider
                )
        }
        .clipShape(.rect(cornerRadius: SteppieCornerRadius.card))
        .contextMenu {
            Button("수정") { viewModel.beginEditRoutine(routine) }
            Button("삭제", role: .destructive) { viewModel.requestDelete(routine) }
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
        .padding(.bottom, SteppieSpacing.small)
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

    private var displayedRoutines: [Routine] {
        guard let routineDragOrder else { return viewModel.routines }
        let routinesByID = Dictionary(uniqueKeysWithValues: viewModel.routines.map { ($0.id, $0) })
        let orderedRoutines = routineDragOrder.compactMap { routinesByID[$0] }
        return orderedRoutines.count == viewModel.routines.count ? orderedRoutines : viewModel.routines
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
