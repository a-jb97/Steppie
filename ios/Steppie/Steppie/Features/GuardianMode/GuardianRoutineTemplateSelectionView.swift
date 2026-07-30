import SwiftUI

struct GuardianRoutineTemplateSelectionView: View {
    @Environment(\.locale) private var locale
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    let isWide: Bool
    let templates: [BuiltInRoutineTemplate]
    let selectedTemplate: BuiltInRoutineTemplate?
    let prepareSelection: () -> Void
    let onTemplateSelected: (BuiltInRoutineTemplate) -> Void
    let onClose: () -> Void
    let onSave: () -> Void
    let onDone: () -> Void
    let onInteraction: () -> Void

    var body: some View {
        Group {
            if isWide {
                HStack(spacing: 0) {
                    templateListPane(isWide: true)
                        .frame(width: SteppieLayout.splitListWidth)
                    Rectangle()
                        .fill(Color.steppieBorderSubtle)
                        .frame(width: SteppieStroke.divider)
                    templatePreviewPane
                        .frame(maxWidth: .infinity)
                }
            } else {
                templateListPane(isWide: false)
            }
        }
        .onAppear(perform: prepareSelection)
    }

    private func templateListPane(isWide: Bool) -> some View {
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

                ForEach(templates) { template in
                    templateRow(template)
                        .listRowInsets(routineListRowInsets)
                        .listRowSeparator(.hidden)
                        .listRowBackground(Color.clear)
                }
            }

            if !isWide {
                Section {
                    templatePreviewCard
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

    private func templateRow(_ template: BuiltInRoutineTemplate) -> some View {
        let isSelected = template.id == selectedTemplate?.id
        return Button {
            onTemplateSelected(template)
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

    private var templatePreviewPane: some View {
        ScrollView {
            templatePreviewCard
                .frame(maxWidth: SteppieLayout.focusCardTabletMaximumWidth)
                .frame(maxWidth: .infinity, alignment: .center)
                .padding(SteppieLayout.guardianScreenPadding)
        }
        .background(Color.steppieBackgroundSecondary)
    }

    @ViewBuilder
    private var templatePreviewCard: some View {
        if let selectedTemplate {
            VStack(alignment: .leading, spacing: SteppieSpacing.medium) {
                header(
                    title: localizedText(selectedTemplate.name),
                    subtitle: "\(selectedTemplate.steps.count)개 활동이 새 루틴 세트로 저장됩니다"
                )

                VStack(spacing: SteppieSpacing.small) {
                    ForEach(Array(selectedTemplate.steps.enumerated()), id: \.element.id) { order, step in
                        templateStepRow(step, order: order)
                    }
                }

                HStack(spacing: SteppieSpacing.medium) {
                    SteppieButton("뒤로", role: .secondary) {
                        onClose()
                        onInteraction()
                    }
                    SteppieButton("새 루틴 세트로 저장") {
                        onSave()
                        onInteraction()
                    }
                }
                .padding(.top, SteppieSpacing.extraSmall)
            }
        } else {
            messageState(title: "템플릿이 없어요", message: "저장할 템플릿을 찾지 못했어요.")
        }
    }

    private func templateStepRow(_ step: BuiltInRoutineTemplateStep, order: Int) -> some View {
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

    private func localizedText(_ text: LocalizedText) -> String {
        text.resolved(
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
