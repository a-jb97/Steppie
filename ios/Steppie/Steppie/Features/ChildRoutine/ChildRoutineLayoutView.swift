import SwiftUI

enum ChildRoutineLayout: Equatable {
    case singlePane
    case splitPane
}

enum ChildRoutineLayoutPolicy {
    static func layout(for availableWidth: CGFloat) -> ChildRoutineLayout {
        availableWidth >= SteppieLayout.splitMinimumWidth ? .splitPane : .singlePane
    }
}

struct ChildRoutineLayoutView<PhoneFocus: View, PhoneList: View, SplitList: View, SplitFocus: View>: View {
    let layout: ChildRoutineLayout
    let page: ChildRoutinePage
    let onShowList: () -> Void
    let onShowFocus: () -> Void
    private let phoneFocus: PhoneFocus
    private let phoneList: PhoneList
    private let splitList: SplitList
    private let splitFocus: SplitFocus

    init(
        layout: ChildRoutineLayout,
        page: ChildRoutinePage,
        onShowList: @escaping () -> Void,
        onShowFocus: @escaping () -> Void,
        @ViewBuilder phoneFocus: () -> PhoneFocus,
        @ViewBuilder phoneList: () -> PhoneList,
        @ViewBuilder splitList: () -> SplitList,
        @ViewBuilder splitFocus: () -> SplitFocus
    ) {
        self.layout = layout
        self.page = page
        self.onShowList = onShowList
        self.onShowFocus = onShowFocus
        self.phoneFocus = phoneFocus()
        self.phoneList = phoneList()
        self.splitList = splitList()
        self.splitFocus = splitFocus()
    }

    @ViewBuilder
    var body: some View {
        switch layout {
        case .splitPane:
            HStack(spacing: 0) {
                splitList
                    .frame(width: SteppieLayout.splitListWidth)

                Rectangle()
                    .fill(Color.steppieBorderSubtle)
                    .frame(width: SteppieStroke.divider)
                    .accessibilityHidden(true)

                splitFocus
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        case .singlePane:
            switch page {
            case .focus:
                phoneFocus
                    .contentShape(.rect)
                    .gesture(verticalSwipe(up: onShowList))
            case .list:
                phoneList
                    .contentShape(.rect)
                    .simultaneousGesture(verticalSwipe(down: onShowFocus))
            }
        }
    }

    private func verticalSwipe(
        up action: @escaping () -> Void
    ) -> some Gesture {
        DragGesture(minimumDistance: 60)
            .onEnded { value in
                guard abs(value.translation.height) > abs(value.translation.width),
                      value.translation.height < -60 else { return }
                action()
            }
    }

    private func verticalSwipe(
        down action: @escaping () -> Void
    ) -> some Gesture {
        DragGesture(minimumDistance: 60)
            .onEnded { value in
                guard abs(value.translation.height) > abs(value.translation.width),
                      value.translation.height > 60 else { return }
                action()
            }
    }
}
