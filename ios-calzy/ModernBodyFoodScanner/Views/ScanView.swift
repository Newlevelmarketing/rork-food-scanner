import PhotosUI
import SwiftUI

/// Full-screen meal scanner: live camera, library import and AI analysis.
struct ScanView: View {
    @Environment(AppStore.self) private var store
    @Environment(\.dismiss) private var dismiss

    @State private var camera = CameraService()
    @State private var pickerItem: PhotosPickerItem?
    @State private var stagedImage: UIImage?
    @State private var isAnalyzing: Bool = false
    @State private var result: AnalysisResult?
    @State private var errorMessage: String?
    @State private var scanLine: CGFloat = 0

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()

            if camera.status == .running {
                CameraPreview(session: camera.session)
                    .ignoresSafeArea()
            } else {
                placeholder
            }

            if let stagedImage {
                Image(uiImage: stagedImage)
                    .resizable()
                    .aspectRatio(contentMode: .fill)
                    .ignoresSafeArea()
                    .overlay(Color.black.opacity(0.35).ignoresSafeArea())
                    .transition(.opacity)
            }

            frameOverlay
            controls

            if isAnalyzing {
                analyzingOverlay
                    .transition(.opacity)
            }
        }
        .statusBarHidden(true)
        .task { await camera.start() }
        .onDisappear { camera.stop() }
        .onChange(of: camera.capturedImage) { _, image in
            guard let image else { return }
            handle(image)
        }
        .onChange(of: pickerItem) { _, item in
            guard let item else { return }
            Task {
                if let data = try? await item.loadTransferable(type: Data.self),
                   let image = UIImage(data: data) {
                    handle(image)
                }
            }
        }
        .fullScreenCover(item: $result) { value in
            MealResultView(image: stagedImage, result: value, source: .photo)
                .onDisappear { dismiss() }
        }
        .alert("Scan failed", isPresented: Binding(
            get: { errorMessage != nil },
            set: { if !$0 { errorMessage = nil } }
        )) {
            Button("Try again") {
                errorMessage = nil
                withAnimation { stagedImage = nil }
                // handle() stops the session, so retrying has to restart it or the
                // preview stays dead. Clearing the two sources also matters: they
                // are watched with onChange, so re-picking the same photo or
                // re-capturing an identical frame would otherwise not fire.
                camera.capturedImage = nil
                pickerItem = nil
                Task { await camera.start() }
            }
            Button("Close", role: .cancel) { dismiss() }
        } message: {
            Text(errorMessage ?? "")
        }
    }

    // MARK: - Layers

    private var placeholder: some View {
        VStack(spacing: 14) {
            // .unavailable is terminal, not transient. Showing "Preparing camera…"
            // for it leaves the user waiting forever for something that is never
            // going to happen - on the Simulator, and on any device whose camera
            // cannot be configured.
            Image(systemName: placeholderIcon)
                .font(.system(size: 42, weight: .light))
                .foregroundStyle(.white.opacity(0.65))
            Text(placeholderTitle)
                .font(.system(size: 17, weight: .semibold))
                .foregroundStyle(.white)
            if camera.status == .unavailable {
                Text("You can still pick a photo from your library, or add a meal by searching the food database.")
                    .font(.system(size: 14))
                    .foregroundStyle(.white.opacity(0.7))
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 44)
            }
            if camera.status == .denied {
                Text("Enable camera access in Settings to scan meals, or pick a photo from your library.")
                    .font(.system(size: 14))
                    .foregroundStyle(.white.opacity(0.7))
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 44)
                Button("Open Settings") {
                    if let url = URL(string: UIApplication.openSettingsURLString) {
                        UIApplication.shared.open(url)
                    }
                }
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(.black)
                .padding(.horizontal, 20)
                .padding(.vertical, 11)
                .background(.white, in: Capsule())
            }
        }
    }

    private var placeholderIcon: String {
        switch camera.status {
        case .denied: "lock.slash"
        case .unavailable: "camera.fill"
        default: "camera.metering.unknown"
        }
    }

    private var placeholderTitle: String {
        switch camera.status {
        case .denied: "Camera access is off"
        case .unavailable: "Camera unavailable"
        default: "Preparing camera…"
        }
    }

    private var frameOverlay: some View {
        GeometryReader { geo in
            let size = min(geo.size.width - 56, 320)
            ZStack {
                RoundedRectangle(cornerRadius: 32, style: .continuous)
                    .strokeBorder(.white.opacity(0.85), lineWidth: 2.5)
                    .frame(width: size, height: size)
                    .overlay(alignment: .top) {
                        if isAnalyzing {
                            LinearGradient(
                                colors: [.clear, Theme.flame.opacity(0.85), .clear],
                                startPoint: .top,
                                endPoint: .bottom
                            )
                            .frame(height: 60)
                            .offset(y: scanLine)
                            .mask {
                                RoundedRectangle(cornerRadius: 32, style: .continuous)
                                    .frame(width: size, height: size)
                            }
                        }
                    }
            }
            .frame(width: geo.size.width, height: geo.size.height)
        }
        .allowsHitTesting(false)
    }

    private var controls: some View {
        VStack {
            HStack {
                Button {
                    Haptics.tap()
                    dismiss()
                } label: {
                    Image(systemName: "xmark")
                        .font(.system(size: 16, weight: .bold))
                        .foregroundStyle(.white)
                        .frame(width: 40, height: 40)
                        .background(.black.opacity(0.35), in: Circle())
                }
                Spacer()
                Text("Point at your meal")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(.white)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 8)
                    .background(.black.opacity(0.35), in: Capsule())
                Spacer()
                Color.clear.frame(width: 40, height: 40)
            }
            .padding(.horizontal, 20)
            .padding(.top, 12)

            Spacer()

            HStack(spacing: 34) {
                PhotosPicker(selection: $pickerItem, matching: .images) {
                    Image(systemName: "photo.on.rectangle")
                        .font(.system(size: 20, weight: .semibold))
                        .foregroundStyle(.white)
                        .frame(width: 52, height: 52)
                        .background(.white.opacity(0.16), in: Circle())
                }
                .accessibilityLabel("Choose a meal photo from your library")
                .accessibilityHint("Opens your photo library. The selected photo is analyzed for calories and macros.")

                Button {
                    Haptics.rigid()
                    camera.capture()
                } label: {
                    ZStack {
                        Circle().stroke(.white, lineWidth: 4).frame(width: 78, height: 78)
                        Circle().fill(.white).frame(width: 64, height: 64)
                        Image(systemName: "sparkles")
                            .font(.system(size: 22, weight: .bold))
                            .foregroundStyle(.black)
                    }
                }
                .buttonStyle(PressableButtonStyle())
                .disabled(camera.status != .running || isAnalyzing)
                .opacity(camera.status == .running ? 1 : 0.4)
                .accessibilityLabel("Scan meal")
                .accessibilityHint("Takes a photo of your meal and analyzes its calories and macros.")

                Color.clear.frame(width: 52, height: 52)
            }
            .padding(.bottom, 44)
        }
    }

    private var analyzingOverlay: some View {
        ZStack {
            Color.black.opacity(0.55).ignoresSafeArea()
            VStack(spacing: 16) {
                ZStack {
                    Circle()
                        .stroke(.white.opacity(0.18), lineWidth: 4)
                        .frame(width: 68, height: 68)
                    Circle()
                        .trim(from: 0, to: 0.28)
                        .stroke(Theme.flame, style: StrokeStyle(lineWidth: 4, lineCap: .round))
                        .frame(width: 68, height: 68)
                        .rotationEffect(.degrees(scanLine * 6))
                    Image(systemName: "fork.knife")
                        .font(.system(size: 22, weight: .semibold))
                        .foregroundStyle(.white)
                }
                Text("Reading your plate…")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundStyle(.white)
                Text("Identifying ingredients and portion sizes")
                    .font(.system(size: 13))
                    .foregroundStyle(.white.opacity(0.7))
            }
        }
    }

    // MARK: - Actions

    private func handle(_ image: UIImage) {
        withAnimation(.easeOut(duration: 0.2)) {
            stagedImage = image
            isAnalyzing = true
        }
        camera.stop()
        withAnimation(.linear(duration: 1.2).repeatForever(autoreverses: true)) {
            scanLine = 260
        }

        Task {
            do {
                let analysis = try await NutritionAI.analyze(
                    image: image,
                    jesterMode: store.profile.jesterMode,
                    language: store.language.englishName
                )
                await MainActor.run {
                    Haptics.success()
                    isAnalyzing = false
                    result = analysis
                }
            } catch {
                await MainActor.run {
                    Haptics.warning()
                    isAnalyzing = false
                    errorMessage = NutritionAIError.from(error).errorDescription
                        ?? "Something went wrong. Please try again."
                }
            }
        }
    }
}

extension AnalysisResult: Identifiable {
    nonisolated public var id: String { title + "\(items.count)" }
}
