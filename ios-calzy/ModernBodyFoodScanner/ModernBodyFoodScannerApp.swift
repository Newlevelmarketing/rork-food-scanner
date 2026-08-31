//
//  ModernBodyFoodScannerApp.swift
//  ModernBodyFoodScanner
//

import SwiftUI

@main
struct ModernBodyFoodScannerApp: App {
    @State private var store = AppStore()

    init() {
        // Must happen once, before any view can ask for offerings. No-ops when
        // no RevenueCat key is present.
        PurchaseManager.configureSDK()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environment(store)
                .tint(Theme.ink)
                .preferredColorScheme(.light)
                .task {
                    // Long-lived entitlement stream; kept separate so it never
                    // blocks the reminder sync below.
                    await PurchaseManager.shared.startListening()
                }
                .task {
                    // Keep the OS schedule in sync with the saved preference without
                    // ever prompting for permission at launch.
                    await ReminderService.shared.refreshAuthorization()
                    guard ReminderService.shared.authorization == .authorized else { return }
                    await ReminderService.shared.reschedule(
                        enabled: store.profile.remindersEnabled,
                        hours: store.profile.reminderTimes
                    )
                }
        }
    }
}
