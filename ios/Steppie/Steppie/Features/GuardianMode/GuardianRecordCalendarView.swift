import SwiftUI

struct GuardianRecordCalendarPreparation {
    let recordDates: Set<String>
    let selectedDate: String?
}

struct GuardianRecordCalendarView: View {
    @Environment(\.locale) private var locale

    @State private var visibleMonth = Date()
    @State private var pickerYear = Calendar.current.component(.year, from: Date())
    @State private var pickerMonth = Calendar.current.component(.month, from: Date())
    @State private var isDatePickerPresented = false

    let isWide: Bool
    let recordDates: Set<String>
    let selectedDate: String?
    let selectedDetail: GuardianRecordDetail?
    let prepareCalendar: () -> GuardianRecordCalendarPreparation
    let onDateSelected: (String) -> Void
    let onClose: () -> Void
    let onInteraction: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                HStack(alignment: .top, spacing: SteppieSpacing.small) {
                    header

                    if isWide {
                        Button {
                            onClose()
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

                calendar
                    .padding(.bottom, SteppieSpacing.small * 3)

                if let selectedDetail {
                    detailContent(selectedDetail)
                } else if let selectedDate {
                    emptySelectedDateState(selectedDate)
                } else {
                    noRecordsState
                }
            }
            .frame(maxWidth: SteppieLayout.focusCardTabletMaximumWidth)
            .frame(maxWidth: .infinity)
            .padding(SteppieLayout.guardianScreenPadding)
        }
        .background(Color.steppieBackgroundSecondary)
        .onAppear {
            let preparation = prepareCalendar()
            syncVisibleMonth(
                recordDates: preparation.recordDates,
                selectedDate: preparation.selectedDate
            )
        }
        .sheet(isPresented: $isDatePickerPresented) {
            datePickerSheet
                .presentationDetents([.medium])
        }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: SteppieSpacing.twoExtraSmall) {
            Text("전체 기록")
                .steppieTextStyle(.guardianTitle)
                .foregroundStyle(Color.steppieTextSecondary)
                .fixedSize(horizontal: false, vertical: true)
                .accessibilityAddTraits(.isHeader)
            Text("기록이 있는 날짜만 선택할 수 있습니다")
                .steppieTextStyle(.guardianCaption)
                .foregroundStyle(Color.steppieTextPrimary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.bottom, SteppieSpacing.small)
    }

    private var calendar: some View {
        VStack(alignment: .leading, spacing: SteppieSpacing.small) {
            HStack(spacing: SteppieSpacing.small) {
                Button {
                    moveMonth(by: -1)
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
                    prepareMonthPicker()
                    isDatePickerPresented = true
                    onInteraction()
                } label: {
                    Text(monthTitle)
                        .steppieTextStyle(.guardianSection)
                        .foregroundStyle(Color.steppieTextSecondary)
                        .frame(maxWidth: .infinity, minHeight: SteppieLayout.guardianMinimumTouchTarget)
                }
                .buttonStyle(.plain)
                .accessibilityLabel(Text("\(monthTitle) 연월 선택"))
                .accessibilityHint(Text("연월 선택기를 엽니다"))
                .accessibilityAddTraits(.isHeader)

                Button {
                    moveMonth(by: 1)
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

            LazyVGrid(columns: calendarColumns, spacing: SteppieSpacing.extraSmall) {
                ForEach(weekdaySymbols, id: \.self) { symbol in
                    Text(symbol)
                        .steppieTextStyle(.guardianCaption)
                        .foregroundStyle(Color.steppieTextPrimary)
                        .frame(maxWidth: .infinity, minHeight: 28)
                }
                ForEach(Array(calendarDays.enumerated()), id: \.offset) { _, date in
                    dayCell(date)
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

    private var datePickerSheet: some View {
        NavigationStack {
            HStack(spacing: SteppieSpacing.small) {
                Picker("연도", selection: $pickerYear) {
                    ForEach(yearRange, id: \.self) { year in
                        Text(String(year) + "년").tag(year)
                    }
                }
                .pickerStyle(.wheel)
                .frame(maxWidth: .infinity)
                .clipped()

                Picker("월", selection: $pickerMonth) {
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
                        isDatePickerPresented = false
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("완료") {
                        applyPickerDate()
                    }
                }
            }
        }
    }

    private func dayCell(_ date: Date?) -> some View {
        Group {
            if let date {
                let localDate = DailyLog.localDateString(for: date, calendar: Calendar.current)
                let hasRecord = recordDates.contains(localDate)
                if hasRecord {
                    Button {
                        onDateSelected(localDate)
                        onInteraction()
                    } label: {
                        dayContent(date: date, localDate: localDate, hasRecord: true)
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(Text("\(GuardianRecordFormatting.fullDateText(localDate, locale: locale)) 기록"))
                    .accessibilityValue(Text("기록 있음"))
                    .accessibilityHint(Text("날짜 기록 보기"))
                } else {
                    dayContent(date: date, localDate: localDate, hasRecord: false)
                        .accessibilityElement(children: .ignore)
                        .accessibilityLabel(Text(GuardianRecordFormatting.fullDateText(localDate, locale: locale)))
                        .accessibilityValue(Text("기록 없음"))
                }
            } else {
                Color.clear
                    .frame(minHeight: 58)
                    .accessibilityHidden(true)
            }
        }
    }

    private func dayContent(date: Date, localDate: String, hasRecord: Bool) -> some View {
        let isSelected = selectedDate == localDate
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

    private func detailContent(_ detail: GuardianRecordDetail) -> some View {
        VStack(alignment: .leading, spacing: SteppieSpacing.medium) {
            GuardianRecordStatsView(detail: detail)
            VStack(alignment: .leading, spacing: SteppieSpacing.small) {
                Text("루틴별 상태")
                    .steppieTextStyle(.guardianSection)
                    .foregroundStyle(Color.steppieTextSecondary)
                    .fixedSize(horizontal: false, vertical: true)
                ForEach(detail.rows) { row in
                    GuardianRecordRoutineRowView(row: row)
                }
            }
        }
    }

    private func emptySelectedDateState(_ date: String) -> some View {
        let isToday = date == DailyLog.localDateString(for: Date(), calendar: Calendar.current)
        let fullDate = GuardianRecordFormatting.fullDateText(date, locale: locale)
        return VStack(alignment: .leading, spacing: SteppieSpacing.small) {
            Text(fullDate)
                .steppieTextStyle(.guardianTitle)
                .foregroundStyle(Color.steppieTextSecondary)
                .fixedSize(horizontal: false, vertical: true)
                .accessibilityAddTraits(.isHeader)
            Text(isToday ? "오늘은 아직 루틴이 없어요." : "\(fullDate)에는 완료 기록이 없습니다.")
                .steppieTextStyle(.guardianBody)
                .foregroundStyle(Color.steppieTextPrimary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .accessibilityElement(children: .combine)
    }

    private var noRecordsState: some View {
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

    private var calendarColumns: [GridItem] {
        Array(
            repeating: GridItem(.flexible(minimum: 36), spacing: SteppieSpacing.twoExtraSmall),
            count: 7
        )
    }

    private var weekdaySymbols: [String] {
        ["일", "월", "화", "수", "목", "금", "토"]
    }

    private var monthTitle: String {
        let formatter = DateFormatter()
        formatter.locale = locale
        formatter.calendar = Calendar(identifier: .gregorian)
        formatter.dateFormat = "yyyy년 M월"
        return formatter.string(from: visibleMonth)
    }

    private var calendarDays: [Date?] {
        let calendar = Calendar.current
        guard let monthStart = startOfMonth(for: visibleMonth),
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

    private func syncVisibleMonth(recordDates: Set<String>, selectedDate: String?) {
        let selected = selectedDate.flatMap { GuardianRecordFormatting.date(from: $0) }
        let latest = recordDates.sorted(by: >).first.flatMap {
            GuardianRecordFormatting.date(from: $0)
        }
        if let month = startOfMonth(for: selected ?? latest ?? Date()) {
            visibleMonth = month
        }
    }

    private func moveMonth(by value: Int) {
        if let moved = Calendar.current.date(byAdding: .month, value: value, to: visibleMonth),
           let month = startOfMonth(for: moved) {
            visibleMonth = month
        }
    }

    private var yearRange: ClosedRange<Int> {
        let currentYear = Calendar.current.component(.year, from: Date())
        let recordYears = recordDates.compactMap { localDate -> Int? in
            GuardianRecordFormatting.date(from: localDate).map {
                Calendar.current.component(.year, from: $0)
            }
        }
        let minimumYear = min(recordYears.min() ?? currentYear, currentYear) - 1
        let maximumYear = max(recordYears.max() ?? currentYear, currentYear) + 1
        return minimumYear...maximumYear
    }

    private func prepareMonthPicker() {
        let calendar = Calendar.current
        pickerYear = calendar.component(.year, from: visibleMonth)
        pickerMonth = calendar.component(.month, from: visibleMonth)
    }

    private func applyPickerDate() {
        var components = DateComponents()
        components.year = pickerYear
        components.month = pickerMonth
        components.day = 1

        if let date = Calendar.current.date(from: components),
           let month = startOfMonth(for: date) {
            visibleMonth = month
        }

        isDatePickerPresented = false
        onInteraction()
    }

    private func startOfMonth(for date: Date) -> Date? {
        let calendar = Calendar.current
        let components = calendar.dateComponents([.year, .month], from: date)
        return calendar.date(from: components)
    }
}
