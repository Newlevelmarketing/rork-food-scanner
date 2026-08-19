import SwiftUI

/// The macro tile shown in the home carousel (protein / carbs / fat).
struct MacroTile: View {
    let title: String
    let emoji: String
    let eaten: Double
    let goal: Int
    let tint: Color

    private var progress: Double {
        goal > 0 ? min(eaten / Double(goal), 1) : 0
    }

    var body: some View {
        VStack(spacing: 10) {
            ZStack {
                Circle()
                    .stroke(tint.opacity(0.16), lineWidth: 5)
                Circle()
                    .trim(from: 0, to: progress)
                    .stroke(tint, style: StrokeStyle(lineWidth: 5, lineCap: .round))
                    .rotationEffect(.degrees(-90))
                    .animation(.spring(response: 0.7, dampingFraction: 0.85), value: progress)
                Text(emoji).font(.system(size: 26))
            }
            .frame(width: 62, height: 62)

            Text(title)
                .font(.system(size: 13, weight: .medium))
                .foregroundStyle(Theme.inkSoft)

            HStack(alignment: .firstTextBaseline, spacing: 2) {
                Text("\(Int(eaten.rounded()))")
                    .font(.metric(21, .bold))
                    .foregroundStyle(Theme.ink)
                    .contentTransition(.numericText(value: eaten))
                Text("/\(goal)g")
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(Theme.inkFaint)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 16)
        .cardStyle(radius: 22, padding: 0)
    }
}

/// Small stat tile used on the Progress screen.
struct StatTile<Trailing: View>: View {
    let icon: String
    let iconColor: Color
    let title: String
    let value: String
    let unit: String
    @ViewBuilder var trailing: () -> Trailing

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 7) {
                Image(systemName: icon)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(iconColor)
                Text(title)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(Theme.inkSoft)
                Spacer(minLength: 0)
            }

            HStack(alignment: .firstTextBaseline, spacing: 4) {
                Text(value)
                    .font(.metric(30, .bold))
                    .foregroundStyle(Theme.ink)
                Text(unit)
                    .font(.system(size: 14, weight: .medium))
                    .foregroundStyle(Theme.inkFaint)
            }

            trailing()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .cardStyle(radius: 22, padding: 16)
    }
}

/// Section header with an icon and optional trailing action.
struct SectionHeader<Trailing: View>: View {
    let icon: String
    let title: String
    @ViewBuilder var trailing: () -> Trailing

    var body: some View {
        HStack(spacing: 8) {
            Image(systemName: icon)
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(Theme.inkSoft)
            Text(title)
                .font(.system(size: 19, weight: .bold))
                .foregroundStyle(Theme.ink)
            Spacer(minLength: 0)
            trailing()
        }
    }
}

/// A single entry in the home quick-action strip.
struct QuickAction: Identifiable {
    let id: String
    let icon: String
    let title: String
    let tint: Color
    let action: () -> Void

    init(icon: String, title: String, tint: Color, action: @escaping () -> Void) {
        self.id = title
        self.icon = icon
        self.title = title
        self.tint = tint
        self.action = action
    }
}

/// Compact control strip for the home quick actions.
///
/// The five actions share one card and are separated by hairlines so the row
/// reads as a single deliberate control rather than five competing tiles.
struct QuickActionBar: View {
    let actions: [QuickAction]

    var body: some View {
        HStack(spacing: 0) {
            ForEach(Array(actions.enumerated()), id: \.element.id) { index, action in
                if index > 0 {
                    Rectangle()
                        .fill(Theme.hairline)
                        .frame(width: 1, height: 24)
                }
                QuickActionCell(action: action)
            }
        }
        .cardStyle(radius: 22, padding: 0)
    }
}

private struct QuickActionCell: View {
    let action: QuickAction

    var body: some View {
        Button {
            Haptics.tap()
            action.action()
        } label: {
            VStack(spacing: 5) {
                Image(systemName: action.icon)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(action.tint)
                    .frame(height: 17)
                Text(action.title)
                    .font(.system(size: 10, weight: .semibold))
                    .kerning(0.1)
                    .foregroundStyle(Theme.inkSoft)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 11)
            .contentShape(.rect)
        }
        .buttonStyle(QuickActionCellStyle(tint: action.tint))
    }
}

/// Presses tint and settle the individual cell instead of the whole strip.
private struct QuickActionCellStyle: ButtonStyle {
    let tint: Color

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .background(tint.opacity(configuration.isPressed ? 0.09 : 0))
            .scaleEffect(configuration.isPressed ? 0.93 : 1)
            .animation(.spring(response: 0.26, dampingFraction: 0.72), value: configuration.isPressed)
    }
}
