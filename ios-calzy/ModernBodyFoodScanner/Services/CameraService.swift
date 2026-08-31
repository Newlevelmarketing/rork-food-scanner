import AVFoundation
import SwiftUI
import UIKit

nonisolated enum CameraStatus: Equatable {
    case idle
    case running
    case denied
    case unavailable
}

/// Thin AVFoundation wrapper that powers the meal scanner.
@Observable
final class CameraService: NSObject {
    var status: CameraStatus = .idle
    var capturedImage: UIImage?

    let session = AVCaptureSession()
    private let output = AVCapturePhotoOutput()
    private let queue = DispatchQueue(label: "com.calzy.camera.session")
    private var isConfigured = false

    func start() async {
        let granted = await requestAccess()
        guard granted else {
            status = .denied
            return
        }
        if !isConfigured {
            let ok = configure()
            guard ok else {
                status = .unavailable
                return
            }
            isConfigured = true
        }
        status = .running
        queue.async { [session] in
            if !session.isRunning { session.startRunning() }
        }
    }

    func stop() {
        // Drop out of .running as well as stopping the session. Without this the
        // view still believes the camera is live and keeps rendering a preview
        // layer over a stopped session, which shows the last frame frozen.
        if status == .running { status = .idle }
        queue.async { [session] in
            if session.isRunning { session.stopRunning() }
        }
    }

    private func requestAccess() async -> Bool {
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized: return true
        case .notDetermined: return await AVCaptureDevice.requestAccess(for: .video)
        default: return false
        }
    }

    private func configure() -> Bool {
        session.beginConfiguration()
        session.sessionPreset = .photo

        let discovery = AVCaptureDevice.DiscoverySession(
            deviceTypes: [.builtInWideAngleCamera, .builtInDualCamera, .external],
            mediaType: .video,
            position: .unspecified
        )
        guard
            let device = discovery.devices.first(where: { $0.position == .back }) ?? discovery.devices.first,
            let input = try? AVCaptureDeviceInput(device: device),
            session.canAddInput(input)
        else {
            session.commitConfiguration()
            return false
        }
        session.addInput(input)

        guard session.canAddOutput(output) else {
            session.commitConfiguration()
            return false
        }
        session.addOutput(output)
        session.commitConfiguration()
        return true
    }

    func capture() {
        guard status == .running else { return }
        let settings = AVCapturePhotoSettings()
        output.capturePhoto(with: settings, delegate: self)
    }
}

extension CameraService: AVCapturePhotoCaptureDelegate {
    nonisolated func photoOutput(
        _ output: AVCapturePhotoOutput,
        didFinishProcessingPhoto photo: AVCapturePhoto,
        error: Error?
    ) {
        guard error == nil, let data = photo.fileDataRepresentation(),
              let image = UIImage(data: data) else { return }
        Task { @MainActor in
            self.capturedImage = image
        }
    }
}

/// UIKit preview layer bridged into SwiftUI.
struct CameraPreview: UIViewRepresentable {
    let session: AVCaptureSession

    func makeUIView(context: Context) -> PreviewView {
        let view = PreviewView()
        view.videoPreviewLayer.session = session
        view.videoPreviewLayer.videoGravity = .resizeAspectFill
        return view
    }

    func updateUIView(_ uiView: PreviewView, context: Context) {}

    final class PreviewView: UIView {
        override class var layerClass: AnyClass { AVCaptureVideoPreviewLayer.self }
        var videoPreviewLayer: AVCaptureVideoPreviewLayer {
            layer as! AVCaptureVideoPreviewLayer
        }
    }
}
