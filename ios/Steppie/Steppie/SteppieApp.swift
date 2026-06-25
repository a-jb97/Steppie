//
//  SteppieApp.swift
//  Steppie
//
//  Created by 전민돌 on 6/19/26.
//

import SwiftUI
import SwiftData

@main
struct SteppieApp: App {
    private let sharedModelContainer: ModelContainer
    private let routineRepository: SwiftDataRoutineRepository

    init() {
        SteppieFontRegistrar.registerBundledFonts()

        let schema = RoutinePersistenceSchema.schema
        let modelConfiguration = ModelConfiguration(schema: schema, isStoredInMemoryOnly: false)

        do {
            let container = try ModelContainer(
                for: schema,
                configurations: [modelConfiguration]
            )
            sharedModelContainer = container
            routineRepository = SwiftDataRoutineRepository(modelContainer: container)
        } catch {
            fatalError("Could not create ModelContainer: \(error)")
        }
    }

    var body: some Scene {
        WindowGroup {
            ContentView(
                repository: routineRepository,
                speechGuide: IOSRoutineSpeechGuide(),
                feedbackPerformer: IOSRoutineFeedbackPerformer()
            )
        }
        .modelContainer(sharedModelContainer)
    }
}
