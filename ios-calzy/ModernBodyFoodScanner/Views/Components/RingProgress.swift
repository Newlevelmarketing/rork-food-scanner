import SwiftUI

/// Animated circular progress ring used for calories, water and macros.
struct RingProgress<Content: View>: View {
    let progress: Double
    var lineWidth: CGFloat = 12
    var tint: LinearGradient
    var trackColor: Color = Color.black.opacity(0.06)
    @ViewBuilder var content: () -> Content

    @State private var animated: Double = 0

    var body: some View {
        ZStack {
            Circle()
                .stroke(trackColor, style: StrokeStyle(lineWidth: lineWidth, lineCap: .round))

            Circle()
                .trim(from: 0, to: min(max(animated, 0), 1))
                .stroke(tint, style: StrokeStyle(lineWidth: lineWidth, lineCap: .round))
                .rotationEffect(.degrees(-90))

            if animated > 1 {
                Circle()
                    .trim(from: 0, to: min(animated - 1, 1))
                    .stroke(
                        LinearGradient(colors: [Theme.flame, Theme.protein], startPoint: .top, endPoint: .bottom),
                        style: StrokeStyle(lineWidth: lineWidth * 0.45, lineCap: .round)
                    )
                    .rotationEffect(.degrees(-90))
                    .padding(lineWidth * 0.9)
            }

            content()
        }
        .onAppear {
            withAnimation(.spring(response: 0.9, dampingFraction: 0.82).delay(0.05)) {
                animated = progress
            }
        }
        .onChange(of: progress) { _, newValue in
            withAnimation(.spring(response: 0.6, dampingFraction: 0.8)) {
                animated = newValue
            }
        }
    }
}

/// A small linear bar with animated fill, used in macro breakdowns.
struct MacroBar: View {
    let progress: Double
    let color: Color
    var height: CGFloat = 6

    @State private var animated: Double = 0

    var body: some View {
        GeometryReader { geo in
            ZStack(alignment: .leading) {
                Capsule().fill(Color.black.opacity(0.06))
                Capsule()
                    .fill(
                        LinearGradient(
                            colors: [color.opacity(0.75), color],
                            startPoint: .leading,
                            endPoint: .trailing
                        )
                    )
                    .frame(width: geo.size.width * min(max(animated, 0), 1))
            }
        }
        .frame(height: height)
        .onAppear {
            withAnimation(.spring(response: 0.8, dampingFraction: 0.85)) { animated = progress }
        }
        .onChange(of: progress) { _, newValue in
            withAnimation(.spring(response: 0.5, dampingFraction: 0.85)) { animated = newValue }
        }
    }
}

/// Number that rolls up when its value changes.
struct AnimatedNumber: View {
    let value: Int
    var font: Font = .metric(34, .bold)
    var color: Color = Theme.ink

    var body: some View {
        Text("\(value)")
            .font(font)
            .foregroundStyle(color)
            .contentTransition(.numericText(value: Double(value)))
            .animation(.spring(response: 0.45, dampingFraction: 0.85), value: value)
            .monospacedDigit()
    }
}
