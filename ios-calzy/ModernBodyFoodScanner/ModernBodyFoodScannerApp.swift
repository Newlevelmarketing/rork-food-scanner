//
//  ModernBodyFoodScannerApp.swift
//  ModernBodyFoodScanner
//
//  Created by Rork on August 1, 2026.
//

import SwiftUI

@main
struct ModernBodyFoodScannerApp: App {
    @State private var store = AppStore()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environment(store)
                .tint(Theme.ink)
                .preferredColorScheme(.light)
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
