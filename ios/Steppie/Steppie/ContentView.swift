import SwiftUI

struct ContentView: View {
    @State private var viewModel: ChildRoutineViewModel

    init(
        repository: any RoutineRepository,
        speechGuide: (any RoutineSpeechGuiding)? = nil,
        feedbackPerformer: (any RoutineFeedbackPerforming)? = nil
    ) {
        _viewModel = State(
            initialValue: ChildRoutineViewModel(
                repository: repository,
                speechGuide: speechGuide,
                feedbackPerformer: feedbackPerformer
            )
        )
    }

    var body: some View {
        ChildRoutineView(viewModel: viewModel)
    }
}

#Preview {
    let repository = try! RoutinePreviewStore.makeSampleRepository()
    ContentView(repository: repository)
}
