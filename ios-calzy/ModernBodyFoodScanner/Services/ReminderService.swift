import Foundation
import UserNotifications

nonisolated enum ReminderAuthorization: Equatable {
    case notDetermined
    case authorized
    case denied
}

/// Schedules the daily local notifications behind Settings › Tracking Reminders.
///
/// Reminders are the only notification ModernBody posts, they are entirely local,
/// and authorization is requested the first time the user switches them on rather
/// than at launch.
@Observable
final class ReminderService {
    static let shared = ReminderService()

    private(set) var authorization: ReminderAuthorization = .notDetermined

    private let center = UNUserNotificationCenter.current()
    private let identifierPrefix = "modernbody.reminder."

    private init() {}

    func refreshAuthorization() async {
        let settings = await center.notificationSettings()
        authorization = Self.map(settings.authorizationStatus)
    }

    /// Returns true when reminders may be scheduled.
    @discardableResult
    func requestAuthorization() async -> Bool {
        let settings = await center.notificationSettings()
        switch settings.authorizationStatus {
        case .notDetermined:
            let granted = (try? await center.requestAuthorization(options: [.alert, .sound, .badge])) ?? false
            authorization = granted ? .authorized : .denied
            return granted
        case .denied:
            authorization = .denied
            return false
        default:
            authorization = .authorized
            return true
        }
    }

    /// Replaces every scheduled reminder with one repeating notification per hour.
    func reschedule(enabled: Bool, hours: [Int]) async {
        cancelAll()
        guard enabled, !hours.isEmpty else { return }
        guard await requestAuthorization() else { return }

        for hour in Set(hours).sorted() {
            var components = DateComponents()
            components.hour = hour
            components.minute = 0

            let content = UNMutableNotificationContent()
            content.title = Self.title(forHour: hour)
            content.body = Self.body(forHour: hour)
            content.sound = .default

            let request = UNNotificationRequest(
                identifier: "\(identifierPrefix)\(hour)",
                content: content,
                trigger: UNCalendarNotificationTrigger(dateMatching: components, repeats: true)
            )

            do {
                try await center.add(request)
            } catch {
                print("[ReminderService] could not schedule \(hour):00 reminder")
            }
        }
    }

    func cancelAll() {
        center.getPendingNotificationRequests { [center, identifierPrefix] requests in
            let ids = requests
                .map(\.identifier)
                .filter { $0.hasPrefix(identifierPrefix) }
            guard !ids.isEmpty else { return }
            center.removePendingNotificationRequests(withIdentifiers: ids)
        }
    }

    // MARK: - Copy

    private static func title(forHour hour: Int) -> String {
        switch hour {
        case ..<11: "Breakfast logged?"
        case 11..<15: "Lunch logged?"
        case 15..<18: "Afternoon check-in"
        case 18..<21: "Dinner logged?"
        default: "Wrap up your day"
        }
    }

    private static func body(forHour hour: Int) -> String {
        switch hour {
        case ..<11: "Scan or type your first meal to start the day on target."
        case 11..<15: "A quick scan keeps your calorie ring honest."
        case 15..<18: "Snacks count too — add anything you've missed."
        case 18..<21: "Log dinner while you remember what was on the plate."
        default: "Add anything still missing so tomorrow starts clean."
        }
    }

    private static func map(_ status: UNAuthorizationStatus) -> ReminderAuthorization {
        switch status {
        case .notDetermined: .notDetermined
        case .denied: .denied
        default: .authorized
        }
    }
}
