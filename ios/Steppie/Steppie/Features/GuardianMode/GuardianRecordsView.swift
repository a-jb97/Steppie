import SwiftUI

struct GuardianRecordsView: View {
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @Environment(\.locale) private var locale

    let summaries: [GuardianRecordSummary]
    let selectedDate: String?
    let selectedDetail: GuardianRecordDetail?
    let onDateSelected: (String) -> Void
    let onCalendarRequested: () -> Void

    var body: some View {
        GeometryReader { proxy in
            let useSplit = proxy.size.width >= 720 && !dynamicTypeSize.isAccessibilitySize
            Group {
                if useSplit {
                    HStack(spacing: 0) {
                        summaryPane
                            .frame(width: 330)
                        Rectangle()
                            .fill(Color.steppieBorderSubtle)
                            .frame(width: SteppieStroke.divider)
                        detailPane
                            .frame(maxWidth: .infinity)
                    }
                } else {
                    ScrollView {
                        VStack(alignment: .leading, spacing: SteppieSpacing.medium) {
                            summaryContent
                            detailContent
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

    private var summaryPane: some View {
        ScrollView {
            summaryContent
                .padding(SteppieLayout.guardianScreenPadding)
        }
        .background(Color.steppieBackgroundSecondary)
    }

    private var detailPane: some View {
        ScrollView {
            detailContent
                .frame(maxWidth: SteppieLayout.focusCardTabletMaximumWidth)
                .frame(maxWidth: .infinity)
                .padding(SteppieLayout.guardianScreenPadding)
        }
        .background(Color.steppieBackgroundSecondary)
    }

    private var summaryContent: some View {
        VStack(alignment: .leading, spacing: SteppieSpacing.medium) {
            recordHeader
            VStack(spacing: SteppieSpacing.small) {
                ForEach(summaries) { summary in
                    summaryRow(summary)
                }
            }
        }
    }

    private var recordHeader: some View {
        HStack(alignment: .top, spacing: SteppieSpacing.small) {
            header(
                title: "진행 기록",
                subtitle: "날짜별 완료 수와 루틴 상태를 함께 봅니다"
            )
            .padding(.bottom, 0)
            Button(action: onCalendarRequested) {
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
    private var detailContent: some View {
        if let selectedDetail {
            VStack(alignment: .leading, spacing: SteppieSpacing.medium) {
                GuardianRecordStatsView(detail: selectedDetail)
                if selectedDetail.isEmpty {
                    GuardianRecordEmptyStateView(date: selectedDetail.date)
                } else {
                    VStack(alignment: .leading, spacing: SteppieSpacing.small) {
                        Text("루틴별 상태")
                            .steppieTextStyle(.guardianSection)
                            .foregroundStyle(Color.steppieTextSecondary)
                            .fixedSize(horizontal: false, vertical: true)
                        ForEach(selectedDetail.rows) { row in
                            GuardianRecordRoutineRowView(row: row)
                        }
                    }
                }
            }
        } else {
            messageState(
                title: "기록을 불러오지 못했어요",
                message: "잠시 후 다시 시도해 주세요."
            )
        }
    }

    private func summaryRow(_ summary: GuardianRecordSummary) -> some View {
        let isSelected = summary.date == selectedDate
        return Button {
            onDateSelected(summary.date)
        } label: {
            adaptiveCardStack(spacing: SteppieSpacing.small) {
                VStack(alignment: .leading, spacing: SteppieSpacing.twoExtraSmall) {
                    Text(summary.weekdaySymbol)
                        .steppieTextStyle(.guardianSection)
                        .foregroundStyle(Color.steppieTextSecondary)
                    Text(GuardianRecordFormatting.shortDateText(summary.date, locale: locale))
                        .steppieTextStyle(.guardianCaption)
                        .foregroundStyle(Color.steppieTextPrimary)
                }
                .frame(minWidth: dynamicTypeSize.isAccessibilitySize ? 0 : 48, alignment: .leading)

                GuardianRecordProgressView(
                    completedCount: summary.completedCount,
                    totalCount: summary.totalCount
                )
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
}

struct GuardianRecordStatsView: View {
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @Environment(\.locale) private var locale

    let detail: GuardianRecordDetail

    var body: some View {
        VStack(alignment: .leading, spacing: SteppieSpacing.small) {
            header
            adaptiveCardStack(spacing: SteppieSpacing.small) {
                statTile(title: "완료 수", value: "\(detail.completedCount)")
                statTile(title: "전체 수", value: "\(detail.totalCount)")
                statTile(title: "완료율", value: "\(detail.percentage)%")
            }
            GuardianRecordProgressView(
                completedCount: detail.completedCount,
                totalCount: detail.totalCount
            )
            .padding(.top, SteppieSpacing.twoExtraSmall)
            .accessibilityHidden(true)
        }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: SteppieSpacing.twoExtraSmall) {
            Text(GuardianRecordFormatting.fullDateText(detail.date, locale: locale))
                .steppieTextStyle(.guardianTitle)
                .foregroundStyle(Color.steppieTextSecondary)
                .fixedSize(horizontal: false, vertical: true)
                .accessibilityAddTraits(.isHeader)
            Text("\(detail.completedCount)/\(detail.totalCount) 완료, 완료율 \(detail.percentage)%")
                .steppieTextStyle(.guardianCaption)
                .foregroundStyle(Color.steppieTextPrimary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.bottom, SteppieSpacing.small)
    }

    private func statTile(title: String, value: String) -> some View {
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
}

struct GuardianRecordRoutineRowView: View {
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @Environment(\.locale) private var locale

    let row: GuardianRecordRoutineRow

    var body: some View {
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
                Text(recordRoutineMeta)
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
        .accessibilityValue(Text(recordRoutineAccessibilityValue))
    }

    private var recordRoutineMeta: String {
        var parts = [row.statusText]
        if let completedAt = row.completedAt {
            parts.append("\(GuardianRecordFormatting.timeText(completedAt, locale: locale)) 완료")
        }
        if let availabilityText = row.availabilityText {
            parts.append(availabilityText)
        }
        return parts.joined(separator: " · ")
    }

    private var recordRoutineAccessibilityValue: String {
        var parts = [row.statusText]
        if let availabilityText = row.availabilityText {
            parts.append(availabilityText)
        }
        if let completedAt = row.completedAt {
            parts.append("\(GuardianRecordFormatting.timeText(completedAt, locale: locale))에 완료")
        }
        return parts.joined(separator: ", ")
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
}

struct GuardianRecordProgressView: View {
    let completedCount: Int
    let totalCount: Int

    @ViewBuilder
    var body: some View {
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
}

struct GuardianRecordEmptyStateView: View {
    @Environment(\.locale) private var locale

    let date: String

    var body: some View {
        VStack(alignment: .leading, spacing: SteppieSpacing.small) {
            Image(systemName: "calendar.badge.exclamationmark")
                .font(.title2.weight(.semibold))
                .foregroundStyle(Color.steppieTextSecondary)
                .accessibilityHidden(true)
            Text("기록이 없어요")
                .steppieTextStyle(.guardianSection)
                .foregroundStyle(Color.steppieTextSecondary)
            Text("\(GuardianRecordFormatting.fullDateText(date, locale: locale))에는 완료 기록이 없습니다.")
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
}

enum GuardianRecordFormatting {
    static func date(from localDate: String) -> Date? {
        localDateFormatter.date(from: localDate)
    }

    static func shortDateText(_ localDate: String, locale: Locale) -> String {
        guard let date = localDateFormatter.date(from: localDate) else {
            return localDate
        }
        let formatter = DateFormatter()
        formatter.locale = locale
        formatter.calendar = Calendar(identifier: .gregorian)
        formatter.dateFormat = "M/d"
        return formatter.string(from: date)
    }

    static func fullDateText(_ localDate: String, locale: Locale) -> String {
        guard let date = localDateFormatter.date(from: localDate) else {
            return localDate
        }
        let formatter = DateFormatter()
        formatter.locale = locale
        formatter.calendar = Calendar(identifier: .gregorian)
        formatter.dateFormat = "yyyy년 M월 d일"
        return formatter.string(from: date)
    }

    static func timeText(_ date: Date, locale: Locale) -> String {
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
}
