import SwiftUI

struct RoutineVisualView: View {
    let icon: IconRef
    let size: SteppieRoutineIconSize

    var body: some View {
        if icon.type == .builtin,
           let name = icon.name.flatMap(RoutineIconName.init(rawValue:)) {
            SteppieRoutineIcon(name, size: size)
        } else {
            photoPlaceholder
        }
    }

    private var photoPlaceholder: some View {
        ZStack {
            RoundedRectangle(cornerRadius: size == .card ? 32 : SteppieCornerRadius.control)
                .fill(Color.steppieBackgroundPrimary)
            Image(systemName: "photo")
                .resizable()
                .scaledToFit()
                .foregroundStyle(Color.steppieTextSecondary)
                .padding(size == .card ? SteppieSpacing.extraLarge : SteppieSpacing.small)
        }
        .accessibilityHidden(true)
    }
}
