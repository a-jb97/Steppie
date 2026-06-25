import SwiftUI

struct ContentView: View {
    @State private var viewModel: ChildRoutineViewModel

    init(repository: any RoutineRepository) {
        _viewModel = State(
            initialValue: ChildRoutineViewModel(repository: repository)
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
