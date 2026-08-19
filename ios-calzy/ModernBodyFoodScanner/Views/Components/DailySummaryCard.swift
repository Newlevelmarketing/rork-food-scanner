import SwiftUI

/// Immutable snapshot of one day, rendered into the shareable summary card.
///
/// Held as plain values rather than reading `AppStore` so the card can be
/// rasterised by `ImageRenderer` outside the live view hierarchy.
nonisolated struct DailySummary {
    let date: Date
    let eaten: Int
    let target: Int
    let burned: Int
    let protein: Double
    let carbs: Double
    let fat: Double
    let proteinTarget: Int
    let carbsTarget: Int
    let fatTarget: Int
    let mealCount: Int
    let water: Int
    let streak: Int

    var budget: Int { target + burned }
    var remaining: Int { budget - eaten }
    var isOver: Bool { remaining < 0 }
}

/// The square-ish card users share to their story or a chat thread.
///
/// Rendered at a fixed width so `ImageRenderer` produces a predictable image on
/// every device.
struct DailySummaryCard: View {
    let summary: DailySummary

    static let width: CGFloat = 340

    private var progress: Double {
        guard summary.budget > 0 else { return 0 }
        return min(Double(summary.eaten) / Double(summary.budget), 1)
    }

    private var accent: Color { summary.isOver ? Theme.flame : Theme.mint }

    private var dateLine: String {
        summary.date.formatted(.dateTime.weekday(.wide).month(.abbreviated).day())
            .uppercased()
    }

    var body: some View {
        VStack(spacing: 0) {
            heading
            ring.padding(.top, 22)
            remainingLine.padding(.top, 18)
            macros.padding(.top, 26)
            chips.padding(.top, 20)
            footer.padding(.top, 22)
        }
        .padding(.horizontal, 24)
        .padding(.top, 26)
        .frame(width: Self.width)
        .background(Color.white)
    }

    // MARK: - Heading

    private var heading: some View {
        VStack(spacing: 6) {
            Text(dateLine)
                .font(.system(size: 12, weight: .semibold))
                .kerning(1.3)
                .foregroundStyle(Theme.inkFaint)

            Text("Daily Summary")
                .font(.system(size: 28, weight: .bold, design: .rounded))
                .foregroundStyle(Theme.ink)
        }
    }

    // MARK: - Ring

    private var ring: some View {
        ZStack {
            Circle()
                .stroke(Theme.well, lineWidth: 15)

            Circle()
                .trim(from: 0, to: max(progress, 0.001))
                .stroke(accent, style: StrokeStyle(lineWidth: 15, lineCap: .round))
                .rotationEffect(.degrees(-90))

            VStack(spacing: 2) {
                Text("\(summary.eaten)")
                    .font(.metric(46, .bold))
                    .foregroundStyle(Theme.ink)
                Text("of \(summary.budget) kcal")
                    .font(.system(size: 14, weight: .medium))
                    .foregroundStyle(Theme.inkFaint)
            }
        }
        .frame(width: 176, height: 176)
    }

    private var remainingLine: some View {
        Text(
            summary.isOver
                ? "\(-summary.remaining) kcal over"
                : "\(summary.remaining) kcal remaining"
        )
        .font(.system(size: 16, weight: .semibold))
        .foregroundStyle(accent)
    }

    // MARK: - Macros

    private var macros: some View {
        VStack(spacing: 14) {
            MacroSummaryRow(
                label: "Protein",
                value: summary.protein,
                target: summary.proteinTarget,
                tint: Theme.protein
            )
            MacroSummaryRow(
                label: "Carbs",
                value: summary.carbs,
                target: summary.carbsTarget,
                tint: Theme.carbs
            )
            MacroSummaryRow(
                label: "Fat",
                value: summary.fat,
                target: summary.fatTarget,
                tint: Theme.fat
            )
        }
    }

    // MARK: - Chips

    private var chips: some View {
        HStack(spacing: 8) {
            SummaryChip(
                value: "\(summary.mealCount)",
                label: summary.mealCount == 1 ? "Meal" : "Meals",
                tint: Theme.mint
            )
            SummaryChip(
                value: summary.water >= 1000
                    ? String(format: "%.1fL", Double(summary.water) / 1000)
                    : "\(summary.water)ml",
                label: "Water",
                tint: Theme.water
            )
            SummaryChip(value: "\(summary.burned)", label: "Burned", tint: Theme.flame)
        }
    }

    // MARK: - Footer

    private var footer: some View {
        VStack(spacing: 0) {
            Rectangle()
                .fill(Theme.hairline)
                .frame(height: 1)

            HStack(spacing: 9) {
                Image("LaunchIcon")
                    .resizable()
                    .aspectRatio(contentMode: .fill)
                    .frame(width: 28, height: 28)
                    .clipShape(.rect(cornerRadius: 8, style: .continuous))

                Text("Tracked with ")
                    .font(.system(size: 14, weight: .medium))
                    .foregroundStyle(Theme.inkFaint)
                    + Text("ModernBody")
                    .font(.system(size: 14, weight: .bold, design: .rounded))
                    .foregroundStyle(Theme.ink)

                if summary.streak > 1 {
                    Spacer(minLength: 0)
                    HStack(spacing: 3) {
                        Image(systemName: "flame.fill").font(.system(size: 11, weight: .bold))
                        Text("\(summary.streak)").font(.metric(13, .bold))
                    }
                    .foregroundStyle(Theme.flame)
                }
            }
            .padding(.vertical, 14)
        }
    }
}

// MARK: - Pieces

private struct MacroSummaryRow: View {
    let label: String
    let value: Double
    let target: Int
    let tint: Color

    private var fraction: Double {
        guard target > 0 else { return 0 }
        return min(value / Double(target), 1)
    }

    var body: some View {
        VStack(spacing: 7) {
            HStack {
                Text(label)
                    .font(.system(size: 15, weight: .bold))
                    .foregroundStyle(Theme.ink)
                Spacer()
                Text("\(Int(value.rounded()))g / \(target)g")
                    .font(.system(size: 14, weight: .medium))
                    .foregroundStyle(Theme.inkFaint)
            }

            GeometryReader { proxy in
                ZStack(alignment: .leading) {
                    Capsule().fill(tint.opacity(0.18))
                    Capsule()
                        .fill(tint)
                        .frame(width: max(proxy.size.width * fraction, fraction > 0 ? 8 : 0))
                }
            }
            .frame(height: 8)
        }
    }
}

private struct SummaryChip: View {
    let value: String
    let label: String
    let tint: Color

    var body: some View {
        VStack(spacing: 2) {
            Text(value)
                .font(.metric(18, .bold))
                .foregroundStyle(Theme.ink)
                .minimumScaleFactor(0.7)
                .lineLimit(1)
            Text(label)
                .font(.system(size: 12, weight: .medium))
                .foregroundStyle(Theme.inkSoft)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 11)
        .background(tint.opacity(0.12), in: RoundedRectangle(cornerRadius: 14, style: .continuous))
    }
}
