import SwiftUI

nonisolated enum AppTab: String, CaseIterable, Identifiable {
    case home, progress, settings
    var id: String { rawValue }

    var labelKey: String {
        switch self {
        case .home: "tab.home"
        case .progress: "tab.progress"
        case .settings: "tab.settings"
        }
    }

    var symbol: String {
        switch self {
        case .home: "house.fill"
        case .progress: "chart.line.uptrend.xyaxis"
        case .settings: "gearshape.fill"
        }
    }
}

/// Root shell: onboarding gate, tab content and the floating pill tab bar.
struct ContentView: View {
    @Environment(AppStore.self) private var store

    @State private var tab: AppTab = .home
    @State private var route: HomeRoute?

    var body: some View {
        Group {
            if store.profile.hasOnboarded {
                main
            } else {
                OnboardingView()
                    .transition(.opacity.combined(with: .scale(scale: 1.02)))
            }
        }
        .animation(.spring(response: 0.5, dampingFraction: 0.9), value: store.profile.hasOnboarded)
        .environment(\.layoutDirection, Localization.shared.layoutDirection)
    }

    private var main: some View {
        ZStack(alignment: .bottom) {
            Theme.backdrop

            Group {
                switch tab {
                case .home:
                    HomeView(route: $route)
                case .progress:
                    ProgressDashboardView()
                case .settings:
                    SettingsView()
                }
            }
            .transition(.opacity)

            tabBar
        }
        .ignoresSafeArea(.keyboard)
        .fullScreenCover(item: $route) { destination in
            switch destination {
            case .scan: ScanView()
            case .describe: DescribeMealView()
            case .search: SearchFoodView()
            case .saved: SavedFoodsView()
            case .exercise: ExerciseView()
            }
        }
    }

    private var tabBar: some View {
        HStack(spacing: 4) {
            ForEach(AppTab.allCases) { item in
                Button {
                    Haptics.tap()
                    withAnimation(.spring(response: 0.35, dampingFraction: 0.85)) { tab = item }
                } label: {
                    VStack(spacing: 4) {
                        Image(systemName: item.symbol)
                            .font(.system(size: 19, weight: .semibold))
                        Text(L(item.labelKey))
                            .font(.system(size: 11, weight: .semibold))
                            .lineLimit(1)
                            .minimumScaleFactor(0.75)
                    }
                    .foregroundStyle(tab == item ? Theme.ink : Theme.inkFaint)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 11)
                    .background {
                        if tab == item {
                            Capsule()
                                .fill(Theme.ink.opacity(0.07))
                                .matchedGeometryEffect(id: "tab", in: tabNamespace)
                        }
                    }
                }
                .buttonStyle(PressableButtonStyle())
            }
        }
        .padding(6)
        .background {
            Capsule()
                .fill(.white.opacity(0.82))
                .background {
                    Capsule().fill(.ultraThinMaterial)
                }
                .overlay {
                    Capsule().strokeBorder(.white.opacity(0.7), lineWidth: 1)
                }
                .shadow(color: .black.opacity(0.1), radius: 20, y: 8)
        }
        .clipShape(Capsule())
        .padding(.horizontal, 46)
        .padding(.bottom, 6)
    }

    @Namespace private var tabNamespace
}

#Preview {
    ContentView()
        .environment(AppStore())
}
