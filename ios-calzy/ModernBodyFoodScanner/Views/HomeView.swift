import SwiftUI

/// Main dashboard: date strip, calorie + water rings, macros and the day's meals.
struct HomeView: View {
    @Environment(AppStore.self) private var store

    @Binding var route: HomeRoute?
    @State private var carouselPage: Int = 0
    @State private var selectedMeal: MealEntry?
    @State private var editingMeal: MealEntry?
    @State private var waterPulse: Bool = false
    @State private var isSharing: Bool = false

    private var date: Date { store.selectedDate }
    private var targets: NutritionTargets { store.targets }

    private var eaten: Int { store.caloriesEaten(on: date) }
    private var burned: Int { store.caloriesBurned(on: date) }
    private var budget: Int { targets.calories + burned }
    private var remaining: Int { budget - eaten }

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                header
                dateStrip
                energyCard
                carousel
                quickActions
                mealsSection
            }
            .padding(.bottom, 148)
        }
        .scrollIndicators(.hidden)
        .sheet(item: $selectedMeal) { meal in
            MealDetailView(meal: meal)
        }
        .sheet(item: $editingMeal) { meal in
            EditMealSheet(meal: meal)
        }
        .sheet(isPresented: $isSharing) {
            ShareSummaryView(summary: summary)
        }
    }

    // MARK: - Header

    private var header: some View {
        HStack(spacing: 11) {
            Image("LaunchIcon")
                .resizable()
                .aspectRatio(contentMode: .fill)
                .frame(width: 38, height: 38)
                .clipShape(.rect(cornerRadius: 11, style: .continuous))

            Text("ModernBody")
                .font(.system(size: 26, weight: .bold, design: .rounded))
                .foregroundStyle(Theme.ink)

            Spacer()

            streakPill
            shareButton
        }
        .padding(.horizontal, 20)
        .padding(.top, 6)
    }

    /// Consecutive logged days. Always visible so the streak stays a goal to
    /// chase rather than a badge that only appears once you already have one.
    private var streakPill: some View {
        let streak = store.streak
        let isLit = streak > 0

        return HStack(spacing: 5) {
            Image(systemName: "flame.fill")
                .font(.system(size: 13, weight: .bold))
            Text("\(streak)")
                .font(.metric(15, .bold))
                .contentTransition(.numericText())
        }
        .foregroundStyle(isLit ? Theme.flame : Theme.inkFaint)
        .padding(.horizontal, 11)
        .frame(height: 34)
        .background {
            Capsule()
                .fill(isLit ? Theme.flame.opacity(0.12) : Color.white.opacity(0.6))
                .overlay {
                    Capsule().strokeBorder(Color.white.opacity(0.7), lineWidth: 1)
                }
        }
        .animation(.spring(response: 0.35, dampingFraction: 0.75), value: streak)
        .accessibilityLabel("\(streak) day streak")
    }

    private var shareButton: some View {
        Button {
            Haptics.tap()
            isSharing = true
        } label: {
            Image(systemName: "square.and.arrow.up")
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(Theme.ink)
                .frame(width: 34, height: 34)
                .background {
                    Circle()
                        .fill(Color.white.opacity(0.72))
                        .overlay { Circle().strokeBorder(Color.white.opacity(0.7), lineWidth: 1) }
                        .shadow(color: .black.opacity(0.05), radius: 8, x: 0, y: 3)
                }
        }
        .buttonStyle(PressableButtonStyle())
        .accessibilityLabel("Share your day")
    }

    /// Snapshot of the selected day handed to the shareable summary card.
    private var summary: DailySummary {
        DailySummary(
            date: date,
            eaten: eaten,
            target: targets.calories,
            burned: burned,
            protein: store.protein(on: date),
            carbs: store.carbs(on: date),
            fat: store.fat(on: date),
            proteinTarget: targets.protein,
            carbsTarget: targets.carbs,
            fatTarget: targets.fat,
            mealCount: store.meals(on: date).count,
            water: store.water(on: date),
            streak: store.streak
        )
    }

    private var dateStrip: some View {
        DateStrip(
            selected: Binding(
                get: { store.selectedDate },
                set: { store.selectedDate = $0 }
            ),
            hasLogs: { store.hasLogs(on: $0) }
        )
    }

    // MARK: - Energy card

    private var energyCard: some View {
        HStack(spacing: 0) {
            calorieRing
                .frame(maxWidth: .infinity)

            Rectangle()
                .fill(Theme.hairline)
                .frame(width: 1, height: 96)

            waterRing
                .frame(maxWidth: .infinity)
        }
        .padding(.vertical, 15)
        .cardStyle(radius: 28, padding: 0)
        .padding(.horizontal, 20)
    }

    private var calorieRing: some View {
        VStack(spacing: 8) {
            ZStack {
                RingProgress(
                    progress: targets.calories > 0 ? Double(eaten) / Double(budget) : 0,
                    lineWidth: 10,
                    tint: LinearGradient(
                        colors: [Theme.ink, Theme.ink.opacity(0.72)],
                        startPoint: .top,
                        endPoint: .bottomTrailing
                    )
                ) {
                    VStack(spacing: 0) {
                        AnimatedNumber(value: eaten, font: .metric(27, .bold))
                        Text("/\(budget)")
                            .font(.system(size: 12, weight: .medium))
                            .foregroundStyle(Theme.inkFaint)
                    }
                }
                .frame(width: 104, height: 104)
            }
            .overlay(alignment: .topLeading) {
                if burned > 0 {
                    HStack(spacing: 3) {
                        Image(systemName: "flame.fill").font(.system(size: 10, weight: .bold))
                        Text("+\(burned)").font(.metric(13, .bold))
                    }
                    .foregroundStyle(Theme.flame)
                    .offset(x: -14, y: 26)
                }
            }

            VStack(spacing: 2) {
                Text(L("h.eaten"))
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(Theme.inkSoft)
                    .lineLimit(1)
                    .minimumScaleFactor(0.8)
                Text(remaining >= 0 ? "\(remaining) \(L("h.left"))" : "\(-remaining) \(L("h.over"))")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundStyle(remaining >= 0 ? Theme.mint : Theme.protein)
            }
        }
    }

    private var waterRing: some View {
        let ml = store.water(on: date)
        let goal = max(store.profile.waterGoalMl, 1)

        return VStack(spacing: 8) {
            RingProgress(
                progress: Double(ml) / Double(goal),
                lineWidth: 10,
                tint: LinearGradient(
                    colors: [Theme.water, Theme.water.opacity(0.55)],
                    startPoint: .topTrailing,
                    endPoint: .bottom
                ),
                trackColor: Theme.water.opacity(0.12)
            ) {
                VStack(spacing: 0) {
                    Image(systemName: "drop.fill")
                        .font(.system(size: 15))
                        .foregroundStyle(Theme.water)
                        .scaleEffect(waterPulse ? 1.22 : 1)
                    AnimatedNumber(value: ml, font: .metric(22, .bold))
                    Text("ml")
                        .font(.system(size: 10, weight: .medium))
                        .foregroundStyle(Theme.inkFaint)
                }
            }
            .frame(width: 104, height: 104)

            Button {
                Haptics.soft()
                store.addWater(250, on: date)
                withAnimation(.spring(response: 0.28, dampingFraction: 0.5)) { waterPulse = true }
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.22) {
                    withAnimation(.spring(response: 0.35, dampingFraction: 0.6)) { waterPulse = false }
                }
            } label: {
                Text("+ 250ml")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(Theme.water)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 6)
                    .background(Theme.water.opacity(0.14), in: Capsule())
            }
            .buttonStyle(PressableButtonStyle())
            .contextMenu {
                Button("Add 500 ml") { store.addWater(500, on: date) }
                Button("Add 1000 ml") { store.addWater(1000, on: date) }
                Button("Undo last", role: .destructive) { store.undoWater(on: date) }
            }
        }
    }

    // MARK: - Macro carousel

    private var carousel: some View {
        VStack(spacing: 10) {
            TabView(selection: $carouselPage) {
                HStack(spacing: 12) {
                    MacroTile(
                        title: "Protein eaten",
                        emoji: "🍗",
                        eaten: store.protein(on: date),
                        goal: targets.protein,
                        tint: Theme.protein
                    )
                    MacroTile(
                        title: "Carbs eaten",
                        emoji: "🍞",
                        eaten: store.carbs(on: date),
                        goal: targets.carbs,
                        tint: Theme.carbs
                    )
                    MacroTile(
                        title: "Fat eaten",
                        emoji: "🥑",
                        eaten: store.fat(on: date),
                        goal: targets.fat,
                        tint: Theme.fat
                    )
                }
                .padding(.horizontal, 20)
                .tag(0)

                insightsPage
                    .padding(.horizontal, 20)
                    .tag(1)
            }
            .tabViewStyle(.page(indexDisplayMode: .never))
            .frame(height: 176)

            HStack(spacing: 6) {
                ForEach(0..<2, id: \.self) { index in
                    Capsule()
                        .fill(carouselPage == index ? Theme.ink : Theme.inkFaint.opacity(0.4))
                        .frame(width: carouselPage == index ? 22 : 7, height: 7)
                        .animation(.spring(response: 0.3, dampingFraction: 0.8), value: carouselPage)
                }
            }
        }
    }

    private var insightsPage: some View {
        VStack(spacing: 12) {
            HStack(spacing: 12) {
                insightTile(
                    icon: "chart.line.uptrend.xyaxis",
                    tint: Theme.plum,
                    title: "Burned",
                    value: "\(burned)",
                    unit: "kcal"
                )
                insightTile(
                    icon: "scalemass.fill",
                    tint: Theme.mint,
                    title: "Weight",
                    value: String(format: "%.1f", store.profile.currentWeightKg),
                    unit: "kg"
                )
                insightTile(
                    icon: "leaf.fill",
                    tint: Theme.protein,
                    title: "Food score",
                    value: healthScoreText,
                    unit: "/10"
                )
            }
            Text(coachLine)
                .font(.system(size: 13, weight: .medium))
                .foregroundStyle(Theme.inkSoft)
                .lineLimit(2)
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
        }
    }

    private func insightTile(icon: String, tint: Color, title: String, value: String, unit: String) -> some View {
        VStack(spacing: 8) {
            Image(systemName: icon)
                .font(.system(size: 17, weight: .semibold))
                .foregroundStyle(tint)
                .frame(width: 42, height: 42)
                .background(tint.opacity(0.13), in: Circle())
            Text(title)
                .font(.system(size: 12, weight: .medium))
                .foregroundStyle(Theme.inkSoft)
            HStack(alignment: .firstTextBaseline, spacing: 2) {
                Text(value).font(.metric(19, .bold)).foregroundStyle(Theme.ink)
                Text(unit).font(.system(size: 11, weight: .medium)).foregroundStyle(Theme.inkFaint)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 14)
        .cardStyle(radius: 22, padding: 0)
    }

    private var healthScoreText: String {
        let meals = store.meals(on: date)
        guard !meals.isEmpty else { return "—" }
        let average = meals.reduce(0) { $0 + $1.healthScore } / meals.count
        return "\(average)"
    }

    private var coachLine: String {
        let meals = store.meals(on: date)
        if meals.isEmpty { return "Log your first meal to unlock today's insights." }
        if remaining < 0 { return "You're \(-remaining) kcal over — a walk would even it out." }
        let proteinLeft = targets.protein - Int(store.protein(on: date))
        if proteinLeft > 30 { return "\(proteinLeft)g of protein still to go today." }
        return "Great balance so far — \(remaining) kcal left in the tank."
    }

    // MARK: - Quick actions

    private var quickActions: some View {
        QuickActionBar(actions: [
            QuickAction(icon: "viewfinder", title: "Scan", tint: Theme.ink) { route = .scan },
            QuickAction(icon: "character.cursor.ibeam", title: "Type", tint: Theme.ink) { route = .describe },
            QuickAction(icon: "magnifyingglass", title: "Search", tint: Theme.ink) { route = .search },
            QuickAction(icon: "bookmark.fill", title: "Saved", tint: Theme.ink) { route = .saved },
            QuickAction(icon: "flame.fill", title: "Exercise", tint: Theme.ink) { route = .exercise },
        ])
        .padding(.horizontal, 20)
        .padding(.top, 2)
    }

    // MARK: - Meals

    private var mealsSection: some View {
        VStack(spacing: 12) {
            SectionHeader(icon: "clock.arrow.circlepath", title: "Your meals") {
                if !store.meals(on: date).isEmpty {
                    Text("\(eaten) kcal")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundStyle(Theme.inkFaint)
                }
            }
            .padding(.horizontal, 20)
            .padding(.top, 8)

            let meals = store.meals(on: date)
            if meals.isEmpty {
                EmptyMealsState()
            } else {
                VStack(spacing: 10) {
                    ForEach(meals) { meal in
                        MealRow(
                            meal: meal,
                            thumbnail: store.image(named: meal.photoFileName),
                            onOpen: { selectedMeal = meal },
                            onEdit: { editingMeal = meal }
                        )
                        .contextMenu {
                            Button {
                                editingMeal = meal
                            } label: {
                                Label("Edit meal", systemImage: "pencil")
                            }
                            Button(role: .destructive) {
                                withAnimation { store.deleteMeal(meal) }
                            } label: {
                                Label("Delete meal", systemImage: "trash")
                            }
                        }
                        .transition(.asymmetric(
                            insertion: .scale(scale: 0.94).combined(with: .opacity),
                            removal: .opacity
                        ))
                    }
                }
                .padding(.horizontal, 20)
                .animation(.spring(response: 0.4, dampingFraction: 0.85), value: meals.count)
            }

            let workouts = store.exercises(on: date)
            if !workouts.isEmpty {
                SectionHeader(icon: "figure.run", title: "Exercise") { EmptyView() }
                    .padding(.horizontal, 20)
                    .padding(.top, 10)

                VStack(spacing: 10) {
                    ForEach(workouts) { entry in
                        HStack(spacing: 13) {
                            Image(systemName: entry.symbol)
                                .font(.system(size: 18, weight: .semibold))
                                .foregroundStyle(Theme.flame)
                                .frame(width: 46, height: 46)
                                .background(Theme.flame.opacity(0.12), in: RoundedRectangle(cornerRadius: 14, style: .continuous))

                            VStack(alignment: .leading, spacing: 3) {
                                Text(entry.name)
                                    .font(.system(size: 16, weight: .semibold))
                                    .foregroundStyle(Theme.ink)
                                Text("\(entry.minutes) min")
                                    .font(.system(size: 13))
                                    .foregroundStyle(Theme.inkSoft)
                            }
                            Spacer()
                            Text("−\(entry.calories)")
                                .font(.metric(17, .bold))
                                .foregroundStyle(Theme.flame)
                        }
                        .padding(12)
                        .cardStyle(radius: 22, padding: 0)
                        .contextMenu {
                            Button(role: .destructive) {
                                withAnimation { store.deleteExercise(entry) }
                            } label: {
                                Label("Delete", systemImage: "trash")
                            }
                        }
                    }
                }
                .padding(.horizontal, 20)
            }
        }
    }
}

nonisolated enum HomeRoute: String, Identifiable {
    case scan, describe, search, saved, exercise
    var id: String { rawValue }
}
