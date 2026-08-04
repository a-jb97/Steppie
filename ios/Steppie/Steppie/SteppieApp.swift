//
//  SteppieApp.swift
//  Steppie
//
//  Created by 전민돌 on 6/19/26.
//

import SwiftUI
import OSLog
import UserNotifications

@main
struct SteppieApp: App {
    private static let logger = Logger(
        subsystem: Bundle.main.bundleIdentifier ?? "Steppie",
        category: "Persistence"
    )

    private let persistence: PersistenceInitialization
    private let isPreview: Bool
    private let notificationRouter: RoutineNotificationRouter
    private let notificationDelegate: RoutineNotificationDelegate
    private let routinePhotoStore: FileRoutinePhotoStore

    init() {
        SteppieFontRegistrar.registerBundledFonts()
        let router = RoutineNotificationRouter()
        notificationRouter = router
        notificationDelegate = RoutineNotificationDelegate(router: router)
        routinePhotoStore = FileRoutinePhotoStore()

        isPreview = ProcessInfo.processInfo.environment["XCODE_RUNNING_FOR_PREVIEWS"] == "1"
        if isPreview {
            persistence = .preview()
            logInitializationFailureIfNeeded()
            return
        }
        UNUserNotificationCenter.current().delegate = notificationDelegate
        persistence = .persistent()
        logInitializationFailureIfNeeded()
    }

    var body: some Scene {
        WindowGroup {
            rootView
                .environment(\.routinePhotoReader, routinePhotoStore)
        }
    }

    @ViewBuilder
    private var rootView: some View {
        if let repository = persistence.repository {
            if isPreview {
                ContentView(
                    repository: repository,
                    photoStore: routinePhotoStore,
                    notificationScheduler: NoopRoutineNotificationScheduler(),
                    isNotificationSchedulingEnabled: false,
                    notificationRouter: notificationRouter
                )
            } else {
                ContentView(
                    repository: repository,
                    photoStore: routinePhotoStore,
                    speechGuide: IOSRoutineSpeechGuide(),
                    feedbackPerformer: IOSRoutineFeedbackPerformer(),
                    notificationScheduler: IOSRoutineNotificationScheduler(),
                    notificationRouter: notificationRouter
                )
            }
        } else {
            PersistenceInitializationErrorView()
        }
    }

    private func logInitializationFailureIfNeeded() {
        guard let errorDescription = persistence.errorDescription else { return }
        Self.logger.fault(
            "Failed to initialize persistence: \(errorDescription, privacy: .private)"
        )
    }
}

private struct PersistenceInitializationErrorView: View {
    var body: some View {
        VStack(spacing: SteppieSpacing.large) {
            Image(systemName: "externaldrive.badge.exclamationmark")
                .font(.system(size: 52, weight: .semibold))
                .foregroundStyle(Color.steppieWarning)
                .accessibilityHidden(true)

            Text("startup.persistence.error.title")
                .steppieTextStyle(.childScreenTitle)
                .foregroundStyle(Color.steppieTextPrimary)
                .multilineTextAlignment(.center)

            Text("startup.persistence.error.message")
                .steppieTextStyle(.childSubtitle)
                .foregroundStyle(Color.steppieTextSecondary)
                .multilineTextAlignment(.center)
        }
        .padding(SteppieSpacing.extraLarge)
        .frame(maxWidth: 560)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color.steppieBackgroundSecondary)
        .accessibilityElement(children: .combine)
    }
}
