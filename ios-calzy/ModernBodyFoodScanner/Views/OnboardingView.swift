import SwiftUI

/// First-run flow that builds the user's profile and calculates their daily plan.
struct OnboardingView: View {
    @Environment(AppStore.self) private var store

    @State private var step: Int = 0
    @State private var name: String = ""
    @State private var sex: Sex = .male
    @State private var birthYear: Double = 1996
    @State private var height: Double = 176
    @State private var weight: Double = 80
    @State private var goal: GoalDirection = .lose
    @State private var goalWeight: Double = 74
    @State private var rate: Double = 0.5
    @State private var activity: ActivityLevel = .light
    @State private var isCalculating: Bool = false

    private let totalSteps: Int = 6

    var body: some View {
        ZStack {
            Theme.backdrop

            VStack(spacing: 0) {
                progressBar
                    .padding(.horizontal, 20)
                    .padding(.top, 14)

                TabView(selection: $step) {
                    welcomeStep.tag(0)
                    aboutStep.tag(1)
                    bodyStep.tag(2)
                    goalStep.tag(3)
                    activityStep.tag(4)
                    planStep.tag(5)
                }
                .tabViewStyle(.page(indexDisplayMode: .never))
                .animation(.spring(response: 0.42, dampingFraction: 0.86), value: step)

                footer
            }
        }
    }

    // MARK: - Chrome

    private var progressBar: some View {
        HStack(spacing: 6) {
            ForEach(0..<totalSteps, id: \.self) { index in
                Capsule()
                    .fill(index <= step ? Theme.ink : Theme.inkFaint.opacity(0.22))
                    .frame(height: 4)
                    .animation(.spring(response: 0.4, dampingFraction: 0.85), value: step)
            }
        }
    }

    private var footer: some View {
        VStack(spacing: 10) {
            Button {
                Haptics.tap()
                if step == totalSteps - 1 {
                    finish()
                } else {
                    withAnimation(.spring(response: 0.42, dampingFraction: 0.86)) { step += 1 }
                }
            } label: {
                Text(step == totalSteps - 1 ? "Start tracking" : "Continue")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 17)
                    .background(Theme.ink, in: Capsule())
            }
            .buttonStyle(PressableButtonStyle())

            if step > 0 {
                Button("Back") {
                    withAnimation(.spring(response: 0.42, dampingFraction: 0.86)) { step -= 1 }
                }
                .font(.system(size: 14, weight: .medium))
                .foregroundStyle(Theme.inkFaint)
            } else {
                Color.clear.frame(height: 18)
            }
        }
        .padding(.horizontal, 22)
        .padding(.bottom, 14)
    }

    private func stepShell<Content: View>(
        title: String,
        subtitle: String,
        @ViewBuilder content: () -> Content
    ) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 22) {
                VStack(alignment: .leading, spacing: 8) {
                    Text(title)
                        .font(.system(size: 30, weight: .bold))
                        .foregroundStyle(Theme.ink)
                    Text(subtitle)
                        .font(.system(size: 16))
                        .foregroundStyle(Theme.inkSoft)
                }
                content()
            }
            .padding(.horizontal, 22)
            .padding(.top, 34)
            .padding(.bottom, 30)
        }
        .scrollIndicators(.hidden)
    }

    // MARK: - Steps

    private var welcomeStep: some View {
        VStack(spacing: 26) {
            Spacer()
            ZStack {
                Circle()
                    .fill(RadialGradient(colors: [Theme.flame.opacity(0.28), .clear], center: .center, startRadius: 0, endRadius: 130))
                    .frame(width: 260, height: 260)
                Image("LaunchIcon")
                    .resizable()
                    .aspectRatio(contentMode: .fill)
                    .frame(width: 100, height: 100)
                    .clipShape(.rect(cornerRadius: 28, style: .continuous))
                    .shadow(color: .black.opacity(0.18), radius: 24, y: 12)
            }

            VStack(spacing: 12) {
                Text("ModernBody")
                    .font(.system(size: 44, weight: .bold, design: .rounded))
                    .foregroundStyle(Theme.ink)
                Text("Point your camera at any meal.\nWe'll do the counting.")
                    .font(.system(size: 17))
                    .foregroundStyle(Theme.inkSoft)
                    .multilineTextAlignment(.center)
                    .lineSpacing(3)
            }
            Spacer()
        }
        .frame(maxWidth: .infinity)
    }

    private var aboutStep: some View {
        stepShell(title: "First, the basics", subtitle: "We use these to calculate your daily energy needs.") {
            VStack(spacing: 16) {
                VStack(alignment: .leading, spacing: 8) {
                    Text("What should we call you?").settingsLabel()
                    TextField("Your name", text: $name)
                        .font(.system(size: 17, weight: .medium))
                        .padding(14)
                        .background(Theme.well, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                }
                .cardStyle(radius: 22, padding: 16)

                VStack(alignment: .leading, spacing: 10) {
                    Text("Sex").settingsLabel()
                    Picker("Sex", selection: $sex) {
                        ForEach(Sex.allCases) { Text($0.label).tag($0) }
                    }
                    .pickerStyle(.segmented)
                }
                .cardStyle(radius: 22, padding: 16)

                VStack(alignment: .leading, spacing: 8) {
                    HStack {
                        Text("Birth year").settingsLabel()
                        Spacer()
                        Text("\(Int(birthYear))").font(.metric(17, .bold)).foregroundStyle(Theme.ink)
                    }
                    Slider(value: $birthYear, in: 1940...2012, step: 1).tint(Theme.ink)
                }
                .cardStyle(radius: 22, padding: 16)
            }
        }
    }

    private var bodyStep: some View {
        stepShell(title: "Your body today", subtitle: "Be honest — this only ever lives on your device.") {
            VStack(spacing: 16) {
                bigSlider(title: "Height", value: $height, range: 130...220, step: 1, format: "%.0f", unit: "cm")
                bigSlider(title: "Weight", value: $weight, range: 35...200, step: 0.1, format: "%.1f", unit: "kg")

                HStack(spacing: 8) {
                    Text("BMI").font(.system(size: 13, weight: .medium)).foregroundStyle(Theme.inkSoft)
                    Text(String(format: "%.1f", bmi)).font(.metric(17, .bold)).foregroundStyle(Theme.ink)
                    Text(BMICategory.from(bmi).rawValue)
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(Theme.inkSoft)
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 10)
                .background(Theme.well, in: Capsule())
            }
        }
    }

    private var goalStep: some View {
        stepShell(title: "What's the mission?", subtitle: "You can change this at any time.") {
            VStack(spacing: 14) {
                HStack(spacing: 9) {
                    ForEach(GoalDirection.allCases) { option in
                        Button {
                            Haptics.selection()
                            withAnimation(.spring(response: 0.3, dampingFraction: 0.82)) {
                                goal = option
                                if option == .lose { goalWeight = max(40, weight - 6) }
                                if option == .gain { goalWeight = weight + 5 }
                                if option == .maintain { goalWeight = weight }
                            }
                        } label: {
                            VStack(spacing: 8) {
                                Image(systemName: option.symbol).font(.system(size: 17, weight: .bold))
                                Text(option.label)
                                    .font(.system(size: 12, weight: .semibold))
                                    .multilineTextAlignment(.center)
                            }
                            .foregroundStyle(goal == option ? .white : Theme.ink)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 18)
                            .background {
                                RoundedRectangle(cornerRadius: 20, style: .continuous)
                                    .fill(goal == option ? Theme.ink : Color.white.opacity(0.78))
                            }
                        }
                        .buttonStyle(PressableButtonStyle())
                    }
                }

                if goal != .maintain {
                    bigSlider(title: "Goal weight", value: $goalWeight, range: 35...200, step: 0.1, format: "%.1f", unit: "kg")
                    bigSlider(title: "Weekly pace", value: $rate, range: 0.1...1.2, step: 0.1, format: "%.1f", unit: "kg / week")
                }
            }
        }
    }

    private var activityStep: some View {
        stepShell(title: "How active are you?", subtitle: "Outside of what you'll log as exercise.") {
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
                        }
                        .padding(15)
                        .background {
                            RoundedRectangle(cornerRadius: 20, style: .continuous)
                                .fill(activity == level ? Theme.ink : Color.white.opacity(0.78))
                        }
                    }
                    .buttonStyle(PressableButtonStyle())
                }
            }
        }
    }

    private var planStep: some View {
        let targets = draftProfile.targets

        return stepShell(
            title: "Your daily plan",
            subtitle: "Built from your body, goal and activity level."
        ) {
            VStack(spacing: 16) {
                VStack(spacing: 10) {
                    Text("DAILY CALORIES")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundStyle(Theme.inkFaint)
                    HStack(alignment: .firstTextBaseline, spacing: 5) {
                        Text("\(targets.calories)")
                            .font(.metric(56, .bold))
                            .foregroundStyle(Theme.ink)
                            .contentTransition(.numericText(value: Double(targets.calories)))
                        Text("kcal").font(.system(size: 17, weight: .medium)).foregroundStyle(Theme.inkFaint)
                    }
                    Text("Maintenance is about \(Int(draftProfile.maintenance)) kcal")
                        .font(.system(size: 13))
                        .foregroundStyle(Theme.inkSoft)
                }
                .frame(maxWidth: .infinity)
                .cardStyle(radius: 26, padding: 22)

                HStack(spacing: 12) {
                    planTile("Protein", "\(targets.protein)g", Theme.protein, "🍗")
                    planTile("Carbs", "\(targets.carbs)g", Theme.carbs, "🍞")
                    planTile("Fat", "\(targets.fat)g", Theme.fat, "🥑")
                }

                if goal != .maintain, abs(goalWeight - weight) > 0.1 {
                    HStack(spacing: 9) {
                        Image(systemName: "calendar")
                            .font(.system(size: 14))
                            .foregroundStyle(Theme.mint)
                        Text(etaText)
                            .font(.system(size: 14, weight: .medium))
                            .foregroundStyle(Theme.inkSoft)
                        Spacer(minLength: 0)
                    }
                    .cardStyle(radius: 20, padding: 15)
                }

                VStack(alignment: .leading, spacing: 7) {
                    HStack(spacing: 7) {
                        Image(systemName: "info.circle")
                            .font(.system(size: 13, weight: .semibold))
                        Text("Before you start")
                            .font(.system(size: 13, weight: .bold))
                    }
                    .foregroundStyle(Theme.inkSoft)

                    Text(Legal.wellnessDisclaimer)
                        .font(.system(size: 12))
                        .foregroundStyle(Theme.inkFaint)
                        .lineSpacing(3)
                        .fixedSize(horizontal: false, vertical: true)

                    Text("No account is needed and your data stays on this iPhone. By continuing you agree to the Terms of Use and Privacy Policy, both available in Settings.")
                        .font(.system(size: 12))
                        .foregroundStyle(Theme.inkFaint)
                        .lineSpacing(3)
                        .fixedSize(horizontal: false, vertical: true)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .cardStyle(radius: 20, padding: 15)
            }
        }
    }

    private func planTile(_ title: String, _ value: String, _ color: Color, _ emoji: String) -> some View {
        VStack(spacing: 7) {
            Text(emoji).font(.system(size: 24))
            Text(value).font(.metric(19, .bold)).foregroundStyle(Theme.ink)
            Text(title).font(.system(size: 12, weight: .medium)).foregroundStyle(Theme.inkSoft)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 16)
        .background(color.opacity(0.12), in: RoundedRectangle(cornerRadius: 22, style: .continuous))
    }

    private func bigSlider(
        title: String,
        value: Binding<Double>,
        range: ClosedRange<Double>,
        step: Double,
        format: String,
        unit: String
    ) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text(title).settingsLabel()
                Spacer()
                HStack(alignment: .firstTextBaseline, spacing: 3) {
                    Text(String(format: format, value.wrappedValue))
                        .font(.metric(22, .bold))
                        .foregroundStyle(Theme.ink)
                        .contentTransition(.numericText(value: value.wrappedValue))
                    Text(unit).font(.system(size: 12, weight: .medium)).foregroundStyle(Theme.inkFaint)
                }
            }
            Slider(value: value, in: range, step: step)
                .tint(Theme.ink)
                .onChange(of: value.wrappedValue) { _, _ in Haptics.selection() }
        }
        .cardStyle(radius: 22, padding: 16)
    }

    // MARK: - Derived

    private var bmi: Double {
        let m = height / 100
        return m > 0 ? weight / (m * m) : 0
    }

    private var etaText: String {
        let weeks = abs(goalWeight - weight) / max(rate, 0.1)
        let date = Calendar.current.date(byAdding: .day, value: Int(weeks * 7), to: Date()) ?? Date()
        return "Reach \(String(format: "%.1f", goalWeight)) kg around \(date.formatted(.dateTime.month(.wide).day()))"
    }

    private var draftProfile: UserProfile {
        var profile = UserProfile()
        profile.name = name
        profile.sex = sex
        profile.birthYear = Int(birthYear)
        profile.heightCm = height
        profile.startWeightKg = weight
        profile.currentWeightKg = weight
        profile.goalWeightKg = goalWeight
        profile.goal = goal
        profile.weeklyRateKg = rate
        profile.activity = activity
        return profile
    }

    private func finish() {
        var profile = draftProfile
        profile.hasOnboarded = true
        store.profile = profile
        store.completeOnboarding()
        Haptics.success()
    }
}
