import SwiftUI
import UIKit

struct RoutineVisualView: View {
    let icon: IconRef
    let size: SteppieRoutineIconSize
    private let photoStore: any RoutinePhotoReading

    init(
        icon: IconRef,
        size: SteppieRoutineIconSize,
        photoStore: (any RoutinePhotoReading)? = nil
    ) {
        self.icon = icon
        self.size = size
        self.photoStore = photoStore ?? FileRoutinePhotoStore()
    }

    var body: some View {
        if icon.type == .builtin,
           let name = icon.name.flatMap(RoutineIconName.init(rawValue:)) {
            SteppieRoutineIcon(name, size: size)
        } else if let image = photoImage {
            Image(uiImage: image)
                .resizable()
                .scaledToFill()
                .frame(width: canvasSize, height: canvasSize)
                .clipShape(.rect(cornerRadius: cornerRadius))
                .accessibilityHidden(true)
        } else {
            photoPlaceholder
        }
    }

    private var photoImage: UIImage? {
        guard icon.type == .photo,
              let backupAssetName = icon.backupAssetName,
              let data = try? photoStore.data(forBackupAssetName: backupAssetName) else {
            return nil
        }
        return UIImage(data: data)
    }

    private var photoPlaceholder: some View {
        ZStack {
            RoundedRectangle(cornerRadius: cornerRadius)
                .fill(Color.steppieBackgroundPrimary)
            Image(systemName: "photo")
                .resizable()
                .scaledToFit()
                .foregroundStyle(Color.steppieTextSecondary)
                .padding(size == .card ? SteppieSpacing.extraLarge : SteppieSpacing.small)
        }
        .frame(width: canvasSize, height: canvasSize)
        .accessibilityHidden(true)
    }

    private var canvasSize: CGFloat {
        size == .card ? 128 : 48
    }

    private var cornerRadius: CGFloat {
        size == .card ? 32 : SteppieCornerRadius.control
    }
}
