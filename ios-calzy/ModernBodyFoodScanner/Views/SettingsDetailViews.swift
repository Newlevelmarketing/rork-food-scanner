import SwiftUI

// MARK: - Account

struct AccountView: View {
    @Environment(AppStore.self) private var store
    @State private var name: String = ""
    @State private var birthYear: Double = 1996
    @State private var sex: Sex = .male

    var body: some View {
        SettingsScaffold(title: "Profile") {
            VStack(alignment: .leading, spacing: 8) {
                Text("These details are used only to calculate your calorie and macro targets. They stay on this iPhone.")
                    .font(.system(size: 12))
                    .foregroundStyle(Theme.inkFaint)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .padding(.horizontal, 24)

            VStack(spacing: 0) {
                HStack {
                    Text("Name").settingsLabel()
                    Spacer()
                    TextField("Your name", text: $name)
                        .multilineTextAlignment(.trailing)
                        .font(.system(size: 16, weight: .medium))
                        .foregroundStyle(Theme.ink)
                }
                .padding(16)

                Divider().overlay(Theme.hairline)

                HStack {
                    Text("Sex").settingsLabel()
                    Spacer()
                    Picker("Sex", selection: $sex) {
                        ForEach(Sex.allCases) { Text($0.label).tag($0) }
                    }
                    .pickerStyle(.segmented)
                    .frame(width: 170)
                }
                .padding(16)

                Divider().overlay(Theme.hairline)

                VStack(alignment: .leading, spacing: 8) {
                    HStack {
                        Text("Birth year").settingsLabel()
                        Spacer()
                        Text("\(Int(birthYear))")
                            .font(.metric(16, .bold))
                            .foregroundStyle(Theme.ink)
                    }
                    Slider(value: $birthYear, in: 1940...2012, step: 1).tint(Theme.ink)
                }
                .padding(16)
            }
            .cardStyle(radius: 22, padding: 0)
            .padding(.horizontal, 20)
        } onSave: {
            var profile = store.profile
            profile.name = name
            profile.birthYear = Int(birthYear)
            profile.sex = sex
            store.profile = profile
        }
        .onAppear {
            name = store.profile.name
            birthYear = Double(store.profile.birthYear)
            sex = store.profile.sex
        }
    }
}

// MARK: - Nutrition goals

struct NutritionGoalsView: View {
    @Environment(AppStore.self) private var store

    @State private var custom: Bool = false
    @State private var calories: Double = 2200
    @State private var protein: Double = 140
    @State private var carbs: Double = 240
    @State private var fat: Double = 70
    @State private var water: Double = 2500

    private var recommended: NutritionTargets {
        var profile = store.profile
        profile.usesCustomTargets = false
        return profile.targets
    }

    var body: some View {
        SettingsScaffold(title: "Nutrition Goals") {
            VStack(spacing: 16) {
                VStack(spacing: 12) {
                    HStack {
                        VStack(alignment: .leading, spacing: 3) {
                            Text("Custom targets")
                                .font(.system(size: 16, weight: .semibold))
                                .foregroundStyle(Theme.ink)
                            Text("Off = calculated from your body and goal")
                                .font(.system(size: 12))
                                .foregroundStyle(Theme.inkFaint)
                        }
                        Spacer()
                        Toggle("", isOn: $custom).labelsHidden().tint(Theme.ink)
                    }
                }
                .cardStyle(radius: 22, padding: 16)

                VStack(spacing: 18) {
                    goalSlider("Calories", value: $calories, range: 1200...5000, step: 10, unit: "kcal", tint: Theme.ink)
                    goalSlider("Protein", value: $protein, range: 40...300, step: 5, unit: "g", tint: Theme.protein)
                    goalSlider("Carbs", value: $carbs, range: 40...600, step: 5, unit: "g", tint: Theme.carbs)
                    goalSlider("Fat", value: $fat, range: 20...200, step: 5, unit: "g", tint: Theme.fat)
                }
                .cardStyle(radius: 22, padding: 16)
                .opacity(custom ? 1 : 0.45)
                .disabled(!custom)

                VStack(spacing: 10) {
                    goalSlider("Water", value: $water, range: 500...6000, step: 250, unit: "ml", tint: Theme.water)
                }
                .cardStyle(radius: 22, padding: 16)

                if !custom {
                    VStack(spacing: 6) {
                        Text("RECOMMENDED FOR YOU")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundStyle(Theme.inkFaint)
                        Text("\(recommended.calories) kcal · \(recommended.protein)P · \(recommended.carbs)C · \(recommended.fat)F")
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundStyle(Theme.ink)
                    }
                    .frame(maxWidth: .infinity)
                    .cardStyle(radius: 20, padding: 14)
                }
            }
            .padding(.horizontal, 20)
        } onSave: {
            var profile = store.profile
            profile.usesCustomTargets = custom
            profile.customCalories = Int(calories)
            profile.customProtein = Int(protein)
            profile.customCarbs = Int(carbs)
            profile.customFat = Int(fat)
            profile.waterGoalMl = Int(water)
            store.profile = profile
        }
        .onAppear {
            let profile = store.profile
            custom = profile.usesCustomTargets
            let targets = profile.targets
            calories = Double(targets.calories)
            protein = Double(targets.protein)
            carbs = Double(targets.carbs)
            fat = Double(targets.fat)
            water = Double(profile.waterGoalMl)
        }
    }

    private func goalSlider(
        _ title: String,
        value: Binding<Double>,
        range: ClosedRange<Double>,
        step: Double,
        unit: String,
        tint: Color
    ) -> some View {
        VStack(alignment: .leading, spacing: 7) {
            HStack {
                Text(title).settingsLabel()
                Spacer()
                Text("\(Int(value.wrappedValue)) \(unit)")
                    .font(.metric(16, .bold))
                    .foregroundStyle(tint)
            }
            Slider(value: value, in: range, step: step).tint(tint)
        }
    }
}

// MARK: - Goals & weight

struct GoalsWeightView: View {
    @Environment(AppStore.self) private var store

    @State private var goal: GoalDirection = .lose
    @State private var current: Double = 80
    @State private var target: Double = 75
    @State private var rate: Double = 0.5

    var body: some View {
        SettingsScaffold(title: "Goals & Weight") {
            VStack(spacing: 16) {
                HStack(spacing: 9) {
                    ForEach(GoalDirection.allCases) { option in
                        Button {
                            Haptics.selection()
                            withAnimation(.spring(response: 0.3, dampingFraction: 0.8)) { goal = option }
                        } label: {
                            VStack(spacing: 7) {
                                Image(systemName: option.symbol)
                                    .font(.system(size: 16, weight: .bold))
                                Text(option.label)
                                    .font(.system(size: 12, weight: .semibold))
                                    .multilineTextAlignment(.center)
                            }
                            .foregroundStyle(goal == option ? .white : Theme.ink)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 16)
                            .background {
                                RoundedRectangle(cornerRadius: 20, style: .continuous)
                                    .fill(goal == option ? Theme.ink : Color.white.opacity(0.78))
                            }
                        }
                        .buttonStyle(PressableButtonStyle())
                    }
                }

                VStack(spacing: 18) {
                    sliderRow("Current weight", value: $current, range: 35...200, step: 0.1, format: "%.1f kg")
                    sliderRow("Goal weight", value: $target, range: 35...200, step: 0.1, format: "%.1f kg")
                    if goal != .maintain {
                        sliderRow("Weekly pace", value: $rate, range: 0.1...1.2, step: 0.1, format: "%.1f kg / week")
                    }
                }
                .cardStyle(radius: 22, padding: 16)

                VStack(spacing: 5) {
                    Text("PROJECTED TARGET")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundStyle(Theme.inkFaint)
                    Text("\(projectedCalories) kcal / day")
                        .font(.metric(24, .bold))
                        .foregroundStyle(Theme.ink)
                    if goal != .maintain, abs(target - current) > 0.1 {
                        Text(etaText)
                            .font(.system(size: 13))
                            .foregroundStyle(Theme.inkSoft)
                    }
                }
                .frame(maxWidth: .infinity)
                .cardStyle(radius: 22, padding: 18)
            }
            .padding(.horizontal, 20)
        } onSave: {
            var profile = store.profile
            profile.goal = goal
            profile.currentWeightKg = current
            profile.goalWeightKg = target
            profile.weeklyRateKg = rate
            store.profile = profile
            store.logWeight((current * 10).rounded() / 10)
        }
        .onAppear {
            let profile = store.profile
            goal = profile.goal
            current = profile.currentWeightKg
            target = profile.goalWeightKg
            rate = profile.weeklyRateKg
        }
    }

    private var projectedCalories: Int {
        var profile = store.profile
        profile.goal = goal
        profile.currentWeightKg = current
        profile.weeklyRateKg = rate
        profile.usesCustomTargets = false
        return profile.targets.calories
    }

    private var etaText: String {
        let weeks = abs(target - current) / max(rate, 0.1)
        guard weeks.isFinite, weeks > 0 else { return "" }
        let date = Calendar.current.date(byAdding: .day, value: Int(weeks * 7), to: Date()) ?? Date()
        return "On track for \(date.formatted(.dateTime.month(.wide).day()))"
    }

    private func sliderRow(
        _ title: String,
        value: Binding<Double>,
        range: ClosedRange<Double>,
        step: Double,
        format: String
    ) -> some View {
        VStack(alignment: .leading, spacing: 7) {
            HStack {
                Text(title).settingsLabel()
                Spacer()
                Text(String(format: format, value.wrappedValue))
                    .font(.metric(16, .bold))
                    .foregroundStyle(Theme.ink)
            }
            Slider(value: value, in: range, step: step).tint(Theme.ink)
        }
    }
}

// MARK: - Reminders

struct RemindersView: View {
    @Environment(AppStore.self) private var store

    @State private var enabled: Bool = true
    @State private var hours: Set<Int> = [9, 13, 19]
    @State private var authorization: ReminderAuthorization = .notDetermined

    private let slots: [(Int, String, String)] = [
        (8, "Breakfast", "sunrise.fill"),
        (9, "Mid-morning", "cup.and.saucer.fill"),
        (13, "Lunch", "sun.max.fill"),
        (16, "Afternoon", "leaf.fill"),
        (19, "Dinner", "moon.stars.fill"),
        (21, "Evening check-in", "bed.double.fill")
    ]

    var body: some View {
        SettingsScaffold(title: "Tracking Reminders") {
            VStack(spacing: 16) {
                HStack {
                    VStack(alignment: .leading, spacing: 3) {
                        Text("Daily nudges")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundStyle(Theme.ink)
                        Text("Gentle reminders so nothing goes unlogged")
                            .font(.system(size: 12))
                            .foregroundStyle(Theme.inkFaint)
                    }
                    Spacer()
                    Toggle("", isOn: $enabled).labelsHidden().tint(Theme.ink)
                }
                .cardStyle(radius: 22, padding: 16)

                if enabled, authorization == .denied {
                    VStack(alignment: .leading, spacing: 10) {
                        HStack(alignment: .top, spacing: 9) {
                            Image(systemName: "bell.slash")
                                .font(.system(size: 14, weight: .semibold))
                                .foregroundStyle(Theme.protein)
                            Text("Notifications are turned off for ModernBody, so reminders can't be delivered. Everything else in the app keeps working.")
                                .font(.system(size: 12))
                                .foregroundStyle(Theme.inkSoft)
                                .fixedSize(horizontal: false, vertical: true)
                        }
                        Button("Open iOS Settings") {
                            if let url = URL(string: UIApplication.openSettingsURLString) {
                                UIApplication.shared.open(url)
                            }
                        }
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(Theme.ink)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .cardStyle(radius: 20, padding: 16)
                }

                VStack(spacing: 0) {
                    ForEach(Array(slots.enumerated()), id: \.offset) { index, slot in
                        Button {
                            Haptics.selection()
                            if hours.contains(slot.0) { hours.remove(slot.0) } else { hours.insert(slot.0) }
                        } label: {
                            HStack(spacing: 13) {
                                Image(systemName: slot.2)
                                    .font(.system(size: 16))
                                    .foregroundStyle(Theme.ink)
                                    .frame(width: 26)
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(slot.1)
                                        .font(.system(size: 16, weight: .medium))
                                        .foregroundStyle(Theme.ink)
                                    Text(String(format: "%02d:00", slot.0))
                                        .font(.system(size: 12))
                                        .foregroundStyle(Theme.inkFaint)
                                }
                                Spacer()
                                Image(systemName: hours.contains(slot.0) ? "checkmark.circle.fill" : "circle")
                                    .font(.system(size: 20))
                                    .foregroundStyle(hours.contains(slot.0) ? Theme.ink : Theme.inkFaint.opacity(0.5))
                            }
                            .padding(.horizontal, 16)
                            .padding(.vertical, 13)
                        }

                        if index < slots.count - 1 {
                            Divider().overlay(Theme.hairline).padding(.leading, 55)
                        }
                    }
                }
                .cardStyle(radius: 22, padding: 0)
                .opacity(enabled ? 1 : 0.45)
                .disabled(!enabled)
            }
            .padding(.horizontal, 20)
        } onSave: {
            var profile = store.profile
            profile.remindersEnabled = enabled
            profile.reminderTimes = hours.sorted()
            store.profile = profile

            let isEnabled = enabled
            let selected = hours.sorted()
            Task {
                await ReminderService.shared.reschedule(enabled: isEnabled, hours: selected)
            }
        }
        .onAppear {
            enabled = store.profile.remindersEnabled
            hours = Set(store.profile.reminderTimes)
        }
        .task {
            await ReminderService.shared.refreshAuthorization()
            authorization = ReminderService.shared.authorization
        }
        .onChange(of: enabled) { _, isOn in
            guard isOn else { return }
            Task {
                await ReminderService.shared.requestAuthorization()
                authorization = ReminderService.shared.authorization
            }
        }
    }
}

// MARK: - Activity

struct ActivitySettingsView: View {
    @Environment(AppStore.self) private var store
    @State private var activity: ActivityLevel = .light

    var body: some View {
        SettingsScaffold(title: "Activity Settings") {
            VStack(spacing: 10) {
                ForEach(ActivityLevel.allCases) { level in
                    Button {
                        Haptics.selection()
                        withAnimation(.spring(response: 0.3, dampingFraction: 0.82)) { activity = level }
                    } label: {
                        HStack(spacing: 13) {
                            Image(systemName: level.symbol)
                                .font(.system(size: 17, weight: .semibold))
                                .foregroundStyle(activity == level ? .white : Theme.ink)
                                .frame(width: 26)
                            VStack(alignment: .leading, spacing: 2) {
                                Text(level.label)
                                    .font(.system(size: 16, weight: .semibold))
                                    .foregroundStyle(activity == level ? .white : Theme.ink)
                                Text(level.detail)
                                    .font(.system(size: 12))
                                    .foregroundStyle(activity == level ? .white.opacity(0.75) : Theme.inkFaint)
                            }
                            Spacer()
                            if activity == level {
                                Image(systemName: "checkmark")
                                    .font(.system(size: 14, weight: .bold))
                                    .foregroundStyle(.white)
                            }
                        }
                        .padding(16)
                        .background {
                            RoundedRectangle(cornerRadius: 22, style: .continuous)
                                .fill(activity == level ? Theme.ink : Color.white.opacity(0.78))
                        }
                    }
                    .buttonStyle(PressableButtonStyle())
                }
            }
            .padding(.horizontal, 20)
        } onSave: {
            var profile = store.profile
            profile.activity = activity
            store.profile = profile
        }
        .onAppear { activity = store.profile.activity }
    }
}

// MARK: - Shared scaffold

/// Shared settings sub-screen chrome with a save button that commits on exit.
struct SettingsScaffold<Content: View>: View {
    @Environment(\.dismiss) private var dismiss

    let title: String
    @ViewBuilder var content: () -> Content
    let onSave: () -> Void

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                content()
            }
            .padding(.top, 10)
            .padding(.bottom, 110)
        }
        .scrollIndicators(.hidden)
        .background(Theme.backdrop)
        .navigationTitle(title)
        .navigationBarTitleDisplayMode(.inline)
        .safeAreaInset(edge: .bottom) {
            Button {
                onSave()
                Haptics.success()
                dismiss()
            } label: {
                Text("Save changes")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 17)
                    .background(Theme.ink, in: Capsule())
                    .padding(.horizontal, 20)
                    .padding(.bottom, 10)
            }
            .buttonStyle(PressableButtonStyle())
            .background(.ultraThinMaterial)
        }
    }
}

extension Text {
    func settingsLabel() -> some View {
        self.font(.system(size: 15, weight: .medium)).foregroundStyle(Theme.inkSoft)
    }
}
