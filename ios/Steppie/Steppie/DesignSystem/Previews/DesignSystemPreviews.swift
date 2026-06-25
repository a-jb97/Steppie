import SwiftUI

private struct DesignSystemPreview: View {
    let horizontalLayout: Bool

    var body: some View {
        ScrollView {
            Group {
                if horizontalLayout {
                    HStack(alignment: .top, spacing: SteppieSpacing.extraLarge) {
                        cardSamples
                        buttonSamples
                    }
                } else {
                    VStack(spacing: SteppieSpacing.extraLarge) {
                        cardSamples
                        buttonSamples
                    }
                }
            }
            .padding(SteppieLayout.childScreenPadding)
        }
        .background(Color.steppieBackgroundPrimary)
    }

    private var cardSamples: some View {
        VStack(spacing: SteppieSpacing.medium) {
            RoutineCard(
                title: Text("아침 준비하기"),
                metadata: Text("카드를 누르면 완료"),
                presentation: .focus,
                state: .current
            ) {} visual: {
                SteppieRoutineIcon(.wakeUp, size: .card)
            }

            RoutineCard(
                title: Text("아침 준비하기"),
                metadata: Text("1 · 지금"),
                presentation: .list,
                state: .current
            ) {} visual: {
                SteppieRoutineIcon(.wakeUp, size: .list)
            }
        }
        .frame(maxWidth: SteppieLayout.focusCardTabletMaximumWidth)
    }

    private var buttonSamples: some View {
        VStack(spacing: SteppieSpacing.medium) {
            SteppieButton("완료", size: .childLarge) {}
            SteppieButton("나중에", role: .secondary, size: .childLarge) {}
        }
        .frame(maxWidth: 360)
    }
}

#Preview("Design System · iPhone Portrait", traits: .fixedLayout(width: 393, height: 852)) {
    DesignSystemPreview(horizontalLayout: false)
}

#Preview("Design System · iPad Landscape", traits: .fixedLayout(width: 1180, height: 820)) {
    DesignSystemPreview(horizontalLayout: true)
}

#Preview("Design System · Accessibility Text", traits: .fixedLayout(width: 393, height: 852)) {
    DesignSystemPreview(horizontalLayout: false)
        .environment(\.dynamicTypeSize, .accessibility3)
}
