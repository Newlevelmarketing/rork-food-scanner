import SwiftUI

/// Quick-correction form for an already-logged meal.
///
/// AI estimates and database matches are close but rarely exact, so this lets a
/// user fix the two things they actually notice on the timeline: what the meal
/// is called and how many calories it cost them. Macros scale with the calorie
/// edit so the split they already saw stays believable.
struct EditMealSheet: View {
    @Environment(AppStore.self) private var store
    @Environment(\.dismiss) private var dismiss

    let meal: MealEntry
    /// Called with the saved meal so a presenting detail view can refresh.
    var onSave: ((MealEntry) -> Void)?

    @State private var title: String
    @State private var calories: Int
    @FocusState private var isNameFocused: Bool

    init(meal: MealEntry, onSave: ((MealEntry) -> Void)? = nil) {
        self.meal = meal
        self.onSave = onSave
        _title = State(initialValue: meal.title)
        _calories = State(initialValue: meal.calories)
    }

    private var trimmedTitle: String {
        title.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private var preview: MealEntry {
        meal.settingCalories(calories)
    }

    private var canSave: Bool {
        !trimmedTitle.isEmpty && (trimmedTitle != meal.title || calories != meal.calories)
    }

    private var delta: Int { calories - meal.calories }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 14) {
                    nameCard
                    caloriesCard
                    macroPreview
                }
                .padding(.horizontal, 20)
                .padding(.top, 6)
                .padding(.bottom, 32)
            }
            .scrollIndicators(.hidden)
            .scrollDismissesKeyboard(.interactively)
            .background(Theme.backdrop)
            .navigationTitle("Edit meal")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Cancel") { dismiss() }
                        .foregroundStyle(Theme.inkSoft)
                }
            }
            .safeAreaInset(edge: .bottom) { saveBar }
        }
        .presentationDetents([.large])
        .presentationDragIndicator(.visible)
    }

    // MARK: - Sections

    private var nameCard: some View {
        VStack(alignment: .leading, spacing: 9) {
            Text("Meal name")
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(Theme.inkSoft)

            HStack(spacing: 10) {
                TextField("e.g. Chicken burrito bowl", text: $title)
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundStyle(Theme.ink)
                    .focused($isNameFocused)
                    .submitLabel(.done)
                    .autocorrectionDisabled()

                if !title.isEmpty {
                    Button {
                        Haptics.selection()
                        title = ""
                        isNameFocused = true
                    } label: {
                        Image(systemName: "xmark.circle.fill")
                            .font(.system(size: 16))
                            .foregroundStyle(Theme.inkFaint)
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel("Clear meal name")
                }
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 13)
            .background(Theme.well, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .cardStyle(radius: 22, padding: 16)
    }

    private var caloriesCard: some View {
        VStack(spacing: 14) {
            HStack {
                Text("Calories")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(Theme.inkSoft)
                Spacer()
                if delta != 0 {
                    Text(delta > 0 ? "+\(delta) kcal" : "\(delta) kcal")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(delta > 0 ? Theme.flame : Theme.mint)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(
                            (delta > 0 ? Theme.flame : Theme.mint).opacity(0.12),
                            in: Capsule()
                        )
                        .transition(.scale.combined(with: .opacity))
                }
            }

            HStack(spacing: 18) {
                stepButton("minus", enabled: calories > 0) { step(-10) }

                VStack(spacing: 2) {
                    TextField("0", value: $calories, format: .number)
                        .keyboardType(.numberPad)
                        .multilineTextAlignment(.center)
                        .font(.metric(40, .bold))
                        .foregroundStyle(Theme.ink)
                        .frame(minWidth: 120)
                    Text("kcal")
                        .font(.system(size: 12, weight: .medium))
                        .foregroundStyle(Theme.inkFaint)
                }

                stepButton("plus", enabled: calories < 20000) { step(10) }
            }

            HStack(spacing: 8) {
                ForEach([-100, -50, 50, 100], id: \.self) { amount in
                    Button {
                        step(amount)
                    } label: {
                        Text(amount > 0 ? "+\(amount)" : "\(amount)")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundStyle(Theme.ink)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 9)
                            .background(Theme.well, in: Capsule())
                    }
                    .buttonStyle(PressableButtonStyle())
                }
            }
        }
        .animation(.spring(response: 0.3, dampingFraction: 0.8), value: delta)
        .frame(maxWidth: .infinity)
        .cardStyle(radius: 22, padding: 16)
    }

    private var macroPreview: some View {
        VStack(spacing: 12) {
            HStack {
                Text("Macros scale to match")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(Theme.inkSoft)
                Spacer()
            }

            HStack(spacing: 10) {
                macroPill("Protein", preview.protein, Theme.protein)
                macroPill("Carbs", preview.carbs, Theme.carbs)
                macroPill("Fat", preview.fat, Theme.fat)
            }
        }
        .frame(maxWidth: .infinity)
        .cardStyle(radius: 22, padding: 16)
    }

    private var saveBar: some View {
        Button(action: save) {
            HStack(spacing: 8) {
                Image(systemName: "checkmark.circle.fill")
                Text("Save changes")
            }
            .font(.system(size: 17, weight: .semibold))
            .foregroundStyle(.white)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 16)
            .background(canSave ? Theme.ink : Theme.inkFaint, in: Capsule())
        }
        .buttonStyle(PressableButtonStyle())
        .disabled(!canSave)
        .padding(.horizontal, 20)
        .padding(.bottom, 8)
    }

    // MARK: - Pieces

    private func macroPill(_ name: String, _ value: Double, _ color: Color) -> some View {
        VStack(spacing: 4) {
            Text("\(Int(value.rounded()))g")
                .font(.metric(18, .bold))
                .foregroundStyle(Theme.ink)
                .contentTransition(.numericText(value: value))
            Text(name)
                .font(.system(size: 11, weight: .medium))
                .foregroundStyle(Theme.inkSoft)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 11)
        .background(color.opacity(0.09), in: RoundedRectangle(cornerRadius: 16, style: .continuous))
    }

    private func stepButton(_ symbol: String, enabled: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: symbol)
                .font(.system(size: 15, weight: .bold))
                .foregroundStyle(enabled ? Theme.ink : Theme.inkFaint)
                .frame(width: 40, height: 40)
                .background(Theme.well, in: Circle())
        }
        .buttonStyle(PressableButtonStyle())
        .disabled(!enabled)
        .accessibilityLabel(symbol == "plus" ? "Increase calories" : "Decrease calories")
    }

    // MARK: - Actions

    private func step(_ amount: Int) {
        Haptics.selection()
        withAnimation(.spring(response: 0.28, dampingFraction: 0.8)) {
            calories = min(20000, max(0, calories + amount))
        }
    }

    private func save() {
        guard canSave else { return }
        var updated = meal.settingCalories(calories)
        updated.title = trimmedTitle
        store.updateMeal(updated)
        Haptics.success()
        onSave?(updated)
        dismiss()
    }
}
