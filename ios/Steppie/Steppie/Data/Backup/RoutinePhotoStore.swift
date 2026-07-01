import Foundation
import UIKit

protocol RoutinePhotoReading {
    func data(forBackupAssetName name: String) throws -> Data?
}

protocol RoutinePhotoStoring: RoutinePhotoReading {
    func savePhotoData(_ data: Data) throws -> IconRef
}

struct FileRoutinePhotoStore: RoutinePhotoStoring {
    private let assetStore: FileBackupAssetStore

    init(fileManager: FileManager = .default) {
        assetStore = FileBackupAssetStore(fileManager: fileManager)
    }

    func data(forBackupAssetName name: String) throws -> Data? {
        try assetStore.data(forBackupAssetName: name)
    }

    func savePhotoData(_ data: Data) throws -> IconRef {
        guard let image = UIImage(data: data) else {
            throw BackupError.invalidData
        }

        let assetID = UUID()
        let backupAssetName = "routine-photo-\(assetID.uuidString.lowercased()).jpg"
        let jpegData = try compressedJPEGData(from: image)
        try assetStore.saveAssetData(jpegData, backupAssetName: backupAssetName)
        return try IconRef.photo(localAssetID: assetID, backupAssetName: backupAssetName)
    }

    private func compressedJPEGData(from image: UIImage) throws -> Data {
        let normalizedImage = image.normalizedForRoutinePhoto(maxPixelDimension: 1_200)
        let qualities: [CGFloat] = [0.82, 0.7, 0.58, 0.46]
        for quality in qualities {
            if let data = normalizedImage.jpegData(compressionQuality: quality), data.count <= 5_000_000 {
                return data
            }
        }
        guard let fallback = normalizedImage.jpegData(compressionQuality: 0.35) else {
            throw BackupError.invalidData
        }
        return fallback
    }
}

private extension UIImage {
    func normalizedForRoutinePhoto(maxPixelDimension: CGFloat) -> UIImage {
        let largestSide = max(size.width, size.height)
        guard largestSide > maxPixelDimension else { return self }

        let scale = maxPixelDimension / largestSide
        let targetSize = CGSize(width: size.width * scale, height: size.height * scale)
        let renderer = UIGraphicsImageRenderer(size: targetSize)
        return renderer.image { _ in
            draw(in: CGRect(origin: .zero, size: targetSize))
        }
    }
}
