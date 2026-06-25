//
//  SteppieApp.swift
//  Steppie
//
//  Created by 전민돌 on 6/19/26.
//

import SwiftUI
import SwiftData
import UserNotifications

@main
struct SteppieApp: App {
    private let sharedModelContainer: ModelContainer?
    private let routineRepository: any RoutineRepository
    private let isPreview: Bool
    private let notificationRouter: RoutineNotificationRouter
    private let notificationDelegate: RoutineNotificationDelegate

    init() {
        SteppieFontRegistrar.registerBundledFonts()
        let router = RoutineNotificationRouter()
        notificationRouter = router
        notificationDelegate = RoutineNotificationDelegate(router: router)

        isPreview = ProcessInfo.processInfo.environment["XCODE_RUNNING_FOR_PREVIEWS"] == "1"
        if isPreview {
            do {
                sharedModelContainer = nil
                routineRepository = try RoutinePreviewStore.makeLightweightSampleRepository()
            } catch {
                fatalError("Could not create preview repository: \(error)")
            }
            return
        }
        UNUserNotificationCenter.current().delegate = notificationDelegate

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
            rootView
        }
    }

    @ViewBuilder
    private var rootView: some View {
        if isPreview {
            ContentView(
                repository: routineRepository,
                notificationScheduler: NoopRoutineNotificationScheduler(),
                isNotificationSchedulingEnabled: false,
                notificationRouter: notificationRouter
            )
        } else {
            ContentView(
                repository: routineRepository,
                speechGuide: IOSRoutineSpeechGuide(),
                feedbackPerformer: IOSRoutineFeedbackPerformer(),
                notificationScheduler: IOSRoutineNotificationScheduler(),
                notificationRouter: notificationRouter
            )
        }
    }
}
