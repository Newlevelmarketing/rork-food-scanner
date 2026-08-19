import SwiftUI

/// Central design tokens for ModernBodyFoodScanner: colors, gradients, radii and shadows.
enum Theme {
    // Core neutrals
    static let ink = Color(hex: 0x0B0B0C)
    static let inkSoft = Color(hex: 0x6B6B72)
    static let inkFaint = Color(hex: 0xA8A8AF)
    static let hairline = Color(hex: 0x000000).opacity(0.06)

    // Surfaces
    static let card = Color.white.opacity(0.72)
    static let cardSolid = Color.white
    static let well = Color(hex: 0xF2F2F5)

    // Accents
    static let flame = Color(hex: 0xFF6B2C)
    static let water = Color(hex: 0x39A0FF)
    static let protein = Color(hex: 0xFF5A6E)
    static let carbs = Color(hex: 0x4C8DFF)
    static let fat = Color(hex: 0xF5A524)
    static let mint = Color(hex: 0x2FBF71)
    static let plum = Color(hex: 0xB06AF0)

    /// Ambient app background — a soft lavender / peach mist over off-white.
    ///
    /// Radii are expressed as a fraction of the container's diagonal so the mist
    /// covers the full screen on every device instead of fading into a flat grey
    /// slab below the fold.
    static var backdrop: some View {
        GeometryReader { proxy in
            let diagonal = sqrt(proxy.size.width * proxy.size.width + proxy.size.height * proxy.size.height)

            ZStack {
                Color(hex: 0xF6F5F8)

                mist(0xE6DCF7, 0.95, x: 0.02, y: -0.02, radius: diagonal * 0.62)
                mist(0xFFE2E0, 0.90, x: 1.02, y: 0.04, radius: diagonal * 0.58)
                mist(0xDCE9FF, 0.75, x: -0.14, y: 0.46, radius: diagonal * 0.55)
                mist(0xFFE9DC, 0.65, x: 1.14, y: 0.60, radius: diagonal * 0.55)
                mist(0xE4DCF5, 0.70, x: 0.32, y: 1.04, radius: diagonal * 0.62)
                mist(0xDFF0E8, 0.45, x: 0.96, y: 0.98, radius: diagonal * 0.45)
            }
        }
        .ignoresSafeArea()
    }

    private static func mist(
        _ hex: UInt32,
        _ opacity: Double,
        x: CGFloat,
        y: CGFloat,
        radius: CGFloat
    ) -> some View {
        RadialGradient(
            colors: [Color(hex: hex).opacity(opacity), Color(hex: hex).opacity(0)],
            center: .init(x: x, y: y),
            startRadius: 0,
            endRadius: radius
        )
    }
}

extension Color {
    init(hex: UInt32, alpha: Double = 1) {
        self.init(
            .sRGB,
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255,
            opacity: alpha
        )
    }
}

extension Font {
    /// Rounded numeric display face used for all big metrics.
    static func metric(_ size: CGFloat, _ weight: Font.Weight = .bold) -> Font {
        .system(size: size, weight: weight, design: .rounded)
    }
}

// MARK: - Card container

struct CardBackground: ViewModifier {
    var radius: CGFloat = 24
    var padding: CGFloat = 18

    func body(content: Content) -> some View {
        content
            .padding(padding)
            .background {
                RoundedRectangle(cornerRadius: radius, style: .continuous)
                    .fill(.white.opacity(0.78))
                    .background {
                        RoundedRectangle(cornerRadius: radius, style: .continuous)
                            .fill(.ultraThinMaterial)
                    }
                    .overlay {
                        RoundedRectangle(cornerRadius: radius, style: .continuous)
                            .strokeBorder(Color.white.opacity(0.65), lineWidth: 1)
                    }
                    .shadow(color: .black.opacity(0.05), radius: 18, x: 0, y: 8)
            }
            .clipShape(.rect(cornerRadius: radius, style: .continuous))
    }
}

extension View {
    func cardStyle(radius: CGFloat = 24, padding: CGFloat = 18) -> some View {
        modifier(CardBackground(radius: radius, padding: padding))
    }

    /// Applies a subtle press-down scale to any tappable surface.
    func pressable() -> some View {
        buttonStyle(PressableButtonStyle())
    }
}

struct PressableButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.955 : 1)
            .opacity(configuration.isPressed ? 0.9 : 1)
            .animation(.spring(response: 0.28, dampingFraction: 0.7), value: configuration.isPressed)
    }
}
