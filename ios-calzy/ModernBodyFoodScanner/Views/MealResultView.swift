import SwiftUI

/// Review + confirm screen shown after a scan, description or search pick.
struct MealResultView: View {
    @Environment(AppStore.self) private var store
    @Environment(\.dismiss) private var dismiss

    let image: UIImage?
    let result: AnalysisResult
    let source: EntrySource

    @State private var title: String
    @State private var items: [FoodItem]
    @State private var portions: Double
    @State private var slot: MealSlot
    @State private var appeared: Bool = false

    init(image: UIImage?, result: AnalysisResult, source: EntrySource) {
        self.image = image
        self.result = result
        self.source = source
        _title = State(initialValue: result.title)
        _items = State(initialValue: result.foodItems)
        _portions = State(initialValue: 1)
        _slot = State(initialValue: MealSlot.current())
    }

    private var totalCalories: Int {
        Int((Double(items.reduce(0) { $0 + $1.calories }) * portions).rounded())
    }
    private var totalProtein: Double { items.reduce(0) { $0 + $1.protein } * portions }
    private var totalCarbs: Double { items.reduce(0) { $0 + $1.carbs } * portions }
    private var totalFat: Double { items.reduce(0) { $0 + $1.fat } * portions }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    if let image {
                        Color(.secondarySystemBackground)
                            .frame(height: 210)
                            .overlay {
                                Image(uiImage: image)
                                    .resizable()
                                    .aspectRatio(contentMode: .fill)
                                    .allowsHitTesting(false)
                            }
                            .clipShape(.rect(cornerRadius: 26, style: .continuous))
                            .overlay(alignment: .bottomLeading) {
                                HStack(spacing: 5) {
                                    Image(systemName: "sparkles").font(.system(size: 11, weight: .bold))
                                    Text("AI estimate").font(.system(size: 12, weight: .semibold))
                                }
                                .foregroundStyle(.white)
                                .padding(.horizontal, 11)
                                .padding(.vertical, 6)
                                .background(.black.opacity(0.42), in: Capsule())
                                .padding(14)
                            }
                    }

                    summaryCard
                    EstimateDisclaimer()
                    portionCard
                    itemsCard

                    if let quip = result.quip, !quip.isEmpty {
                        HStack(alignment: .top, spacing: 10) {
                            Image(systemName: store.profile.jesterMode ? "theatermasks.fill" : "sparkles")
                                .font(.system(size: 15))
                                .foregroundStyle(store.profile.jesterMode ? Theme.fat : Theme.plum)
                            Text(quip)
                                .font(.system(size: 14, weight: .medium))
                                .foregroundStyle(Theme.inkSoft)
                            Spacer(minLength: 0)
                        }
                        .cardStyle(radius: 20, padding: 14)
                    }
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 110)
                .opacity(appeared ? 1 : 0)
                .offset(y: appeared ? 0 : 14)
            }
            .scrollIndicators(.hidden)
            .background(Theme.backdrop)
            .navigationTitle("Review meal")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Cancel") { dismiss() }
                        .foregroundStyle(Theme.inkSoft)
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        store.toggleSaved(title: title, items: items, slot: slot)
                        Haptics.tap()
                    } label: {
                        Image(systemName: store.isSaved(title: title) ? "bookmark.fill" : "bookmark")
                            .foregroundStyle(Theme.ink)
                    }
                }
            }
            .safeAreaInset(edge: .bottom) {
                Button(action: save) {
                    HStack(spacing: 8) {
                        Image(systemName: "checkmark.circle.fill")
                        Text("Log \(totalCalories) kcal")
                    }
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
            .onAppear {
                withAnimation(.spring(response: 0.5, dampingFraction: 0.85)) { appeared = true }
            }
        }
    }

    private var summaryCard: some View {
        VStack(spacing: 16) {
            TextField("Meal name", text: $title)
                .font(.system(size: 21, weight: .bold))
                .foregroundStyle(Theme.ink)
                .multilineTextAlignment(.center)

            HStack(alignment: .firstTextBaseline, spacing: 4) {
                Text("\(totalCalories)")
                    .font(.metric(46, .bold))
                    .foregroundStyle(Theme.ink)
                    .contentTransition(.numericText(value: Double(totalCalories)))
                Text("kcal")
                    .font(.system(size: 16, weight: .medium))
                    .foregroundStyle(Theme.inkFaint)
            }

            HStack(spacing: 10) {
                macroPill("Protein", totalProtein, Theme.protein)
                macroPill("Carbs", totalCarbs, Theme.carbs)
                macroPill("Fat", totalFat, Theme.fat)
            }

            HStack(spacing: 6) {
                Image(systemName: "leaf.fill").font(.system(size: 11))
                Text("Health score \(result.healthScore)/10")
                    .font(.system(size: 12, weight: .semibold))
            }
            .foregroundStyle(scoreColor)
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .background(scoreColor.opacity(0.12), in: Capsule())
        }
        .frame(maxWidth: .infinity)
        .cardStyle(radius: 26, padding: 20)
    }

    private var scoreColor: Color {
        switch result.healthScore {
        case 8...: Theme.mint
        case 5...7: Theme.fat
        default: Theme.protein
        }
    }

    private func macroPill(_ name: String, _ value: Double, _ color: Color) -> some View {
        VStack(spacing: 3) {
            Text("\(Int(value.rounded()))g")
                .font(.metric(17, .bold))
                .foregroundStyle(Theme.ink)
            Text(name)
                .font(.system(size: 11, weight: .medium))
                .foregroundStyle(Theme.inkSoft)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 10)
        .background(color.opacity(0.1), in: RoundedRectangle(cornerRadius: 16, style: .continuous))
    }

    private var portionCard: some View {
        VStack(spacing: 14) {
            HStack {
                Text("Portions")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(Theme.ink)
                Spacer()
                HStack(spacing: 16) {
                    stepButton("minus") {
                        portions = max(0.25, portions - 0.25)
                    }
                    Text(portions.formatted(.number.precision(.fractionLength(0...2))))
                        .font(.metric(19, .bold))
                        .foregroundStyle(Theme.ink)
                        .frame(minWidth: 42)
                    stepButton("plus") {
                        portions = min(10, portions + 0.25)
                    }
                }
            }

            Divider().overlay(Theme.hairline)

            HStack {
                Text("Meal")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(Theme.ink)
                Spacer()
                Picker("Meal", selection: $slot) {
                    ForEach(MealSlot.allCases) { Text($0.label).tag($0) }
                }
                .pickerStyle(.menu)
                .tint(Theme.ink)
            }
        }
        .cardStyle(radius: 22, padding: 16)
    }

    private func stepButton(_ symbol: String, action: @escaping () -> Void) -> some View {
        Button {
            Haptics.selection()
            withAnimation(.spring(response: 0.3, dampingFraction: 0.8)) { action() }
        } label: {
            Image(systemName: symbol)
                .font(.system(size: 14, weight: .bold))
                .foregroundStyle(Theme.ink)
                .frame(width: 34, height: 34)
                .background(Theme.well, in: Circle())
        }
        .buttonStyle(PressableButtonStyle())
    }

    private var itemsCard: some View {
        VStack(spacing: 0) {
            ForEach(Array(items.enumerated()), id: \.element.id) { index, item in
                HStack(spacing: 12) {
                    VStack(alignment: .leading, spacing: 3) {
                        Text(item.name)
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundStyle(Theme.ink)
                        Text(item.quantity)
                            .font(.system(size: 12))
                            .foregroundStyle(Theme.inkFaint)
                    }
                    Spacer(minLength: 0)
                    Text("\(item.calories)")
                        .font(.metric(16, .bold))
                        .foregroundStyle(Theme.inkSoft)
                    Button {
                        Haptics.tap()
                        withAnimation { items.remove(at: index) }
                    } label: {
                        Image(systemName: "xmark.circle.fill")
                            .font(.system(size: 17))
                            .foregroundStyle(Theme.inkFaint.opacity(0.6))
                    }
                }
                .padding(.vertical, 12)

                if index < items.count - 1 {
                    Divider().overlay(Theme.hairline)
                }
            }
        }
        .cardStyle(radius: 22, padding: 16)
    }

    private func save() {
        guard !items.isEmpty else { return }
        var fileName: String?
        if let image { fileName = store.saveImage(image) }

        let meal = MealEntry(
            title: title.isEmpty ? "Meal" : title,
            date: mergedDate,
            slot: slot,
            source: source,
            items: items,
            portions: portions,
            photoFileName: fileName,
            healthScore: result.healthScore,
            quip: result.quip
        )
        store.addMeal(meal)
        Haptics.success()
        dismiss()
    }

    /// Logs against the day the user is currently viewing, keeping the current
    /// clock time. Seconds are carried over so two meals logged onto a past day
    /// in the same minute still order correctly.
    private var mergedDate: Date {
        let calendar = Calendar.current
        let now = Date()
        if calendar.isDateInToday(store.selectedDate) { return now }
        let time = calendar.dateComponents([.hour, .minute, .second], from: now)
        return calendar.date(
            bySettingHour: time.hour ?? 12,
            minute: time.minute ?? 0,
            second: time.second ?? 0,
            of: store.selectedDate
        ) ?? store.selectedDate
    }
}
