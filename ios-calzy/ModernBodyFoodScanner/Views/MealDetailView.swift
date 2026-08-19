import SwiftUI

/// Detail sheet for an already-logged meal.
struct MealDetailView: View {
    @Environment(AppStore.self) private var store
    @Environment(\.dismiss) private var dismiss

    @State private var meal: MealEntry
    @State private var showDeleteConfirm: Bool = false
    @State private var isEditing: Bool = false

    init(meal: MealEntry) {
        _meal = State(initialValue: meal)
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    if let image = store.image(named: meal.photoFileName) {
                        Color(.secondarySystemBackground)
                            .frame(height: 220)
                            .overlay {
                                Image(uiImage: image)
                                    .resizable()
                                    .aspectRatio(contentMode: .fill)
                                    .allowsHitTesting(false)
                            }
                            .clipShape(.rect(cornerRadius: 26, style: .continuous))
                    }

                    VStack(spacing: 14) {
                        Text(meal.title)
                            .font(.system(size: 23, weight: .bold))
                            .foregroundStyle(Theme.ink)
                            .multilineTextAlignment(.center)

                        HStack(alignment: .firstTextBaseline, spacing: 4) {
                            Text("\(meal.calories)")
                                .font(.metric(44, .bold))
                                .foregroundStyle(Theme.ink)
                                .contentTransition(.numericText(value: Double(meal.calories)))
                            Text("kcal").font(.system(size: 16, weight: .medium)).foregroundStyle(Theme.inkFaint)
                        }

                        HStack(spacing: 10) {
                            macroColumn("Protein", meal.protein, store.targets.protein, Theme.protein)
                            macroColumn("Carbs", meal.carbs, store.targets.carbs, Theme.carbs)
                            macroColumn("Fat", meal.fat, store.targets.fat, Theme.fat)
                        }

                        Label(
                            "\(meal.slot.label) · \(meal.date.formatted(date: .abbreviated, time: .shortened))",
                            systemImage: meal.slot.symbol
                        )
                        .font(.system(size: 12, weight: .medium))
                        .foregroundStyle(Theme.inkFaint)
                    }
                    .frame(maxWidth: .infinity)
                    .cardStyle(radius: 26, padding: 20)

                    EstimateDisclaimer()

                    VStack(spacing: 12) {
                        HStack {
                            Text("Portions")
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundStyle(Theme.ink)
                            Spacer()
                            HStack(spacing: 16) {
                                circleButton("minus") { adjust(-0.25) }
                                Text(meal.portions.formatted(.number.precision(.fractionLength(0...2))))
                                    .font(.metric(19, .bold))
                                    .frame(minWidth: 42)
                                circleButton("plus") { adjust(0.25) }
                            }
                        }
                    }
                    .cardStyle(radius: 22, padding: 16)

                    VStack(spacing: 0) {
                        ForEach(Array(meal.items.enumerated()), id: \.element.id) { index, item in
                            HStack {
                                VStack(alignment: .leading, spacing: 3) {
                                    Text(item.name)
                                        .font(.system(size: 15, weight: .semibold))
                                        .foregroundStyle(Theme.ink)
                                    Text(item.quantity)
                                        .font(.system(size: 12))
                                        .foregroundStyle(Theme.inkFaint)
                                }
                                Spacer()
                                VStack(alignment: .trailing, spacing: 3) {
                                    Text("\(item.calories) kcal")
                                        .font(.system(size: 14, weight: .semibold))
                                        .foregroundStyle(Theme.inkSoft)
                                    Text("\(Int(item.protein))P · \(Int(item.carbs))C · \(Int(item.fat))F")
                                        .font(.system(size: 11))
                                        .foregroundStyle(Theme.inkFaint)
                                }
                            }
                            .padding(.vertical, 12)

                            if index < meal.items.count - 1 {
                                Divider().overlay(Theme.hairline)
                            }
                        }
                    }
                    .cardStyle(radius: 22, padding: 16)

                    if let quip = meal.quip, !quip.isEmpty {
                        HStack(alignment: .top, spacing: 10) {
                            Image(systemName: "quote.opening")
                                .font(.system(size: 13))
                                .foregroundStyle(Theme.plum)
                            Text(quip)
                                .font(.system(size: 14, weight: .medium))
                                .foregroundStyle(Theme.inkSoft)
                            Spacer(minLength: 0)
                        }
                        .cardStyle(radius: 20, padding: 14)
                    }

                    Button(role: .destructive) {
                        showDeleteConfirm = true
                    } label: {
                        Label("Delete meal", systemImage: "trash")
                            .font(.system(size: 15, weight: .semibold))
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 15)
                            .background(Theme.protein.opacity(0.1), in: Capsule())
                    }
                    .tint(Theme.protein)
                    .buttonStyle(PressableButtonStyle())
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 40)
            }
            .scrollIndicators(.hidden)
            .background(Theme.backdrop)
            .navigationTitle("Meal")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button {
                        Haptics.tap()
                        isEditing = true
                    } label: {
                        Image(systemName: "pencil")
                            .foregroundStyle(Theme.ink)
                    }
                    .accessibilityLabel("Edit meal name or calories")
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Done") { dismiss() }
                        .fontWeight(.semibold)
                        .foregroundStyle(Theme.ink)
                }
            }
            .sheet(isPresented: $isEditing) {
                EditMealSheet(meal: meal) { updated in
                    withAnimation(.spring(response: 0.35, dampingFraction: 0.85)) {
                        meal = updated
                    }
                }
            }
            .confirmationDialog("Delete this meal?", isPresented: $showDeleteConfirm, titleVisibility: .visible) {
                Button("Delete", role: .destructive) {
                    store.deleteMeal(meal)
                    Haptics.warning()
                    dismiss()
                }
            }
        }
    }

    private func macroColumn(_ name: String, _ value: Double, _ goal: Int, _ color: Color) -> some View {
        VStack(spacing: 7) {
            Text("\(Int(value.rounded()))g")
                .font(.metric(17, .bold))
                .foregroundStyle(Theme.ink)
            Text(name)
                .font(.system(size: 11, weight: .medium))
                .foregroundStyle(Theme.inkSoft)
            MacroBar(progress: goal > 0 ? value / Double(goal) : 0, color: color, height: 4)
                .padding(.horizontal, 12)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 11)
        .background(color.opacity(0.09), in: RoundedRectangle(cornerRadius: 16, style: .continuous))
    }

    private func circleButton(_ symbol: String, action: @escaping () -> Void) -> some View {
        Button {
            Haptics.selection()
            action()
        } label: {
            Image(systemName: symbol)
                .font(.system(size: 14, weight: .bold))
                .foregroundStyle(Theme.ink)
                .frame(width: 34, height: 34)
                .background(Theme.well, in: Circle())
        }
        .buttonStyle(PressableButtonStyle())
    }

    private func adjust(_ delta: Double) {
        withAnimation(.spring(response: 0.3, dampingFraction: 0.8)) {
            meal.portions = min(10, max(0.25, meal.portions + delta))
        }
        store.updateMeal(meal)
    }
}
