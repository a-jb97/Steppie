import SwiftUI

extension RoutineIconName {
    fileprivate var category: RoutineIconCategory {
        switch self {
        case .wakeUp, .washFace, .brushTeeth, .getDressed, .breakfast, .packBag:
            .morning
        case .school, .book, .pencil, .lunch, .playground, .bus:
            .school
        case .bath, .pajamas, .storyBook, .toilet, .sleep, .star:
            .bedtime
        case .home, .meal, .snack, .medicine, .walk, .therapy, .music, .art, .cleanUp, .timer:
            .general
        }
    }
}

enum SteppieRoutineIconSize: Sendable {
    case list
    case card

    fileprivate var assetPrefix: String {
        switch self {
        case .list: "routine-icon-"
        case .card: "routine-card-icon-"
        }
    }

    fileprivate var canvasSize: CGFloat {
        switch self {
        case .list: 48
        case .card: 128
        }
    }

    fileprivate var pictogramSize: CGFloat {
        switch self {
        case .list: 40
        case .card: 96
        }
    }

    fileprivate var cornerRadius: CGFloat {
        switch self {
        case .list: SteppieCornerRadius.control
        case .card: 32
        }
    }
}

struct SteppieRoutineIcon: View {
    let name: RoutineIconName
    let size: SteppieRoutineIconSize

    init(_ name: RoutineIconName, size: SteppieRoutineIconSize) {
        self.name = name
        self.size = size
    }

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: size.cornerRadius)
                .fill(name.category.backgroundColor)

            Image(size.assetPrefix + name.rawValue)
                .resizable()
                .scaledToFit()
                .frame(width: size.pictogramSize, height: size.pictogramSize)
        }
        .frame(width: size.canvasSize, height: size.canvasSize)
        .accessibilityHidden(true)
    }
}

private enum RoutineIconCategory {
    case morning
    case school
    case bedtime
    case general

    var backgroundColor: Color {
        switch self {
        case .morning: .steppieCardPeach
        case .school: .steppieCardSky
        case .bedtime: .steppieCardLavender
        case .general: .steppieCardMint
        }
    }
}

#Preview("Routine Icons · List") {
    ScrollView {
        LazyVGrid(columns: [GridItem(.adaptive(minimum: 72))], spacing: SteppieSpacing.medium) {
            ForEach(RoutineIconName.allCases) { icon in
                VStack(spacing: SteppieSpacing.extraSmall) {
                    SteppieRoutineIcon(icon, size: .list)
                    Text(icon.rawValue)
                        .font(.caption2)
                        .multilineTextAlignment(.center)
                }
            }
        }
        .padding()
    }
    .background(Color.steppieBackgroundPrimary)
}

#Preview("Routine Icons · Card") {
    ScrollView {
        LazyVGrid(columns: [GridItem(.adaptive(minimum: 152))], spacing: SteppieSpacing.large) {
            ForEach(RoutineIconName.allCases) { icon in
                VStack(spacing: SteppieSpacing.extraSmall) {
                    SteppieRoutineIcon(icon, size: .card)
                    Text(icon.rawValue)
                        .font(.caption)
                }
            }
        }
        .padding()
    }
    .background(Color.steppieBackgroundPrimary)
}
