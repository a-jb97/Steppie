import SwiftUI
import UIKit

struct RoutineCameraCaptureButton: View {
    @State private var isCameraPresented = false
    @State private var isCameraUnavailableAlertPresented = false
    let onImagePicked: (UIImage) -> Void
    let onInteraction: () -> Void

    var body: some View {
        SteppieButton("사진 촬영", role: .secondary) {
            if UIImagePickerController.isSourceTypeAvailable(.camera) {
                isCameraPresented = true
            } else {
                isCameraUnavailableAlertPresented = true
            }
            onInteraction()
        }
        .accessibilityHint(
            Text(
                UIImagePickerController.isSourceTypeAvailable(.camera)
                    ? "카메라로 활동 사진을 촬영합니다"
                    : "이 기기에서는 카메라를 사용할 수 없습니다"
            )
        )
        .fullScreenCover(isPresented: $isCameraPresented) {
            RoutineCameraPicker(
                onImagePicked: { image in
                    isCameraPresented = false
                    onImagePicked(image)
                },
                onCancel: {
                    isCameraPresented = false
                }
            )
            .ignoresSafeArea()
        }
        .alert("카메라를 사용할 수 없어요", isPresented: $isCameraUnavailableAlertPresented) {
            Button("확인", role: .cancel) {}
        } message: {
            Text("시뮬레이터 또는 카메라가 없는 기기에서는 사진 촬영을 사용할 수 없습니다.")
        }
    }
}
