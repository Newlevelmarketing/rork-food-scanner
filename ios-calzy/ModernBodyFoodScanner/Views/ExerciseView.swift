import SwiftUI

/// Log a workout to add calories back to the day's budget.
struct ExerciseView: View {
    @Environment(AppStore.self) private var store
    @Environment(\.dismiss) private var dismiss

    @State private var selected: ExercisePreset = ExercisePreset.all[0]
    @State private var minutes: Double = 30

    private var burned: Int {
        selected.calories(minutes: Int(minutes), weightKg: store.profile.currentWeightKg)
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 18) {
                    VStack(spacing: 6) {
                        Image(systemName: selected.symbol)
                            .font(.system(size: 34, weight: .semibold))
                            .foregroundStyle(Theme.flame)
                            .frame(height: 44)
                            .contentTransition(.symbolEffect(.replace))

                        HStack(alignment: .firstTextBaseline, spacing: 4) {
                            Text("\(burned)")
                                .font(.metric(52, .bold))
                                .foregroundStyle(Theme.ink)
                                .contentTransition(.numericText(value: Double(burned)))
                            Text("kcal")
                                .font(.system(size: 17, weight: .medium))
                                .foregroundStyle(Theme.inkFaint)
                        }

                        Text("\(Int(minutes)) minutes of \(selected.name.lowercased())")
                            .font(.system(size: 14, weight: .medium))
                            .foregroundStyle(Theme.inkSoft)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 8)
                    .cardStyle(radius: 26, padding: 20)

                    VStack(alignment: .leading, spacing: 12) {
                        HStack {
                            Text("Duration")
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundStyle(Theme.ink)
                            Spacer()
                            Text("\(Int(minutes)) min")
                                .font(.metric(16, .bold))
                                .foregroundStyle(Theme.inkSoft)
                        }
                        Slider(value: $minutes, in: 5...180, step: 5)
                            .tint(Theme.flame)
                            .onChange(of: minutes) { _, _ in Haptics.selection() }
                    }
                    .cardStyle(radius: 22, padding: 16)

                    LazyVGrid(columns: [GridItem(.adaptive(minimum: 104), spacing: 10)], spacing: 10) {
                        ForEach(ExercisePreset.all) { preset in
                            Button {
                                Haptics.tap()
                                withAnimation(.spring(response: 0.3, dampingFraction: 0.8)) {
                                    selected = preset
                                }
                            } label: {
                                VStack(spacing: 8) {
                                    Image(systemName: preset.symbol)
                                        .font(.system(size: 18, weight: .semibold))
                                        .foregroundStyle(selected == preset ? .white : Theme.ink)
                                    Text(preset.name)
                                        .font(.system(size: 12, weight: .medium))
                                        .foregroundStyle(selected == preset ? .white : Theme.inkSoft)
                                }
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 15)
                                .background {
                                    RoundedRectangle(cornerRadius: 20, style: .continuous)
                                        .fill(selected == preset ? Theme.ink : Color.white.opacity(0.78))
                                }
                            }
                            .buttonStyle(PressableButtonStyle())
                        }
                    }
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 110)
            }
            .scrollIndicators(.hidden)
            .background(Theme.backdrop)
            .navigationTitle("Log exercise")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Cancel") { dismiss() }.foregroundStyle(Theme.inkSoft)
                }
            }
            .safeAreaInset(edge: .bottom) {
                Button {
                    store.addExercise(
                        ExerciseEntry(
                            name: selected.name,
                            date: Date(),
                            minutes: Int(minutes),
                            calories: burned,
                            symbol: selected.symbol
                        )
                    )
                    Haptics.success()
                    dismiss()
                } label: {
                    Text("Add \(burned) kcal back")
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
}
