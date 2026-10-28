import Shared
import SwiftUI
import UIKit

@main
struct TrackMeCaloriesApp: App {
    var body: some Scene {
        WindowGroup {
            // Compose draws edge to edge and pads itself for the bars and the keyboard.
            ComposeView().ignoresSafeArea()
        }
    }
}

/** The shared Compose UI (shared/src/iosMain/.../IosApp.kt). */
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        IosAppKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
