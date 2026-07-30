import SwiftUI

struct GuardianFeedbackSettingsView: View {
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    let settings: AppSettings
    let updateSettings: ((AppSettings) throws -> AppSettings) -> Void
    let onDone: () -> Void
    let onInteraction: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: SteppieSpacing.medium) {
                header

                settingsSegmentBlock(
                    title: "칭찬 애니메이션",
                    options: feedbackIntensityOptions,
                    selected: settings.feedbackIntensity,
                    label: feedbackIntensityLabel
                ) { intensity in
                    updateSettings { try $0.replacing(feedbackIntensity: intensity) }
                }

                settingsSegmentBlock(
                    title: "음성 안내",
                    options: [true, false],
                    selected: settings.ttsEnabled,
                    label: enabledLabel
                ) { isEnabled in
                    updateSettings { try $0.replacing(ttsEnabled: isEnabled) }
                }

                settingsSliderBlock(
                    title: "음성 속도",
                    valueText: String(format: "%.1fx", settings.ttsRate),
                    value: ttsRateBinding,
                    range: 0.5...1.5
                )

                settingsSliderBlock(
                    title: "음성 볼륨",
                    valueText: "\(Int((settings.ttsVolume * 100).rounded()))%",
                    value: ttsVolumeBinding,
                    range: 0.0...1.0
                )

                settingsSegmentBlock(
                    title: "효과음",
                    options: [true, false],
                    selected: settings.soundEnabled,
                    label: enabledLabel
                ) { isEnabled in
                    updateSettings { try $0.replacing(soundEnabled: isEnabled) }
                }

                settingsSegmentBlock(
                    title: "햅틱/진동",
                    options: [true, false],
                    selected: settings.hapticEnabled,
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

    private var header: some View {
        VStack(alignment: .leading, spacing: SteppieSpacing.twoExtraSmall) {
            Text("환경 설정")
                .steppieTextStyle(.guardianTitle)
                .foregroundStyle(Color.steppieTextSecondary)
                .fixedSize(horizontal: false, vertical: true)
                .accessibilityAddTraits(.isHeader)
            Text("자극 강도를 아이에게 맞춥니다")
                .steppieTextStyle(.guardianCaption)
                .foregroundStyle(Color.steppieTextPrimary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.bottom, SteppieSpacing.small)
    }

    private var notificationLeadTimeBlock: some View {
        let leadTimes = settings.notificationLeadTimes
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
                                    settings.quietHoursStart ?? (try LocalTime(hour: 21, minute: 0)),
                                    settings.quietHoursEnd ?? (try LocalTime(hour: 7, minute: 0))
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
        settings.quietHoursStart != nil && settings.quietHoursEnd != nil
    }

    private var quietHoursSummary: String {
        guard let start = settings.quietHoursStart,
              let end = settings.quietHoursEnd else {
            return "꺼짐"
        }
        return "\(start)-\(end)"
    }

    private func toggleLeadTime(_ leadTime: Int) {
        var leadTimes = Set(settings.notificationLeadTimes)
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
            get: { settings.ttsRate },
            set: { newValue in
                updateSettings { try $0.replacing(ttsRate: newValue) }
                onInteraction()
            }
        )
    }

    private var ttsVolumeBinding: Binding<Double> {
        Binding(
            get: { settings.ttsVolume },
            set: { newValue in
                updateSettings { try $0.replacing(ttsVolume: newValue) }
                onInteraction()
            }
        )
    }

    private var quietHoursStartBinding: Binding<Date> {
        Binding(
            get: {
                date(for: settings.quietHoursStart ?? (try? LocalTime(hour: 21, minute: 0)))
            },
            set: { newValue in
                let start = localTime(from: newValue) ?? settings.quietHoursStart
                updateSettings {
                    try $0.replacing(
                        quietHours: (
                            start,
                            settings.quietHoursEnd ?? (try LocalTime(hour: 7, minute: 0))
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
                date(for: settings.quietHoursEnd ?? (try? LocalTime(hour: 7, minute: 0)))
            },
            set: { newValue in
                let end = localTime(from: newValue) ?? settings.quietHoursEnd
                updateSettings {
                    try $0.replacing(
                        quietHours: (
                            settings.quietHoursStart ?? (try LocalTime(hour: 21, minute: 0)),
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
            VStack(spacing: SteppieSpacing.twoExtraSmall, content: content)
        } else {
            HStack(spacing: SteppieSpacing.twoExtraSmall, content: content)
        }
    }
}
